package org.ergoplatform.playgrounds

import org.ergoplatform.dsl.ContractSyntax.TokenId
import org.ergoplatform.playground.{SignedTransaction, UnsignedTransaction}
import special.collection.Coll

trait Wallet {

  def getAddress: Address = ???

//  def selectBoxes(toSpend: Long): List[InputBox]                                   = ???
//  def selectBoxes(toSpend: Long, tokenToSpend: (Coll[Byte], Long)): List[InputBox] = ???

  def sign(tx: UnsignedTransaction): SignedTransaction = ???

}
//trait InputBoxesList extends Seq[InputBox] {
//
//  def totalValue: Long       = thisCollection.map(_.value).sum
//  def totalTokenAmount: Long = thisCollection.map(_.value).sum
//}
