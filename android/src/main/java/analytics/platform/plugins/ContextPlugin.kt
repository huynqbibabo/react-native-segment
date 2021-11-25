package analytics.platform.plugins

import analytics.Analytics
import analytics.BaseEvent
import analytics.Constants.LIBRARY_VERSION
import analytics.platform.Plugin
import analytics.utilities.putAll
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Analytics plugin used to populate events with basic context data.
 * Auto-added to analytics client on construction
 */
class ContextPlugin : Plugin {
    override val type: Plugin.Type = Plugin.Type.Before
    override lateinit var analytics: Analytics

    private lateinit var library: JsonObject

    companion object {
        // Library
        const val LIBRARY_KEY = "library"
        const val LIBRARY_NAME_KEY = "name"
        const val LIBRARY_VERSION_KEY = "version"
    }

    override fun setup(analytics: Analytics) {
        super.setup(analytics)
        library = buildJsonObject {
            put(LIBRARY_NAME_KEY, "analytics-kotlin")
            put(LIBRARY_VERSION_KEY, LIBRARY_VERSION)
        }
    }

    private fun applyContextData(event: BaseEvent) {
        val newContext = buildJsonObject {
            // copy existing context
            putAll(event.context)

            // putLibrary
            put(LIBRARY_KEY, library)
        }
        event.context = newContext
    }

    override fun execute(event: BaseEvent): BaseEvent {
        applyContextData(event)
        return event
    }
}
