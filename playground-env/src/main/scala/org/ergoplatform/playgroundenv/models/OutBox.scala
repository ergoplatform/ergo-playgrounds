package org.ergoplatform.playgroundenv.models

import org.ergoplatform.ErgoBox.{BoxId, NonMandatoryRegisterId}
import org.ergoplatform.ErgoBoxCandidate
import org.ergoplatform.compiler.ErgoContract
import sigmastate.Values.ErgoTree
import special.collection.Coll

case class OutBoxCandidate(
  value: Long,
  tokens: List[TokenAmount],
  registers: List[(NonMandatoryRegisterId, Any)],
  script: ErgoTree
) {

  def toErgoBoxCandidate: ErgoBoxCandidate =
//    new ErgoBoxCandidate(value, script, )
    ???

}

object OutBoxCandidate {

  def apply(
    value: Long,
    script: ErgoContract
  ): OutBoxCandidate = new OutBoxCandidate(value, List(), List(), script.ergoTree)

}

case class OutBox(
  id: BoxId,
  value: Long,
  tokens: List[TokenAmount],
  registers: List[(NonMandatoryRegisterId, Any)],
  script: ErgoTree
) {}
