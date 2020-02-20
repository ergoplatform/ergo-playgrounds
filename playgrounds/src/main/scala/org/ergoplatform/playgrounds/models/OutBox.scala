package org.ergoplatform.playgrounds.models

import org.ergoplatform.ErgoBox.NonMandatoryRegisterId
import sigmastate.Values.SigmaPropValue
import special.collection.Coll

case class OutBoxCandidate(
  value: Long,
  tokens: List[TokenInfo],
  registers: List[(NonMandatoryRegisterId, Any)],
  script: SigmaPropValue
) {}

object OutBoxCandidate {

  def apply(
    value: Long,
    script: SigmaPropValue
  ): OutBoxCandidate = new OutBoxCandidate(value, List(), List(), script)

}

case class OutBox(
  id: Coll[Byte],
  value: Long,
  tokens: List[TokenInfo],
  registers: List[(NonMandatoryRegisterId, Any)],
  script: SigmaPropValue
) {}
