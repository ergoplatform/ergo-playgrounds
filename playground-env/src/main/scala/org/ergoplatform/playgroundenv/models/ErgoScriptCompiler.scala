package org.ergoplatform.playgroundenv.models

import org.ergoplatform.compiler.ErgoContract
import sigmastate.interpreter.Interpreter.ScriptEnv
import sigmastate.lang.{SigmaCompiler, TransformingSigmaBuilder}
import org.ergoplatform.ErgoAddressEncoder.TestnetNetworkPrefix
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
}
