package org.ergoplatform.playgroundenv.models

trait BlockchainContext {}

case class DummyBlockchainContext(blockchain: BlockchainSimulation)
  extends BlockchainContext {}
