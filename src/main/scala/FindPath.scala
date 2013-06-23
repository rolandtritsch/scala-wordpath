/*
__________       .__                     .___
\______   \ ____ |  | _____    ____    __| _/
 |       _//  _ \|  | \__  \  /    \  / __ |
 |    |   (  <_> )  |__/ __ \|   |  \/ /_/ |
 |____|_  /\____/|____(____  /___|  /\____ |
        \/                 \/     \/      \/
Copyright (c), 2013, roland@tritsch.org
http://www.tritsch.org
*/

package org.tritsch.scala.wordpath

import scala.io.Source

import scala.concurrent._
import scala.concurrent.duration._
import ExecutionContext.Implicits.global

import com.typesafe.scalalogging.slf4j.Logging

/** Find *A* shortest path between two words by exchanging one letter at the time.
  *
  * This implementation is using a breadth first search.
  *
  * @author roland@tritsch.org
  *
  * @todo Implement *ALL* shortest path. Find a solution for the mem problem.
  * @todo Put more logging in place. More trace messages.
  * @todo Experiment with depth first searches. Probably finding the longest path.
  * @todo Experiment with tailrec. Not keeping the stack frames should also free up heap.
  * @todo Experiment with par to do some of the mapping in parallel. So far using par makes it perfrorm worse.
  */
object FindPath extends Logging {
  /** @return all neighbors for a given word */
  final def findNeighborsForWord(word: String, dictionary: List[String]): List[String] = {
    logger.trace("Finding neighbors for >" + word + "< ...")
    val neighbors = for(i <- 0 until word.length; pattern = word.updated(i, '.')) yield dictionary.filter(_.matches(pattern))
    // Obviously the pattern match found the word itself. Remove it ...
    neighbors.flatten.toList.distinct.diff(List(word))
  }

  /** @return a map with all neighbors for all words */
  final def findNeighbors(dictionary: List[String]): Map[String, List[String]] = {
    logger.debug("Finding all neighbors in dictionary of size >" + dictionary.size + "< ...")
    val allNeighbors = dictionary.map(word => future(findNeighborsForWord(word, dictionary)))
    dictionary.zip(allNeighbors.map(Await.result(_, Duration(60, SECONDS)))).toMap
  }

  /** @return all words of a given length from the dictionary */
  final def loadingDictionary(fileName: String, wordLength: Int): List[String] = {
    logger.debug("Loading words of length >" + wordLength + "< from >" + fileName + "< ...")
    Source.fromFile(fileName).getLines.filter(_.length == wordLength).map(_.toLowerCase).toList.distinct
  }

  /** @return a path from a given word to a given word */
  final def findPath(toWord: String, allNeighbors: Map[String, List[String]], currentPaths: List[List[String]]): List[String] = {
    logger.debug("FindPath (PathLength/NumberOfPaths): " + currentPaths(0).size + "/" + currentPaths.size)
    currentPaths.find(_.last == toWord).getOrElse {
      val nextPaths = currentPaths.map(path =>
        allNeighbors(path.last).diff(path).map(n => path :+ n) // the diff is to eliminate loops
      ).flatten
      findPath(toWord, allNeighbors, dedup(nextPaths))
    }
  }

  /** This function dedups the solution space.
    *
    * Means it looks for pathes that have resulted in the same last word (so far) and eliminates
    * all but one. With this we will acclerate to find *A* shortest path.
    *
    * @note To find *ALL* shortest path you cannot use this trick. And then (so far) I am struggling
    * to make all possible solutions fit into main mem.
    *
    * @return a list of possible solution pathes removing all dublicated pathes to get to the last word (so far)
    */
  final def dedup(paths: List[List[String]]): List[List[String]] = {
    val uniqueEndpointsSoFar = paths.map(_.last).distinct
    uniqueEndpointsSoFar.map(word => paths.find(_.last == word).get)
  }

  /** Main method: Calls all the functions to to get the job done
    * {{{
    * >fromWord< the word to start with
    * >toWord< the word to look for
    * >fileName< the file name of the dictionary to use
    * }}}
    * @return prints the path from the >fromWord< to the >toWord<
    * @example `sbt "run flux alem /usr/share/dict/words"`
    */
  final def main(args: Array[String]): Unit = {
    require(args.length == 3, "Need 3 parameters - Usage: FindPath <from> <to> <dictionary>")
    val fromWord = args(0); val toWord = args(1); val dictFileName = args(2)
    require(fromWord.length == toWord.length, "<from> and <to> must be of the same length")

    logger.info("Looking for first shortest path from >" + fromWord + "< to >" + toWord + "< ...")
    logger.info("Loading dictionary >" + dictFileName + "< ...")
    val dictionary = loadingDictionary(dictFileName, fromWord.length)
    assert(dictionary.contains(fromWord) && dictionary.contains(toWord), ">" + fromWord + "< and >" + toWord + "< need to be in the dictionary")

    logger.info("Finding neighbors ...")
    val allNeighbors = findNeighbors(dictionary)

    logger.info("Find path ...")
    val start = System.nanoTime
    val path = findPath(toWord, allNeighbors, List(List(fromWord)))
    logger.info("Found solution in >" + (System.nanoTime-start)*1e-6 + "< msecs ...")
    println(path.mkString("->"))

    logger.info("... done!")
    System.exit(0)
  }
}
