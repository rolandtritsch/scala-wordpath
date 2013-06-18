package org.tritsch.scala.wordpath

import org.scalatest._

class FindPathSpec extends FlatSpec {
  val dictFileName = "/usr/share/dict/words"

  "Loading the dictionary" should "load 4994 words" in {
    val dict = FindPath.loadingDictionary(dictFileName, 4)
    assert(dict.size == 4994)
  }
}
