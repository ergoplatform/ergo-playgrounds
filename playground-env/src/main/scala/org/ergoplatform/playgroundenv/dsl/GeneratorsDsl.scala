package org.ergoplatform.playgroundenv.dsl

import org.ergoplatform.ErgoBox.TokenId
import org.ergoplatform.playgroundenv.models.{
  BlockchainContext,
  BlockchainSimulation,
  NaiveBlockchainSimulation,
  NaiveWallet,
  TransactionBuilder,
  Wallet
}
import sigmastate.basics.DLogProtocol.ProveDlog
import sigmastate.eval.CSigmaProp
import sigmastate.eval.Extensions.ArrayOps
import sigmastate.interpreter.CryptoConstants
import special.collection.Coll
import special.sigma.SigmaProp

trait GeneratorsDsl {

  def newBlockChainSimulation: BlockchainSimulation = NaiveBlockchainSimulation()

  def newTransactionBuilder(ctx: BlockchainContext): TransactionBuilder =
    new TransactionBuilder(ctx)

  def newTokenId: Coll[Byte] = ObjectGenerators.newErgoId

  def newWallet: Wallet = NaiveWallet(ObjectGenerators.newSigmaProp)

}

object ObjectGenerators {

  def newErgoId: Coll[Byte] =
    Array.fill(TokenId.size)((scala.util.Random.nextInt(256) - 128).toByte).toColl

  def newSigmaProp: SigmaProp =
    CSigmaProp(ProveDlog(CryptoConstants.dlogGroup.createRandomElement()))

}
