package analytics

import analytics.internal.Utils
import android.text.TextUtils
import android.util.Log
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.util.zip.GZIPOutputStream

//import static java.net.HttpURLConnection.HTTP_OK;
/** HTTP client which can upload payloads and fetch project settings from the Segment public API.  */
class Client(private val writeKey: String, private val connectionFactory: ConnectionFactory) {
  @Throws(IOException::class)
  fun upload(apiHost: String?): Connection {
    val connection = connectionFactory.upload(apiHost, writeKey)
    return createPostConnection(connection)
  }
  //    Connection fetchSettings() throws IOException {
  //        HttpURLConnection connection = connectionFactory.projectSettings(writeKey);
  //        int responseCode = connection.getResponseCode();
  //        if (responseCode != HTTP_OK) {
  //            connection.disconnect();
  //            throw new IOException("HTTP " + responseCode + ": " + connection.getResponseMessage());
  //        }
  //        return createGetConnection(connection);
  //    }
  /** Represents an HTTP exception thrown for unexpected/non 2xx response codes.  */
  internal class HTTPException(val responseCode: Int, responseMessage: String, responseBody: String) : IOException("HTTP $responseCode: $responseMessage. Response: $responseBody") {
    fun is4xx(): Boolean {
      return responseCode in 400..499
    }
  }

  /**
   * Wraps an HTTP connection. Callers can either read from the connection via the [ ] or write to the connection via [OutputStream].
   */
  abstract class Connection(connection: HttpURLConnection?, `is`: InputStream?, os: OutputStream?) : Closeable {
    private val connection: HttpURLConnection
    val `is`: InputStream?

    @JvmField
    val os: OutputStream?

    @Throws(IOException::class)
    override fun close() {
      connection.disconnect()
    }

    init {
      requireNotNull(connection) { "connection == null" }
      this.connection = connection
      this.`is` = `is`
      this.os = os
    }
  }

  companion object {
    const val TAG = "Client"
    @Throws(IOException::class)
    private fun createPostConnection(connection: HttpURLConnection): Connection {
      val outputStream: OutputStream
      // Clients may have opted out of gzip compression via a custom connection factory.
      val contentEncoding = connection.getRequestProperty("Content-Encoding")
      outputStream = if (TextUtils.equals("gzip", contentEncoding)) {
        GZIPOutputStream(connection.outputStream)
      } else {
        connection.outputStream
      }
      return object : Connection(connection, null, outputStream) {
        @Throws(IOException::class)
        override fun close() {
          try {
            val responseCode = connection.responseCode
            Log.i(TAG, "requestProperties: $responseCode")
//            if (responseCode >= HttpURLConnection.HTTP_OK) { // 200
            if (responseCode >= HttpURLConnection.HTTP_MULT_CHOICE) { // 300
              var responseBody: String
              var inputStream: InputStream? = null
              try {
                inputStream = Utils.getInputStream(connection)
                responseBody = Utils.readFully(inputStream)
                Log.i("TAG", "responseBody: $responseBody")
              } catch (e: IOException) {
                responseBody = ("Could not read response body for rejected message: "
                  + e.toString())
              } finally {
                inputStream?.close()
              }
              throw HTTPException(
                responseCode, connection.responseMessage, responseBody)
            }
          } finally {
            super.close()
            os!!.close()
          }
        }
      }
    }

    @Throws(IOException::class)
    private fun createGetConnection(connection: HttpURLConnection): Connection {
      return object : Connection(connection, Utils.getInputStream(connection), null) {
        @Throws(IOException::class)
        override fun close() {
          super.close()
          `is`!!.close()
        }
      }
    }
  }
}
