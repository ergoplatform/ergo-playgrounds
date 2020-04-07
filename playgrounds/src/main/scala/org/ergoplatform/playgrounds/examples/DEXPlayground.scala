package org.ergoplatform.playgrounds.examples

import org.ergoplatform.playgroundenv.models.ErgoScriptCompiler

object DEXPlayground {
  import org.ergoplatform.compiler.ErgoScalaCompiler._
  import org.ergoplatform.playground._
  import sigmastate.interpreter.Interpreter.ScriptEnv

  def buyerContract(
    buyerParty: Party,
    token: TokenInfo,
    tokenPrice: Long,
    dexFeePerToken: Long
  ) = {

    val buyerPk = buyerParty.wallet.getAddress.pubKey

    val buyerContractEnv: ScriptEnv =
      Map("buyerPk" -> buyerPk, "tokenId" -> token.tokenId)

    println(s"buyerPk: $buyerPk")
    val buyerScript = s"""buyerPk || {

      val returnBoxIdx = 0 // getVar[Short](127).get
      val returnBox = OUTPUTS(0)
      val returnTokenData = returnBox.tokens(0)
      val returnTokenId = returnTokenData._1
      val returnTokenAmount = returnTokenData._2
      
      val newOrderBoxId = 1
      val newOrderBox = OUTPUTS(1)
      
      val tokenPrice = $tokenPrice
      val dexFeePerToken = $dexFeePerToken
    
      val iCanSpendReturnBox = returnBox.propositionBytes == buyerPk.propBytes
      val returnBoxRefsMe = returnBox.R4[Coll[Byte]].get == SELF.id
      val tokenIdIsCorrect = returnTokenId == tokenId
    
      val maxReturnTokenErgValue = returnTokenAmount * tokenPrice
      val totalReturnErgValue = maxReturnTokenErgValue + returnBox.value
      val expectedDexFee = dexFeePerToken * returnTokenAmount
      val newOrderBoxValueIsCorrect = newOrderBox.value >= (SELF.value - totalReturnErgValue - expectedDexFee)
    
      val newOrderBoxRefsMe = newOrderBox.R4[Coll[Byte]].get == SELF.id
      val newOrderBoxContractIsCorrect = SELF.propositionBytes == newOrderBox.propositionBytes
    
      allOf(Coll(
          tokenIdIsCorrect,
          returnTokenAmount >= 1,
          iCanSpendReturnBox,
          returnBoxRefsMe,
          newOrderBoxRefsMe,
          newOrderBoxValueIsCorrect,
          newOrderBoxContractIsCorrect
      ))
    }
      """.stripMargin

    ErgoScriptCompiler.compile(buyerContractEnv, buyerScript)
  }

  def sellerOrderContract(
    sellerParty: Party,
    token: TokenInfo,
    tokenPrice: Long,
    dexFeePerToken: Long
  ) = {

    val sellerPk = sellerParty.wallet.getAddress.pubKey

    println(s"sellerPk: $sellerPk")
    val sellerContractEnv: ScriptEnv =
      Map("sellerPk" -> sellerPk, "tokenId" -> token.tokenId)

    val sellerScript = s""" sellerPk || {
      // val outIdx = getVar[Short](127).get
      val returnBox = OUTPUTS(2)
      
      val newOrderBoxId = 1
      val newOrderBox = OUTPUTS(3)
      val newOrderTokenData = newOrderBox.tokens(0)
      val newOrderTokenId = newOrderTokenData._1
      val newOrderTokenAmount = newOrderTokenData._2
      
      val tokenPrice = $tokenPrice
      val dexFeePerToken = $dexFeePerToken
    
      val iCanSpendReturnBox = returnBox.propositionBytes == sellerPk.propBytes
      val returnBoxRefsMe = returnBox.R4[Coll[Byte]].get == SELF.id
      val tokenIdIsCorrect = newOrderTokenId == tokenId
    
      val selfTokenAmount = SELF.tokens(0)._2
      val soldTokenAmount = selfTokenAmount - newOrderTokenAmount
      val minSoldTokenErgValue = soldTokenAmount * tokenPrice
    
      val expectedDexFee = dexFeePerToken * soldTokenAmount
      val newOrderBoxValueIsCorrect = newOrderBox.value >= (SELF.value - minSoldTokenErgValue - expectedDexFee)
    
      val newOrderBoxRefsMe = newOrderBox.R4[Coll[Byte]].get == SELF.id
      val newOrderBoxContractIsCorrect = SELF.propositionBytes == newOrderBox.propositionBytes
    
      allOf(Coll(
        tokenIdIsCorrect,
        soldTokenAmount >= 1,
        iCanSpendReturnBox,
        returnBoxRefsMe,
        newOrderBoxRefsMe,
        newOrderBoxValueIsCorrect,
        newOrderBoxContractIsCorrect
      ))
      }""".stripMargin

    ErgoScriptCompiler.compile(sellerContractEnv, sellerScript)
  }

