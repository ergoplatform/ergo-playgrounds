package org.ergoplatform

import org.ergoplatform.playgrounds.dsl.{BoxDsl, GeneratorsDsl, TransactionDsl, TypesDsl}
import sigmastate.Values.SValue

import scala.language.implicitConversions

object playground
  extends GeneratorsDsl
  with TypesDsl
  with BoxDsl
  with TransactionDsl {

  val MinTxFee: Long = 1000 * 1000
  val MinErg: Long   = 1000 * 1000

  def pk(sigmaProp: SigmaProp): SValue = ???

}
