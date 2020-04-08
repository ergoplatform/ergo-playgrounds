package org.ergoplatform.playgroundenv.dsl

import org.ergoplatform.appkit.Parameters
import org.ergoplatform._
import org.ergoplatform.playgroundenv.utils.TransactionOperations

trait TransactionDsl extends BoxDsl {

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
