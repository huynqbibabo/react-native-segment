package analytics

import analytics.integrations.BasePayload

internal class MiddlewareChainRunner(
  private val index: Int,
  private val payload: BasePayload,
  private val middleware: List<Middleware>,
  private val callback: Middleware.Callback
) : Middleware.Chain {
  override fun payload(): BasePayload {
    return payload
  }


  override fun proceed(payload: BasePayload?) {
    // If there's another middleware in the chain, call that.
    if (index < middleware.size) {
      val chain: MiddlewareChainRunner? = payload?.let { MiddlewareChainRunner(index + 1, it, middleware, callback) }
      middleware[index].intercept(chain)
      return
    }

    // No more interceptors.
    callback.invoke(payload)
  }
}
