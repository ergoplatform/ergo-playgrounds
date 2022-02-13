package org.ergoplatform.playgroundenv.models

import java.util
import org.ergoplatform.appkit.{AppkitProvingInterpreter, Iso, Mnemonic}
import org.ergoplatform.wallet.mnemonic.{Mnemonic => WMnemonic}
import org.ergoplatform.wallet.secrets.ExtendedSecretKey
import org.ergoplatform.{ErgoLikeTransaction, UnsignedErgoLikeTransaction}
import scala.collection.JavaConverters._
import sigmastate.basics.{DiffieHellmanTupleProverInput, ProveDHTuple}

class DummyWalletImpl(
  blockchain: DummyBlockchainSimulationImpl,
  override val name: String
) extends Wallet {

  private val masterKey = {
    val m    = Mnemonic.generateEnglishMnemonic()
    val seed = WMnemonic.toSeed(m, None)
    ExtendedSecretKey.deriveMasterKey(seed)
  }

  override val getAddress: Address = Address(masterKey.publicKey.key)

  override def sign(tx: UnsignedErgoLikeTransaction): ErgoLikeTransaction = {
    val dhtInputs      = new util.ArrayList[DiffieHellmanTupleProverInput](0)

    signWithDHTInputs(dhtInputs, tx)
  }

  override def signWithDHTData(proveDHTuple: ProveDHTuple, tx: UnsignedErgoLikeTransaction): ErgoLikeTransaction = {
    val dhtInputs : util.List[DiffieHellmanTupleProverInput] = List(DiffieHellmanTupleProverInput(masterKey.key.w, proveDHTuple)).asJava

    signWithDHTInputs(dhtInputs, tx)
  }

  private def signWithDHTInputs(dhtInputs: util.List[DiffieHellmanTupleProverInput], tx: UnsignedErgoLikeTransaction) : ErgoLikeTransaction = {
    println(s"......$name: Signing transaction ${tx.id}")
    import Iso._
    val dLogs =
      JListToIndexedSeq(identityIso[ExtendedSecretKey]).from(IndexedSeq(masterKey))
    val boxesToSpend   = tx.inputs.map(i => blockchain.getUnspentBox(i.boxId)).toIndexedSeq
    val dataInputBoxes = tx.dataInputs.map(i => blockchain.getBox(i.boxId)).toIndexedSeq
    val prover         = new AppkitProvingInterpreter(dLogs, dhtInputs, blockchain.parameters)
    prover.sign(tx, boxesToSpend, dataInputBoxes, blockchain.stateContext).get
  }
}
