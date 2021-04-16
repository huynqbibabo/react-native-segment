package analytics.internal

import java.util.*

/*
* Copyright (C) 2011 FasterXML, LLC
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/ /**
 * Jackson's date formatter, pruned to Moshi's needs. Forked from this file:
 *
 * Utilities methods for manipulating dates in iso8601 format. This is much much faster and GC
 * friendly than using SimpleDateFormat so highly suitable if you (un)serialize lots of date
 * objects.
 *
 *
 * Supported parse format:
 * [yyyy-MM-dd|yyyyMMdd][T(hh:mm[:ss[.sss]]|hhmm[ss[.sss]])]?[Z|[+-]hh[:]mm]] and
 * [yyyy-MM-dd|yyyyMMdd][T(hh:mm[:ss[.fffffffff]]|hhmm[ss[.fffffffff]])]?[Z|[+-]hh[:]mm]]
 *
 * @see [this specification](http://www.w3.org/TR/NOTE-datetime)
 */
internal object Iso8601Utils {
  /** ID to represent the 'GMT' string  */
  const val GMT_ID = "GMT"

  /** The GMT timezone, prefetched to avoid more lookups.  */
  val TIMEZONE_Z = TimeZone.getTimeZone(GMT_ID)

  /** Returns `date` formatted as yyyy-MM-ddThh:mm:ss.sssZ  */
  @JvmStatic
  fun format(date: Date?): String {
    val calendar: Calendar = GregorianCalendar(TIMEZONE_Z, Locale.US)
    calendar.time = date

    // estimate capacity of buffer as close as we can (yeah, that's pedantic ;)
    val capacity = "yyyy-MM-ddThh:mm:ss.sssZ".length
    val formatted = StringBuilder(capacity)
    padInt(formatted, calendar[Calendar.YEAR], "yyyy".length)
    formatted.append('-')
    padInt(formatted, calendar[Calendar.MONTH] + 1, "MM".length)
    formatted.append('-')
    padInt(formatted, calendar[Calendar.DAY_OF_MONTH], "dd".length)
    formatted.append('T')
    padInt(formatted, calendar[Calendar.HOUR_OF_DAY], "hh".length)
    formatted.append(':')
    padInt(formatted, calendar[Calendar.MINUTE], "mm".length)
    formatted.append(':')
    padInt(formatted, calendar[Calendar.SECOND], "ss".length)
    formatted.append('.')
    padInt(formatted, calendar[Calendar.MILLISECOND], "sss".length)
    formatted.append('Z')
    return formatted.toString()
  }

  /** Returns `date` formatted as yyyy-MM-ddThh:mm:ss.fffffffffZ  */
  @JvmStatic
  fun formatNanos(date: Date?): String {
    val calendar: Calendar = GregorianCalendar(TIMEZONE_Z, Locale.US)
    calendar.time = date

    // estimate capacity of buffer as close as we can (yeah, that's pedantic ;)
    val capacity = "yyyy-MM-ddThh:mm:ss.fffffffffZ".length
    val formatted = StringBuilder(capacity)
    padInt(formatted, calendar[Calendar.YEAR], "yyyy".length)
    formatted.append('-')
    padInt(formatted, calendar[Calendar.MONTH] + 1, "MM".length)
    formatted.append('-')
    padInt(formatted, calendar[Calendar.DAY_OF_MONTH], "dd".length)
    formatted.append('T')
    padInt(formatted, calendar[Calendar.HOUR_OF_DAY], "hh".length)
    formatted.append(':')
    padInt(formatted, calendar[Calendar.MINUTE], "mm".length)
    formatted.append(':')
    padInt(formatted, calendar[Calendar.SECOND], "ss".length)
    formatted.append('.')
    if (date is NanoDate) {
      val nanos = date.nanos()
      padLong(formatted, nanos % 1000000000, "fffffffff".length)
    } else {
      padLong(formatted, calendar[Calendar.MILLISECOND].toLong(), "fffffffff".length)
    }
    formatted.append('Z')
    return formatted.toString()
  }