  def swapScenario = {

    val blockchainSim = newBlockChainSimulationScenario(
      "SwapWithPartialAndThenTotalMatching"
    )

    val token = blockchainSim.newToken("TKN")

    val buyerParty          = blockchainSim.newParty("buyer")
    val buyerBidTokenAmount = 100L
    val buyersBidTokenPrice = 5000000L
    val buyersBidNanoErgs   = buyersBidTokenPrice * buyerBidTokenAmount
    val buyerDexFee         = 10000000L
    val buyerDexFeePerToken = buyerDexFee / buyerBidTokenAmount
    val buyOrderTxFee       = MinTxFee
    val buyerSwapBoxValue   = MinErg

    buyerParty
      .generateUnspentBoxes(
        toSpend = buyersBidNanoErgs + buyOrderTxFee + buyerDexFee
      )

    val sellerParty          = blockchainSim.newParty("seller")
    val sellerAskTokenPrice  = 5000000L
    val sellerAskTokenAmount = 100L
    val sellerAskNanoErgs    = sellerAskTokenPrice * sellerAskTokenAmount
    val sellerDexFee         = 10000000L
    val sellerDexFeePerToken = sellerDexFee / sellerAskTokenAmount
    val sellOrderTxFee       = MinTxFee

    sellerParty.generateUnspentBoxes(
      toSpend       = sellOrderTxFee + sellerDexFee,
      tokensToSpend = List(token -> sellerAskTokenAmount)
    )

    sellerParty.printUnspentAssets()
    buyerParty.printUnspentAssets()

    val buyOrderContract =
      buyerContract(
        buyerParty,
        token,
        buyersBidTokenPrice,
        buyerDexFeePerToken
      )

    val buyOrderBoxValue = buyersBidTokenPrice * buyerBidTokenAmount + buyerDexFee
    val buyOrderBox      = Box(value = buyOrderBoxValue, script = buyOrderContract)

    val buyOrderTransaction = Transaction(
      inputs       = buyerParty.selectUnspentBoxes(toSpend = buyOrderBoxValue + buyOrderTxFee),
      outputs      = List(buyOrderBox),
      fee          = buyOrderTxFee,
      sendChangeTo = buyerParty.wallet.getAddress
    )

    // TODO: pass context extension
    val buyOrderTxSigned = buyerParty.wallet.sign(buyOrderTransaction)

    blockchainSim.send(buyOrderTxSigned)

    val sellOrderContract = sellerOrderContract(
      sellerParty,
      token,
      sellerAskTokenPrice,
      sellerDexFeePerToken
    )

    val sellOrderBox = Box(
      value  = sellerDexFee,
      token  = (token -> sellerAskTokenAmount),
      script = sellOrderContract
    )

    val sellerBalanceBoxes = sellerParty.selectUnspentBoxes(
      toSpend       = sellerDexFee + sellOrderTxFee,
      tokensToSpend = List(token -> sellerAskTokenAmount)
    )

    val sellerOrderTx = Transaction(
      inputs       = sellerBalanceBoxes,
      outputs      = List(sellOrderBox),
      fee          = sellOrderTxFee,
      sendChangeTo = sellerParty.wallet.getAddress
    )

    // TODO: pass context extension
    val sellOrderTxSigned = sellerParty.wallet.sign(sellerOrderTx)

    blockchainSim.send(sellOrderTxSigned)

    val sellerTokenAmountSold       = sellerAskTokenAmount / 2
    val sellerDexFeeForPartialMatch = sellerDexFeePerToken * sellerTokenAmountSold
    val sellerOutBoxPartialMatch = Box(
      value    = sellerTokenAmountSold * sellerAskTokenPrice,
      register = (R4 -> sellOrderTxSigned.outputs(0).id),
      script   = contract(sellerParty.wallet.getAddress.pubKey)
    )

    // reuse old contract, nothing is changed
    val newSellOrderContract = sellOrderContract

    val newSellOrderBox = Box(
      value    = sellOrderBox.value - sellerDexFeeForPartialMatch,
      token    = (token -> (sellerAskTokenAmount - sellerTokenAmountSold)),
      register = (R4 -> sellOrderTxSigned.outputs(0).id),
      script   = newSellOrderContract
    )

    val buyerTokenAmountBought     = buyerBidTokenAmount / 2
    val buyerDexFeeForPartialMatch = buyerDexFeePerToken * buyerTokenAmountBought
    val buyerOutBoxPartialMatch = Box(
      value    = buyerSwapBoxValue,
      token    = (token -> buyerTokenAmountBought),
      register = (R4 -> buyOrderTxSigned.outputs(0).id),
      script   = contract(buyerParty.wallet.getAddress.pubKey)
    )

    // reuse old contract, nothing is changed
    val newBuyOrderContract = buyOrderContract

    val newBuyOrderBoxValue = buyOrderBox.value - buyerTokenAmountBought * buyersBidTokenPrice - buyerDexFeeForPartialMatch
    val newBuyOrderBox = Box(
      value    = newBuyOrderBoxValue,
      register = (R4 -> buyOrderTxSigned.outputs(0).id),
      script   = newBuyOrderContract
    )

    val dexParty = blockchainSim.newParty("DEX")

    val swapTxFee                = MinTxFee
    val dexFeeForPartialMatching = sellerDexFeeForPartialMatch + buyerDexFeeForPartialMatch - swapTxFee - buyerSwapBoxValue

    val dexFeeOutBoxForPartialMatching = Box(
      value  = dexFeeForPartialMatching,
      script = contract(dexParty.wallet.getAddress.pubKey)
    )

    val swapTxPartialMatching = Transaction(
      inputs = List(buyOrderTxSigned.outputs(0), sellOrderTxSigned.outputs(0)),
      outputs = List(
        buyerOutBoxPartialMatch,
        newBuyOrderBox,
        sellerOutBoxPartialMatch,
        newSellOrderBox,
        dexFeeOutBoxForPartialMatching
      ),
      fee = swapTxFee
    )

    val swapTxPartialMatchingSigned = dexParty.wallet.sign(swapTxPartialMatching)

    blockchainSim.send(swapTxPartialMatchingSigned)

    sellerParty.printUnspentAssets()
    buyerParty.printUnspentAssets()
    dexParty.printUnspentAssets()

    // ------------------------------------------------------------------------
    // Total matching
    // ------------------------------------------------------------------------
    {
      val buyOrder  = swapTxPartialMatchingSigned.outputs(1)
      val sellOrder = swapTxPartialMatchingSigned.outputs(3)

      val sellerTokenAmountSold = sellOrder.additionalTokens(0)._2
      val sellerDexFee          = sellerDexFeePerToken * sellerTokenAmountSold
      val sellerOutBox = Box(
        value    = sellerTokenAmountSold * sellerAskTokenPrice,
        register = (R4 -> sellOrder.id),
        script   = contract(sellerParty.wallet.getAddress.pubKey)
      )

      val buyerTokenAmountBought = sellerTokenAmountSold
      val buyerDexFee            = buyerDexFeePerToken * buyerTokenAmountBought
      val buyerOutBox = Box(
        value    = buyerSwapBoxValue,
        token    = (token -> buyerTokenAmountBought),
        register = (R4 -> buyOrder.id),
        script   = contract(buyerParty.wallet.getAddress.pubKey)
      )

      val swapTxFee = MinTxFee
      val dexFee    = sellerDexFee + buyerDexFee - swapTxFee - buyerSwapBoxValue

      val dexFeeOutBox = Box(
        value  = dexFee,
        script = contract(dexParty.wallet.getAddress.pubKey)
      )

      val swapTx = Transaction(
        inputs = List(buyOrder, sellOrder),
        outputs = List(
          buyerOutBox,
          sellerOutBox,
          dexFeeOutBox
        ),
        fee = swapTxFee
      )

      val swapTxSigned = dexParty.wallet.sign(swapTx)

      blockchainSim.send(swapTxSigned)

      sellerParty.printUnspentAssets()
      buyerParty.printUnspentAssets()
      dexParty.printUnspentAssets()
    }

  }

