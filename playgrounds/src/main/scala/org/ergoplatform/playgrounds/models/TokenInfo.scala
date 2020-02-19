package org.ergoplatform.playgrounds.models

import special.collection.Coll

import scala.language.implicitConversions

case class TokenInfo(tokenId: Coll[Byte], tokenAmount: Long)

object TokenInfo {
  implicit def apply(t: (Coll[Byte], Long)): TokenInfo = new TokenInfo(t._1, t._2)
}

