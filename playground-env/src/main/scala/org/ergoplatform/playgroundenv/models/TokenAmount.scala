package org.ergoplatform.playgroundenv.models

import scorex.util.encode.Base16
import special.collection.Coll

import scala.language.implicitConversions

case class TokenAmount(token: TokenInfo, tokenAmount: Long) {

  override def toString: String =
    s"(${Base16.encode(token.tokenId.toArray)}, $tokenAmount)"
}

object TokenAmount {
  implicit def apply(t: (TokenInfo, Long)): TokenAmount = new TokenAmount(t._1, t._2)
}

case class TokenInfo(tokenId: Coll[Byte], tokenName: String)
