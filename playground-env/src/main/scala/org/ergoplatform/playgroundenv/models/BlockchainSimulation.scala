package org.ergoplatform.playgroundenv.models

import org.ergoplatform.compiler.ErgoScalaCompiler
import org.ergoplatform.playgroundenv.dsl.ObjectGenerators
import special.sigma.SigmaProp

case class PKBlockchainStats(pk: SigmaProp, totalNanoErgs: Long, totalToken: TokenInfo) {
//  override def toString: String = s"$pk, $totalNanoErgs, $totalToken"
}

trait BlockchainSimulation {

  val ctx: BlockchainContext

  def send(tx: SignedTransaction): Unit

  def generateUnspentBoxesFor(
    pk: SigmaProp,
    toSpend: Long,
    tokensToSpend: List[TokenInfo] = List()
  ): Unit

  def selectUnspentBoxesFor(
    pk: SigmaProp,
    toSpend: Long,
    tokensToSpend: List[TokenInfo] = List()
  ): List[InputBox]

  def getUnspentAssetsFor(pk: SigmaProp): PKBlockchainStats

}

case class NaiveBlockchainSimulation() extends BlockchainSimulation {

  override val ctx: BlockchainContext = DummyBlockchainContext(this)

  override def send(tx: SignedTransaction): Unit = {}

  override def generateUnspentBoxesFor(
    pk: SigmaProp,
    toSpend: Long,
    tokensToSpend: List[TokenInfo]
  ): Unit = {}

  override def selectUnspentBoxesFor(
    pk: SigmaProp,
    toSpend: Long,
    tokensToSpend: List[TokenInfo]
  ): List[InputBox] =
    List(InputBox(toSpend, tokensToSpend, ErgoScalaCompiler.contract(pk).ergoTree))

  override def getUnspentAssetsFor(pk: SigmaProp): PKBlockchainStats =
    PKBlockchainStats(pk, 1000L, TokenInfo(ObjectGenerators.newErgoId, 100L))

}
