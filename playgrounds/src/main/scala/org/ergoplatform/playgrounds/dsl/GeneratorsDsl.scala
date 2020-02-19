package org.ergoplatform.playgrounds.dsl

import org.ergoplatform.ErgoBox.TokenId
import org.ergoplatform.playgrounds.models.{BlockchainSimulation, NaiveBlockchainSimulation, NaiveWallet, Wallet}
import sigmastate.basics.DLogProtocol.ProveDlog
import sigmastate.eval.CSigmaProp
import sigmastate.eval.Extensions.ArrayOps
import sigmastate.interpreter.CryptoConstants
import special.collection.Coll
import special.sigma.SigmaProp

trait GeneratorsDsl {

  def newBlockChainSimulation: BlockchainSimulation = NaiveBlockchainSimulation()

  def newTokenId: Coll[Byte] =
    Array.fill(TokenId.size)((scala.util.Random.nextInt(256) - 128).toByte).toColl

  def newWallet: Wallet = NaiveWallet(ObjectGenerators.newSigmaProp)

}

object ObjectGenerators {

  def newSigmaProp: SigmaProp =
    CSigmaProp(ProveDlog(CryptoConstants.dlogGroup.createRandomElement()))

}
