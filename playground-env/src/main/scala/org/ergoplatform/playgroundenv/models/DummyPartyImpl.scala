package org.ergoplatform.playgroundenv.models

import org.ergoplatform.ErgoBox

class DummyPartyImpl(blockchain: DummyBlockchainSimulationImpl, override val name: String)
  extends Party {

  override val wallet: Wallet =
    new DummyWalletImpl(blockchain, s"$name Wallet")

  override def generateUnspentBoxes(
    toSpend: Long,
    tokensToSpend: List[TokenAmount]
  ): Unit = {
    blockchain.generateUnspentBoxesFor(wallet.getAddress, toSpend, tokensToSpend)
    println(
      s"....$name: Generating unspent boxes for $toSpend nanoERGs and tokens: ${TokenAmount
        .prettyprintTokens(tokensToSpend)}"
    )
  }

  override def selectUnspentBoxes(
    toSpend: Long,
    tokensToSpend: List[TokenAmount]
  ): List[ErgoBox] =
    blockchain.selectUnspentBoxesFor(wallet.getAddress, toSpend, tokensToSpend)

  override def printUnspentAssets(): Unit = {
    val coins  = blockchain.getUnspentCoinsFor(wallet.getAddress)
    val tokens = blockchain.getUnspentTokensFor(wallet.getAddress)
    println(
      s"....$name: Unspent coins: $coins nanoERGs; tokens: ${TokenAmount.prettyprintTokens(tokens)}"
    )
  }

}

object DummyPartyImpl {

  def apply(blockchain: DummyBlockchainSimulationImpl, name: String): DummyPartyImpl =
    new DummyPartyImpl(blockchain, name)
}
