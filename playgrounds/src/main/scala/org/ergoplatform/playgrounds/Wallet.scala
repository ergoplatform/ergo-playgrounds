package org.ergoplatform.playgrounds

import org.ergoplatform.playgrounds.models.{SignedTransaction, UnsignedTransaction}

trait Wallet {

  def getAddress: Address = ???

  def sign(tx: UnsignedTransaction): SignedTransaction = ???

}
