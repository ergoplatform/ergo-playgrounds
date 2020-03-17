package org.ergoplatform.playgroundenv.models

import special.sigma.SigmaProp
import sigmastate.basics.DLogProtocol.ProveDlog
import sigmastate.eval.CSigmaProp

case class Address(proveDlog: ProveDlog) {
  val pubKey: SigmaProp = CSigmaProp(proveDlog)
}
