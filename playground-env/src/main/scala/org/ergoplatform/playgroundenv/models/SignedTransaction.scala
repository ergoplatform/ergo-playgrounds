package org.ergoplatform.playgroundenv.models

import org.ergoplatform.playgroundenv.models.Types.ErgoId

case class SignedTransaction(id: ErgoId, inputs: Seq[InputBox], outputs: Seq[OutBox])
