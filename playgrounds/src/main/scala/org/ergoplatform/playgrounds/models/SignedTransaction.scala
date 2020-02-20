package org.ergoplatform.playgrounds.models

trait SignedTransaction {
  def inputs: Seq[InputBox]
  def outputs: Seq[OutBox]
}
