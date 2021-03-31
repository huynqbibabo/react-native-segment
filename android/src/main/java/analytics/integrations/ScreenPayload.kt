package analytics.integrations

import analytics.Properties
import analytics.internal.Private
import analytics.internal.Utils
import java.util.*

class ScreenPayload @Private internal constructor(
  messageId: String,
  timestamp: Date,
  context: Map<String?, Any?>,
  integrations: Map<String, Any>,
  userId: String?,
  anonymousId: String,
  name: String?,
  category: String?,
  properties: Map<String?, Any?>,
  nanosecondTimestamps: Boolean
) : BasePayload(
  Type.screen,
  messageId,
  timestamp,
  context,
  integrations,
  userId,
  anonymousId,
  nanosecondTimestamps) {
  /** The category of the page or screen. We recommend using title case, like "Docs".  */
  @Deprecated("")
  fun category(): String? {
    return getString(CATEGORY_KEY)
  }

  /** The name of the page or screen. We recommend using title case, like "About".  */
  fun name(): String? {
    return getString(NAME_KEY)
  }

  /** Either the name or category of the screen payload.  */
  fun event(): String {
    val name = name()
    return if (!Utils.isNullOrEmpty(name)) {
      name!!
    } else category()!!
  }

  /** The page and screen methods also take a properties dictionary, just like track.  */
  fun properties(): Properties {
    return getValueMap(PROPERTIES_KEY, Properties::class.java)
  }

  override fun toString(): String {
    return "ScreenPayload{name=\"" + name() + ",category=\"" + category() + "\"}"
  }

  override fun toBuilder(): Builder {
    return Builder(this)
  }

  /** Fluent API for creating [ScreenPayload] instances.  */
  class Builder : BasePayload.Builder<ScreenPayload?, Builder?> {
    private var name: String? = null
    private var category: String? = null
    private var properties: Map<String?, Any?>? = null

    constructor() {
      // Empty constructor.
    }

    @Private
    internal constructor(screen: ScreenPayload) : super(screen) {
      name = screen.name()
      properties = screen.properties()
    }

    fun name(name: String?): Builder {
      this.name = name
      return this
    }

    @Deprecated("")
    fun category(category: String?): Builder {
      this.category = category
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
    ): ScreenPayload {
      if (Utils.isNullOrEmpty(name) && Utils.isNullOrEmpty(category)) {
        throw NullPointerException("either name or category is required")
      }
      var properties = properties
      if (Utils.isNullOrEmpty(properties)) {
        properties = emptyMap<String?, Any>()
      }
      return ScreenPayload(
        messageId,
        timestamp,
        context,
        integrations,
        userId,
        anonymousId,
        name,
        category,
        properties!!,
        nanosecondTimestamps)
    }

    override fun self(): Builder {
      return this
    }
  }

  companion object {
    const val CATEGORY_KEY = "category"
    const val NAME_KEY = "name"
    const val PROPERTIES_KEY = "properties"
  }

  init {
    if (!Utils.isNullOrEmpty(name)) {
      put(NAME_KEY, name)
    }
    if (!Utils.isNullOrEmpty(category)) {
      put(CATEGORY_KEY, category)
    }
    put(PROPERTIES_KEY, properties)
  }
}