  /**
   * Parse a date from ISO-8601 formatted string. It expects a format
   * [yyyy-MM-dd|yyyyMMdd][T(hh:mm[:ss[.sss]]|hhmm[ss[.sss]])]?[Z|[+-]hh:mm]]
   *
   * @param date ISO string to parse in the appropriate format.
   * @return the parsed date
   */
  @JvmStatic
  fun parse(date: String): Date {
    return try {
      var offset = 0

      // extract year
      val year = parseInt(date, offset, 4.let { offset += it; offset })
      if (checkOffset(date, offset, '-')) {
        offset += 1
      }

      // extract month
      val month = parseInt(date, offset, 2.let { offset += it; offset })
      if (checkOffset(date, offset, '-')) {
        offset += 1
      }

      // extract day
      val day = parseInt(date, offset, 2.let { offset += it; offset })
      // default time value
      var hour = 0
      var minutes = 0
      var seconds = 0
      var milliseconds = 0 // always use 0 otherwise returned date will include millis of current time

      // if the value has no time component (and no time zone), we are done
      val hasT = checkOffset(date, offset, 'T')
      if (!hasT && date.length <= offset) {
        val calendar: Calendar = GregorianCalendar(year, month - 1, day)
        return calendar.time
      }
      if (hasT) {

        // extract hours, minutes, seconds and milliseconds
        hour = parseInt(date, 1.let { offset += it; offset }, 2.let { offset += it; offset })
        if (checkOffset(date, offset, ':')) {
          offset += 1
        }
        minutes = parseInt(date, offset, 2.let { offset += it; offset })
        if (checkOffset(date, offset, ':')) {
          offset += 1
        }
        // second and milliseconds can be optional
        if (date.length > offset) {
          val c = date[offset]
          if (c != 'Z' && c != '+' && c != '-') {
            seconds = parseInt(date, offset, 2.let { offset += it; offset })
            if (seconds > 59 && seconds < 63) {
              seconds = 59 // truncate up to 3 leap seconds
            }
            // milliseconds can be optional in the format
            if (checkOffset(date, offset, '.')) {
              offset += 1
              val endOffset = indexOfNonDigit(date, offset + 1) // assume at least one digit
              val parseEndOffset = Math.min(endOffset, offset + 3) // parse up to 3 digits
              val fraction = parseInt(date, offset, parseEndOffset)
              milliseconds = (Math.pow(10.0, (3 - (parseEndOffset - offset)).toDouble()) * fraction).toInt()
              offset = endOffset
            }
          }
        }
      }

      // extract timezone
      require(date.length > offset) { "No time zone indicator" }
      val timezone: TimeZone
      val timezoneIndicator = date[offset]
      if (timezoneIndicator == 'Z') {
        timezone = TIMEZONE_Z
      } else if (timezoneIndicator == '+' || timezoneIndicator == '-') {
        val timezoneOffset = date.substring(offset)
        // 18-Jun-2015, tatu: Minor simplification, skip offset of "+0000"/"+00:00"
        if ("+0000" == timezoneOffset || "+00:00" == timezoneOffset) {
          timezone = TIMEZONE_Z
        } else {
          // 18-Jun-2015, tatu: Looks like offsets only work from GMT, not UTC...
          //    not sure why, but it is what it is.
          val timezoneId = GMT_ID + timezoneOffset
          timezone = TimeZone.getTimeZone(timezoneId)
          val act = timezone.id
          if (act != timezoneId) {
            /* 22-Jan-2015, tatu: Looks like canonical version has colons, but we may be given
                         *    one without. If so, don't sweat.
                         *   Yes, very inefficient. Hopefully not hit often.
                         *   If it becomes a perf problem, add 'loose' comparison instead.
                         */
            val cleaned = act.replace(":", "")
            if (cleaned != timezoneId) {
              throw IndexOutOfBoundsException(
                "Mismatching time zone indicator: "
                  + timezoneId
                  + " given, resolves to "
                  + timezone.id)
            }
          }
        }
      } else {
        throw IndexOutOfBoundsException(
          "Invalid time zone indicator '$timezoneIndicator'")
      }
      val calendar: Calendar = GregorianCalendar(timezone)
      calendar.isLenient = false
      calendar[Calendar.YEAR] = year
      calendar[Calendar.MONTH] = month - 1
      calendar[Calendar.DAY_OF_MONTH] = day
      calendar[Calendar.HOUR_OF_DAY] = hour
      calendar[Calendar.MINUTE] = minutes
      calendar[Calendar.SECOND] = seconds
      calendar[Calendar.MILLISECOND] = milliseconds
      calendar.time
      // If we get a ParseException it'll already have the right message/offset.
      // Other exception types can convert here.
    } catch (e: IndexOutOfBoundsException) {
      throw RuntimeException("Not an RFC 3339 date: $date", e)
    } catch (e: IllegalArgumentException) {
      throw RuntimeException("Not an RFC 3339 date: $date", e)
    }
  }

