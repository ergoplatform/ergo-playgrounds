package org.ergoplatform

import org.ergoplatform.ErgoBox.{NonMandatoryRegisterId, RegisterId}
import org.ergoplatform.playgrounds.{InputBox, OutBox, OutBoxCandidate}
import sigmastate.Values.SValue

import scala.language.implicitConversions

object playground
  extends playgrounds.Generators
  with playgrounds.Wallet {

  type Coll[A]   = special.collection.Coll[A]
  type SigmaProp = special.sigma.SigmaProp

  val MinTxFee: Long = 1000 * 1000
  val MinErg: Long   = 1000 * 1000

  def pk(sigmaProp: SigmaProp): SValue = ???

  val R4 = ErgoBox.R4

  case class TokenInfo(tokenId: Coll[Byte], tokenAmount: Long)

  object TokenInfo {
    implicit def apply(t: (Coll[Byte], Long)): TokenInfo = new TokenInfo(t._1, t._2)
  }

  def Box(value: Long, script: SValue): OutBoxCandidate                   = ???
  def Box(value: Long, token: TokenInfo, script: SValue): OutBoxCandidate = ???

  def Box(
    value: Long,
    register: (NonMandatoryRegisterId, Any),
    script: SValue
  ): OutBoxCandidate = ???

  def Box(
    value: Long,
    token: (Coll[Byte], Long),
    register: (NonMandatoryRegisterId, Any),
    script: SValue
  ): OutBoxCandidate = ???

  trait UnsignedTransaction {
    def inputs: Seq[InputBox]
    def outputs: Seq[OutBoxCandidate]
  }

  trait SignedTransaction {
    def inputs: Seq[InputBox]
    def outputs: Seq[OutBox]
  }

  // TODO check input and output sums checks out and miner's fee (check the change is 0)
  def Transaction(
    inputs: List[InputBox],
    outputs: List[OutBoxCandidate],
    fee: Long
  ): UnsignedTransaction = ???

  implicit class ListOps(l: List[InputBox]) {
    def totalValue: Long       = l.map(_.value).sum
    def totalTokenAmount: Long = l.map(_.value).sum
  }

  implicit def outBoxToInputBox(in: OutBox): InputBox = ???

}
