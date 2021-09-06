package analytics

import analytics.internal.Private
import analytics.internal.Utils
import android.os.*
import android.util.Pair
import java.util.*

class Stats {
  private val statsThread: HandlerThread = HandlerThread(STATS_THREAD_NAME, Process.THREAD_PRIORITY_BACKGROUND)
  private val handler: StatsHandler
  private var flushCount: Long = 0
  private var flushEventCount: Long = 0
  private var integrationOperationCount: Long = 0
  private var integrationOperationDuration: Long = 0
  private var integrationOperationDurationByIntegration: MutableMap<String, Long> = HashMap()
  fun shutdown() {
    statsThread.quit()
  }

  fun dispatchFlush(eventCount: Int) {
    handler.sendMessage(
      handler //
        .obtainMessage(StatsHandler.TRACK_FLUSH, eventCount, 0))
  }

  fun performFlush(eventCount: Int) {
    flushCount++
    flushEventCount += eventCount.toLong()
  }

  fun dispatchIntegrationOperation(key: String, duration: Long) {
    handler.sendMessage(
      handler //
        .obtainMessage(
          StatsHandler.TRACK_INTEGRATION_OPERATION, Pair(key, duration)))
  }

  fun performIntegrationOperation(durationForIntegration: Pair<String, Long>) {
    integrationOperationCount++
    integrationOperationDuration += durationForIntegration.second
    val duration = integrationOperationDurationByIntegration[durationForIntegration.first]
    if (duration == null) {
      integrationOperationDurationByIntegration[durationForIntegration.first] = durationForIntegration.second
    } else {
      integrationOperationDurationByIntegration[durationForIntegration.first] = duration + durationForIntegration.second
    }
  }

  fun createSnapshot(): StatsSnapshot {
    return StatsSnapshot(
      System.currentTimeMillis(),
      flushCount,
      flushEventCount,
      integrationOperationCount,
      integrationOperationDuration,
      Collections.unmodifiableMap(integrationOperationDurationByIntegration))
  }

  class StatsHandler internal constructor(looper: Looper?, private val stats: Stats) : Handler(
    looper!!
  ) {
    override fun handleMessage(msg: Message) {
      when (msg.what) {
        TRACK_FLUSH -> stats.performFlush(msg.arg1)
        TRACK_INTEGRATION_OPERATION -> stats.performIntegrationOperation(msg.obj as Pair<String, Long>)
        else -> throw AssertionError("Unknown Stats handler message: $msg")
      }
    }

    companion object {
      @Private
      val TRACK_FLUSH = 1

      @Private
      val TRACK_INTEGRATION_OPERATION = 2
    }
  }

  companion object {
    private const val STATS_THREAD_NAME = Utils.THREAD_PREFIX + "Stats"
  }

  init {
    statsThread.start()
    handler = StatsHandler(statsThread.looper, this)
  }
}
