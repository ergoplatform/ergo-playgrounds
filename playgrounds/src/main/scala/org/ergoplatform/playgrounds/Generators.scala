package org.ergoplatform.playgrounds

import special.collection.Coll

trait Generators {

  def newBlockChainSimulation: BlockchainSimulation = ???

  def newTokenId: Coll[Byte] = ???

  def newWallet: Wallet = ???

}
