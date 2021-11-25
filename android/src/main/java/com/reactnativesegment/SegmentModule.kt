package com.reactnativesegment

import analytics.Analytics
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageInfo
import android.content.res.Resources
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import android.provider.Settings.Secure.getString
import androidx.core.content.pm.PackageInfoCompat
import com.facebook.react.bridge.*
import java.util.*

class SegmentModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
  override fun getName(): String {
    return "RNSegment"
  }

  @ReactMethod
  fun getFacebookCampaignId(promise: Promise) {
    try {
      promise.resolve(Facebook.instance?.facebookCampaignId)
    } catch (e: Exception) {
      promise.reject("-1", null, e)
    }
  }
}
