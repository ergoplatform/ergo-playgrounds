package org.ergoplatform.playground.examples.test

import org.ergoplatform.playground.examples.AssetsAtomicExchangePlayground
import org.scalatest.PropSpec

class AssetAtomicExchangePlaygroundSpec extends PropSpec {

  property("run") {
    val p = AssetsAtomicExchangePlayground
    println(p)
  }
}
