package org.ergoplatform.playgroundenv.dsl

trait TypesDsl {

  type Coll[A]              = special.collection.Coll[A]
  type SigmaProp            = special.sigma.SigmaProp
  type ErgoContract         = org.ergoplatform.compiler.ErgoContract
  type BlockchainSimulation = org.ergoplatform.playgroundenv.models.BlockchainSimulation
  type Address              = org.ergoplatform.playgroundenv.models.Address
  type Party                = org.ergoplatform.playgroundenv.models.Party
  type TokenInfo            = org.ergoplatform.playgroundenv.models.TokenInfo
  type ScriptEnv            = sigmastate.interpreter.Interpreter.ScriptEnv
}
