package org.tritsch.scala.wordpath

import org.scalatest._

class FindPathSpec extends FlatSpec {
  final val dictFileName = "/usr/share/dict/words"
  final val dictionary = FindPath.loadingDictionary(dictFileName, 4)
  final val expectedDictSize = 4994

  final val neighborsForTupi = FindPath.findNeighborsForWord("tupi", dictionary)
  final val expectedNeighborsForTupi = List("topi", "turi", "tuwi")

  final val allNeighbors = FindPath.findNeighbors(dictionary)

  "Loading the dictionary" should "load " + expectedDictSize + " words" in {
    assert(dictionary.size == expectedDictSize)
  }

  "Finding the neighbor for tupi" should "return " + expectedNeighborsForTupi.mkString(",") in {
    assert(neighborsForTupi.size == expectedNeighborsForTupi.size)
    assert(neighborsForTupi == expectedNeighborsForTupi)
  }

  "Finding all neighbors" should "show that all words have 0 to N neighbors" in {
    assert(allNeighbors.size == dictionary.size)
  }

  it should "return " + expectedNeighborsForTupi.mkString(",") + " for tupi" in {
    assert(allNeighbors("tupi") == expectedNeighborsForTupi)
  }

  it should "show that zyga has no neighbors" in {
    assert(allNeighbors("zyga").isEmpty)
  }

  "Finding the path from flux to flux" should "return flux" in {
    assert(FindPath.findPath("flux", allNeighbors, List(List("flux"))) == List("flux"))
  }

  final val expectedPaths = List(
    List("flex", "alex", "alem"),
    List("flux", "flex", "alex", "alem"),
    List("rial", "real", "feal", "foal", "foul", "foud"),
    List("dung", "dunt", "dent", "gent", "geet", "geez"),
    List("doeg", "dong", "song", "sing", "sink", "sick"),
    List("jehu", "jesu", "jest", "gest", "gent", "gena", "guna", "guha"),
//    List("broo", "brod", "brad", "arad", "adad", "adai", "admi", "ammi", "immi"),
    List("yagi", "yali", "pali", "palp", "paup", "plup", "blup"),
//    List("bitt", "butt", "burt", "bert", "berm", "germ", "geum", "meum"),
//    List("jina", "pina", "pint", "pent", "peat", "prat", "pray"),
    List("fike", "fake", "cake", "came", "camp"),
    List("flem", "alem")
  )
  for(p <- expectedPaths) {
    "Finding the path from " + p.head + " to " + p.last should "return " + p.mkString("->") in {
      assert(FindPath.findPath(p.last, allNeighbors, List(List(p.head))) === p)
    }
  }
}
