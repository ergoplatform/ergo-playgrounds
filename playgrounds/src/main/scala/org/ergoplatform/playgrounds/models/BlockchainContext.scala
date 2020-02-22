package org.ergoplatform.playgrounds.models

trait BlockchainContext {}

case class DummyBlockchainContext(blockchain: BlockchainSimulation)
  extends BlockchainContext {}
