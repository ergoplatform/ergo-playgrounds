package org.ergoplatform.playgroundenv.models

import org.ergoplatform.compiler.ErgoScalaCompiler
import org.ergoplatform.playgroundenv.dsl.ObjectGenerators

trait Party {

  def name: String
  def wallet: Wallet

  def generateUnspentBoxes(
    toSpend: Long,
    tokensToSpend: List[TokenInfo] = List()
  ): Unit

  def selectUnspentBoxes(
    toSpend: Long,
    tokensToSpend: List[TokenInfo] = List()
  ): List[InputBox]

  def printUnspentAssets(): Unit
}

class NaiveParty(blockchain: BlockchainSimulation, override val name: String)
  extends Party {

  override def wallet: Wallet =
    NaiveWallet(blockchain.context, ObjectGenerators.newSigmaProp, s"$name Wallet")

  override def generateUnspentBoxes(
    toSpend: Long,
    tokensToSpend: List[TokenInfo]
  ): Unit =
    println(
      s"....$name: Generating unspent boxes for $toSpend nanoERGs and tokens: $tokensToSpend"
    )

  override def selectUnspentBoxes(
    toSpend: Long,
    tokensToSpend: List[TokenInfo]
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

object NaiveParty {

  def apply(blockchain: BlockchainSimulation, name: String): NaiveParty =
    new NaiveParty(blockchain, name)
}
