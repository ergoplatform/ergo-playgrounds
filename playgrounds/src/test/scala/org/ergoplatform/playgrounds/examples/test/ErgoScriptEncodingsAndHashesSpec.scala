package org.ergoplatform.playgrounds.examples.test

import org.ergoplatform.playgroundenv.utils.ErgoScriptCompiler
import org.scalatest.PropSpec

class ErgoScriptEncodingsAndHashesSpec extends PropSpec {
  property("test script encodings and hashes from URL") {
    ErgoScriptCompiler.printErgoScriptEncodingsAndHashesFromUrl(
      "https://raw.githubusercontent.com/kettlebell/rust_hello_world/main/ergoscript/pool_contract.es"
    )
  }
}
