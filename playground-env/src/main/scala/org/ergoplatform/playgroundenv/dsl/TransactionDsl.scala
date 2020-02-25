package org.ergoplatform.playgroundenv.dsl

import org.ergoplatform.compiler.ErgoContract
import org.ergoplatform.playgroundenv.models.{
  InputBox,
  OutBoxCandidate,
  UnsignedTransaction
}

trait TransactionDsl {

  // TODO: make a box for a change
  // TODO check input and output sums checks out and miner's fee (check the change is 0)

  def Transaction(
    inputs: List[InputBox],
    outputs: List[OutBoxCandidate],
    fee: Long
  ): UnsignedTransaction = UnsignedTransaction(inputs, outputs)

  def Transaction(
    inputs: List[InputBox],
    outputs: List[OutBoxCandidate],
    fee: Long,
    sendChangeTo: ErgoContract
  ): UnsignedTransaction = UnsignedTransaction(inputs, outputs)

}
