package org.tritsch.scala.wordpath

import scala.annotation.tailrec

import scala.io.Source
import java.util.concurrent.{Executors, ExecutorService}

import com.weiglewilczek.slf4s.Logging

import org.tritsch.scala.util.Profiler

/**
 * Collecting stats and results while looking for all
 * solutions.
 */

class Solution {
  var from: String = ""
  var to: String = ""

  var pool: List[String] = Nil
  var poolElaspseTime: Long = 0
  var maxDepth: Long = 0

  var paths: List[List[String]] = List()
  var pathsElapseTime: Long = 0
  var nodesVisited: Long = 0
}

object Solutions {
  var dictLoadElapseTime: Long = 0
  var allNeighborsElapseTime: Long = 0

  var all: List[Solution] = List()
  var allElapseTime: Long = 0

  var findAll: Boolean = true
}

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

class Finder(solution: Solution, allNeighbors: Map[String, List[String]]) extends Runnable with Logging {
  @tailrec
  final def buildPool(fromWords: List[String], toWord: String, currentPool: List[String], allNeighbors: Map[String, List[String]], depth: Int): (List[String], Int) = {
    val neighbors = (for(from <- fromWords) yield allNeighbors(from).diff(currentPool)).flatten.distinct
    val newPool = currentPool.union(neighbors)
    logger.info("(" + depth + ") Building pool - Adding(" + neighbors.size + ") >" + neighbors.mkString(",") + "< to >" + currentPool.mkString(",") + "<...")
    if(newPool.contains(toWord)) {
      (newPool, depth)
    } else {
      buildPool(neighbors, toWord, newPool, allNeighbors, depth + 1)
    }
  }

  final def findAllPathsDF(fromWords: List[String], toWord: String, currentPath: List[String], pool: List[String], allNeighbors: Map[String, List[String]], depth: Int, solution: Solution) {
    solution.nodesVisited += 1
    logger.info("(" + depth + ") Look to find >" + toWord + "< in >" + fromWords.mkString(",") + "< coming from >" + currentPath.mkString("->") + "< after >" + solution.nodesVisited + "< nodes visited with >" + solution.paths.size + "< paths found (so far) ...")
    if(fromWords.contains(toWord)) {
      val path = (toWord :: currentPath).reverse
      logger.info("Found solution(" + path.size + "): " + path.mkString("->") + " ...")
      solution.paths ::= path
    } else if(depth >= solution.maxDepth) {
      logger.info("Max depth reached ...")
    } else {
      for(from <- fromWords; neighbors = allNeighbors(from).intersect(pool); if(Solutions.findAll || (solution.paths.size == 0))) {
	findAllPathsDF(neighbors, toWord, from :: currentPath, pool.diff(from :: currentPath), allNeighbors, depth + 1, solution)
      }      
    }
  }

  final def runDF() {
    logger.info("Building the pool ...")
    logger.info("(" + 0 + ") Building pool - Adding >" + solution.from + "< to >< ...")
    val pool = Profiler.time0 {
      buildPool(List(solution.from), solution.to, List(solution.from), allNeighbors, 1)
    }
    solution.pool = pool._1
    solution.maxDepth = pool._2
    solution.poolElaspseTime = Profiler.elapseTime

    logger.info("Done building the pool with >" + solution.pool.size + "< words and a max depth of >" + solution.maxDepth + "< ...")
    logger.debug(solution.pool.mkString(","))

    Profiler.time0 {
      findAllPathsDF(List(solution.from), solution.to, List(), solution.pool, allNeighbors, 0, solution)
    }
    solution.pathsElapseTime = Profiler.elapseTime
  }

  final def run() {
    runDF()
  }
}

object FindPath extends Logging {
  val threadPool: ExecutorService = Executors.newFixedThreadPool(8)

  final def findNeighborsForWord(word: String, dictionary: List[String]): List[String] = {
    logger.debug("Finding neighbors for >" + word + "< ...")
    val neighbors = for(i <- 0 until word.length; pattern = word.updated(i, '.')) yield dictionary.filter(_.matches(pattern))
    neighbors.flatten.toList.distinct.diff(List(word))
  }

  final def findNeighbors(dictionary: List[String]): Map[String, List[String]] = {
    val neighbors = for(word <- dictionary) yield findNeighborsForWord(word, dictionary)
    dictionary.zip(neighbors).toMap
  }

  final def main(args: Array[String]): Unit = { 
    assert(args.length == 3, "Need 3 parameters - Usage: FindPath <pairs> <dictionary> <all>")
    val pairsFileName = args(0)
    val dictFileName = args(1)
    Solutions.findAll = args(2).toBoolean
    logger.info("Looking for all path(s) >" + Solutions.findAll + "< ...")

    logger.info("Loading pairs from >" + pairsFileName + "< ...")
    val pairs = Source.fromFile(pairsFileName).getLines.map(_.split('\t')).toList

    logger.info("Loading dictionary >" + dictFileName + "< ...")
    val dictionary = Profiler.time0 {
      Source.fromFile(dictFileName).getLines.filter(_.length == (pairs.head)(0).length).map(_.toLowerCase).toList.distinct
    }
    Solutions.dictLoadElapseTime = Profiler.elapseTime
    logger.info("Loaded >" + dictionary.size + "< words ...")
    logger.trace(dictionary.mkString(","))
    
    logger.info("Finding neighbors ...")
    val allNeighbors = Profiler.time0 {
      findNeighbors(dictionary)
    }
    Solutions.allNeighborsElapseTime = Profiler.elapseTime
    logger.trace(allNeighbors.mkString(","))

    logger.info("Processing pairs ...")
    Profiler.time0 {
      for(p <- pairs; fromWord = p(0); toWord = p(1); if(fromWord != toWord)) {
	assert(fromWord.length == toWord.length, ">" + fromWord + "< and >" + toWord + "< need to be of equal length")
	assert(fromWord != toWord, ">" + fromWord + "< and >" + toWord + "< need to be different")
	assert(dictionary.contains(fromWord) && dictionary.contains(toWord), ">" + fromWord + "< and >" + toWord + "< need to be in the dictionary")

	logger.info("Searching solution from >" + fromWord + "< to >" + toWord + "< ...")
	val solution = new Solution()
	Solutions.all ::= solution
	solution.from = fromWord
	solution.to = toWord

	// threadPool.execute(new Finder(solution, allNeighbors))	
        val f: Finder = new Finder(solution, allNeighbors); f.run
      }
      threadPool.shutdown()
    }
    Solutions.allElapseTime = Profiler.elapseTime

    logger.info("Show solutions ...")
    println("Loaded dictionary >" + dictFileName + "< of size >" + dictionary.size + "< in >" + Solutions.dictLoadElapseTime + "< (msec) ...")
    println("Found all neighbors in >" + Solutions.allNeighborsElapseTime + "< (msec) ...")
    println("Processed pairs >" + pairsFileName + "< of size >" + pairs.size + "< and found >" + Solutions.all.size + "< solutions in >" + Solutions.allElapseTime + "< (msec) ...")
    for(s <- Solutions.all) {
      println("From >" + s.from + "< to >" + s.to + "< got >" + s.paths.size + "< solutions with >" + s.nodesVisited + "< nodes visited in >" + s.pathsElapseTime + "< (msec) on a pool of size >" + s.pool.size + "< that was build in >" + s.poolElaspseTime + "< (msec) with a maxDepth of >" + s.maxDepth + "< ...")
      for(p <- s.paths) {
	println("  Path(" + p.size + "): " + p.mkString("->"))
      }      
    }    
  }
}

