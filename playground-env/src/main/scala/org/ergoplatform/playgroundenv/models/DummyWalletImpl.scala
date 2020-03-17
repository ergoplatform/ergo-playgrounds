package org.ergoplatform.playgroundenv.models

import org.ergoplatform.appkit.{AppkitProvingInterpreter, Iso, Mnemonic}
import org.ergoplatform.wallet.mnemonic.{Mnemonic => WMnemonic}
import org.ergoplatform.wallet.secrets.ExtendedSecretKey
import org.ergoplatform.{ErgoLikeTransaction, UnsignedErgoLikeTransaction}
import sigmastate.basics.DiffieHellmanTupleProverInput
import sigmastate.eval.CSigmaProp

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
    println(s"......$name: Signing transaction ShortTxDesc")
    import Iso._
    val dlogs =
      JListToIndexedSeq(identityIso[ExtendedSecretKey]).from(IndexedSeq(masterKey))
    val dhtInputs    = new java.util.ArrayList[DiffieHellmanTupleProverInput](0)
    val prover       = new AppkitProvingInterpreter(dlogs, dhtInputs, blockchain.parameters)
    val boxesToSpend = tx.inputs.map(i => blockchain.getBox(i.boxId)).toIndexedSeq
    prover.sign(tx, boxesToSpend, IndexedSeq(), blockchain.stateContext).get
  }
}
