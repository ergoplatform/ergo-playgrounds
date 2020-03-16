package org.ergoplatform.playgroundenv.models

import org.ergoplatform.ErgoBox.BoxId
import org.ergoplatform.{ErgoBox, ErgoLikeTransaction}
import org.ergoplatform.compiler.ErgoScalaCompiler
import org.ergoplatform.wallet.protocol.context.{ErgoLikeParameters, ErgoLikeStateContext}
import scorex.crypto.authds.ADDigest
import scorex.crypto.hash.Digest32
import scorex.util.encode.Base16
import sigmastate.eval.{CGroupElement, CPreHeader, Colls}
import sigmastate.interpreter.CryptoConstants
import special.collection.Coll
import special.sigma.{Header, PreHeader}

import scala.collection.mutable

case class DummyBlockchainSimulationImpl(scenarioName: String)
  extends BlockchainSimulation {

  private val boxes: mutable.ArrayBuffer[ErgoBox] =
    new mutable.ArrayBuffer[ErgoBox]()

  val stateContext: ErgoLikeStateContext = new ErgoLikeStateContext {

    override def sigmaLastHeaders: Coll[Header] = Colls.emptyColl

    override def previousStateDigest: ADDigest =
      Base16
        .decode("a5df145d41ab15a01e0cd3ffbab046f0d029e5412293072ad0f5827428589b9302")
        .map(ADDigest @@ _)
        .getOrElse(throw new Error(s"Failed to parse genesisStateDigest"))

    override def sigmaPreHeader: PreHeader = CPreHeader(
      version   = 0,
      parentId  = Colls.emptyColl[Byte],
      timestamp = 0,
      nBits     = 0,
      height    = 0,
      minerPk   = CGroupElement(CryptoConstants.dlogGroup.generator),
      votes     = Colls.emptyColl[Byte]
    )
  }

  val parameters: ErgoLikeParameters = new ErgoLikeParameters {

    override def storageFeeFactor: Int = 1250000

    override def minValuePerByte: Int = 360

    override def maxBlockSize: Int = 524288

    override def tokenAccessCost: Int = 100

    override def inputCost: Int = 2000

    override def dataInputCost: Int = 100

    override def outputCost: Int = 100

    override def maxBlockCost: Long = 1000000

    override def softForkStartingHeight: Option[Int] = None

    override def softForkVotesCollected: Option[Int] = None

    override def blockVersion: Byte = 1
  }

  def generateUnspentBoxesFor(
    address: Address,
    toSpend: Long,
    tokensToSpend: List[TokenAmount]
  ): Unit =
    boxes.append(
      ErgoBox(
        value          = toSpend,
        ergoTree       = ErgoScalaCompiler.contract(address.pubKey).ergoTree,
        creationHeight = 0,
        additionalTokens =
          tokensToSpend.map(ta => (Digest32 @@ ta.token.tokenId.toArray, ta.tokenAmount))
      )
    )

  def selectUnspentBoxesFor(
    address: Address,
    toSpend: Long,
    tokensToSpend: List[TokenAmount]
  ): List[ErgoBox] = {
    val treeToFind = ErgoScalaCompiler.contract(address.pubKey).ergoTree
    boxes.filter { b =>
      b.ergoTree == treeToFind
    }.toList
  }

  def getBox(id: BoxId): ErgoBox =
    boxes.find(b => java.util.Arrays.equals(b.id, id)).get

  override def newParty(name: String): Party = {
    println(s"..$scenarioName: Creating new party: $name")
    DummyPartyImpl(this, name)
  }

  override def send(tx: ErgoLikeTransaction): Unit = {
    boxes.appendAll(tx.outputs)
    println(s"..$scenarioName: Accepting transaction ShortTxDesc to the blockchain")
  }
}
