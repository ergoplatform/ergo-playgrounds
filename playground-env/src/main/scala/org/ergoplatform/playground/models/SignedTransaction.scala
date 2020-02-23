package org.ergoplatform.playground.models

import org.ergoplatform.playground.models.Types.ErgoId

case class SignedTransaction(id: ErgoId, inputs: Seq[InputBox], outputs: Seq[OutBox])
