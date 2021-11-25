package analytics.compat

import kotlinx.serialization.json.JsonObject

interface JsonSerializable {
    fun serialize() : JsonObject
}
