package org.ergoplatform.playgroundenv.dsl

import org.ergoplatform.appkit.Parameters
import org.ergoplatform._
import org.ergoplatform.playgroundenv.utils.TransactionOperations

trait TransactionDsl extends BoxDsl {

  // TODO: make a box for a change
  // TODO check input and output sums checks out and miner's fee (check the change is 0)

  implicit val addressEncoder = ErgoAddressEncoder(
    ErgoAddressEncoder.TestnetNetworkPrefix
  )

  def Transaction(
    inputs: List[ErgoBox],
    outputs: List[ErgoBoxCandidate],
    fee: Long
  ): UnsignedErgoLikeTransaction = {
    TransactionOperations.buildUnsignedErgoTx(
      inputs.toIndexedSeq,
      IndexedSeq(),
      outputs,
      fee,
      None,
      0
    )
  }

  def Transaction(
    inputs: List[ErgoBox],
    outputs: List[ErgoBoxCandidate],
    fee: Long,
    sendChangeTo: Address
  ): UnsignedErgoLikeTransaction = {
    TransactionOperations.buildUnsignedErgoTx(
      inputs.toIndexedSeq,
      IndexedSeq(),
      outputs,
      fee,
      Some(P2PKAddress(sendChangeTo.proveDlog)),
      0
    )
  }

}
