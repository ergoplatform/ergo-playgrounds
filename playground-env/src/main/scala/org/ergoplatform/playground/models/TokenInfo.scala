package org.ergoplatform.playground.models

import scorex.util.encode.Base16
import special.collection.Coll

import scala.language.implicitConversions

case class TokenInfo(tokenId: Coll[Byte], tokenAmount: Long) {
  override def toString: String = s"(${Base16.encode(tokenId.toArray)}, $tokenAmount)"
}

object TokenInfo {
  implicit def apply(t: (Coll[Byte], Long)): TokenInfo = new TokenInfo(t._1, t._2)
}
