package com.distributed.application.hw2

import com.distributed.application.hw2.PSQB2Dpt.freqItems
import org.apache.log4j.{Level, Logger}
import org.apache.spark.SparkConf
import org.apache.spark.mllib.fpm.PrefixSpan
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession;

/**
  *
  * @author Create by xuantang
  * @date on 4/23/18
  */
object PSQB2Bnd {
  val AppName = "PSQB2Dpt"
  val Master = "local[*]"
  val Memory = "spark.executor.memory"
  val dataset = "new_"
  def main(args: Array[String]): Unit = {

    Logger.getLogger("org").setLevel(Level.ERROR)
    val conf = new SparkConf()
      .setAppName(AppName)
      .setMaster("local[*]")
    val ss = SparkSession.builder
      .config(conf)
      .getOrCreate()

    val data = ss.sparkContext.textFile("data/seq_trade_" + dataset + "bndno_b.txt")

    val rdd1: RDD[Array[String]] = data.map(line => line.trim.split(";"))
    val sequences: RDD[Array[Array[String]]] = rdd1.map(row => row.iterator.map(item => item.replace(" ", "")
      .trim.split(",")).toArray)
    val count = sequences.count()
    sequences.cache()
    val arr =  Array(2, 4, 6, 8, 10, 12)
    var costArr: Array[Long] = Array()
    for (minSupport <- arr) {
      // cal time
      val start = System.currentTimeMillis()
      // calculate
      freqItems(sequences, minSupport, count)

      val end = System.currentTimeMillis()
      costArr = costArr :+ (end - start)
    }
    costArr.foreach(println(_))
  }


  def freqItems(sequences: RDD[Array[Array[String]]], minSupport: Int, count: Long): Unit = {
    val count = sequences.count()
    sequences.cache()
    val prefixSpan = new PrefixSpan()
      .setMinSupport(minSupport / count.toDouble)

    val model = prefixSpan.run(sequences)
    model.freqSequences.collect().foreach { freqSequence =>
      println(
        freqSequence.sequence.map(_.mkString("[", ", ", "]")).mkString("[", ", ", "]") +
          ", " + freqSequence.freq)
    }
  }
}
