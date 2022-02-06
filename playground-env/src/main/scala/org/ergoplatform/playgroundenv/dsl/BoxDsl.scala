package org.ergoplatform.playgroundenv.dsl

import org.ergoplatform.ErgoBox.{NonMandatoryRegisterId, TokenId}
import org.ergoplatform.playgroundenv.models.TokenAmount
import org.ergoplatform.{ErgoBox, ErgoBoxCandidate}
import scorex.crypto.hash.Digest32
import sigmastate.SType
import sigmastate.Values.{
  BooleanConstant,
  ByteArrayConstant,
  ByteConstant,
  EvaluatedValue,
  GroupElementConstant,
  IntConstant,
  LongConstant,
  ShortConstant,
  SigmaBoolean,
  SigmaPropConstant,
  LongArrayConstant
}
import sigmastate.eval.Extensions._
import sigmastate.eval._
import special.sigma.GroupElement

import scala.language.implicitConversions

trait BoxDsl extends TypesDsl {

  val R4 = ErgoBox.R4
  val R5 = ErgoBox.R5
  val R6 = ErgoBox.R6
  val R7 = ErgoBox.R7
  val R8 = ErgoBox.R8
  val R9 = ErgoBox.R9

  private var currentHeight: Int = 0

  def setCurrentHeight(height: Int): Unit = {
    currentHeight = height
  }

  def Box(value: Long, script: ErgoContract): ErgoBoxCandidate = {
    require(value > 0, s"box value shoulde be > 0, got $value")
    new ErgoBoxCandidate(value, script.ergoTree, 0)
  }

  def Box(value: Long, token: TokenAmount, script: ErgoContract): ErgoBoxCandidate = {
    require(value > 0, s"box value shoulde be > 0, got $value")
    new ErgoBoxCandidate(
      value,
      script.ergoTree,
      currentHeight,
      Array[(TokenId, Long)]((Digest32 @@ token.token.tokenId.toArray, token.tokenAmount)).toColl
    )
  }

  private def liftVal[T](v: T): EvaluatedValue[SType] = v match {
    case ba: Array[Byte]  => ByteArrayConstant(ba)
    case by: Byte         => ByteConstant(by)
    case s: Short         => ShortConstant(s)
    case i: Int           => IntConstant(i)
    case l: Long          => LongConstant(l)
    case b: Boolean       => BooleanConstant(b)
    case ge: GroupElement => GroupElementConstant(ge)
    case sb: SigmaBoolean => SigmaPropConstant(sb)
    case sp: SigmaProp    => SigmaPropConstant(sp)
    case la: Array[Long]  => LongArrayConstant(la)
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
      currentHeight,
      Array[(TokenId, Long)]().toColl,
      Map((register._1, liftVal(register._2)))
    )
  }

  def Box(
    value: Long,
    registers: Map[NonMandatoryRegisterId, Any],
    script: ErgoContract
  ): ErgoBoxCandidate = {
    require(value > 0, s"box value shoulde be > 0, got $value")
    new ErgoBoxCandidate(
      value,
      script.ergoTree,
      currentHeight,
      Array[(TokenId, Long)]().toColl,
      registers.mapValues(liftVal)
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
      currentHeight,
      Array[(TokenId, Long)]((Digest32 @@ token._1.tokenId.toArray, token._2)).toColl,
      Map((register._1, liftVal(register._2)))
    )
  }

  def Box(
    value: Long,
    token: (TokenInfo, Long),
    registers: Map[NonMandatoryRegisterId, Any],
    script: ErgoContract
  ): ErgoBoxCandidate = {
    require(value > 0, s"box value shoulde be > 0, got $value")
    new ErgoBoxCandidate(
      value,
      script.ergoTree,
      currentHeight,
      Array[(TokenId, Long)]((Digest32 @@ token._1.tokenId.toArray, token._2)).toColl,
      registers.mapValues(liftVal)
    )
  }

  def Box(
    value: Long,
    tokens: List[(TokenInfo, Long)],
    script: ErgoContract
  ): ErgoBoxCandidate = {
    require(value > 0, s"box value shoulde be > 0, got $value")
    new ErgoBoxCandidate(
      value,
      script.ergoTree,
      currentHeight,
      tokens.map(token => (Digest32 @@ token._1.tokenId.toArray, token._2)).toColl
    )
  }

  def Box(
    value: Long,
    tokens: List[(TokenInfo, Long)],
    register: (NonMandatoryRegisterId, Any),
    script: ErgoContract
  ): ErgoBoxCandidate = {
    require(value > 0, s"box value shoulde be > 0, got $value")
    new ErgoBoxCandidate(
      value,
      script.ergoTree,
      currentHeight,
      tokens.map(token => (Digest32 @@ token._1.tokenId.toArray, token._2)).toColl,
      Map((register._1, liftVal(register._2)))
    )
  }

  def Box(
    value: Long,
    tokens: List[(TokenInfo, Long)],
    registers: Map[NonMandatoryRegisterId, Any],
    script: ErgoContract
  ): ErgoBoxCandidate = {
    require(value > 0, s"box value shoulde be > 0, got $value")
    new ErgoBoxCandidate(
      value,
      script.ergoTree,
      currentHeight,
      tokens.map(token => (Digest32 @@ token._1.tokenId.toArray, token._2)).toColl,
      registers.mapValues(liftVal)
    )
  }

}
