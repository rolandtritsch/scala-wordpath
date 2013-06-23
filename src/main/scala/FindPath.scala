package org.tritsch.scala.wordpath

import scala.io.Source

import scala.concurrent._
import scala.concurrent.duration._
import ExecutionContext.Implicits.global

import com.typesafe.scalalogging.slf4j.Logging

/*
 * Look for *A* shortest path >from< word >to< word by
 * doing a breadth first search.
 *
 * NOTE: Looking for *ALL* shortest path is an excerice for the reader :)
 */

object FindPath extends Logging {
  final def findNeighborsForWord(word: String, dictionary: List[String]): List[String] = {
    logger.debug("Finding neighbors for >" + word + "< ...")
    val neighbors = for(i <- 0 until word.length; pattern = word.updated(i, '.')) yield dictionary.filter(_.matches(pattern))
    val result = neighbors.flatten.toList.distinct.diff(List(word))
    logger.trace(result.size + ": " + result.mkString(","))
    result
  }

  final def findNeighbors(dictionary: List[String]): Map[String, List[String]] = {
    val neighbors = dictionary.map(word => future(findNeighborsForWord(word, dictionary)))
    dictionary.zip(neighbors.map(Await.result(_, Duration(60, SECONDS)))).toMap
  }

  final def loadingDictionary(fileName: String, wordLength: Int): List[String] = {
    Source.fromFile(fileName).getLines.filter(_.length == wordLength).map(_.toLowerCase).toList.distinct
  }

  final def findPath(toWord: String, allNeighbors: Map[String, List[String]], currentPaths: List[List[String]]): List[String] = {
    currentPaths.find(_.last == toWord).getOrElse {
      val nextPaths = currentPaths.map(path =>
        allNeighbors(path.last).diff(path).par.map(n => path :+ n) // the diff is to eliminate loops
      ).flatten
      findPath(toWord, allNeighbors, dedup(nextPaths))
    }
  }

  /* This function dedups the solution space.
   *
   * Means it looks for pathes that have resulted in the same last word (so far) and eliminates
   * all but one. With this we will acclerate to find *A* shortest path.
   *
   * NOTE: To find *ALL* shortest path you cannot use this trick. And then (so far) I am struggling
   * to make all possible solutions fit into main mem.
   */
  def dedup(paths: List[List[String]]): List[List[String]] = {
    val uniqueEndpointsSoFar = paths.map(_.last).distinct
    uniqueEndpointsSoFar.map(word => paths.find(_.last == word).get)
  }

  final def main(args: Array[String]): Unit = {
    assert(args.length == 3, "Need 3 parameters - Usage: FindPath <from> <to> <dictionary>")
    val fromWord = args(0); val toWord = args(1); val dictFileName = args(2)
    assert(fromWord.length == toWord.length, "<from> and <to> must be of the same length")

    logger.info("Looking for first shortest path from >" + fromWord + "< to >" + toWord + "< ...")

    logger.info("Loading dictionary >" + dictFileName + "< ...")
    val dictionary = loadingDictionary(dictFileName, fromWord.length)
    logger.info("Loaded >" + dictionary.size + "< words ...")
    logger.trace(dictionary.mkString(","))
    assert(dictionary.contains(fromWord) && dictionary.contains(toWord), ">" + fromWord + "< and >" + toWord + "< need to be in the dictionary")

    logger.info("Finding neighbors ...")
    val allNeighbors = findNeighbors(dictionary)
    logger.trace(allNeighbors.mkString(","))
    assert(dictionary.size == allNeighbors.size, "Every word in the dictionary is suppose to have 0-N neighbors")

    logger.info("Find path ...")
    val start = System.nanoTime
    val path = findPath(toWord, allNeighbors, List(List(fromWord)))
    logger.info("Found solution in >" + (System.nanoTime-start)*1e-6 + "< msecs ...")
    println(path.mkString("->"))

    logger.info("... done!")
  }
}
