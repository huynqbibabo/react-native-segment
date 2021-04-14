package analytics

import analytics.Cartographer.Builder
import android.util.JsonReader
import android.util.JsonToken
import android.util.JsonWriter
import java.io.*
import java.lang.reflect.Array
import java.util.*

/**
 * Cartographer creates [Map] objects from JSON encoded streams and decodes [Map]
 * objects into JSON streams. Use [Builder] to construct instances.
 */
class Cartographer internal constructor(private val isLenient: Boolean, private val prettyPrint: Boolean) {
  /**
   * Deserializes the specified json into a [Map]. If you have the Json in a [Reader]
   * form instead of a [String], use [.fromJson] instead.
   */
  @Throws(IOException::class)
  fun fromJson(json: String?): Map<String, Any?> {
    requireNotNull(json) { "json == null" }
    require(json.length != 0) { "json empty" }
    return fromJson(StringReader(json))
  }

  /**
   * Deserializes the json read from the specified [Reader] into a [Map]. If you have
   * the Json in a String form instead of a [Reader], use [.fromJson] instead.
   */
  @Throws(IOException::class)
  fun fromJson(reader: Reader?): Map<String, Any?> {
    requireNotNull(reader) { "reader == null" }
    val jsonReader = JsonReader(reader)
    jsonReader.isLenient = isLenient
    return try {
      readerToMap(jsonReader)
    } finally {
      reader.close()
    }
  }

  /**
   * Serializes the map into it's json representation and returns it as a String. If you want to
   * write the json to [Writer] instead of retrieving it as a String, use [ ][.toJson] instead.
   */
  fun toJson(map: Map<*, *>?): String {
    val stringWriter = StringWriter()
    try {
      toJson(map, stringWriter)
    } catch (e: IOException) {
      throw AssertionError(e) // No I/O writing to a Buffer.
    }
    return stringWriter.toString()
  }

  /**
   * Serializes the map into it's json representation into the provided [Writer]. If you
   * want to retrieve the json as a string, use [.toJson] instead.
   */
  @Throws(IOException::class)
  fun toJson(map: Map<*, *>?, writer: Writer?) {
    requireNotNull(map) { "map == null" }
    requireNotNull(writer) { "writer == null" }
    val jsonWriter = JsonWriter(writer)
    jsonWriter.isLenient = isLenient
    if (prettyPrint) {
      jsonWriter.setIndent("  ")
    }
    try {
      mapToWriter(map, jsonWriter)
    } finally {
      jsonWriter.close()
    }
  }

  /** Fluent API to construct instances of [Cartographer].  */
  class Builder {
    private var isLenient = false
    private var prettyPrint = false

    /**
     * Configure this parser to be be liberal in what it accepts. By default, this parser is
     * strict and only accepts JSON as specified by [RFC 4627](http://www.ietf.org/rfc/rfc4627.txt). See [ ][JsonReader.setLenient] for more details.
     */
    fun lenient(isLenient: Boolean): Builder {
      this.isLenient = isLenient
      return this
    }

    /**
     * Configures Cartographer to output Json that fits in a page for pretty printing. This
     * option only affects Json serialization.
     */
    fun prettyPrint(prettyPrint: Boolean): Builder {
      this.prettyPrint = prettyPrint
      return this
    }

    fun build(): Cartographer {
      return Cartographer(isLenient, prettyPrint)
    }
  }

  companion object {
    @JvmField
    val INSTANCE = Builder().lenient(true).prettyPrint(false).build()
    // Decoding
    /** Reads the [JsonReader] into a [Map].  */
    @Throws(IOException::class)
    private fun readerToMap(reader: JsonReader): Map<String, Any?> {
      val map: MutableMap<String, Any?> = LinkedHashMap()
      reader.beginObject()
      while (reader.hasNext()) {
        map[reader.nextName()] = readValue(reader)
      }
      reader.endObject()
      return map
    }

    /** Reads the [JsonReader] into a [List].  */
    @Throws(IOException::class)
    private fun readerToList(reader: JsonReader): List<Any?> {
      // todo: try to infer the type of the List?
      val list: MutableList<Any?> = ArrayList()
      reader.beginArray()
      while (reader.hasNext()) {
        list.add(readValue(reader))
      }
      reader.endArray()
      return list
    }

    /** Reads the next value in the [JsonReader].  */
    @Throws(IOException::class)
    private fun readValue(reader: JsonReader): Any? {
      val token = reader.peek()
      return when (token) {
        JsonToken.BEGIN_OBJECT -> readerToMap(reader)
        JsonToken.BEGIN_ARRAY -> readerToList(reader)
        JsonToken.BOOLEAN -> reader.nextBoolean()
        JsonToken.NULL -> {
          reader.nextNull() // consume the null token
          null
        }
        JsonToken.NUMBER -> reader.nextDouble()
        JsonToken.STRING -> reader.nextString()
        else -> throw IllegalStateException("Invalid token $token")
      }
    }
    // Encoding
    /** Encode the given [Map] into the [JsonWriter].  */
    @Throws(IOException::class)
    private fun mapToWriter(map: Map<*, *>, writer: JsonWriter) {
      writer.beginObject()
      for ((key, value) in map) {
        writer.name(key.toString())
        writeValue(value!!, writer)
      }
      writer.endObject()
    }

    /** Print the json representation of a List to the given writer.  */
    @Throws(IOException::class)
    private fun listToWriter(list: List<*>, writer: JsonWriter) {
      writer.beginArray()
      for (value in list) {
        if (value != null) {
          writeValue(value, writer)
        }
      }
      writer.endArray()
    }

    /**
     * Print the json representation of an array to the given writer. Primitive arrays cannot be
     * cast to Object[], to this method accepts the raw object and uses [ ][Array.getLength] and [Array.get] to read the array.
     */
    @Throws(IOException::class)
    private fun arrayToWriter(array: Any, writer: JsonWriter) {
      writer.beginArray()
      var i = 0
      val size = Array.getLength(array)
      while (i < size) {
        writeValue(Array.get(array, i)!!, writer)
        i++
      }
      writer.endArray()
    }

    /**
     * Writes the given [Object] to the [JsonWriter].
     *
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun writeValue(value: Any?, writer: JsonWriter) {
      var value: Any? = value
      when {
        value == null -> {
          writer.nullValue()
        }
        value is Number -> {
          if (value is Double
            && (java.lang.Double.isNaN((value as Double?)!!) || java.lang.Double.isInfinite((value as Double?)!!))) {
            value = 0.0
          }
          if (value is Float
            && (java.lang.Float.isNaN((value as Float?)!!) || java.lang.Float.isInfinite((value as Float?)!!))) {
            value = 0.0
          }
          writer.value(value as Number?)
        }
        value is Boolean -> {
          writer.value((value as Boolean?)!!)
        }
        value is List<*> -> {
          listToWriter(value, writer)
        }
        value is Map<*, *> -> {
          mapToWriter(value, writer)
        }
        value.javaClass.isArray -> {
          arrayToWriter(value, writer)
        }
        else -> {
          writer.value(value.toString())
        }
      }
    }
  }
}
