package org.ergoplatform.playgroundenv.models

import org.ergoplatform.compiler.ErgoContract
import sigmastate.interpreter.Interpreter.ScriptEnv
import sigmastate.lang.{SigmaCompiler, TransformingSigmaBuilder}
import org.ergoplatform.ErgoAddressEncoder.TestnetNetworkPrefix
import sigmastate.eval.CompiletimeIRContext

object ErgoScriptCompiler {

  val compiler = SigmaCompiler(TestnetNetworkPrefix, TransformingSigmaBuilder)

  implicit var ctx: CompiletimeIRContext = new CompiletimeIRContext()

  def compile(env: ScriptEnv, ergoScript: String): ErgoContract = {
    val prop = compiler.compile(env, ergoScript)
    ErgoContract(_ => ???, prop)
  }
}
