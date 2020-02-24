package org.ergoplatform.playgrounds.examples

object AssetsAtomicExchangePlayground {
  import org.ergoplatform.compiler.ErgoScalaCompiler._
  import org.ergoplatform.playground._

  // TODO: move generateUnspentBoxes to Party

  // TODO: don't pass BlockchainSim, instead of Address pass Party, make Party.selectUnspentBoxes(amountToSpend)
  def buyerOrder(
    blockchainSim: BlockchainSimulation,
    tokenId: Coll[Byte],
    tokenAmount: Long,
    ergAmount: Long,
    buyerAddress: Address
  ) = {

    val BuyerContract = contract {
      buyerAddress.pubKey || {
        (OUTPUTS.nonEmpty && OUTPUTS(0).R4[Coll[Byte]].isDefined) && {
          val tokens = OUTPUTS(0).tokens
          val tokenDataCorrect = tokens.nonEmpty &&
            tokens(0)._1 == tokenId &&
            tokens(0)._2 >= tokenAmount

          val knownId = OUTPUTS(0).R4[Coll[Byte]].get == SELF.id
          tokenDataCorrect && OUTPUTS(0).propositionBytes == buyerAddress.pubKey.propBytes && knownId
        }
      }
    }

    val buyerBalance =
      blockchainSim.selectUnspentBoxesFor(buyerAddress, toSpend = ergAmount)

    val buyerBidBox = Box(value = ergAmount, script = BuyerContract)

    Transaction(
      inputs       = buyerBalance,
      outputs      = List(buyerBidBox),
      fee          = MinTxFee,
      sendChangeTo = contract(buyerAddress.pubKey)
    )
  }

  def sellerOrder(
    blockchainSim: BlockchainSimulation,
    tokenId: Coll[Byte],
    tokenAmount: Long,
    ergAmount: Long,
    sellerAddress: Address
  ) = {

    val SellerContract = contract {
      sellerAddress.pubKey || (
        OUTPUTS.size > 1 &&
        OUTPUTS(1).R4[Coll[Byte]].isDefined
      ) && {
        val knownBoxId = OUTPUTS(1).R4[Coll[Byte]].get == SELF.id
        OUTPUTS(1).value >= ergAmount &&
        knownBoxId &&
        OUTPUTS(1).propositionBytes == sellerAddress.pubKey.propBytes
      }
    }

    val sellerBalanceBoxes = blockchainSim.selectUnspentBoxesFor(
      sellerAddress,
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
      sendChangeTo = contract(sellerAddress.pubKey)
    )
  }

  def swapScenario = {

    val blockchainSim = newBlockChainSimulationScenario("Swap")

    val buyerParty          = blockchainSim.newParty("buyer")
    val buyerBidTokenAmount = 100
    val buyersBidNanoErgs   = 100000000

    blockchainSim.generateUnspentBoxesFor(
      buyerParty.wallet.getAddress,
      toSpend = buyersBidNanoErgs
    )
    val tokenId = newTokenId

    val buyOrderTransaction =
      buyerOrder(
        blockchainSim,
        tokenId,
        buyerBidTokenAmount,
        buyersBidNanoErgs,
        buyerParty.wallet.getAddress
      )

    val buyOrderTransactionSigned = buyerParty.wallet.sign(buyOrderTransaction)

    blockchainSim.send(buyOrderTransactionSigned)

    val sellerParty          = blockchainSim.newParty("seller")
    val sellerAskNanoErgs    = 50000000
    val sellerAskTokenAmount = 100L

    blockchainSim.generateUnspentBoxesFor(
      sellerParty.wallet.getAddress,
      toSpend       = MinErg,
      tokensToSpend = List(tokenId -> sellerAskTokenAmount)
    )

    val sellOrderTransaction =
      sellerOrder(
        blockchainSim,
        tokenId,
        sellerAskTokenAmount,
        sellerAskNanoErgs,
        sellerParty.wallet.getAddress
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

    blockchainSim.printUnspentAssetsFor(sellerParty)
    blockchainSim.printUnspentAssetsFor(buyerParty)
    blockchainSim.printUnspentAssetsFor(dexParty)
  }

  def refundBuyOrderScenario = {

    val blockchainSim = newBlockChainSimulationScenario("Refund buy order")

    val buyerParty          = blockchainSim.newParty("buyer")
    val buyerBidTokenAmount = 100
    val buyersBidNanoErgs   = 100000000

    blockchainSim.generateUnspentBoxesFor(
      buyerParty.wallet.getAddress,
      toSpend = buyersBidNanoErgs
    )
    val tokenId = newTokenId

    val buyOrderTransaction =
      buyerOrder(
        blockchainSim,
        tokenId,
        buyerBidTokenAmount,
        buyersBidNanoErgs,
        buyerParty.wallet.getAddress
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

    blockchainSim.printUnspentAssetsFor(buyerParty)
  }

  def refundSellOrderScenario = {

    val blockchainSim = newBlockChainSimulationScenario("Refund sell order")

    val tokenId              = newTokenId
    val sellerParty          = blockchainSim.newParty("seller")
    val sellerAskNanoErgs    = 50000000
    val sellerAskTokenAmount = 100L

    blockchainSim.generateUnspentBoxesFor(
      sellerParty.wallet.getAddress,
      toSpend       = MinErg,
      tokensToSpend = List(tokenId -> sellerAskTokenAmount)
    )

    val sellOrderTransaction =
      sellerOrder(
        blockchainSim,
        tokenId,
        sellerAskTokenAmount,
        sellerAskNanoErgs,
        sellerParty.wallet.getAddress
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

    blockchainSim.printUnspentAssetsFor(sellerParty)
  }

  swapScenario
  refundSellOrderScenario
  refundBuyOrderScenario
}
