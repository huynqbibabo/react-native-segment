package analytics

import android.content.Context

abstract class JSMiddleware(protected var context: Context) {
  @JvmField
  var sourceMiddleware: List<Middleware>? = null
  @JvmField
  var destinationMiddleware: Map<String, List<Middleware>>? = null
  protected var settings: ValueMap? = null
  abstract fun setEdgeFunctionData(data: ValueMap?)
  abstract fun addToDataBridge(key: String?, value: Any?)
  abstract fun removeFromDataBridge(key: String?)
  abstract val dataBridgeSnapshot: Map<String?, Any?>?

}
