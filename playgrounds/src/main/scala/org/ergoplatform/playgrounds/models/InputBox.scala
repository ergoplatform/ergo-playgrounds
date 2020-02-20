package org.ergoplatform.playgrounds.models

import sigmastate.Values.{SValue, SigmaPropValue}

case class InputBox(
  value: Long,
  tokens: List[TokenInfo],
  script: SigmaPropValue
)

object InputBox {

  def apply(value: Long, script: SigmaPropValue): InputBox =
    new InputBox(value, List(), script)
}
