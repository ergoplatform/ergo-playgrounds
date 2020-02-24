package org.ergoplatform.playgroundenv.models

import org.ergoplatform.compiler.ErgoScalaCompiler
import org.ergoplatform.playgroundenv.dsl.ObjectGenerators
import special.sigma.SigmaProp

case class PKBlockchainStats(pk: SigmaProp, totalNanoErgs: Long, totalToken: TokenInfo) {
//  override def toString: String = s"$pk, $totalNanoErgs, $totalToken"
}

trait BlockchainSimulation {

  def newParty(name: String): Party

  def send(tx: SignedTransaction): Unit

  def generateUnspentBoxesFor(
    pk: Address,
    toSpend: Long,
    tokensToSpend: List[TokenInfo] = List()
  ): Unit

  def selectUnspentBoxesFor(
    pk: Address,
    toSpend: Long,
    tokensToSpend: List[TokenInfo] = List()
  ): List[InputBox]

  def printUnspentAssetsFor(party: Party)
}

case class NaiveBlockchainSimulation(scenarioName: String) extends BlockchainSimulation {

  private val ctx: BlockchainContext = DummyBlockchainContext(this)

  override def newParty(name: String): Party = NaiveParty(ctx, name)

  override def send(tx: SignedTransaction): Unit = {}

  override def generateUnspentBoxesFor(
    pk: Address,
    toSpend: Long,
    tokensToSpend: List[TokenInfo]
  ): Unit = {}

  override def selectUnspentBoxesFor(
    pk: Address,
    toSpend: Long,
    tokensToSpend: List[TokenInfo]
  ): List[InputBox] =
    List(InputBox(toSpend, tokensToSpend, ErgoScalaCompiler.contract(pk.pubKey).ergoTree))

  override def printUnspentAssetsFor(party: Party): Unit =
    println("STATS")
}
