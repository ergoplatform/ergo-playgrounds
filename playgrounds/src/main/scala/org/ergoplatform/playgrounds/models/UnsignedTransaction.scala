package org.ergoplatform.playgrounds.models

trait UnsignedTransaction {
  def inputs: Seq[InputBox]
  def outputs: Seq[OutBoxCandidate]
}
