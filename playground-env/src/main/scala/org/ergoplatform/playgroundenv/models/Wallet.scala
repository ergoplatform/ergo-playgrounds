package org.ergoplatform.playgroundenv.models

import org.ergoplatform.playgroundenv.dsl.ObjectGenerators
import special.sigma.SigmaProp

trait Wallet {

  def name: String

  def getAddress: Address

  def sign(tx: UnsignedTransaction): SignedTransaction

}

class NaiveWallet(ctx: BlockchainContext, pk: SigmaProp, override val name: String)
  extends Wallet {

  override def getAddress: Address = Address(pk)

  override def sign(tx: UnsignedTransaction): SignedTransaction = {
    val id = ObjectGenerators.newErgoId
    val outs = tx.outputs.map { b =>
      OutBox(id, b.value, b.tokens, b.registers, b.script)
    }
    SignedTransaction(id, tx.inputs, outs)
  }
}

object NaiveWallet {

  def apply(ctx: BlockchainContext, pk: SigmaProp, name: String): NaiveWallet =
    new NaiveWallet(ctx, pk, name)
}
