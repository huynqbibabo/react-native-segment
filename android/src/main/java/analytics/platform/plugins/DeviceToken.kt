package analytics.platform.plugins

import analytics.Analytics
import analytics.BaseEvent
import analytics.platform.Plugin
import analytics.utilities.putInContextUnderKey

/**
 * Analytics plugin to add device token to events
 */
class DeviceToken(var token: String) : Plugin {
    override var type = Plugin.Type.Before
    override lateinit var analytics: Analytics

    override fun execute(event: BaseEvent): BaseEvent {
        event.putInContextUnderKey("device", "token", token)
        return event
    }
}

/**
 * Set a device token in your payload's context
 * @param token [String] Device Token to add to payloads
 */
fun Analytics.setDeviceToken(token: String) {
    var tokenPlugin = find(DeviceToken::class)
    if (tokenPlugin != null) {
        tokenPlugin.token = token
    } else {
        tokenPlugin = DeviceToken(token)
        add(tokenPlugin)
    }
}
