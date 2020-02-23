package org.ergoplatform.playgrounds.models

import org.ergoplatform.compiler.ErgoContract
import sigmastate.Values.{ErgoTree, SValue, SigmaPropValue}

case class InputBox(
  value: Long,
  tokens: List[TokenInfo],
  script: ErgoTree
)

object InputBox {

  def apply(value: Long, script: ErgoContract): InputBox =
    new InputBox(value, List(), script.ergoTree)
}
