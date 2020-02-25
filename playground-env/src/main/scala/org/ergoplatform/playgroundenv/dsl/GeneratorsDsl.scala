package org.ergoplatform.playgroundenv.dsl

import org.ergoplatform.ErgoBox.TokenId
import org.ergoplatform.playgroundenv.models.{
  BlockchainSimulation,
  NaiveBlockchainSimulation
}
import sigmastate.basics.DLogProtocol.ProveDlog
import sigmastate.eval.CSigmaProp
import sigmastate.eval.Extensions.ArrayOps
import sigmastate.interpreter.CryptoConstants
import special.collection.Coll
import special.sigma.SigmaProp

trait GeneratorsDsl {

  def newBlockChainSimulationScenario(scenarioName: String): BlockchainSimulation = {
    println(s"Creating blockchain simulation scenario $scenarioName")
    NaiveBlockchainSimulation(scenarioName)
  }

  def newTokenId: Coll[Byte] = ObjectGenerators.newErgoId
}

object ObjectGenerators {

  def newErgoId: Coll[Byte] =
    Array.fill(TokenId.size)((scala.util.Random.nextInt(256) - 128).toByte).toColl

  def newSigmaProp: SigmaProp =
    CSigmaProp(ProveDlog(CryptoConstants.dlogGroup.createRandomElement()))

}