  // def refundBuyOrderScenario = {

  //   val blockchainSim = newBlockChainSimulationScenario("Refund buy order")

  //   val buyerParty          = blockchainSim.newParty("buyer")
  //   val buyerBidTokenAmount = 100
  //   val buyersBidNanoErgs   = 100000000
  //   val buyOrderTxFee       = MinTxFee
  //   val buyerDexFee         = 1000000L
  //   val buyerSwapBoxValue   = MinErg
  //   val cancelTxFee         = MinTxFee

  //   buyerParty
  //     .generateUnspentBoxes(
  //       toSpend = buyersBidNanoErgs + buyerDexFee + buyOrderTxFee + buyerSwapBoxValue + cancelTxFee
  //     )
  //   val token = blockchainSim.newToken("TKN")

  //   val buyOrderTransaction =
  //     buyerOrder(
  //       buyerParty,
  //       token,
  //       buyerBidTokenAmount,
  //       buyersBidNanoErgs + buyerSwapBoxValue,
  //       buyerDexFee,
  //       buyOrderTxFee
  //     )

  //   val buyOrderTransactionSigned = buyerParty.wallet.sign(buyOrderTransaction)

  //   blockchainSim.send(buyOrderTransactionSigned)

  //   val buyerRefundBox =
  //     Box(
  //       value  = buyersBidNanoErgs,
  //       token  = (blockchainSim.newToken("DEXCNCL") -> 1L),
  //       script = contract(buyerParty.wallet.getAddress.pubKey)
  //     )

