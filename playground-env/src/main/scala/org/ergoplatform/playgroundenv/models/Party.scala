package org.ergoplatform.playgroundenv.models

import org.ergoplatform.playgroundenv.dsl.ObjectGenerators

trait Party {

  def name: String
  def wallet: Wallet

}

class NaiveParty(ctx: BlockchainContext, override val name: String) extends Party {

  override def wallet: Wallet =
    NaiveWallet(ctx, ObjectGenerators.newSigmaProp, s"$name Wallet")
}

object NaiveParty {
  def apply(ctx: BlockchainContext, name: String): NaiveParty = new NaiveParty(ctx, name)
}
