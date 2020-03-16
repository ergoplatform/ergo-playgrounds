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
      s"....$name: Generating unspent boxes for $toSpend nanoERGs and tokens: $tokensToSpend"
    )
  }

  override def selectUnspentBoxes(
    toSpend: Long,
    tokensToSpend: List[TokenAmount]
  ): List[ErgoBox] =
    blockchain.selectUnspentBoxesFor(wallet.getAddress, toSpend, tokensToSpend)

  override def printUnspentAssets(): Unit =
    println(s"....$name: Unspent coins: XXX nanoERGs; tokens: (tokenName -> tokenAmount)")

}

object DummyPartyImpl {

  def apply(blockchain: DummyBlockchainSimulationImpl, name: String): DummyPartyImpl =
    new DummyPartyImpl(blockchain, name)
}
