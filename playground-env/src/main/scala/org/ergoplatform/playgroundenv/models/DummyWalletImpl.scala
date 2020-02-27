package org.ergoplatform.playgroundenv.models

import org.ergoplatform.{UnsignedErgoLikeTransaction, UnsignedInput}
import org.ergoplatform.appkit.{
  AppkitProvingInterpreter,
  Helpers,
  Iso,
  JavaHelpers,
  Mnemonic
}
import org.ergoplatform.playgroundenv.dsl.ObjectGenerators
import org.ergoplatform.wallet.secrets.ExtendedSecretKey
import org.ergoplatform.wallet.mnemonic.{Mnemonic => WMnemonic}
import org.ergoplatform.wallet.protocol.context.{ErgoLikeParameters, ErgoLikeStateContext}
import scorex.crypto.authds.{ADDigest, ADKey}
import scorex.util.encode.Base16
import sigmastate.basics.{
  DiffieHellmanTupleInteractiveProver,
  DiffieHellmanTupleProverInput
}
import sigmastate.eval.{CGroupElement, CPreHeader, CSigmaProp, Colls}
import sigmastate.interpreter.CryptoConstants
import special.collection.Coll
import special.sigma.{Header, PreHeader, SigmaProp}

class DummyWalletImpl(ctx: BlockchainContext, override val name: String) extends Wallet {

  private val masterKey = {
    val m    = Mnemonic.generateEnglishMnemonic()
    val seed = WMnemonic.toSeed(m, None)
    ExtendedSecretKey.deriveMasterKey(seed)
  }

  val pk: CSigmaProp = CSigmaProp(masterKey.publicKey.key)

  override def getAddress: Address = Address(pk)

  // TODO: extract
  private val stateContext = new ErgoLikeStateContext {

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

  override def sign(tx: UnsignedTransaction): SignedTransaction = {
    println(s"......$name: Signing transaction ShortTxDesc")

    import Iso._
    import Helpers._

    val dlogs =
      JListToIndexedSeq(identityIso[ExtendedSecretKey]).from(IndexedSeq(masterKey))

    // TODO extract
    val parameters = new ErgoLikeParameters {

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

    val dhtInputs = new java.util.ArrayList[DiffieHellmanTupleProverInput](0)
    val prover    = new AppkitProvingInterpreter(dlogs, dhtInputs, parameters)

    val inputs = tx.inputs.map { ib =>
      new UnsignedInput(ib.id)
    }.toIndexedSeq
    val outBoxCandidates = tx.outputs.map(_.toErgoBoxCandidate).toIndexedSeq
    val unsignedTx       = UnsignedErgoLikeTransaction(inputs, outBoxCandidates)
    val boxesToSpend     = tx.inputs.map(_.toErgoBox(0)).toIndexedSeq
    val signedTx         = prover.sign(unsignedTx, boxesToSpend, IndexedSeq(), stateContext).get

    // TODO:
    signedTx
  }
}

object DummyWalletImpl {

  def apply(ctx: BlockchainContext, name: String): DummyWalletImpl =
    new DummyWalletImpl(ctx, name)
}
