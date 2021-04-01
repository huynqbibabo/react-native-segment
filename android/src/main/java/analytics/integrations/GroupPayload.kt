package analytics.integrations

import analytics.Traits
import analytics.internal.Private
import analytics.internal.Utils
import java.util.*

class GroupPayload @Private constructor(
//  messageId: String,
//  timestamp: Date,
//  context: Map<String?, Any?>,
//  integrations: Map<String, Any>,
  userId: String?
//  anonymousId: String,
//  groupId: String,
//  traits: Map<String?, Any?>,
//  nanosecondTimestamps: Boolean
) : BasePayload(
  Type.group,
//  messageId
//  timestamp,
//  context,
//  integrations,
  userId
//  anonymousId,
//  nanosecondTimestamps
) {
  /**
   * A unique identifier that refers to the group in your database. For example, if your product
   * groups people by "organization" you would use the organization's ID in your database as the
   * group ID.
   */
  fun groupId(): String {
    return getString(GROUP_ID_KEY)
  }

  /** The group method also takes a traits dictionary, just like identify.  */
  fun traits(): Traits {
    return getValueMap(TRAITS_KEY, Traits::class.java)
  }

  override fun toString(): String {
    return "GroupPayload{groupId=\"" + groupId() + "\"}"
  }

  override fun toBuilder(): Builder {
    return Builder(this)
  }

  /** Fluent API for creating [GroupPayload] instances.  */
  class Builder : BasePayload.Builder<GroupPayload?, Builder?> {
    private var groupId: String? = null
    private var traits: Map<String?, Any?>? = null

    constructor() {
      // Empty constructor.
    }

    @Private
    internal constructor(group: GroupPayload) : super(group) {
      groupId = group.groupId()
      traits = group.traits()
    }

    fun groupId(groupId: String): Builder {
      this.groupId = Utils.assertNotNullOrEmpty(groupId, "groupId")
      return this
    }

    fun traits(traits: Map<String?, *>): Builder {
      Utils.assertNotNull(traits, "traits")
      this.traits = Collections.unmodifiableMap(LinkedHashMap(traits))
      return this
    }

    override fun realBuild(
//      messageId: String,
//      timestamp: Date,
//      context: Map<String?, Any?>,
//      integrations: Map<String, Any>,
      userId: String?
//      anonymousId: String,
//      nanosecondTimestamps: Boolean
    ): GroupPayload {
      Utils.assertNotNullOrEmpty(groupId, "groupId")
      var traits = traits
      if (Utils.isNullOrEmpty(traits)) {
        traits = emptyMap<String?, Any>()
      }
      return GroupPayload(
//        messageId,
//        timestamp,
//        context,
//        integrations,
        userId
//        anonymousId,
//        groupId!!,
//        traits!!,
//        nanosecondTimestamps
      )
    }

    override fun self(): Builder {
      return this
    }
  }

  companion object {
    const val GROUP_ID_KEY = "groupId"
    const val TRAITS_KEY = "traits"
  }

  init {
//    put(GROUP_ID_KEY, groupId)
//    put(TRAITS_KEY, traits)
  }
}
