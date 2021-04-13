package analytics

import analytics.PayloadQueue.MemoryQueue
import analytics.PayloadQueue.PersistentQueue
import analytics.integrations.*
import analytics.internal.Private
import analytics.internal.Utils
import analytics.internal.Utils.AnalyticsThreadFactory
import android.content.Context
import android.os.*
import android.util.JsonWriter
import android.util.Log
import java.io.*
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Entity that queues payloads on disks and uploads them periodically.
 */
internal class SegmentIntegration(
  private val context: Context,
  private val client: Client,
  private val cartographer: Cartographer,
  private val networkExecutor: ExecutorService,
  private val payloadQueue: PayloadQueue,
  private val stats: Stats,
  private val bundledIntegrations: Map<String, Boolean>,
  flushIntervalInMillis: Long,
  private val flushQueueSize: Int,
  private val logger: Logger,
  crypto: Crypto,
  apiHost: String
) : Integration<Void?>() {
  private val handler: Handler
  private val segmentThread: HandlerThread
  private val flushScheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1, AnalyticsThreadFactory())
  private val apiHost: String

  /**
   * We don't want to stop adding payloads to our disk queue when we're uploading payloads. So we
   * upload payloads on a network executor instead.
   *
   *
   * Given: 1. Peek returns the oldest elements 2. Writes append to the tail of the queue 3.
   * Methods on QueueFile are synchronized (so only thread can access it at a time)
   *
   *
   * We offload flushes to the network executor, read the QueueFile and remove entries on it,
   * while we continue to add payloads to the QueueFile on the default Dispatcher thread.
   *
   *
   * We could end up in a case where (assuming MAX_QUEUE_SIZE is 10): 1. Executor reads 10
   * payloads from the QueueFile 2. Dispatcher is told to add an payloads (the 11th) to the queue.
   * 3. Dispatcher sees that the queue size is at it's limit (10). 4. Dispatcher removes an
   * payloads. 5. Dispatcher adds a payload. 6. Executor finishes uploading 10 payloads and
   * proceeds to remove 10 elements from the file. Since the dispatcher already removed the 10th
   * element and added a 11th, this would actually delete the 11th payload that will never get
   * uploaded.
   *
   *
   * This lock is used ensure that the Dispatcher thread doesn't remove payloads when we're
   * uploading.
   */
  @Private
  val flushLock = Any()
  private val crypto: Crypto
  override fun identify(identify: IdentifyPayload?) {
    if (identify != null) {
      dispatchEnqueue(identify)
    }
  }

  override fun group(group: GroupPayload?) {
    if (group != null) {
      dispatchEnqueue(group)
    }
  }

  override fun track(track: TrackPayload?) {
    if (track != null) {
      dispatchEnqueue(track)
    }
  }

  override fun alias(alias: AliasPayload?) {
    if (alias != null) {
      dispatchEnqueue(alias)
    }
  }

  override fun screen(screen: ScreenPayload?) {
    if (screen != null) {
      dispatchEnqueue(screen)
    }
  }

  private fun dispatchEnqueue(payload: BasePayload) {
    handler.sendMessage(
      handler.obtainMessage(SegmentDispatcherHandler.REQUEST_ENQUEUE, payload))
  }

  fun performEnqueue(original: BasePayload) {
    // Override any user provided values with anything that was bundled.
    // e.g. If user did Mixpanel: true and it was bundled, this would correctly override it with
    // false so that the server doesn't send that event as well.
//    val providedIntegrations = original.integrations()
//    val combinedIntegrations = LinkedHashMap<String, Any>(providedIntegrations.size + bundledIntegrations.size)
//    combinedIntegrations.putAll(providedIntegrations)
//    combinedIntegrations.putAll(bundledIntegrations)
//    combinedIntegrations.remove("webhook_bibabo") // don't include the Segment integration.
    // Make a copy of the payload so we don't mutate the original.
    val payload = ValueMap()
    payload.putAll(original)
//    payload["integrations"] = combinedIntegrations
    if (payloadQueue.size() >= MAX_QUEUE_SIZE) {
      synchronized(flushLock) {
        // Double checked locking, the network executor could have removed payload from the
        // queue
        // to bring it below our capacity while we were waiting.
        if (payloadQueue.size() >= MAX_QUEUE_SIZE) {
          logger.info(
            "Queue is at max capacity (%s), removing oldest payload.",
            payloadQueue.size())
          try {
            payloadQueue.remove(1)
          } catch (e: IOException) {
            logger.error(e, "Unable to remove oldest payload from queue.")
            return
          }
        }
      }
    }
    try {
      val bos = ByteArrayOutputStream()
      val cos = crypto.encrypt(bos)
      cartographer.toJson(payload, OutputStreamWriter(cos))
      val bytes = bos.toByteArray()
      if (bytes.isEmpty() || (bytes.size > MAX_PAYLOAD_SIZE)) {
        throw IOException("Could not serialize payload $payload")
      }
      payloadQueue.add(bytes)
    } catch (e: IOException) {
      logger.error(e, "Could not add payload %s to queue: %s.", payload, payloadQueue)
      return
    }
    logger.verbose(
      "Enqueued %s payload. %s elements in the queue.", original, payloadQueue.size())
    if (payloadQueue.size() >= flushQueueSize) {
      submitFlush()
    }
  }

  /**
   * Enqueues a flush message to the handler.
   */
  override fun flush() {
    handler.sendMessage(handler.obtainMessage(SegmentDispatcherHandler.REQUEST_FLUSH))
  }

  /**
   * Submits a flush message to the network executor.
   */
  fun submitFlush() {
    if (!shouldFlush()) {
      return
    }
    if (networkExecutor.isShutdown) {
      logger.info(
        "A call to flush() was made after shutdown() has been called.  In-flight events may not be uploaded right away.")
      return
    }
    networkExecutor.submit { synchronized(flushLock) { performFlush() } }
  }

  private fun shouldFlush(): Boolean {
    return payloadQueue.size() > 0 && Utils.isConnected(context)
  }

  /**
   * Upload payloads to our servers and remove them from the queue file.
   */
  private fun performFlush() {
    // Conditions could have changed between enqueuing the task and when it is run.
    if (!shouldFlush()) {
      return
    }
    logger.verbose("Uploading payloads in queue to Segment.")
    var payloadsUploaded = 0
    var connection: Client.Connection? = null
    try {
      // Open a connection.
      connection = client.upload(apiHost)

      // Write the payloads into the OutputStream.
      val writer = BatchPayloadWriter(connection.os) //
        .beginObject() //
        .beginBatchArray()
      val payloadWriter = PayloadWriter(writer, crypto)
      payloadQueue.forEach(payloadWriter)
      writer.endBatchArray().endObject().close()
      // Don't use the result of QueueFiles#forEach, since we may not upload the last element.
      payloadsUploaded = payloadWriter.payloadCount

      // Upload the payloads.
      connection.close()
    } catch (e: Client.HTTPException) {
      if (e.is4xx() && e.responseCode != 429) {
        // Simply log and proceed to remove the rejected payloads from the queue.
        logger.error(e, "Payloads were rejected by server. Marked for removal.")
        try {
          payloadQueue.remove(payloadsUploaded)
        } catch (e1: IOException) {
          logger.error(
            e, "Unable to remove $payloadsUploaded payload(s) from queue.")
        }
      } else {
        logger.error(e, "Error while uploading payloads")
      }
      return
    } catch (e: IOException) {
      logger.error(e, "Error while uploading payloads")
      return
    } finally {
      Utils.closeQuietly(connection)
    }
    try {
      payloadQueue.remove(payloadsUploaded)
    } catch (e: IOException) {
      logger.error(e, "Unable to remove $payloadsUploaded payload(s) from queue.")
      return
    }
    logger.verbose(
      "Uploaded %s payloads. %s remain in the queue.",
      payloadsUploaded, payloadQueue.size())
    stats.dispatchFlush(payloadsUploaded)
    if (payloadQueue.size() > 0) {
      performFlush() // Flush any remaining items.
    }
  }

  fun shutdown() {
    flushScheduler.shutdownNow()
    segmentThread.quit()
    Utils.closeQuietly(payloadQueue)
  }

  internal class PayloadWriter(private val writer: BatchPayloadWriter, private val crypto: Crypto) : PayloadQueue.ElementVisitor {
    var size = 0
    var payloadCount = 0

    @Throws(IOException::class)
    override fun read(`in`: InputStream?, length: Int): Boolean {
      val `is` = crypto.decrypt(`in`)
      val newSize = size + length
      if (newSize > MAX_BATCH_SIZE) {
        return false
      }
      size = newSize
      val data = ByteArray(length)
      `is`.read(data, 0, length)
      // Remove trailing whitespace.
      Log.i("TAG", "read: ${String(data, UTF_8).trim { it <= ' ' }}")
      writer.emitPayloadObject(String(data, UTF_8).trim { it <= ' ' })
      payloadCount++
      return true
    }
  }

  /**
   * A wrapper that emits a JSON formatted batch payload to the underlying writer.
   */
  internal class BatchPayloadWriter(stream: OutputStream?) : Closeable {
    private val jsonWriter: JsonWriter

    /**
     * Keep around for writing payloads as Strings.
     */
    private val bufferedWriter: BufferedWriter = BufferedWriter(OutputStreamWriter(stream))
    private var needsComma = false

    @Throws(IOException::class)
    fun beginObject(): BatchPayloadWriter {
      jsonWriter.beginObject()
      return this
    }

    @Throws(IOException::class)
    fun beginBatchArray(): BatchPayloadWriter {
      jsonWriter.name("batch").beginArray()
      needsComma = false
      return this
    }

    @Throws(IOException::class)
    fun emitPayloadObject(payload: String?): BatchPayloadWriter {
      // Payloads already serialized into json when storing on disk. No need to waste cycles
      // deserializing them.
      if (needsComma) {
        bufferedWriter.write(','.toInt())
      } else {
        needsComma = true
      }
      bufferedWriter.write(payload)
      return this
    }

    @Throws(IOException::class)
    fun endBatchArray(): BatchPayloadWriter {
      if (!needsComma) {
        throw IOException("At least one payload must be provided.")
      }
      jsonWriter.endArray()
      return this
    }

    @Throws(IOException::class)
    fun endObject(): BatchPayloadWriter {
      /**
       * The sent timestamp is an ISO-8601-formatted string that, if present on a message, can
       * be used to correct the original timestamp in situations where the local clock cannot
       * be trusted, for example in our mobile libraries. The sentAt and receivedAt timestamps
       * will be assumed to have occurred at the same time, and therefore the difference is
       * the local clock skew.
       */
      jsonWriter.name("sentAt").value(Utils.toISO8601Date(Date())).endObject()
      return this
    }

    @Throws(IOException::class)
    override fun close() {
      jsonWriter.close()
    }

    init {
      jsonWriter = JsonWriter(bufferedWriter)
    }
  }

  internal class SegmentDispatcherHandler(looper: Looper?, private val segmentIntegration: SegmentIntegration) : Handler(looper) {
    override fun handleMessage(msg: Message) {
      when (msg.what) {
        REQUEST_ENQUEUE -> {
          val payload = msg.obj as BasePayload
          segmentIntegration.performEnqueue(payload)
        }
        REQUEST_FLUSH -> segmentIntegration.submitFlush()
        else -> throw AssertionError("Unknown dispatcher message: " + msg.what)
      }
    }

    companion object {
      const val REQUEST_FLUSH = 1

      @Private
      val REQUEST_ENQUEUE = 0
    }
  }

  companion object {
    @JvmField
    val FACTORY: Factory = object : Factory {
      override fun create(settings: ValueMap?, analytics: Analytics?): Integration<*>? {
        return analytics?.application?.let {
          create(
            it,
            analytics.client,
            analytics.cartographer,
            analytics.networkExecutor,
            analytics.stats,
            Collections.unmodifiableMap(analytics.bundledIntegrations),
            analytics.tag,
            analytics.flushIntervalInMillis,
            analytics.flushQueueSize,
            analytics.logger,
            analytics.crypto,
            settings!!)
        }
      }

      override fun key(): String {
        return SEGMENT_KEY
      }
    }

    /**
     * Drop old payloads if queue contains more than 1000 items. Since each item can be at most
     * 32KB, this bounds the queue size to ~32MB (ignoring headers), which also leaves room for
     * QueueFile's 2GB limit.
     */
    const val MAX_QUEUE_SIZE = 1000

    /**
     * Our servers only accept payloads < 32KB.
     */
    const val MAX_PAYLOAD_SIZE = 32000 // 32KB.

    /**
     * Our servers only accept batches < 500KB. This limit is 475KB to account for extra data that
     * is not present in payloads themselves, but is added later, such as `sentAt`, `integrations` and other json tokens.
     */
    @Private
    val MAX_BATCH_SIZE = 475000 // 475KB.

    @Private
    val UTF_8: Charset = Charset.forName("UTF-8")
    private const val SEGMENT_THREAD_NAME = Utils.THREAD_PREFIX + "SegmentDispatcher"

    const val SEGMENT_KEY = "webhook_bibabo"

    /**
     * Create a [QueueFile] in the given folder with the given name. If the underlying file is
     * somehow corrupted, we'll delete it, and try to recreate the file. This method will throw an
     * [IOException] if the directory doesn't exist and could not be created.
     */
    @Throws(IOException::class)
    fun createQueueFile(folder: File?, name: String?): QueueFile {
      Utils.createDirectory(folder)
      val file = File(folder, name)
      return QueueFile(file)
    }

    @Synchronized
    fun create(
      context: Context,
      client: Client,
      cartographer: Cartographer,
      networkExecutor: ExecutorService,
      stats: Stats,
      bundledIntegrations: Map<String, Boolean>,
      tag: String?,
      flushIntervalInMillis: Long,
      flushQueueSize: Int,
      logger: Logger,
      crypto: Crypto,
      settings: ValueMap
    ): SegmentIntegration {
      val payloadQueue: PayloadQueue = try {
        val folder = context.getDir("segment-disk-queue", Context.MODE_PRIVATE)
        val queueFile = createQueueFile(folder, tag)
        PersistentQueue(queueFile)
      } catch (e: IOException) {
        logger.error(e, "Could not create disk queue. Falling back to memory queue.")
        MemoryQueue()
      }
      val apiHost = settings.getString("apiHost")
      return SegmentIntegration(
        context,
        client,
        cartographer,
        networkExecutor,
        payloadQueue,
        stats,
        bundledIntegrations,
        flushIntervalInMillis,
        flushQueueSize,
        logger,
        crypto,
        apiHost)
    }
  }

  init {
    this.crypto = crypto
    this.apiHost = apiHost
    segmentThread = HandlerThread(SEGMENT_THREAD_NAME, Process.THREAD_PRIORITY_BACKGROUND)
    segmentThread.start()
    handler = SegmentDispatcherHandler(segmentThread.looper, this)
    val initialDelay = if (payloadQueue.size() >= flushQueueSize) 0L else flushIntervalInMillis
    flushScheduler.scheduleAtFixedRate(
      { flush() },
      initialDelay,
      flushIntervalInMillis,
      TimeUnit.MILLISECONDS)
  }
}
