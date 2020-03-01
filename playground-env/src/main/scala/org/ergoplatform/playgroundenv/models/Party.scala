package org.ergoplatform.playgroundenv.models

import org.ergoplatform.ErgoBox

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
  ): List[ErgoBox]

  def printUnspentAssets(): Unit
}
