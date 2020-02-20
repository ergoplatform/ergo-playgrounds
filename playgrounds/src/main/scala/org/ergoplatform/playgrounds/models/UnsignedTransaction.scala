package org.ergoplatform.playgrounds.models

case class UnsignedTransaction(inputs: Seq[InputBox], outputs: Seq[OutBoxCandidate])
