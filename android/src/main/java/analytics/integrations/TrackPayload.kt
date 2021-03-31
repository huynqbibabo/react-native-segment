package analytics.integrations

import analytics.Properties
import analytics.internal.Private
import analytics.internal.Utils
import java.util.*

class TrackPayload @Private internal constructor(
  messageId: String,
  timestamp: Date,
  context: Map<String?, Any?>,
  integrations: Map<String, Any>,
  userId: String?,
  anonymousId: String,
  event: String,
  properties: Map<String?, Any?>,
  nanosecondTimestamps: Boolean
) : BasePayload(
  Type.track,
  messageId,
  timestamp,
  context,
  integrations,
  userId,
  anonymousId,
  nanosecondTimestamps) {
  /**
   * The name of the event. We recommend using title case and past tense for event names, like
   * "Signed Up".
   */
  fun event(): String {
    return getString(EVENT_KEY)
  }

  /**
   * A dictionary of properties that give more information about the event. We have a collection
   * of special properties that we recognize with semantic meaning. You can also add your own
   * custom properties.
   */
  fun properties(): Properties {
    return getValueMap(PROPERTIES_KEY, Properties::class.java)
  }

  override fun toString(): String {
    return "TrackPayload{event=\"" + event() + "\"}"
  }

  override fun toBuilder(): Builder {
    return Builder(this)
  }

  /** Fluent API for creating [TrackPayload] instances.  */
  class Builder : BasePayload.Builder<TrackPayload?, Builder?> {
    private var event: String? = null
    private var properties: Map<String?, Any?>? = null

    constructor() {
      // Empty constructor.
    }

    @Private
    internal constructor(track: TrackPayload) : super(track) {
      event = track.event()
      properties = track.properties()
    }

    fun event(event: String): Builder {
      this.event = Utils.assertNotNullOrEmpty(event, "event")
      return this
    }

    fun properties(properties: Map<String?, *>): Builder {
      Utils.assertNotNull(properties, "properties")
      this.properties = Collections.unmodifiableMap(LinkedHashMap(properties))
      return this
    }

    override fun realBuild(
      messageId: String,
      timestamp: Date,
      context: Map<String?, Any?>,
      integrations: Map<String, Any>,
      userId: String?,
      anonymousId: String,
      nanosecondTimestamps: Boolean
    ): TrackPayload {
      Utils.assertNotNullOrEmpty(event, "event")
      var properties = properties
      if (Utils.isNullOrEmpty(properties)) {
        properties = emptyMap<String?, Any>()
      }
      return TrackPayload(
        messageId,
        timestamp,
        context,
        integrations,
        userId,
        anonymousId,
        event!!,
        properties!!,
        nanosecondTimestamps)
    }

    override fun self(): Builder {
      return this
    }
  }

  companion object {
    const val EVENT_KEY = "event"
    const val PROPERTIES_KEY = "properties"
  }

  init {
    put(EVENT_KEY, event)
    put(PROPERTIES_KEY, properties)
  }
}
