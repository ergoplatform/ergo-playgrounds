package org.ergoplatform.playgroundenv.models

import org.ergoplatform.ErgoBox.BoxId
import org.ergoplatform.{ErgoBox, ErgoLikeTransaction}
import org.ergoplatform.compiler.ErgoScalaCompiler._
import org.ergoplatform.wallet.protocol.context.{ErgoLikeParameters, ErgoLikeStateContext}
import scorex.crypto.authds.ADDigest
import scorex.crypto.hash.Digest32
import scorex.util.encode.Base16
import scorex.util._
import sigmastate.eval.{CGroupElement, CPreHeader, Colls}
import sigmastate.interpreter.CryptoConstants
import special.collection.Coll
import special.sigma.{Header, PreHeader}
import sigmastate.eval.Extensions._

import scala.collection.mutable
import org.ergoplatform.playgroundenv.dsl.ObjectGenerators
import org.ergoplatform.playgroundenv.utils.TransactionVerifier

case class DummyBlockchainSimulationImpl(scenarioName: String)
  extends BlockchainSimulation {

  private var boxes: mutable.ArrayBuffer[ErgoBox] = new mutable.ArrayBuffer[ErgoBox]()

  private var unspentBoxes: mutable.ArrayBuffer[ErgoBox] =
    new mutable.ArrayBuffer[ErgoBox]()
  private val tokenNames: mutable.Map[ModifierId, String] = mutable.Map()
  private var chainHeight: Int                            = 0

  private def getUnspentBoxesFor(address: Address): List[ErgoBox] =
    unspentBoxes.filter { b =>
      contract(address.pubKey).ergoTree == b.ergoTree
    }.toList

  def stateContext: ErgoLikeStateContext = new ErgoLikeStateContext {

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
      height    = chainHeight,
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

    override def maxBlockCost: Long = 2000000

    override def softForkStartingHeight: Option[Int] = None

    override def softForkVotesCollected: Option[Int] = None

    override def blockVersion: Byte = 1
  }

  def generateUnspentBoxesFor(
    address: Address,
    toSpend: Long,
    tokensToSpend: List[TokenAmount]
  ): Unit = {
    tokensToSpend.foreach { t =>
      tokenNames += (t.token.tokenId.toArray.toModifierId -> t.token.tokenName)
    }
    val b = ErgoBox(
      value          = toSpend,
      ergoTree       = contract(address.pubKey).ergoTree,
      creationHeight = chainHeight,
      additionalTokens =
        tokensToSpend.map(ta => (Digest32 @@ ta.token.tokenId.toArray, ta.tokenAmount))
    )
    unspentBoxes.append(b)
    boxes.append(b)
  }

  def selectUnspentBoxesFor(
    address: Address,
    toSpend: Long,
    tokensToSpend: List[TokenAmount]
  ): List[ErgoBox] = {
    val treeToFind = contract(address.pubKey).ergoTree
    val filtered = unspentBoxes.filter { b =>
      b.ergoTree == treeToFind
    }.toList
    filtered
  }

  def getUnspentBox(id: BoxId): ErgoBox =
    unspentBoxes.find(b => java.util.Arrays.equals(b.id, id)).get

  def getBox(id: BoxId): ErgoBox =
    boxes.find(b => java.util.Arrays.equals(b.id, id)).get

  override def newParty(name: String): Party = {
    val party = DummyPartyImpl(this, name)
    val pk    = party.wallet.getAddress.proveDlog
    println(s"..$scenarioName: Creating new party: $name, pk: $pk")
    party
  }

  override def send(tx: ErgoLikeTransaction): Unit = {
    val boxesToSpend   = tx.inputs.map(i => getUnspentBox(i.boxId)).toIndexedSeq
    val dataInputBoxes = tx.dataInputs.map(i => getBox(i.boxId)).toIndexedSeq
    TransactionVerifier.verify(tx, boxesToSpend, dataInputBoxes, parameters, stateContext)

    val newBoxes: mutable.ArrayBuffer[ErgoBox] = new mutable.ArrayBuffer[ErgoBox]()
    newBoxes.appendAll(tx.outputs)
    newBoxes.appendAll(
      unspentBoxes.filterNot(b =>
        tx.inputs.map(_.boxId.toModifierId).contains(b.id.toModifierId)
      )
    )
    unspentBoxes = newBoxes
    boxes.appendAll(tx.outputs)
    println(s"..$scenarioName: Accepting transaction ${tx.id} to the blockchain")
  }

  override def newToken(name: String): TokenInfo = {
    val tokenId = ObjectGenerators.newErgoId
    tokenNames += (tokenId.toArray.toModifierId -> name)
    TokenInfo(tokenId, name)
  }

  def getUnspentCoinsFor(address: Address): Long =
    getUnspentBoxesFor(address).map(_.value).sum

  def getUnspentTokensFor(address: Address): List[TokenAmount] =
    getUnspentBoxesFor(address).flatMap { b =>
      b.additionalTokens.toArray.map { t =>
        TokenAmount(TokenInfo(t._1.toColl, tokenNames(t._1.toModifierId)), t._2)
      }
    }

  def getHeight: Int = chainHeight

  def setHeight(height: Int): Unit = { chainHeight = height }
}
