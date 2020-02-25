package org.ergoplatform.playgrounds.examples

object AssetsAtomicExchangePlayground {
  import org.ergoplatform.compiler.ErgoScalaCompiler._
  import org.ergoplatform.playground._

  def buyerOrder(
    buyerParty: Party,
    tokenId: Coll[Byte],
    tokenAmount: Long,
    ergAmount: Long
  ) = {

    val buyerPk = buyerParty.wallet.getAddress.pubKey

    val BuyerContract = contract {
      buyerPk || {
        (OUTPUTS.nonEmpty && OUTPUTS(0).R4[Coll[Byte]].isDefined) && {
          val tokens = OUTPUTS(0).tokens
          val tokenDataCorrect = tokens.nonEmpty &&
            tokens(0)._1 == tokenId &&
            tokens(0)._2 >= tokenAmount

          val knownId = OUTPUTS(0).R4[Coll[Byte]].get == SELF.id
          tokenDataCorrect && OUTPUTS(0).propositionBytes == buyerPk.propBytes && knownId
        }
      }
    }

    val buyerBidBox = Box(value = ergAmount, script = BuyerContract)

    Transaction(
      inputs       = buyerParty.selectUnspentBoxes(toSpend = ergAmount),
      outputs      = List(buyerBidBox),
      fee          = MinTxFee,
      sendChangeTo = contract(buyerPk)
    )
  }

  def sellerOrder(
    sellerParty: Party,
    tokenId: Coll[Byte],
    tokenAmount: Long,
    ergAmount: Long
  ) = {

    val sellerPk = sellerParty.wallet.getAddress.pubKey

    val SellerContract = contract {
      sellerPk || (
        OUTPUTS.size > 1 &&
        OUTPUTS(1).R4[Coll[Byte]].isDefined
      ) && {
        val knownBoxId = OUTPUTS(1).R4[Coll[Byte]].get == SELF.id
        OUTPUTS(1).value >= ergAmount &&
        knownBoxId &&
        OUTPUTS(1).propositionBytes == sellerPk.propBytes
      }
    }

    val sellerBalanceBoxes = sellerParty.selectUnspentBoxes(
      toSpend       = MinErg,
      tokensToSpend = List(tokenId -> tokenAmount)
    )

    val sellerAskBox = Box(
      value  = MinErg,
      token  = (tokenId -> tokenAmount),
      script = SellerContract
    )

    Transaction(
      inputs       = sellerBalanceBoxes,
      outputs      = List(sellerAskBox),
      fee          = MinTxFee,
      sendChangeTo = contract(sellerPk)
    )
  }

  def swapScenario = {

    val blockchainSim = newBlockChainSimulationScenario("Swap")

    val tokenId = newTokenId

    val buyerParty          = blockchainSim.newParty("buyer")
    val buyerBidTokenAmount = 100
    val buyersBidNanoErgs   = 100000000

    buyerParty.generateUnspentBoxes(toSpend = buyersBidNanoErgs)

    val buyOrderTransaction =
      buyerOrder(
        buyerParty,
        tokenId,
        buyerBidTokenAmount,
        buyersBidNanoErgs
      )

    val buyOrderTransactionSigned = buyerParty.wallet.sign(buyOrderTransaction)

    blockchainSim.send(buyOrderTransactionSigned)

    val sellerParty          = blockchainSim.newParty("seller")
    val sellerAskNanoErgs    = 50000000
    val sellerAskTokenAmount = 100L

    sellerParty.generateUnspentBoxes(
      toSpend       = MinErg,
      tokensToSpend = List(tokenId -> sellerAskTokenAmount)
    )

    val sellOrderTransaction =
      sellerOrder(
        sellerParty,
        tokenId,
        sellerAskTokenAmount,
        sellerAskNanoErgs
      )

    val sellOrderTransactionSigned = sellerParty.wallet.sign(sellOrderTransaction)

    blockchainSim.send(sellOrderTransactionSigned)

    val sellerOutBox =
      Box(
        value    = sellerAskNanoErgs,
        register = (R4 -> sellOrderTransactionSigned.outputs(0).id),
        script   = contract(sellerParty.wallet.getAddress.pubKey)
      )

    val buyerOutBox = Box(
      value    = MinErg,
      token    = (tokenId -> buyerBidTokenAmount),
      register = (R4 -> buyOrderTransactionSigned.outputs(0).id),
      script   = contract(buyerParty.wallet.getAddress.pubKey)
    )

    val swapTransaction = Transaction(
      inputs =
        List(buyOrderTransactionSigned.outputs(0), sellOrderTransactionSigned.outputs(0)),
      outputs = List(buyerOutBox, sellerOutBox),
      fee     = MinTxFee
    )

    val dexParty = blockchainSim.newParty("DEX")

    val swapTransactionSigned = dexParty.wallet.sign(swapTransaction)

    blockchainSim.send(swapTransactionSigned)

    sellerParty.printUnspentAssets()
    buyerParty.printUnspentAssets()
    dexParty.printUnspentAssets()
  }

