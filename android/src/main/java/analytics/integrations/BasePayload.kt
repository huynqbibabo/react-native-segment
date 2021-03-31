/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Segment.io, Inc.
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

import analytics.AnalyticsContext
import analytics.ValueMap
import analytics.internal.NanoDate
import analytics.internal.Utils
import androidx.annotation.CheckResult
import java.util.*

/**
 * A payload object that will be sent to the server. Clients will not decode instances of this
 * directly, but through one if it's subclasses.
 */
// This ignores projectId, receivedAt and version that are set by the server.
// sentAt is set on SegmentClient#BatchPayload
abstract class BasePayload internal constructor(
  type: Type,
  messageId: String,
  timestamp: Date,
  context: Map<String?, Any?>,
  integrations: Map<String, Any>,
  userId: String?,
  anonymousId: String,
  nanosecondTimestamps: Boolean
) : ValueMap() {
  /** The type of message.  */
  fun type(): Type {
    return getEnum(Type::class.java, TYPE_KEY)
  }

  /**
   * The user ID is an identifier that unique identifies the user in your database. Ideally it
   * should not be an email address, because emails can change, whereas a database ID can't.
   */
  fun userId(): String? {
    return getString(USER_ID_KEY)
  }

  /**
   * The anonymous ID is an identifier that uniquely (or close enough) identifies the user, but
   * isn't from your database. This is useful in cases where you are able to uniquely identifier
   * the user between visits before they sign up thanks to a cookie, or session ID or device ID.
   * In our mobile and browser libraries we will automatically handle sending the anonymous ID.
   */
  fun anonymousId(): String {
    return getString(ANONYMOUS_ID_KEY)
  }

  /** A randomly generated unique id for this message.  */
  fun messageId(): String {
    return getString(MESSAGE_ID)
  }

  /**
   * Set a timestamp the event occurred.
   *
   *
   * This library will automatically create and attach a timestamp to all events.
   *
   * @see [Timestamp](https://segment.com/docs/spec/common/.timestamps)
   */
  fun timestamp(): Date? {
    // It's unclear if this will ever be null. So we're being safe.
    val timestamp = getString(TIMESTAMP_KEY)
    if (Utils.isNullOrEmpty(timestamp)) {
      return null
    }
    return if (timestamp.length == "yyyy-MM-ddThh:mm:ss.fffZ".length) {
      Utils.parseISO8601Date(timestamp)
    } else {
      Utils.parseISO8601DateWithNanos(timestamp)
    }
  }

  /**
   * A dictionary of integration names that the message should be proxied to. 'All' is a special
   * name that applies when no key for a specific integration is found, and is case-insensitive.
   */
  fun integrations(): ValueMap {
    return getValueMap(INTEGRATIONS_KEY)
  }

  /**
   * The context is a dictionary of extra information that provides useful context about a
   * message, for example ip address or locale.
   *
   * @see [Context fields](https://segment.com/docs/spec/common/.context)
   */
  fun context(): AnalyticsContext {
    return getValueMap(CONTEXT_KEY, AnalyticsContext::class.java)
  }

  override fun putValue(key: String, value: Any): BasePayload {
    super.putValue(key, value)
    return this
  }

  abstract fun toBuilder(): Builder<*, *>

  /** @see .TYPE_KEY
   */
  enum class Type {
    alias, group, identify, screen, track
  }

  /**
   * The channel where the request originated from: server, browser or mobile. In the future we
   * may add additional channels as we add libraries, for example console.
   *
   *
   * This is always [Channel.mobile] for us.
   */
  enum class Channel {
    mobile
  }

  abstract class Builder<P : BasePayload?, B : Builder<P, B>?> {
    private var messageId: String? = null
    private var timestamp: Date? = null
    private var context: Map<String?, Any?>? = null
    private var integrationsBuilder: MutableMap<String?, Any?>? = null
    private var userId: String? = null
    private var anonymousId: String? = null
    private var nanosecondTimestamps = false

    internal constructor() {
      // Empty constructor.
    }

    internal constructor(payload: BasePayload) {
      val tsStr = payload.getString(TIMESTAMP_KEY)
      if (tsStr != null
        && tsStr.length > 24) { // [yyyy-MM-ddThh:mm:ss.sssZ] format without nanos
        nanosecondTimestamps = true
      }
      messageId = payload.messageId()
      timestamp = payload.timestamp()
      context = payload.context()
      integrationsBuilder = LinkedHashMap(payload.integrations())
      userId = payload.userId()
      anonymousId = payload.anonymousId()
    }

    /**
     * The Message ID is a unique identifier for each message. If not provided, one will be
     * generated for you. This ID is typically used for deduping - messages with the same IDs as
     * previous events may be dropped.
     *
     * @see [Common Fields](https://segment.com/docs/spec/common/)
     */
    fun messageId(messageId: String): B {
      Utils.assertNotNullOrEmpty(messageId, "messageId")
      this.messageId = messageId
      return self()
    }

    /**
     * Set a timestamp for the event. By default, the current timestamp is used, but you may
     * override it for historical import.
     *
     *
     * This library will automatically create and attach a timestamp to all events.
     *
     * @see [Timestamp](https://segment.com/docs/spec/common/.timestamps)
     */
    fun timestamp(timestamp: Date): B {
      Utils.assertNotNull(timestamp, "timestamp")
      this.timestamp = timestamp
      return self()
    }

    /**
     * Set a map of information about the state of the device. You can add any custom data to
     * the context dictionary that you'd like to have access to in the raw logs.
     *
     *
     * Some keys in the context dictionary have semantic meaning and will be collected for
     * you automatically, depending on the library you send data from. Some keys, such as
     * location and speed need to be manually entered.
     *
     * @see [Context](https://segment.com/docs/spec/common/.context)
     */
    fun context(context: Map<String?, *>): B {
      Utils.assertNotNull(context, "context")
      this.context = Collections.unmodifiableMap(LinkedHashMap(context))
      return self()
    }

    /**
     * Set whether this message is sent to the specified integration or not. 'All' is a special
     * key that applies when no key for a specific integration is found.
     *
     * @see [Integrations](https://segment.com/docs/spec/common/.integrations)
     */
    fun integration(key: String, enable: Boolean): B {
      Utils.assertNotNullOrEmpty(key, "key")
      if (integrationsBuilder == null) {
        integrationsBuilder = LinkedHashMap()
      }
      integrationsBuilder!![key] = enable
      return self()
    }

    /**
     * Pass in some options that will only be used by the target integration. This will
     * implicitly mark the integration as enabled.
     *
     * @see [Integrations](https://segment.com/docs/spec/common/.integrations)
     */
    fun integration(key: String, options: Map<String, Any>): B {
      Utils.assertNotNullOrEmpty(key, "key")
      Utils.assertNotNullOrEmpty(options, "options")
      if (integrationsBuilder == null) {
        integrationsBuilder = LinkedHashMap()
      }
      integrationsBuilder!![key] = Utils.immutableCopyOf(options)
      return self()
    }

    /**
     * Specify a dictionary of options for integrations.
     *
     * @see [Integrations](https://segment.com/docs/spec/common/.integrations)
     */
    fun integrations(integrations: Map<String?, *>?): B {
      if (Utils.isNullOrEmpty(integrations)) {
        return self()
      }
      if (integrationsBuilder == null) {
        integrationsBuilder = LinkedHashMap()
      }
      integrationsBuilder!!.putAll(integrations!!)
      return self()
    }

    /**
     * The Anonymous ID is a pseudo-unique substitute for a User ID, for cases when you don't
     * have an absolutely unique identifier.
     *
     * @see [Identities](https://segment.com/docs/spec/identify/.identities)
     *
     * @see [Anonymous ID](https://segment.com/docs/spec/identify/.anonymous-id)
     */
    fun anonymousId(anonymousId: String): B {
      this.anonymousId = Utils.assertNotNullOrEmpty(anonymousId, "anonymousId")
      return self()
    }

    /**
     * The User ID is a persistent unique identifier for a user (such as a database ID).
     *
     * @see [Identities](https://segment.com/docs/spec/identify/.identities)
     *
     * @see [User ID](https://segment.com/docs/spec/identify/.user-id)
     */
    fun userId(userId: String): B {
      this.userId = Utils.assertNotNullOrEmpty(userId, "userId")
      return self()
    }

    /** Returns true if userId is not-null or non-empty, false otherwise  */
    val isUserIdSet: Boolean
      get() = !Utils.isNullOrEmpty(userId)

    fun nanosecondTimestamps(enabled: Boolean): B {
      nanosecondTimestamps = enabled
      return self()
    }

    abstract fun realBuild(
      messageId: String,
      timestamp: Date,
      context: Map<String?, Any?>,
      integrations: Map<String, Any>,
      userId: String?,
      anonymousId: String,
      nanosecondTimestamps: Boolean
    ): P

    abstract fun self(): B

    /** Create a [BasePayload] instance.  */
    @CheckResult
    fun build(): P {
      if (Utils.isNullOrEmpty(userId) && Utils.isNullOrEmpty(anonymousId)) {
        throw NullPointerException("either userId or anonymousId is required")
      }
      val integrations: Map<String, Any> = if (Utils.isNullOrEmpty(integrationsBuilder)) {
        emptyMap()
      } else {
        integrationsBuilder?.let { Utils.immutableCopyOf(it.toMap() as Map<String, Any>) }!!
      }
      if (Utils.isNullOrEmpty(messageId)) {
        messageId = UUID.randomUUID().toString()
      }
      if (timestamp == null) {
        timestamp = if (nanosecondTimestamps) {
          NanoDate() // captures higher resolution timestamps
        } else {
          Date()
        }
      }
      if (Utils.isNullOrEmpty(context)) {
        context = emptyMap<String?, Any>()
      }
      return realBuild(
        messageId!!,
        timestamp!!,
        context!!,
        integrations,
        userId,
        anonymousId!!,
        nanosecondTimestamps)
    }
  }

  companion object {
    const val TYPE_KEY = "type"
    const val ANONYMOUS_ID_KEY = "anonymousId"
    const val CHANNEL_KEY = "channel"
    const val MESSAGE_ID = "messageId"
    const val CONTEXT_KEY = "context"
    const val INTEGRATIONS_KEY = "integrations"
    const val TIMESTAMP_KEY = "timestamp"
    const val USER_ID_KEY = "userId"
  }

  init {
    put(CHANNEL_KEY, Channel.mobile)
    put(TYPE_KEY, type)
    put(MESSAGE_ID, messageId)
    if (nanosecondTimestamps) {
      put(TIMESTAMP_KEY, Utils.toISO8601NanoFormattedString(timestamp))
    } else {
      put(TIMESTAMP_KEY, Utils.toISO8601String(timestamp))
    }
    put(CONTEXT_KEY, context)
    put(INTEGRATIONS_KEY, integrations)
    if (!Utils.isNullOrEmpty(userId)) {
      put(USER_ID_KEY, userId)
    }
    put(ANONYMOUS_ID_KEY, anonymousId)
  }
}
