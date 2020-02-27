package org.ergoplatform.playgroundenv.models

trait BlockchainContext {}

case class DummyBlockchainContextImpl(blockchain: BlockchainSimulation)
  extends BlockchainContext {}