  //   val cancelBuyTransaction = Transaction(
  //     inputs       = List(buyOrderTransactionSigned.outputs(0)),
  //     outputs      = List(buyerRefundBox),
  //     fee          = cancelTxFee,
  //     sendChangeTo = buyerParty.wallet.getAddress
  //   )

  //   val cancelBuyTransactionSigned = buyerParty.wallet.sign(cancelBuyTransaction)
  //   blockchainSim.send(cancelBuyTransactionSigned)

  //   buyerParty.printUnspentAssets()
  // }

//   def refundSellOrderScenario = {

//     val blockchainSim = newBlockChainSimulationScenario("Refund sell order")

//     val token                = blockchainSim.newToken("TKN")
//     val sellerParty          = blockchainSim.newParty("seller")
//     val sellerAskNanoErgs    = 50000000L
//     val sellerAskTokenAmount = 100L
//     val sellerDexFee         = 1000000L
//     val sellOrderTxFee       = MinTxFee
//     val cancelTxFee          = MinTxFee

//     sellerParty.generateUnspentBoxes(
//       toSpend       = sellerDexFee + sellOrderTxFee + cancelTxFee,
//       tokensToSpend = List(token -> sellerAskTokenAmount)
//     )

//     val sellOrderTransaction =
//       sellerOrder(
//         sellerParty,
//         token,
//         sellerAskTokenAmount,
//         sellerAskNanoErgs,
//         sellerDexFee,
//         sellOrderTxFee
//       )

//     val sellOrderTransactionSigned = sellerParty.wallet.sign(sellOrderTransaction)

//     blockchainSim.send(sellOrderTransactionSigned)
//     val sellerRefundBox =
//       Box(
//         value  = sellerDexFee,
//         token  = (token -> sellerAskTokenAmount),
//         script = contract(sellerParty.wallet.getAddress.pubKey)
//       )

//     val cancelSellTransaction = Transaction(
//       inputs = List(sellOrderTransactionSigned.outputs(0)) ++ sellerParty
//           .selectUnspentBoxes(cancelTxFee),
//       outputs = List(sellerRefundBox),
//       fee     = cancelTxFee
//     )

//     val cancelSellTransactionSigned = sellerParty.wallet.sign(cancelSellTransaction)

//     blockchainSim.send(cancelSellTransactionSigned)

//     sellerParty.printUnspentAssets()
//   }

  swapScenario
//   refundSellOrderScenario
//   refundBuyOrderScenario
}
