package org.ergoplatform.playgroundenv.models

case class UnsignedTransaction(inputs: Seq[InputBox], outputs: Seq[OutBoxCandidate])
