package org.ergoplatform.playgrounds.examples.test

import org.scalatest.PropSpec
import org.ergoplatform.playgrounds.examples.DEXPlayground

class DEXPlaygroundSpec extends PropSpec {

  property("run") {
    val p = DEXPlayground
    println(p)
  }
}