  /**
   * Parse a date from ISO-8601 formatted string. It expects a format
   * [yyyy-MM-dd|yyyyMMdd][T(hh:mm[:ss[.fffffffff]]|hhmm[ss[.fffffffff]])]?[Z|[+-]hh:mm]]
   *
   * @param date ISO string to parse in the appropriate format.
   * @return the parsed date
   */
  @JvmStatic
  fun parseWithNanos(date: String): NanoDate {
    return try {
      var offset = 0

      // extract year
      val year = parseInt(date, offset, 4.let { offset += it; offset })
      if (checkOffset(date, offset, '-')) {
        offset += 1
      }

      // extract month
      val month = parseInt(date, offset, 2.let { offset += it; offset })
      if (checkOffset(date, offset, '-')) {
        offset += 1
      }

      // extract day
      val day = parseInt(date, offset, 2.let { offset += it; offset })
      // default time value
      var hour = 0
      var minutes = 0
      var seconds = 0
      var nanoseconds = 0 // always use 0 otherwise returned date will include nanoseconds of current
      // time

      // if the value has no time component (and no time zone), we are done
      val hasT = checkOffset(date, offset, 'T')
      if (!hasT && date.length <= offset) {
        val calendar: Calendar = GregorianCalendar(year, month - 1, day)
        return NanoDate(calendar.time)
      }
      if (hasT) {

        // extract hours, minutes, seconds and milliseconds
        hour = parseInt(date, 1.let { offset += it; offset }, 2.let { offset += it; offset })
        if (checkOffset(date, offset, ':')) {
          offset += 1
        }
        minutes = parseInt(date, offset, 2.let { offset += it; offset })
        if (checkOffset(date, offset, ':')) {
          offset += 1
        }
        // second and milliseconds can be optional
        if (date.length > offset) {
          val c = date[offset]
          if (c != 'Z' && c != '+' && c != '-') {
            seconds = parseInt(date, offset, 2.let { offset += it; offset })
            if (seconds > 59 && seconds < 63) {
              seconds = 59 // truncate up to 3 leap seconds
            }
            // nanoseconds can be optional in the format
            if (checkOffset(date, offset, '.')) {
              offset += 1
              val endOffset = indexOfNonDigit(date, offset + 1) // assume at least one digit
              val parseEndOffset = Math.min(endOffset, offset + 9) // parse up to 9 digits
              val fraction = parseInt(date, offset, parseEndOffset)
              nanoseconds = (Math.pow(10.0, (9 - (parseEndOffset - offset)).toDouble()) * fraction).toInt()
              offset = endOffset
            }
          }
        }
      }

      // extract timezone
      require(date.length > offset) { "No time zone indicator" }
      val timezone: TimeZone
      val timezoneIndicator = date[offset]
      if (timezoneIndicator == 'Z') {
        timezone = TIMEZONE_Z
      } else if (timezoneIndicator == '+' || timezoneIndicator == '-') {
        val timezoneOffset = date.substring(offset)
        // 18-Jun-2015, tatu: Minor simplification, skip offset of "+0000"/"+00:00"
        if ("+0000" == timezoneOffset || "+00:00" == timezoneOffset) {
          timezone = TIMEZONE_Z
        } else {
          // 18-Jun-2015, tatu: Looks like offsets only work from GMT, not UTC...
          //    not sure why, but it is what it is.
          val timezoneId = GMT_ID + timezoneOffset
          timezone = TimeZone.getTimeZone(timezoneId)
          val act = timezone.id
          if (act != timezoneId) {
            /* 22-Jan-2015, tatu: Looks like canonical version has colons, but we may be given
                         *    one without. If so, don't sweat.
                         *   Yes, very inefficient. Hopefully not hit often.
                         *   If it becomes a perf problem, add 'loose' comparison instead.
                         */
            val cleaned = act.replace(":", "")
            if (cleaned != timezoneId) {
              throw IndexOutOfBoundsException(
                "Mismatching time zone indicator: "
                  + timezoneId
                  + " given, resolves to "
                  + timezone.id)
            }
          }
        }
      } else {
        throw IndexOutOfBoundsException(
          "Invalid time zone indicator '$timezoneIndicator'")
      }
      val calendar: Calendar = GregorianCalendar(timezone)
      calendar.isLenient = false
      calendar[Calendar.YEAR] = year
      calendar[Calendar.MONTH] = month - 1
      calendar[Calendar.DAY_OF_MONTH] = day
      calendar[Calendar.HOUR_OF_DAY] = hour
      calendar[Calendar.MINUTE] = minutes
      calendar[Calendar.SECOND] = seconds
      calendar[Calendar.MILLISECOND] = 0
      val time = calendar.time.time * 1000000 + nanoseconds
      NanoDate(time)
      // If we get a ParseException it'll already have the right message/offset.
      // Other exception types can convert here.
    } catch (e: IndexOutOfBoundsException) {
      throw RuntimeException("Not an RFC 3339 date: $date", e)
    } catch (e: IllegalArgumentException) {
      throw RuntimeException("Not an RFC 3339 date: $date", e)
    }
  }

