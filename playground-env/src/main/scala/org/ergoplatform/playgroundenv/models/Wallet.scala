package org.ergoplatform.playgroundenv.models

import org.ergoplatform.{ErgoLikeTransaction, UnsignedErgoLikeTransaction}

trait Wallet {

  def name: String

  def getAddress: Address

  def sign(tx: UnsignedErgoLikeTransaction): ErgoLikeTransaction

}
