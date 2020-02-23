package org.ergoplatform.playgrounds.examples.test

import org.ergoplatform.playgrounds.examples.AssetsAtomicExchangePlayground
import org.scalatest.PropSpec

class AssetAtomicExchangePlaygroundSpec extends PropSpec {

  property("run") {
    val p = AssetsAtomicExchangePlayground
    println(p)
  }
}
