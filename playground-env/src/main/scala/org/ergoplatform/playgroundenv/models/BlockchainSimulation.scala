package org.ergoplatform.playgroundenv.models

import org.ergoplatform.ErgoBox.BoxId
import org.ergoplatform.compiler.ErgoScalaCompiler
import org.ergoplatform.wallet.protocol.context.{ErgoLikeParameters, ErgoLikeStateContext}
import org.ergoplatform.{ErgoBox, ErgoLikeTransaction}
import scorex.crypto.authds.ADDigest
import scorex.crypto.hash.Digest32
import scorex.util.encode.Base16
import sigmastate.eval.{CGroupElement, CPreHeader, Colls}
import sigmastate.interpreter.CryptoConstants
import special.collection.Coll
import special.sigma.{Header, PreHeader, SigmaProp}

import scala.collection.mutable

case class PKBlockchainStats(
  pk: SigmaProp,
  totalNanoErgs: Long,
  totalToken: TokenAmount
) {
//  override def toString: String = s"$pk, $totalNanoErgs, $totalToken"
}

trait BlockchainSimulation {

  def newParty(name: String): Party

  def send(tx: ErgoLikeTransaction): Unit
}
