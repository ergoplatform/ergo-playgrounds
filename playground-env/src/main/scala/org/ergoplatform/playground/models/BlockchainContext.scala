package org.ergoplatform.playground.models

trait BlockchainContext {}

case class DummyBlockchainContext(blockchain: BlockchainSimulation)
  extends BlockchainContext {}
