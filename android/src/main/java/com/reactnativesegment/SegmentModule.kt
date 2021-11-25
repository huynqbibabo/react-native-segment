package com.reactnativesegment

import com.facebook.react.bridge.*

class SegmentModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
  override fun getName(): String {
    return "RNSegment"
  }

  @ReactMethod
  fun getInstallCampaignId(promise: Promise) {
    try {
      promise.resolve(Facebook.instance?.facebookCampaignId)
    } catch (e: Exception) {
      promise.reject("-1", null, e)
    }
  }
}
