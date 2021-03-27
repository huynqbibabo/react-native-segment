package com.reactnativesegment

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise

class SegmentModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String {
        return "Segment"
    }

    @ReactMethod
    fun getFacebookAdCampaignId(promise: Promise) {
      try {
        promise.resolve(RNAnalytics.instance?.facebookCampaignId)
      } catch (e: Exception) {
        promise.reject("-1", null, e)
      }
    }

}
