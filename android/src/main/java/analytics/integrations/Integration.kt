/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 webhook_bibabo, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package analytics.integrations

import analytics.Analytics
import analytics.ValueMap
import android.app.Activity
import android.os.Bundle

/**
 * Converts Segment messages to a format a bundled integration understands, and calls those methods.
 *
 * @param <T> The type of the backing instance. This isn't strictly necessary (since we return an
 * object), but serves as documentation for what type to expect with [     ][.getUnderlyingInstance].
</T> */
abstract class Integration<T> {
  interface Factory {
    /**
     * Attempts to create an adapter for with `settings`. This returns the adapter if one
     * was created, or null if this factory isn't capable of creating such an adapter.
     */
    fun create(settings: ValueMap?, analytics: Analytics?): Integration<*>?

    /** The key for which this factory can create an [Integration].  */
    fun key(): String
  }

  /** @see android.app.Application.ActivityLifecycleCallbacks
   */
  fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {}

  /** @see android.app.Application.ActivityLifecycleCallbacks
   */
  fun onActivityStarted(activity: Activity?) {}

  /** @see android.app.Application.ActivityLifecycleCallbacks
   */
  fun onActivityResumed(activity: Activity?) {}

  /** @see android.app.Application.ActivityLifecycleCallbacks
   */
  fun onActivityPaused(activity: Activity?) {}

  /** @see android.app.Application.ActivityLifecycleCallbacks
   */
  fun onActivityStopped(activity: Activity?) {}

  /** @see android.app.Application.ActivityLifecycleCallbacks
   */
  fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {}

  /** @see android.app.Application.ActivityLifecycleCallbacks
   */
  fun onActivityDestroyed(activity: Activity?) {}

  /**
   * @see Analytics.identify
   */
  open fun identify(identify: IdentifyPayload?) {}

  /** @see Analytics.group
   */
  open fun group(group: GroupPayload?) {}

  /**
   * @see Analytics.track
   */
  open fun track(track: TrackPayload?) {}

  /** @see Analytics.alias
   */
  open fun alias(alias: AliasPayload?) {}

  /**
   * @see Analytics.screen
   */
  open fun screen(screen: ScreenPayload?) {}

  /** @see Analytics.flush
   */
  open fun flush() {}

  /** @see Analytics.reset
   */
  fun reset() {}

  /**
   * The underlying instance for this provider - used for integration specific actions. This will
   * return `null` for SDK's that only provide interactions with static methods (e.g.
   * Localytics).
   */
  val underlyingInstance: T?
    get() = null
}
