package org.ergoplatform.playgrounds.examples

import org.ergoplatform.compiler.ErgoScalaCompiler._
import org.ergoplatform.playground._

object AssetsAtomicExchangePlayground {

  val blockchainSim = newBlockChainSimulation
  val txBuilder     = newTransactionBuilder(blockchainSim.ctx)

  val tokenId = newTokenId

  val sellerWallet         = newWallet
  val seller               = sellerWallet.getAddress.pubKey
  val sellerAskNanoErgs    = 50000000
  val sellerAskTokenAmount = 100L
  blockchainSim.generateUnspentBoxesFor(
    seller,
    toSpend       = MinErg,
    tokensToSpend = List(tokenId -> sellerAskTokenAmount)
  )

  val buyerWallet         = newWallet
  val buyer               = buyerWallet.getAddress.pubKey
  val buyerBidTokenAmount = 100
  val buyersBidNanoErgs   = 100000000
  blockchainSim.generateUnspentBoxesFor(buyer, toSpend = buyersBidNanoErgs)

  // Buy order
  // --------------------------------------------------------------------------
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

  val buyerBalance =
    blockchainSim.selectUnspentBoxesFor(buyer, toSpend = buyersBidNanoErgs)

  val buyerBidBox = Box(value = buyersBidNanoErgs, script = BuyerContract)

  val buyOrderTransaction = txBuilder.makeTransaction(
    inputs       = buyerBalance,
    outputs      = List(buyerBidBox),
    fee          = MinTxFee,
    sendChangeTo = buyer
  )

  val buyOrderTransactionSigned = buyerWallet.sign(buyOrderTransaction)

  blockchainSim.send(buyOrderTransactionSigned)

  // Sell order
  // --------------------------------------------------------------------------
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

  val sellerBalanceBoxes = blockchainSim.selectUnspentBoxesFor(
    seller,
    toSpend       = MinErg,
    tokensToSpend = List(tokenId -> sellerAskTokenAmount)
  )

  val sellerAskBox = Box(
    value  = MinErg,
    token  = (tokenId -> sellerAskTokenAmount),
    script = SellerContract
  )

  val sellOrderTransaction = txBuilder.makeTransaction(
    inputs       = sellerBalanceBoxes,
    outputs      = List(sellerAskBox),
    fee          = MinTxFee,
    sendChangeTo = seller
  )

  val sellOrderTransactionSigned = sellerWallet.sign(sellOrderTransaction)

  blockchainSim.send(sellOrderTransactionSigned)

  // Swap (match) buy and sell orders
  // --------------------------------------------------------------------------

  val buyerOutBox = Box(
    value    = MinErg,
    token    = (tokenId -> buyerBidTokenAmount),
    register = (R4 -> buyOrderTransactionSigned.outputs(0).id),
    script   = contract(buyer)
  )

  val sellerOutBox =
    Box(
      value    = sellerAskNanoErgs,
      register = (R4 -> sellOrderTransactionSigned.outputs(0).id),
      script   = contract(seller)
    )

  val swapTransaction = txBuilder.makeTransaction(
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
    Box(value = buyersBidNanoErgs, token = (newTokenId -> 1L), script = contract(buyer))

  val cancelBuyTransaction =
    txBuilder.makeTransaction(
      inputs  = List(buyOrderTransactionSigned.outputs(0)),
      outputs = List(buyerRefundBox),
      fee     = MinTxFee
    )

  val cancelBuyTransactionSigned = buyerWallet.sign(cancelBuyTransaction)

  // Refund sell order
  // --------------------------------------------------------------------------

  val sellerRefundBox =
    Box(
      value  = MinErg,
      token  = (tokenId -> sellerAskTokenAmount),
      script = contract(seller)
    )

  val cancelSellTransaction = txBuilder.makeTransaction(
    inputs  = List(sellOrderTransactionSigned.outputs(0)),
    outputs = List(sellerRefundBox),
    fee     = MinTxFee
  )

  val cancelSellTransactionSigned = buyerWallet.sign(cancelSellTransaction)

  // Swap scenario
  // --------------------------------------------------------------------------
  def checkSwapScenario() =
    blockchainSim.send(swapTransactionSigned)

  // Refund scenario
  // --------------------------------------------------------------------------
  def checkRefundScenario() = {
    blockchainSim.send(cancelBuyTransactionSigned)
    blockchainSim.send(cancelSellTransactionSigned)
  }

  // Uncomment only one of them
  checkSwapScenario()
  // checkRefundScenario()

  blockchainSim.getUnspentAssetsFor(seller)
  blockchainSim.getUnspentAssetsFor(buyer)
  blockchainSim.getUnspentAssetsFor(dexWallet.getAddress.pubKey)
}
