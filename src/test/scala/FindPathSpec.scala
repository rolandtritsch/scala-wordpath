package org.tritsch.scala.wordpath

import org.scalatest._

class FindPathSpec extends FlatSpec {
  final val dictFileName = "/usr/share/dict/words"
  final val dictionary = FindPath.loadingDictionary(dictFileName, 4)
  final val expectedDictSize = 4994

  final val neighborsForTupi = FindPath.findNeighborsForWord("tupi", dictionary)
  final val expectedNeighborsForTupi = List("topi", "turi", "tuwi")

  final val allNeighbors = FindPath.findNeighbors(dictionary)

  final val resultPath = FindPath.findPath("flux", "alem", dictionary, allNeighbors)
  final val expectedPath = List("flux", "flex", "flem", "alem")

  "Loading the dictionary" should "load " + expectedDictSize + " words" in {
    assert(dictionary.size == expectedDictSize)
  }

  "Finding the neighbor for tupi" should "return topi, turi and tuwi" in {
    assert(neighborsForTupi.size == expectedNeighborsForTupi.size)
    assert(neighborsForTupi == expectedNeighborsForTupi)
  }

  "Finding all neighbors" should "should show that all words have 0 to N neighbors" in {
    assert(allNeighbors.size == dictionary.size)
  }

  it should "return topi, turi and tuwi for tupi" in {
    assert(allNeighbors("tupi") == expectedNeighborsForTupi)
  }

  it should "show that zyga has no neighbors" in {
    assert(allNeighbors("zyga").isEmpty)
  }

  "Finding the path from flux to alem" should "return " + expectedPath.mkString("->") in {
    assert(resultPath == expectedPath)
  }
}
