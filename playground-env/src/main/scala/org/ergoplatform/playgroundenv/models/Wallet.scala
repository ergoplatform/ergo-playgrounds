package org.ergoplatform.playgroundenv.models

import org.ergoplatform.playgroundenv.dsl.ObjectGenerators
import special.sigma.SigmaProp

trait Wallet {

  def name: String

  def getAddress: Address

  def sign(tx: UnsignedTransaction): SignedTransaction

}
