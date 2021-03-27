package com.reactnativesegment


open class RNAnalytics protected constructor() {

  var facebookCampaignId: String? = null
  companion object {
    private var mInstance: RNAnalytics? = null


    @get:Synchronized
    val instance: RNAnalytics?
      get() {
        if (null == mInstance) {
          mInstance = RNAnalytics()
        }
        return mInstance
      }
  }
}

