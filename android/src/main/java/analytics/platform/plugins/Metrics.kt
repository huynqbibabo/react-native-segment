package analytics.platform.plugins

import analytics.BaseEvent
import analytics.DateSerializer
import analytics.utilities.EncodeDefaultsJson
import analytics.utilities.putInContext
import analytics.utilities.safeJsonArray
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonArray
import java.time.Instant

enum class MetricType(val type: Int) {
    Counter(0), // Not Verbose
    Gauge(1)    // Semi-verbose
}

@Serializable
data class Metric(
    var eventName: String = "",
    var metricName: String = "",
    var value: Double = 0.0,
    var tags: List<String> = emptyList(),
    var type: MetricType = MetricType.Counter,
    @Serializable(with = DateSerializer::class) var timestamp: Instant = Instant.now()
)

fun BaseEvent.addMetric(
    type: MetricType,
    name: String,
    value: Double,
    tags: List<String> = emptyList(),
    timestamp: Instant = Instant.now(),
) {
    val metric = Metric(
        eventName = this.type.name,
        metricName = name,
        value = value,
        tags = tags,
        type = type,
        timestamp = timestamp
    )

    val metrics = buildJsonArray {
        context["metrics"]?.safeJsonArray?.forEach {
            add(it)
        }
        add(EncodeDefaultsJson.encodeToJsonElement(Metric.serializer(), metric))
    }

    putInContext("metrics", metrics)
}
