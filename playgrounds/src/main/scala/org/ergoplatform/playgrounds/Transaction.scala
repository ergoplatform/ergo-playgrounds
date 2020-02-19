package org.ergoplatform.playgrounds

import org.ergoplatform.playgrounds.models.{InputBox, OutBox, OutBoxCandidate, UnsignedTransaction}

trait Transaction {

  // TODO check input and output sums checks out and miner's fee (check the change is 0)
  def Transaction(
                   inputs: List[InputBox],
                   outputs: List[OutBoxCandidate],
                   fee: Long
                 ): UnsignedTransaction = ???

}
