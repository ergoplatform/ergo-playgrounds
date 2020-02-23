package org.ergoplatform.playground.models

case class UnsignedTransaction(inputs: Seq[InputBox], outputs: Seq[OutBoxCandidate])
