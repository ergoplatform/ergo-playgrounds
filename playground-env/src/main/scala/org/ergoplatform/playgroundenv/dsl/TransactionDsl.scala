package org.ergoplatform.playgroundenv.dsl

import org.ergoplatform.appkit.Parameters
import org.ergoplatform._
import org.ergoplatform.playgroundenv.utils.TransactionOperations

trait TransactionDsl extends BoxDsl {

  // TODO: make a box for a change
  // TODO check input and output sums checks out and miner's fee (check the change is 0)

  def Transaction(
    inputs: List[ErgoBox],
    outputs: List[ErgoBoxCandidate],
    fee: Long
  ): UnsignedErgoLikeTransaction = {

    val feeBox = new ErgoBoxCandidate(
      fee,
      ErgoScriptPredef.feeProposition(Parameters.MinerRewardDelay),
      0
    )

    val txInputs = inputs.map { ib =>
      new UnsignedInput(ib.id)
    }.toIndexedSeq
    UnsignedErgoLikeTransaction(txInputs, (outputs ++ Seq(feeBox)).toIndexedSeq)
  }

  def Transaction(
    inputs: List[ErgoBox],
    outputs: List[ErgoBoxCandidate],
    fee: Long,
    sendChangeTo: ErgoContract
  ): UnsignedErgoLikeTransaction = {
    implicit val addressEncoder = ErgoAddressEncoder(
      ErgoAddressEncoder.TestnetNetworkPrefix
    )
    TransactionOperations.buildUnsignedErgoTx(
      inputs.toIndexedSeq,
      IndexedSeq(),
      outputs,
      fee,
      Pay2SHAddress(sendChangeTo.ergoTree),
      0
    )
  }

}