  def refundBuyOrderScenario = {

    val blockchainSim = newBlockChainSimulationScenario("Refund buy order")

    val buyerParty          = blockchainSim.newParty("buyer")
    val buyerBidTokenAmount = 100
    val buyersBidNanoErgs   = 100000000

    buyerParty.generateUnspentBoxes(toSpend = buyersBidNanoErgs)
    val tokenId = newTokenId

    val buyOrderTransaction =
      buyerOrder(
        buyerParty,
        tokenId,
        buyerBidTokenAmount,
        buyersBidNanoErgs
      )

    val buyOrderTransactionSigned = buyerParty.wallet.sign(buyOrderTransaction)

    val buyerRefundBox =
      Box(
        value  = buyersBidNanoErgs,
        token  = (newTokenId -> 1L),
        script = contract(buyerParty.wallet.getAddress.pubKey)
      )

    val cancelBuyTransaction = Transaction(
      inputs  = List(buyOrderTransactionSigned.outputs(0)),
      outputs = List(buyerRefundBox),
      fee     = MinTxFee
    )

    val cancelBuyTransactionSigned = buyerParty.wallet.sign(cancelBuyTransaction)
    blockchainSim.send(cancelBuyTransactionSigned)

    buyerParty.printUnspentAssets()
  }

  def refundSellOrderScenario = {

    val blockchainSim = newBlockChainSimulationScenario("Refund sell order")

    val tokenId              = newTokenId
    val sellerParty          = blockchainSim.newParty("seller")
    val sellerAskNanoErgs    = 50000000
    val sellerAskTokenAmount = 100L

    sellerParty.generateUnspentBoxes(
      toSpend       = MinErg,
      tokensToSpend = List(tokenId -> sellerAskTokenAmount)
    )

    val sellOrderTransaction =
      sellerOrder(
        sellerParty,
        tokenId,
        sellerAskTokenAmount,
        sellerAskNanoErgs
      )

    val sellOrderTransactionSigned = sellerParty.wallet.sign(sellOrderTransaction)

    blockchainSim.send(sellOrderTransactionSigned)
    val sellerRefundBox =
      Box(
        value  = MinErg,
        token  = (tokenId -> sellerAskTokenAmount),
        script = contract(sellerParty.wallet.getAddress.pubKey)
      )

    val cancelSellTransaction = Transaction(
      inputs  = List(sellOrderTransactionSigned.outputs(0)),
      outputs = List(sellerRefundBox),
      fee     = MinTxFee
    )

    val cancelSellTransactionSigned = sellerParty.wallet.sign(cancelSellTransaction)

    blockchainSim.send(cancelSellTransactionSigned)

    sellerParty.printUnspentAssets()
  }

  swapScenario
  refundSellOrderScenario
  refundBuyOrderScenario
}
