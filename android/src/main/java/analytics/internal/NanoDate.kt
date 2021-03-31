package analytics.internal

import java.util.*

class NanoDate @JvmOverloads constructor(private val nanos: Long = NanoClock.currentTimeNanos()) : Date(nanos / 1000000) {
  /*
     * Java Genetic Algorithm Library (@__identifier__@).
     * Copyright (c) @__year__@ Franz Wilhelmstötter
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
     *
     * Author:
     *    Franz Wilhelmstötter (franz.wilhelmstoetter@gmail.com)
     */
  class NanoClock {
    /**
     * This returns the nanosecond-based instant, measured from 1970-01-01T00:00Z (UTC). This
     * method will return valid values till the year 2262.
     *
     * @return the nanosecond-based instant, measured from 1970-01-01T00:00Z (UTC)
     */
    private fun nanos(): Long {
      return System.nanoTime() + OFFSET_NANOS
    }

    companion object {
      /**
       * Jenetics' NanoClock to get higher resolution timestamps, pruned to our needs. Forked from
       * this file
       * https://github.com/jenetics/jenetics/blob/master/jenetics/src/main/java/io/jenetics/util/NanoClock.java
       */
      private val EPOCH_NANOS = System.currentTimeMillis() * 1000000
      private val NANO_START = System.nanoTime()
      private val OFFSET_NANOS = EPOCH_NANOS - NANO_START
      fun currentTimeNanos(): Long {
        return NanoClock().nanos()
      }
    }
  }

  constructor(d: Date) : this(d.time * 1000000) {}

  fun nanos(): Long {
    return nanos
  }

  override fun equals(other: Any?): Boolean {
    if (other is NanoDate) {
      return other.nanos() == nanos()
    } else if (other is Date) {
      return super.equals(other) && nanos % 1000000 == 0L
    }
    return false
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + nanos.hashCode()
    return result
  }
}
