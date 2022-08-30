package org.ergoplatform.playgroundenv.utils

import org.ergoplatform.compiler.ErgoContract
import sigmastate.interpreter.Interpreter.ScriptEnv
import sigmastate.lang.{SigmaCompiler, TransformingSigmaBuilder}
import org.ergoplatform.{ErgoAddressEncoder, Pay2SAddress}
import org.ergoplatform.ErgoAddressEncoder.{MainnetNetworkPrefix, TestnetNetworkPrefix}
import scorex.util.encode.{Base16, Base64};
import sigmastate.eval.CompiletimeIRContext
import sigmastate.eval.Evaluation
import sigmastate.SType
import sigmastate.SType.AnyOps

object ErgoScriptCompiler {

  val compiler = SigmaCompiler(TestnetNetworkPrefix, TransformingSigmaBuilder)

  implicit var IR: CompiletimeIRContext = new CompiletimeIRContext()

  def compile(env: ScriptEnv, ergoScript: String): ErgoContract = {
    val liftedEnv = env.mapValues { v =>
      val tV      = Evaluation.rtypeOf(v).get
      val elemTpe = Evaluation.rtypeToSType(tV)
      IR.builder.mkConstant[SType](v.asWrappedType, elemTpe)
    }
    val prop = compiler.compile(liftedEnv, ergoScript)
    ErgoContract(_ => ???, prop)
  }

  def printErgoScriptEncodingsAndHashesFromUrl(url: String) = {
    val script           = scala.io.Source.fromURL(url).mkString
    val ergoTree         = compile(Map(), script).ergoTree
    implicit var encoder = new ErgoAddressEncoder(MainnetNetworkPrefix)
    var addr             = Pay2SAddress(ergoTree)

    val mainnetEncoded = encoder.toString(addr)
    val ergoTreeHash = Base64.encode(
      ErgoAddressEncoder
        .hash256(ergoTree.bytes)
    )

    encoder = new ErgoAddressEncoder(TestnetNetworkPrefix)
    addr    = Pay2SAddress(ergoTree)
    val testnetEncoded = encoder.toString(addr)

    val serializedErgoTree = Base16.encode(ergoTree.bytes)
    println(
      s"Ergoscript @$url\n" +
      s"P2S(mainnet): $mainnetEncoded\n" +
      s"P2S(testnet): $testnetEncoded\n" +
      s"Hashed ergo-tree bytes (base 64 encoded): $ergoTreeHash\n" +
      s"Serialized ErgoTree (base 16 encoded): $serializedErgoTree"
    )
  }
}