  /**
   * Check if the expected character exist at the given offset in the value.
   *
   * @param value the string to check at the specified offset
   * @param offset the offset to look for the expected character
   * @param expected the expected character
   * @return true if the expected character exist at the given offset
   */
  private fun checkOffset(value: String, offset: Int, expected: Char): Boolean {
    return offset < value.length && value[offset] == expected
  }

  /**
   * Parse an integer located between 2 given offsets in a string
   *
   * @param value the string to parse
   * @param beginIndex the start index for the integer in the string
   * @param endIndex the end index for the integer in the string
   * @return the int
   * @throws NumberFormatException if the value is not a number
   */
  @Throws(NumberFormatException::class)
  private fun parseInt(value: String, beginIndex: Int, endIndex: Int): Int {
    if (beginIndex < 0 || endIndex > value.length || beginIndex > endIndex) {
      throw NumberFormatException(value)
    }
    // use same logic as in Integer.parseInt() but less generic we're not supporting negative
    // values
    var i = beginIndex
    var result = 0
    var digit: Int
    if (i < endIndex) {
      digit = Character.digit(value[i++], 10)
      if (digit < 0) {
        throw NumberFormatException(
          "Invalid number: " + value.substring(beginIndex, endIndex))
      }
      result = -digit
    }
    while (i < endIndex) {
      digit = Character.digit(value[i++], 10)
      if (digit < 0) {
        throw NumberFormatException(
          "Invalid number: " + value.substring(beginIndex, endIndex))
      }
      result *= 10
      result -= digit
    }
    return -result
  }

  /**
   * Zero pad a number to a specified length
   *
   * @param buffer buffer to use for padding
   * @param value the integer value to pad if necessary.
   * @param length the length of the string we should zero pad
   */
  private fun padInt(buffer: StringBuilder, value: Int, length: Int) {
    val strValue = Integer.toString(value)
    for (i in length - strValue.length downTo 1) {
      buffer.append('0')
    }
    buffer.append(strValue)
  }

  /**
   * Zero right pad a number to a specified length
   *
   * @param buffer buffer to use for padding
   * @param value the integer value to pad if necessary.
   * @param length the length of the string we should zero pad
   */
  private fun padLong(buffer: StringBuilder, value: Long, length: Int) {
    val strValue = java.lang.Long.toString(value)
    buffer.append(strValue)
    for (i in length - strValue.length downTo 1) {
      buffer.append('0')
    }
  }

  /**
   * Returns the index of the first character in the string that is not a digit, starting at
   * offset.
   */
  private fun indexOfNonDigit(string: String, offset: Int): Int {
    for (i in offset until string.length) {
      val c = string[i]
      if (c < '0' || c > '9') {
        return i
      }
    }
    return string.length
  }
}
