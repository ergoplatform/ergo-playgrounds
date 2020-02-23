package org.ergoplatform.playgrounds.dsl

trait TypesDsl {

  type Coll[A]      = special.collection.Coll[A]
  type SigmaProp    = special.sigma.SigmaProp
  type ErgoContract = org.ergoplatform.compiler.ErgoContract
}
