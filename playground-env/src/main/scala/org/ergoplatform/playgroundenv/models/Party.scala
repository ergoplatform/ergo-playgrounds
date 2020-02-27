package org.ergoplatform.playgroundenv.models

import org.ergoplatform.compiler.ErgoScalaCompiler
import org.ergoplatform.playgroundenv.dsl.ObjectGenerators

trait Party {

  def name: String
  def wallet: Wallet

  def generateUnspentBoxes(
    toSpend: Long,
    tokensToSpend: List[TokenAmount] = List()
  ): Unit

  def selectUnspentBoxes(
    toSpend: Long,
    tokensToSpend: List[TokenAmount] = List()
  ): List[InputBox]

  def printUnspentAssets(): Unit
}

class DummyPartyImpl(blockchain: BlockchainSimulation, override val name: String)
  extends Party {

  override def wallet: Wallet =
    DummyWalletImpl(blockchain.context, s"$name Wallet")

  override def generateUnspentBoxes(
    toSpend: Long,
    tokensToSpend: List[TokenAmount]
  ): Unit =
    println(
      s"....$name: Generating unspent boxes for $toSpend nanoERGs and tokens: $tokensToSpend"
    )

  override def selectUnspentBoxes(
    toSpend: Long,
    tokensToSpend: List[TokenAmount]
  ): List[InputBox] =
    List(
      InputBox(
        toSpend,
        tokensToSpend,
        ErgoScalaCompiler.contract(wallet.getAddress.pubKey).ergoTree
      )
    )

  override def printUnspentAssets(): Unit =
    println(s"....$name: Unspent coins: XXX nanoERGs; tokens: (tokenName -> tokenAmount)")

}

object DummyPartyImpl {

  def apply(blockchain: BlockchainSimulation, name: String): DummyPartyImpl =
    new DummyPartyImpl(blockchain, name)
}
