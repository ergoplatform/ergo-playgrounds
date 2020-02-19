package org.ergoplatform.playgrounds

import org.ergoplatform.playgrounds.models.BlockchainSimulation
import special.collection.Coll

trait Generators {

  def newBlockChainSimulation: BlockchainSimulation = ???

  def newTokenId: Coll[Byte] = ???

  def newWallet: Wallet = ???

}
