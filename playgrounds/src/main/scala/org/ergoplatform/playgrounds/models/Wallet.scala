package org.ergoplatform.playgrounds.models

import special.sigma.SigmaProp

trait Wallet {

  def getAddress: Address

  def sign(tx: UnsignedTransaction): SignedTransaction

}

case class NaiveWallet(val pk: SigmaProp) extends Wallet {

  override def getAddress: Address = ???

  override def sign(tx: UnsignedTransaction): SignedTransaction = ???
}
