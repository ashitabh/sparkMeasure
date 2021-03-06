package ch.cern.sparkmeasure

import java.nio.file.Paths

import org.apache.spark.SparkConf
import org.apache.spark.scheduler.SparkListenerApplicationEnd
import org.slf4j.LoggerFactory

/**
 * Spark Measure package: proof-of-concept tool for measuring Spark performance metrics
 *   This is based on using Spark Listeners as data source and collecting metrics in a ListBuffer
 *   The list buffer is then transformed into a DataFrame for analysis
 *
 *  Stage Metrics: collects and aggregates metrics at the end of each stage
 *  Task Metrics: collects data at task granularity
 *
 * Use modes:
 *   Interactive mode from the REPL
 *   Flight recorder mode: records data and saves it for later processing
 *
 * Supported languages:
 *   The tool is written in Scala, but it can be used both from Scala and Python
 *
 * Example usage for stage metrics:
 * val stageMetrics = ch.cern.sparkmeasure.StageMetrics(spark)
 * stageMetrics.runAndMeasure(spark.sql("select count(*) from range(1000) cross join range(1000) cross join range(1000)").show)
 *
 * for task metrics:
 * val taskMetrics = ch.cern.sparkmeasure.TaskMetrics(spark)
 * spark.sql("select count(*) from range(1000) cross join range(1000) cross join range(1000)").show()
 * val df = taskMetrics.createTaskMetricsDF()
 *
 * To use in flight recorder mode add:
 * --conf spark.extraListeners=ch.cern.sparkmeasure.FlightRecorderStageMetrics
 *
 * Created by Luca.Canali@cern.ch, March 2017
 *
 */

class FlightRecorderStageMetrics(conf: SparkConf) extends StageInfoRecorderListener {

  lazy val logger = LoggerFactory.getLogger(this.getClass.getName)

  val metricsFileName = conf.get("spark.executorEnv.stageMetricsFileName", "/tmp/stageMetrics.serialized")
  val metricsFormat = conf.get("spark.executorEnv.stageMetricsFormat", "java")
  val fullPath = Paths.get(metricsFileName).toString

  /** when the application stops serialize the content of stageMetricsData into a file in the driver's filesystem */
  override def onApplicationEnd(applicationEnd: SparkListenerApplicationEnd): Unit = {
    logger.warn(s"application end, timestamp = ${applicationEnd.time}")
    if (metricsFormat.equalsIgnoreCase("json")) {
      Utils.writeSerializedJSON(fullPath, stageMetricsData)
    } else {
      Utils.writeSerialized(fullPath, stageMetricsData)
    }
    logger.warn(s"Stagemetrics data serialized to $fullPath")
  }
}

class FlightRecorderTaskMetrics(conf: SparkConf) extends TaskInfoRecorderListener {

  lazy val logger = LoggerFactory.getLogger(this.getClass.getName)

  val metricsFileName = conf.get("spark.executorEnv.taskMetricsFileName", "/tmp/taskMetrics.serialized")
  val metricsFormat = conf.get("spark.executorEnv.taskMetricsFormat", "java")
  val fullPath = Paths.get(metricsFileName).toString

  /** when the application stops serialize the content of taskMetricsData into a file in the driver's filesystem */
  override def onApplicationEnd(applicationEnd: SparkListenerApplicationEnd): Unit = {
    logger.warn(s"application end, timestamp = ${applicationEnd.time}")
    if (metricsFormat.equalsIgnoreCase("json")) {
      Utils.writeSerializedJSON(fullPath, taskMetricsData)
    } else {
      Utils.writeSerialized(fullPath, taskMetricsData)
    }
    logger.warn(s"Taskmetrics data serialized to $fullPath")
  }
}
