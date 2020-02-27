package org.ergoplatform.playgroundenv.models

import org.ergoplatform.ErgoBox
import org.ergoplatform.ErgoBox.BoxId
import org.ergoplatform.compiler.ErgoContract
import scorex.util.ModifierId
import sigmastate.Values.{ErgoTree, SValue, SigmaPropValue}

case class InputBox(
  id: BoxId,
  value: Long,
  tokens: List[TokenAmount],
  script: ErgoTree
) {

  def toErgoBox(creationHeight: Int = 0): ErgoBox =
//    ErgoBox(value, script, creationHeight, )
    ???
}

//object InputBox {
//
//  def apply(value: Long, script: ErgoContract): InputBox =
//    new InputBox(value, List(), script.ergoTree)
//}
