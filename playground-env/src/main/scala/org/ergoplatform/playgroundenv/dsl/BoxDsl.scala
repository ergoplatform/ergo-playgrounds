package org.ergoplatform.playgroundenv.dsl

import org.ergoplatform.ErgoBox.{NonMandatoryRegisterId, TokenId}
import org.ergoplatform.playgroundenv.models.TokenAmount
import org.ergoplatform.{ErgoBox, ErgoBoxCandidate}
import scorex.crypto.hash.Digest32
import sigmastate.SType
import sigmastate.Values.{ByteArrayConstant, EvaluatedValue}
import sigmastate.eval.Extensions._
import sigmastate.eval._

import scala.language.implicitConversions

trait BoxDsl extends TypesDsl {

  val R4 = ErgoBox.R4
  val R5 = ErgoBox.R5
  val R6 = ErgoBox.R6
  val R7 = ErgoBox.R7
  val R8 = ErgoBox.R8
  val R9 = ErgoBox.R9

  def Box(value: Long, script: ErgoContract): ErgoBoxCandidate = {
    require(value > 0, s"box value shoulde be > 0, got $value")
    new ErgoBoxCandidate(value, script.ergoTree, 0)
  }

  def Box(value: Long, token: TokenAmount, script: ErgoContract): ErgoBoxCandidate = {
    require(value > 0, s"box value shoulde be > 0, got $value")
    new ErgoBoxCandidate(
      value,
      script.ergoTree,
      0,
      Array[(TokenId, Long)]((Digest32 @@ token.token.tokenId.toArray, token.tokenAmount)).toColl
    )
  }

  private def liftVal[T](v: T): EvaluatedValue[SType] = v match {
    case ba: Array[Byte] => ByteArrayConstant(ba)
  }

  def Box[T](
    value: Long,
    register: (NonMandatoryRegisterId, T),
    script: ErgoContract
  ): ErgoBoxCandidate = {
    require(value > 0, s"box value shoulde be > 0, got $value")
    new ErgoBoxCandidate(
      value,
      script.ergoTree,
      0,
      Array[(TokenId, Long)]().toColl,
      Map((register._1, liftVal(register._2)))
    )
  }

  def Box(
    value: Long,
    token: (TokenInfo, Long),
    register: (NonMandatoryRegisterId, Any),
    script: ErgoContract
  ): ErgoBoxCandidate = {
    require(value > 0, s"box value shoulde be > 0, got $value")
    new ErgoBoxCandidate(
      value,
      script.ergoTree,
      0,
      Array[(TokenId, Long)]((Digest32 @@ token._1.tokenId.toArray, token._2)).toColl,
      Map((register._1, liftVal(register._2)))
    )
  }
}
