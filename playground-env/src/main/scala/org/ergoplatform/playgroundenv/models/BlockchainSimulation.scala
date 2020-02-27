package org.ergoplatform.playgroundenv.models

import special.sigma.SigmaProp

case class PKBlockchainStats(
  pk: SigmaProp,
  totalNanoErgs: Long,
  totalToken: TokenAmount
) {
//  override def toString: String = s"$pk, $totalNanoErgs, $totalToken"
}

trait BlockchainSimulation {

  def context: BlockchainContext

  def newParty(name: String): Party

  def send(tx: SignedTransaction): Unit
}

case class DummyBlockchainSimulationImpl(scenarioName: String)
  extends BlockchainSimulation {

  val context: BlockchainContext = DummyBlockchainContextImpl(this)

  override def newParty(name: String): Party = {
    println(s"..$scenarioName: Creating new party: $name")
    DummyPartyImpl(this, name)
  }

  override def send(tx: SignedTransaction): Unit =
    println(s"..$scenarioName: Accepting transaction ShortTxDesc to the blockchain")
}
