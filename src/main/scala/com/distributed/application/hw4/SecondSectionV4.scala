package com.distributed.application.hw4

import org.apache.log4j.{Level, Logger}
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.types._
import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import org.apache.spark.streaming.{Seconds, StreamingContext}

/**
  *
  * @author Create by xuantang
  * @date on 12/26/17
  */
object SecondSectionV4 {
  val FILENAME = "/d1/documents/DistributeCompute/dblp-out.xml"

  val AppName = "SecondSection"
  val Master = "local[16]"
  val ExecutorMemory = "3g"
  val DriverMemory = "2048m"
  val NumExecutors = "10"
  val ExecutorCores = "3"
  val Parallelism = "60"   // num-executors * executor-cores * 2


  def main(args: Array[String]): Unit = {
    Logger.getLogger("org").setLevel(Level.ERROR)
    if (args.length < 4) {
      println("You need to input args: [seconds] [filename] [host] [port]")
      return
    }
    val seconds = args(0).toInt
    val filename = args(1).toString
    val hosts = args(2).toString
    val port = args(3).toInt
    val conf = new SparkConf()
      .setAppName(AppName)
     // .setMaster(Master)
    //      .set("spark.executor.memory", ExecutorMemory)
    //      .set("spark.driver.memory", DriverMemory)
    //      .set("spark.default.parallelism", Parallelism)

    val ss = SparkSession.builder
      .config(conf)
      .getOrCreate()

    val ssc = new StreamingContext(ss.sparkContext, Seconds(seconds))

    val INPUT = ssc.socketTextStream(hosts, port)

    var df:DataFrame = null

    INPUT.foreachRDD(tmp => {
      val strings: Array[String] = tmp.collect()
      if (strings.length > 0) {
        val spark = SparkSession.builder.config(tmp.sparkContext.getConf).getOrCreate()
        // cached input data
        if (df == null) {
          df = spark.read.parquet(filename.toString)
          df.cache()
        }
        val AUTHOR = strings(0).substring(strings(0).indexOf("coauthor:") + 9).trim
        val authors = df.filter(row =>
          if (row(1) != null) {
            row(1).toString.toLowerCase.contains(AUTHOR.toLowerCase)
          } else false
        )
        val rdd: RDD[Row] = authors.toJavaRDD.rdd
        val rdd1 = rdd.map(line => line(1).toString.replace("WrappedArray(", "").replace(")", ""))
        val rdd2 = rdd1.flatMap(_.split(",")).map(word => word.trim).filter(!_.contains(AUTHOR))
        // Cache
        rdd2.cache()
        // transform
        val rdd4 = rdd2.map(author => ((AUTHOR, author), 1))
          .reduceByKey(_ + _).sortBy(_._2, ascending = false)
        // count
        println("======================================================")
        println(rdd2.distinct().count())
        rdd4.collect().foreach(line => {
          println(line._1._2 + " : " + line._2)
        })
        println("======================================================")
      }
    })
    ssc.start()             // Start the computation
    ssc.awaitTermination()  // Wait for the computation to terminate
  }
}
