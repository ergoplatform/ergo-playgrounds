package org.ergoplatform.playgrounds

import scalan.RType
import special.collection.Coll
import special.sigma.{AnyValue, AvlTree, Box, Context, Header, PreHeader, SigmaDslBuilder}

trait EmptyContext extends Context {
  override def builder: SigmaDslBuilder = ???

  override def OUTPUTS: Coll[Box] = ???

  override def INPUTS: Coll[Box] = ???

  override def dataInputs: Coll[Box] = ???

  override def HEIGHT: Int = ???

  override def SELF: Box = ???

  override def selfBoxIndex: Int = ???

  override def LastBlockUtxoRootHash: AvlTree = ???

  override def headers: Coll[Header] = ???

  override def preHeader: PreHeader = ???

  override def minerPubKey: Coll[Byte] = ???

  override def getVar[T](id: Byte)(implicit cT: RType[T]): Option[T] = ???

  override def vars: Coll[AnyValue] = ???
}
