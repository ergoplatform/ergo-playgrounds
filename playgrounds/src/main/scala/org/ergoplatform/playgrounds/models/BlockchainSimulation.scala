package org.ergoplatform.playgrounds.models

import org.ergoplatform.playgrounds.dsl.ObjectGenerators
import special.sigma.SigmaProp

case class PKBlockchainStats(pk: SigmaProp, totalNanoErgs: Long, totalToken: TokenInfo) {
//  override def toString: String = s"$pk, $totalNanoErgs, $totalToken"
}

trait BlockchainSimulation {

  def send(tx: SignedTransaction): Unit

  def makeUnspentBoxesFor(pk: SigmaProp, toSpend: Long): List[InputBox]

  def makeUnspentBoxesFor(
    pk: SigmaProp,
    toSpend: Long,
    tokenToSpend: TokenInfo
  ): List[InputBox]

  def getUnspentAssetsFor(pk: SigmaProp): PKBlockchainStats

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
    List(InputBox(toSpend, List(tokenToSpend), pk))

  override def getUnspentAssetsFor(pk: SigmaProp): PKBlockchainStats =
    PKBlockchainStats(pk, 1000L, TokenInfo(ObjectGenerators.newErgoId, 100L))
}
