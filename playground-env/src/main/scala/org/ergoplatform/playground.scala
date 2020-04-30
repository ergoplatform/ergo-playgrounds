package org.ergoplatform

import org.ergoplatform.playgroundenv.dsl.{
  BoxDsl,
  GeneratorsDsl,
  TransactionDsl,
  TypesDsl
}
import sigmastate.Values.{SigmaPropConstant, SigmaPropValue}
import sigmastate.eval.CompiletimeIRContext

object playground extends GeneratorsDsl with TypesDsl with BoxDsl with TransactionDsl {

  implicit override protected def IR = new CompiletimeIRContext()

  val MinTxFee: Long = 1000 * 1000
  val MinErg: Long   = 1000 * 1000

  def pk(sigmaProp: SigmaProp): SigmaPropValue = SigmaPropConstant(sigmaProp)

}
