package com.reactnativesegment

import analytics.Analytics
import analytics.integrations.Integration

object RNAnalytics {
  private val integrations = mutableSetOf<Integration.Factory>()
  private val onReadyCallbacks = mutableMapOf<String, Analytics.Callback<Any?>>()

  fun setIDFA(idfa: String) {
    // do nothing; iOS only.
  }

  fun addIntegration(integration: Integration.Factory) {
    integrations.add(integration)
  }

  fun buildWithIntegrations(builder: Analytics.Builder): Analytics {
    for(integration in integrations) {
      builder.use(integration)
    }

    return builder.build()
  }

  fun addOnReadyCallback(key: String, callback: Analytics.Callback<Any?>) {
    onReadyCallbacks[key] = callback
  }

  fun setupCallbacks(analytics: Analytics) {
    for(integration in onReadyCallbacks.keys) {
      analytics.onIntegrationReady(integration, onReadyCallbacks[integration])
    }
  }
}
