package org.tritsch.scala.wordpath

import scala.annotation.tailrec

import scala.io.Source
import java.util.concurrent.{Executors, ExecutorService}

import com.typesafe.scalalogging.slf4j.Logging

/**
 * Look for the shortest path >from< word >to< word by
 * doing a breadth first search to build a/the pool of
 * words that will connect >from< with >to< and then do
 * a recursive depth first on this pool to find all paths
 * >from< >to<.
 *
 * To build the pool we will find all neighbors to
 * a given list of words (the list of words found
 * on the previous level). To avoid loops, we will
 * remove all words that we have already found (on
 * all levels) from this list, will then add this
 * list to the pool of words already found and will
 * finally check if >to< is in the pool. If not we
 * need to go one level down, otherwise we are done.
 *
 * After we have the pool we do a recursive depth
 * first to find a/all paths >from< >to<.
 *
 * We find a path by starting with a list of >from<
 * words (initially just one). For every word in the
 * list we find all neighbors and intersect it with
 * the pool. We then check, if this list contains the
 * >to< word. If it does we add the >to< word to the
 * currentPath (and with that make it a representation
 * of a valid path/solution) and add this path to the
 * list of solutions. Otherwise we add the current word
 * to the currentPath and go down one level.
 */

object FindPath extends Logging {
  final def findNeighborsForWord(word: String, dictionary: List[String]): List[String] = {
    logger.debug("Finding neighbors for >" + word + "< ...")
    val neighbors = for(i <- 0 until word.length; pattern = word.updated(i, '.')) yield dictionary.filter(_.matches(pattern))
    neighbors.flatten.toList.distinct.diff(List(word))
  }

  final def findNeighbors(dictionary: List[String]): Map[String, List[String]] = {
    val neighbors = for(word <- dictionary) yield findNeighborsForWord(word, dictionary)
    dictionary.zip(neighbors).toMap
  }

  final def loadingDictionary(fileName: String, wordLength: Int): List[String] = {
    Source.fromFile(fileName).getLines.filter(_.length == wordLength).map(_.toLowerCase).toList.distinct
  }

  final def main(args: Array[String]): Unit = {
    assert(args.length == 3, "Need 3 parameters - Usage: FindPath <from> <to> <dictionary>")
    val fromWord = args(0);val toWord = args(1); val dictFileName = args(2)
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
  }
}
