package analytics

import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Options let you control behaviour for a specific analytics action, including setting a custom
 * timestamp and disabling integrations on demand.
 */
class Options {
  private val integrations // passed in by the user
    : MutableMap<String, Any>
  private val context: MutableMap<String, Any>

  constructor() {
    integrations = ConcurrentHashMap()
    context = ConcurrentHashMap()
  }

  constructor(integrations: MutableMap<String, Any>, context: MutableMap<String, Any>) {
    this.integrations = integrations
    this.context = context
  }

  /**
   * Sets whether an action will be sent to the target integration.
   *
   *
   * By default, all integrations are sent a payload, and the value for the [ ][.ALL_INTEGRATIONS_KEY] is `true`. You can disable specific payloads.
   *
   *
   * Example: `options.setIntegration("Google Analytics", false).setIntegration("Countly",
   * false)` will send the event to ALL integrations, except Google Analytic and Countly.
   *
   *
   * If you want to enable only specific integrations, first override the defaults and then
   * enable specific integrations.
   *
   *
   * Example: `options.setIntegration(Options.ALL_INTEGRATIONS_KEY,
   * false).setIntegration("Countly", true).setIntegration("Google Analytics", true)` will
   * only send events to ONLY Countly and Google Analytics.
   *
   * @param integrationKey The integration key
   * @param enabled `true` for enabled, `false` for disabled
   * @return This options object for chaining
   */
  fun setIntegration(integrationKey: String, enabled: Boolean): Options {
    require(SegmentIntegration.SEGMENT_KEY != integrationKey) { "Segment integration cannot be enabled or disabled." }
    integrations[integrationKey] = enabled
    return this
  }

  /**
   * Attach some integration specific options for this call.
   *
   * @param integrationKey The target integration key
   * @param options A map of data that will be used by the integration
   * @return This options object for chaining
   */
  fun setIntegrationOptions(integrationKey: String, options: Map<String?, Any?>): Options {
    integrations[integrationKey] = options
    return this
  }

  /**
   * Attach some additional context information. Unlike with [ ][analytics.Analytics.getAnalyticsContext], this only has effect for this call.
   *
   * @param key The key of the extra context data
   * @param value The value of the extra context data
   * @return This options object for chaining
   */
  fun putContext(key: String, value: Any): Options {
    context[key] = value
    return this
  }

  /** Returns a copy of settings for integrations.  */
  fun integrations(): Map<String, Any> {
    return LinkedHashMap(integrations)
  }

  /** Returns a copy of the context.  */
  fun context(): Map<String, Any> {
    return LinkedHashMap(context)
  }

  companion object {
    /**
     * A special key, whose value which is respected for all integrations, a "default" value, unless
     * explicitly overridden. See the documentation for [.setIntegration] on
     * how to use this key.
     */
    const val ALL_INTEGRATIONS_KEY = "All"
  }
}
