package org.ergoplatform.playgroundenv.models

import special.sigma.SigmaProp

class TransactionBuilder(val ctx: BlockchainContext) {

  // TODO check input and output sums checks out and miner's fee (check the change is 0)
  def makeTransaction(
    inputs: List[InputBox],
    outputs: List[OutBoxCandidate],
    fee: Long
  ): UnsignedTransaction = UnsignedTransaction(inputs, outputs)

  def makeTransaction(
    inputs: List[InputBox],
    outputs: List[OutBoxCandidate],
    fee: Long,
    sendChangeTo: SigmaProp
  ): UnsignedTransaction =
    // TODO: make a box for a change
    UnsignedTransaction(inputs, outputs)

}
