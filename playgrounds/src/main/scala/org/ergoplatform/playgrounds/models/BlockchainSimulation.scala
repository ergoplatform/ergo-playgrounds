package org.ergoplatform.playgrounds.models

import special.sigma.SigmaProp

case class PKBlockchainStats(pk: SigmaProp, totalNanoErgs: Long, totalToken: TokenInfo)

trait BlockchainSimulation {

  def send(tx: SignedTransaction): Unit

  def makeUnspentBoxesFor(pk: SigmaProp, toSpend: Long): List[InputBox]

  def makeUnspentBoxesFor(
    pk: SigmaProp,
    toSpend: Long,
    tokenToSpend: TokenInfo
  ): List[InputBox]

  def getStatsFor(pk: SigmaProp): PKBlockchainStats

}

case class NaiveBlockchainSimulation() extends BlockchainSimulation {

  override def send(tx: SignedTransaction): Unit = {}

  override def makeUnspentBoxesFor(pk: SigmaProp, toSpend: Long): List[InputBox] =
    List(InputBox(toSpend, pk))

  override def makeUnspentBoxesFor(
    pk: SigmaProp,
    toSpend: Long,
    tokenToSpend: TokenInfo
  ): List[InputBox] =
    List(InputBox(toSpend, pk, List(tokenToSpend)))

  override def getStatsFor(pk: SigmaProp): PKBlockchainStats = ???
}
