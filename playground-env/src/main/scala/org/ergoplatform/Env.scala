package org.ergoplatform

import org.ergoplatform.playground.dsl.{BoxDsl, GeneratorsDsl, TypesDsl}
import sigmastate.Values.{SValue, SigmaPropConstant, SigmaPropValue}
import sigmastate.eval.CSigmaProp

import scala.language.implicitConversions

object Env extends GeneratorsDsl with TypesDsl with BoxDsl {

  val MinTxFee: Long = 1000 * 1000
  val MinErg: Long   = 1000 * 1000

  def pk(sigmaProp: SigmaProp): SigmaPropValue = SigmaPropConstant(sigmaProp)

}
