package org.ergoplatform.playgrounds.models

import org.ergoplatform.playgrounds.models.Types.ErgoId

case class SignedTransaction(id: ErgoId, inputs: Seq[InputBox], outputs: Seq[OutBox])
