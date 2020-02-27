package org.ergoplatform.playgroundenv.models

import org.ergoplatform.ErgoBox.NonMandatoryRegisterId
import org.ergoplatform.compiler.ErgoContract
import sigmastate.Values.ErgoTree
import special.collection.Coll

case class OutBoxCandidate(
  value: Long,
  tokens: List[TokenAmount],
  registers: List[(NonMandatoryRegisterId, Any)],
  script: ErgoTree
) {}

object OutBoxCandidate {

  def apply(
    value: Long,
    script: ErgoContract
  ): OutBoxCandidate = new OutBoxCandidate(value, List(), List(), script.ergoTree)

}

case class OutBox(
  id: Coll[Byte],
  value: Long,
  tokens: List[TokenAmount],
  registers: List[(NonMandatoryRegisterId, Any)],
  script: ErgoTree
) {}
