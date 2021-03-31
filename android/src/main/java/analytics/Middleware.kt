package analytics

import analytics.integrations.BasePayload

/** Middlewares run for every message after it is built to process it further.  */
interface Middleware {
  /** Called for every message. This will be called on the same thread the request was made.  */
  fun intercept(chain: Chain?)
  interface Chain {
    fun payload(): BasePayload?
    fun proceed(payload: BasePayload?)
  }

  interface Callback {
    operator fun invoke(payload: BasePayload?)
  }
}
