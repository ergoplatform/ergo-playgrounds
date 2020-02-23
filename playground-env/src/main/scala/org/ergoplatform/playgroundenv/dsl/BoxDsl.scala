package org.ergoplatform.playgroundenv.dsl

import org.ergoplatform.ErgoBox
import org.ergoplatform.ErgoBox.NonMandatoryRegisterId
import org.ergoplatform.compiler.ErgoContract
import org.ergoplatform.playgroundenv.models.{
  InputBox,
  OutBox,
  OutBoxCandidate,
  TokenInfo
}
import sigmastate.Values.{SValue, SigmaPropValue}

import scala.language.implicitConversions

trait BoxDsl extends TypesDsl {

  implicit def outBoxToInputBox(in: OutBox): InputBox =
    InputBox(in.value, in.tokens, in.script)

  implicit class ListOps(l: List[InputBox]) {
    def totalValue: Long       = l.map(_.value).sum
    def totalTokenAmount: Long = l.map(_.value).sum
  }

  val R4 = ErgoBox.R4

  def Box(value: Long, script: ErgoContract): OutBoxCandidate =
    OutBoxCandidate(value, script)

  def Box(value: Long, token: TokenInfo, script: ErgoContract): OutBoxCandidate =
    OutBoxCandidate(value, List(token), List(), script.ergoTree)

  def Box(
    value: Long,
    register: (NonMandatoryRegisterId, Any),
    script: ErgoContract
  ): OutBoxCandidate = OutBoxCandidate(value, List(), List(register), script.ergoTree)

  def Box(
    value: Long,
    token: (Coll[Byte], Long),
    register: (NonMandatoryRegisterId, Any),
    script: ErgoContract
  ): OutBoxCandidate =
    OutBoxCandidate(value, List(token), List(register), script.ergoTree)
}
