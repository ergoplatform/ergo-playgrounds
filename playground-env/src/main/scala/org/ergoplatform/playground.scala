package org.ergoplatform

import org.ergoplatform.playgroundenv.dsl.{BoxDsl, GeneratorsDsl, TypesDsl}
import sigmastate.Values.{SigmaPropConstant, SigmaPropValue}

object playground extends GeneratorsDsl with TypesDsl with BoxDsl {

  val MinTxFee: Long = 1000 * 1000
  val MinErg: Long   = 1000 * 1000

  def pk(sigmaProp: SigmaProp): SigmaPropValue = SigmaPropConstant(sigmaProp)

}
