package org.ergoplatform.playgrounds

import org.ergoplatform.playground.SigmaProp
import org.ergoplatform.playgrounds.models.{InputBox, SignedTransaction, TokenInfo}

trait BlockchainSimulation {

  def send(tx: SignedTransaction): Unit = {}

  def makeUnspentBoxesFor(pk: SigmaProp, toSpend: Long): List[InputBox] = ???

  def makeUnspentBoxesFor(
    pk: SigmaProp,
    toSpend: Long,
    tokenToSpend: TokenInfo
  ): List[InputBox] = ???

  case class PKBlockchainStats(pk: SigmaProp, totalNanoErgs: Long, totalToken: TokenInfo)

  def getStatsFor(pk: SigmaProp): PKBlockchainStats = ???

}
