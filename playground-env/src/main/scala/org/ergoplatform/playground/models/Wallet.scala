package org.ergoplatform.playground.models

import org.ergoplatform.playground.dsl.ObjectGenerators
import special.sigma.SigmaProp

trait Wallet {

  def getAddress: Address

  def sign(tx: UnsignedTransaction): SignedTransaction

}

case class NaiveWallet(val pk: SigmaProp) extends Wallet {

  override def getAddress: Address = Address(pk)

  override def sign(tx: UnsignedTransaction): SignedTransaction = {
    val id = ObjectGenerators.newErgoId
    val outs = tx.outputs.map { b =>
      OutBox(id, b.value, b.tokens, b.registers, b.script)
    }
    SignedTransaction(id, tx.inputs, outs)
  }
}
