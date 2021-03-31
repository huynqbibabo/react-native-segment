package analytics.integrations

import analytics.Analytics
import android.util.Log

/** An abstraction for logging messages.  */
open class Logger(private val tag: String, private val logLevel: Analytics.LogLevel) {
  /** Log a verbose message.  */
  fun verbose(format: String?, vararg extra: Any?) {
    if (shouldLog(Analytics.LogLevel.VERBOSE)) {
      Log.v(tag, String.format(format!!, *extra))
    }
  }

  /** Log an info message.  */
  fun info(format: String?, vararg extra: Any?) {
    if (shouldLog(Analytics.LogLevel.INFO)) {
      Log.i(tag, String.format(format!!, *extra))
    }
  }

  /** Log a debug message.  */
  fun debug(format: String?, vararg extra: Any?) {
    if (shouldLog(Analytics.LogLevel.DEBUG)) {
      Log.d(tag, String.format(format!!, *extra))
    }
  }

  /** Log an error message.  */
  fun error(error: Throwable?, format: String?, vararg extra: Any?) {
    if (shouldLog(Analytics.LogLevel.INFO)) {
      Log.e(tag, String.format(format!!, *extra), error)
    }
  }

  /**
   * Returns a new [Logger] with the same `level` as this one and the given `tag`.
   */
  fun subLog(tag: String): Logger {
    return Logger("$DEFAULT_TAG-$tag", logLevel)
  }

  private fun shouldLog(level: Analytics.LogLevel): Boolean {
    return logLevel.ordinal >= level.ordinal
  }

  companion object {
    private const val DEFAULT_TAG = "Analytics"

    /** Returns a new [Logger] with the give `level`.  */
    @JvmStatic
    fun with(level: Analytics.LogLevel): Logger {
      return Logger(DEFAULT_TAG, level)
    }
  }
}
