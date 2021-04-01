package analytics

import com.reactnativesegment.BuildConfig
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

/**
 * Abstraction to customize how connections are created. This is can be used to point our SDK at
 * your proxy server for instance.
 */
open class ConnectionFactory {
  private fun authorizationHeader(writeKey: String): String {
    return "Bearer $writeKey"
  }
  //    /** Return a {@link HttpURLConnection} that reads JSON formatted project settings. */
  //    public HttpURLConnection projectSettings(String writeKey) throws IOException {
  //        return openConnection(
  //                "https://cdn-settings.segment.com/v1/projects/" + writeKey + "/settings");
  //    }
  /**
   * Return a [HttpURLConnection] that writes batched payloads to `https://api.segment.io/v1/import`.
   */
  @Throws(IOException::class)
  fun upload(apiHost: String?, writeKey: String): HttpURLConnection {
    val connection = openConnection(String.format("https://%s", apiHost))
    connection.setRequestProperty("Authorization", "Bearer $writeKey")
//    connection.setRequestProperty("Content-Encoding", "gzip")
    connection.setRequestProperty("Content-Type", "application/json");
    connection.doOutput = true
    connection.setChunkedStreamingMode(0)
    return connection
  }

  /**
   * Configures defaults for connections opened with [.upload]
   */
  @Throws(IOException::class)
  protected open fun openConnection(url: String): HttpURLConnection {
    val requestedURL: URL = try {
      URL(url)
    } catch (e: MalformedURLException) {
      throw IOException("Attempted to use malformed url: $url", e)
    }
    val connection = requestedURL.openConnection() as HttpURLConnection
    connection.connectTimeout = DEFAULT_CONNECT_TIMEOUT_MILLIS
    connection.readTimeout = DEFAULT_READ_TIMEOUT_MILLIS
    connection.setRequestProperty("Content-Type", "application/json")
    connection.setRequestProperty("User-Agent", USER_AGENT)
    connection.doInput = true
    return connection
  }

  companion object {
    private const val DEFAULT_READ_TIMEOUT_MILLIS = 20 * 1000 // 20s
    private const val DEFAULT_CONNECT_TIMEOUT_MILLIS = 15 * 1000 // 15s
    const val USER_AGENT = "analytics-android/" + BuildConfig.VERSION_NAME
  }
}
