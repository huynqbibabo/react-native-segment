package analytics

import android.content.SharedPreferences

class BooleanPreference(private val preferences: SharedPreferences, private val key: String, private val defaultValue: Boolean) {
  fun get(): Boolean {
    return preferences.getBoolean(key, defaultValue)
  }

  fun set(value: Boolean) {
    preferences.edit().putBoolean(key, value).apply()
  }
}
