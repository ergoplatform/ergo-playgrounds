package org.ergoplatform.playgrounds.examples

import org.ergoplatform.compiler.ErgoScalaCompiler._
import org.ergoplatform.playground._

object AssetsAtomicExchangePlayground {

  val blockchain = newBlockChainSimulation

  val tokenId = newTokenId

  // Buy order
  // --------------------------------------------------------------------------

  val buyerWallet = newWallet
  val buyer       = buyerWallet.getAddress.pubKey

  val buyerBidTokenAmount = 100
  val buyersBidNanoErgs   = 100000000

  val BuyerContract = contract {
    buyer || {
      (OUTPUTS.nonEmpty && OUTPUTS(0).R4[Coll[Byte]].isDefined) && {
        val tokens = OUTPUTS(0).tokens
        val tokenDataCorrect = tokens.nonEmpty &&
          tokens(0)._1 == tokenId &&
          tokens(0)._2 >= buyerBidTokenAmount

        val knownId = OUTPUTS(0).R4[Coll[Byte]].get == SELF.id
        tokenDataCorrect && OUTPUTS(0).propositionBytes == buyer.propBytes && knownId
      }
    }
  }

  val buyOrderTransaction = {

    val buyerBalance = blockchain.makeUnspentBoxesFor(buyer, toSpend = buyersBidNanoErgs)

    val buyerBidBox = Box(value = buyersBidNanoErgs, script = BuyerContract)
    val buyerChangeBox =
      Box(value = buyerBalance.totalValue - buyersBidNanoErgs, script = pk(buyer))

    Transaction(
      inputs  = buyerBalance,
      outputs = List(buyerBidBox, buyerChangeBox),
      fee     = MinTxFee
    )
  }

  val buyOrderTransactionSigned = buyerWallet.sign(buyOrderTransaction)

  blockchain.send(buyOrderTransactionSigned)

  // Sell order
  // --------------------------------------------------------------------------

  val sellerWallet = newWallet
  val seller       = sellerWallet.getAddress.pubKey

  val sellerAskNanoErgs = 50000000

  val SellerContract = contract {
    seller || (
      OUTPUTS.size > 1 &&
      OUTPUTS(1).R4[Coll[Byte]].isDefined
    ) && {
      val knownBoxId = OUTPUTS(1).R4[Coll[Byte]].get == SELF.id
      OUTPUTS(1).value >= sellerAskNanoErgs &&
      knownBoxId &&
      OUTPUTS(1).propositionBytes == seller.propBytes
    }
  }

  val sellerAskTokenAmount = 100L

  val sellOrderTransaction = {

    val sellerBalanceBoxes = blockchain.makeUnspentBoxesFor(
      seller,
      toSpend      = MinErg,
      tokenToSpend = (tokenId -> sellerAskTokenAmount)
    )

    val sellerAskBox = Box(
      value  = MinErg,
      token  = (tokenId -> sellerAskTokenAmount),
      script = SellerContract
    )
    val sellerChangeBox = Box(
      value  = sellerBalanceBoxes.totalValue - MinErg,
      token  = (tokenId, sellerBalanceBoxes.totalTokenAmount - sellerAskTokenAmount),
      script = pk(seller)
    )
    Transaction(
      inputs  = sellerBalanceBoxes,
      outputs = List(sellerAskBox, sellerChangeBox),
      fee     = MinTxFee
    )
  }

  val sellOrderTransactionSigned = sellerWallet.sign(sellOrderTransaction)

  blockchain.send(sellOrderTransactionSigned)

  // Swap (match) buy and sell orders
  // --------------------------------------------------------------------------

  val buyerOutBox = Box(
    value    = MinErg,
    token    = (tokenId -> buyerBidTokenAmount),
    register = (R4 -> buyOrderTransactionSigned.outputs(0).id),
    script   = pk(buyer)
  )

  val sellerOutBox =
    Box(
      value    = sellerAskNanoErgs,
      register = (R4 -> sellOrderTransactionSigned.outputs(0).id),
      script   = pk(seller)
    )

  val swapTransaction = Transaction(
    inputs =
      List(buyOrderTransactionSigned.outputs(0), sellOrderTransactionSigned.outputs(0)),
    outputs = List(buyerOutBox, sellerOutBox),
    fee     = MinTxFee
  )

  val dexWallet = newWallet

  val swapTransactionSigned = dexWallet.sign(swapTransaction)

  // Refund buyer order
  // --------------------------------------------------------------------------

  val buyerRefundBox =
    Box(value = buyersBidNanoErgs, token = (newTokenId -> 1L), script = pk(buyer))

  val cancelBuyTransaction =
    Transaction(
      inputs  = List(buyOrderTransactionSigned.outputs(0)),
      outputs = List(buyerRefundBox),
      fee     = MinTxFee
    )

  val cancelBuyTransactionSigned = buyerWallet.sign(cancelBuyTransaction)

  // Refund sell order
  // --------------------------------------------------------------------------

  val sellerRefundBox =
    Box(value = MinErg, token = (tokenId -> sellerAskTokenAmount), script = pk(seller))

  val cancelSellTransaction =
    Transaction(
      inputs  = List(sellOrderTransactionSigned.outputs(0)),
      outputs = List(sellerRefundBox),
      fee     = MinTxFee
    )

  val cancelSellTransactionSigned = buyerWallet.sign(cancelSellTransaction)

  // Swap scenario
  // --------------------------------------------------------------------------
  def checkSwapScenario() =
    blockchain.send(swapTransactionSigned)

  // Refund scenario
  // --------------------------------------------------------------------------
  def checkRefundScenario() = {
    blockchain.send(cancelBuyTransactionSigned)
    blockchain.send(cancelSellTransactionSigned)
  }

  // Uncomment only one of them
  checkSwapScenario()
  // checkRefundScenario()

  blockchain.getStatsFor(seller)
  blockchain.getStatsFor(buyer)
  blockchain.getStatsFor(dexWallet.getAddress.pubKey)
}
