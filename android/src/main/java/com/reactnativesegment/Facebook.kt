package com.reactnativesegment


open class Facebook protected constructor() {

  var facebookCampaignId: String? = null
  companion object {
    private var mInstance: Facebook? = null


    @get:Synchronized
    val instance: Facebook?
      get() {
        if (null == mInstance) {
          mInstance = Facebook()
        }
        return mInstance
      }
  }
}
