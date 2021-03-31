package analytics

/** Represents all stats for a [Analytics] instance at a single point in time.  */
class StatsSnapshot(
  /** The time at which the snapshot was created.  */
  val timestamp: Long,
  /** Number of times we've flushed events to our servers.  */
  private val flushCount: Long,
  /** Number of events we've flushed to our servers.  */
  private val flushEventCount: Long,
  /**
   * Number of operations sent to all bundled integrations, including lifecycle events and
   * flushes.
   */
  private val integrationOperationCount: Long,
  /**
   * Total time to run operations on all bundled integrations, including lifecycle events and
   * flushes.
   */
  private val integrationOperationDuration: Long,
  integrationOperationDurationByIntegration: Map<String, Long>
) {
  /**
   * Average time to run operations on all bundled integrations, including lifecycle events and
   * flushes.
   */
  private val integrationOperationAverageDuration: Float = if (integrationOperationCount == 0L) 0F else integrationOperationDuration.toFloat() / integrationOperationCount

  /** Total time to run operations, including lifecycle events and flushes, by integration.  */
  private val integrationOperationDurationByIntegration: Map<String, Long> = integrationOperationDurationByIntegration
  override fun toString(): String {
    return ("StatsSnapshot{"
      + "timestamp="
      + timestamp
      + ", flushCount="
      + flushCount
      + ", flushEventCount="
      + flushEventCount
      + ", integrationOperationCount="
      + integrationOperationCount
      + ", integrationOperationDuration="
      + integrationOperationDuration
      + ", integrationOperationAverageDuration="
      + integrationOperationAverageDuration
      + ", integrationOperationDurationByIntegration="
      + integrationOperationDurationByIntegration
      + '}')
  }

}
