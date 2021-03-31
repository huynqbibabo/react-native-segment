package analytics

import analytics.integrations.Logger
import android.content.Context
import android.os.AsyncTask
import android.provider.Settings.Secure
import android.util.Pair
import java.util.concurrent.CountDownLatch

/**
 * An [AsyncTask] that fetches the advertising info and attaches it to the given [ ] instance.
 */
internal class GetAdvertisingIdTask(private val analyticsContext: AnalyticsContext, private val latch: CountDownLatch, private val logger: Logger) : AsyncTask<Context?, Void?, Pair<String?, Boolean>?>() {
  @Throws(Exception::class)
  private fun getGooglePlayServicesAdvertisingID(context: Context): Pair<String?, Boolean> {
    val advertisingInfo = Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient")
      .getMethod("getAdvertisingIdInfo", Context::class.java)
      .invoke(null, context)
    val isLimitAdTrackingEnabled = advertisingInfo
      .javaClass
      .getMethod("isLimitAdTrackingEnabled")
      .invoke(advertisingInfo) as Boolean
    if (isLimitAdTrackingEnabled) {
      logger.debug(
        "Not collecting advertising ID because isLimitAdTrackingEnabled (Google Play Services) is true.")
      return Pair.create(null, false)
    }
    val advertisingId = advertisingInfo.javaClass.getMethod("getId").invoke(advertisingInfo) as String
    return Pair.create(advertisingId, true)
  }

  @Throws(Exception::class)
  private fun getAmazonFireAdvertisingID(context: Context): Pair<String?, Boolean> {
    val contentResolver = context.contentResolver

    // Ref: http://prateeks.link/2uGs6bf
    // limit_ad_tracking != 0 indicates user wants to limit ad tracking.
    val limitAdTracking = Secure.getInt(contentResolver, "limit_ad_tracking") != 0
    if (limitAdTracking) {
      logger.debug(
        "Not collecting advertising ID because limit_ad_tracking (Amazon Fire OS) is true.")
      return Pair.create(null, false)
    }
    val advertisingId = Secure.getString(contentResolver, "advertising_id")
    return Pair.create(advertisingId, true)
  }

//  protected override fun doInBackground(vararg contexts: Context): Pair<String?, Boolean>? {
//
//  }

  override fun onPostExecute(info: Pair<String?, Boolean>?) {
    super.onPostExecute(info)
    try {
      if (info == null) {
        return
      }
      val device = analyticsContext.device()
      if (device == null) {
        logger.debug("Not collecting advertising ID because context.device is null.")
        return
      }
      device.putAdvertisingInfo(info.first, info.second)
    } finally {
      latch.countDown()
    }
  }

  override fun doInBackground(vararg params: Context?): Pair<String?, Boolean>? {
    val context = params[0]
    try {
      return context?.let { getGooglePlayServicesAdvertisingID(it) }
    } catch (e: Exception) {
      logger.error(e, "Unable to collect advertising ID from Google Play Services.")
    }
    try {
      return context?.let { getAmazonFireAdvertisingID(it) }
    } catch (e: Exception) {
      logger.error(e, "Unable to collect advertising ID from Amazon Fire OS.")
    }
    logger.debug(
      "Unable to collect advertising ID from Amazon Fire OS and Google Play Services.")
    return null
  }
}
