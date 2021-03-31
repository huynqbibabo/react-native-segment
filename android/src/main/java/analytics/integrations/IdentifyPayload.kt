package analytics.integrations

import analytics.Traits
import analytics.internal.Private
import analytics.internal.Utils
import java.util.*

class IdentifyPayload internal constructor(
  messageId: String,
  timestamp: Date,
  context: Map<String?, Any?>,
  integrations: Map<String, Any>,
  userId: String?,
  anonymousId: String,
  traits: Map<String?, Any?>,
  nanosecondTimestamps: Boolean
) : BasePayload(
  Type.identify,
  messageId,
  timestamp,
  context,
  integrations,
  userId,
  anonymousId,
  nanosecondTimestamps) {
  /**
   * A dictionary of traits you know about a user, for example email or name. We have a collection
   * of special traits that we recognize with semantic meaning, which you should always use when
   * recording that information. You can also add any custom traits that are specific to your
   * project to the dictionary, like friendCount or subscriptionType.
   */
  fun traits(): Traits {
    return getValueMap(TRAITS_KEY, Traits::class.java)
  }

  override fun toString(): String {
    return "IdentifyPayload{\"userId=\"" + userId() + "\"}"
  }

  override fun toBuilder(): Builder {
    return Builder(this)
  }

  /** Fluent API for creating [IdentifyPayload] instances.  */
  class Builder : BasePayload.Builder<IdentifyPayload?, Builder?> {
    private var traits: Map<String?, Any?>? = null

    constructor() {
      // Empty constructor.
    }

    @Private
    internal constructor(identify: IdentifyPayload) : super(identify) {
      traits = identify.traits()
    }

    fun traits(traits: Map<String?, *>): Builder {
      Utils.assertNotNull(traits, "traits")
      this.traits = Collections.unmodifiableMap(LinkedHashMap(traits))
      return this
    }

    override fun self(): Builder {
      return this
    }

    override fun realBuild(messageId: String, timestamp: Date, context: Map<String?, Any?>, integrations: Map<String, Any>, userId: String?, anonymousId: String, nanosecondTimestamps: Boolean): IdentifyPayload? {
      if (Utils.isNullOrEmpty(userId) && Utils.isNullOrEmpty(traits)) {
        throw NullPointerException("either userId or traits are required")
      }
      return IdentifyPayload(
        messageId,
        timestamp,
        context,
        integrations,
        userId,
        anonymousId,
        traits!!,
        nanosecondTimestamps)
    }
  }

  companion object {
    const val TRAITS_KEY = "traits"
  }

  init {
    put(TRAITS_KEY, traits)
  }
}
