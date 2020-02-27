package org.ergoplatform.playgroundenv.models

import org.ergoplatform.playgroundenv.dsl.ObjectGenerators
import special.sigma.SigmaProp

class DummyWalletImpl(ctx: BlockchainContext, pk: SigmaProp, override val name: String)
  extends Wallet {

  override def getAddress: Address = Address(pk)

  override def sign(tx: UnsignedTransaction): SignedTransaction = {
    println(s"......$name: Signing transaction ShortTxDesc")
    val id = ObjectGenerators.newErgoId
    val outs = tx.outputs.map { b =>
      OutBox(id, b.value, b.tokens, b.registers, b.script)
    }
    SignedTransaction(id, tx.inputs, outs)
  }
}

object DummyWalletImpl {

  def apply(ctx: BlockchainContext, pk: SigmaProp, name: String): DummyWalletImpl =
    new DummyWalletImpl(ctx, pk, name)
}
