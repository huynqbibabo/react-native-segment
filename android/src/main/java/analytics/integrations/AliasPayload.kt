package analytics.integrations

import analytics.internal.Private
import analytics.internal.Utils
import java.util.*

class AliasPayload @Private internal constructor(
  messageId: String,
  timestamp: Date,
  context: Map<String?, Any?>,
  integrations: Map<String, Any>,
  userId: String?,
  anonymousId: String,
  previousId: String,
  nanosecondTimestamps: Boolean
) : BasePayload(
  Type.alias,
  messageId,
  timestamp,
  context,
  integrations,
  userId,
  anonymousId,
  nanosecondTimestamps) {
  /**
   * The previous ID for the user that you want to alias from, that you previously called identify
   * with as their user ID, or the anonymous ID if you haven't identified the user yet.
   */
  fun previousId(): String {
    return getString(PREVIOUS_ID_KEY)
  }

  override fun toString(): String {
    return "AliasPayload{userId=\"" + userId() + ",previousId=\"" + previousId() + "\"}"
  }

  override fun toBuilder(): Builder {
    return Builder(this)
  }

  /** Fluent API for creating [AliasPayload] instances.  */
  class Builder : BasePayload.Builder<AliasPayload?, Builder?> {
    private var previousId: String? = null

    constructor() {
      // Empty constructor.
    }

    @Private
    internal constructor(alias: AliasPayload) : super(alias) {
      previousId = alias.previousId()
    }

    fun previousId(previousId: String): Builder {
      this.previousId = Utils.assertNotNullOrEmpty(previousId, "previousId")
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
    ): AliasPayload {
      Utils.assertNotNullOrEmpty(userId, "userId")
      Utils.assertNotNullOrEmpty(previousId, "previousId")
      return AliasPayload(
        messageId,
        timestamp,
        context,
        integrations,
        userId,
        anonymousId,
        previousId!!,
        nanosecondTimestamps)
    }

    override fun self(): Builder {
      return this
    }
  }

  companion object {
    const val PREVIOUS_ID_KEY = "previousId"
  }

  init {
    put(PREVIOUS_ID_KEY, previousId)
  }
}
