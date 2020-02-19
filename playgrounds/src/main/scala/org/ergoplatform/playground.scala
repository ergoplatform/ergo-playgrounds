package org.ergoplatform

import org.ergoplatform.playgrounds._
import sigmastate.Values.SValue

import scala.language.implicitConversions

object playground
  extends Generators
  with Types
  with Wallet
  with Box
  with Transaction {

  val MinTxFee: Long = 1000 * 1000
  val MinErg: Long   = 1000 * 1000

  def pk(sigmaProp: SigmaProp): SValue = ???

}
