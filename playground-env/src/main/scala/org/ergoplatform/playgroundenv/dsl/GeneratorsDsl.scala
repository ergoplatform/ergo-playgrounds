package org.ergoplatform.playgroundenv.dsl

import org.ergoplatform.ErgoBox.TokenId
import org.ergoplatform.playgroundenv.models.{
  BlockchainSimulation,
  DummyBlockchainSimulationImpl,
  TokenInfo
}
import sigmastate.eval.Extensions.ArrayOps
import special.collection.Coll

trait GeneratorsDsl {

  def newBlockChainSimulationScenario(scenarioName: String): BlockchainSimulation = {
    println(s"Creating scenario: $scenarioName")
    DummyBlockchainSimulationImpl(scenarioName)
  }

}

object ObjectGenerators {

  def newErgoId: Coll[Byte] =
    Array.fill(TokenId.size)((scala.util.Random.nextInt(256) - 128).toByte).toColl

}
