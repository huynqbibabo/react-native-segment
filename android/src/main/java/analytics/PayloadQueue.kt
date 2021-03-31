package analytics

import java.io.ByteArrayInputStream
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.util.*

abstract class PayloadQueue : Closeable {
  abstract fun size(): Int
  @Throws(IOException::class)
  abstract fun remove(n: Int)
  @Throws(IOException::class)
  abstract fun add(data: ByteArray)
  @Throws(IOException::class)
  abstract fun forEach(visitor: ElementVisitor)
  interface ElementVisitor {
    /**
     * Called once per element.
     *
     * @param in stream of element data. Reads as many bytes as requested, unless fewer than the
     * request number of bytes remains, in which case it reads all the remaining bytes. Not
     * buffered.
     * @param length of element data in bytes
     * @return an indication whether the [.forEach] operation should continue; If `true`, continue, otherwise halt.
     */
    @Throws(IOException::class)
    fun read(`in`: InputStream?, length: Int): Boolean
  }

  internal class PersistentQueue(val queueFile: QueueFile) : PayloadQueue() {
    override fun size(): Int {
      return queueFile.size()
    }

    @Throws(IOException::class)
    override fun remove(n: Int) {
      try {
        queueFile.remove(n)
      } catch (e: ArrayIndexOutOfBoundsException) {
        // Guard against ArrayIndexOutOfBoundsException, unfortunately root cause is
        // unknown.
        // Ref: https://github.com/segmentio/analytics-android/issues/449.
        throw IOException(e)
      }
    }

    @Throws(IOException::class)
    override fun add(data: ByteArray) {
      queueFile.add(data)
    }

    @Throws(IOException::class)
    override fun forEach(visitor: ElementVisitor) {
      queueFile.forEach(visitor)
    }

    @Throws(IOException::class)
    override fun close() {
      queueFile.close()
    }
  }

  internal class MemoryQueue : PayloadQueue() {
    private val queue: LinkedList<ByteArray> = LinkedList()
    override fun size(): Int {
      return queue.size
    }

    @Throws(IOException::class)
    override fun remove(n: Int) {
      for (i in 0 until n) {
        queue.remove()
      }
    }

    @Throws(IOException::class)
    override fun add(data: ByteArray) {
      queue.add(data)
    }

    @Throws(IOException::class)
    override fun forEach(visitor: ElementVisitor) {
      for (i in queue.indices) {
        val data = queue[i]
        val shouldContinue = visitor.read(ByteArrayInputStream(data), data.size)
        if (!shouldContinue) {
          return
        }
      }
    }

    @Throws(IOException::class)
    override fun close() {
      // no-op
    }

  }
}
