package org.ergoplatform.playgrounds.examples

object DEXPlayground {
  import org.ergoplatform.compiler.ErgoScalaCompiler._
  import org.ergoplatform.playgroundenv.utils.ErgoScriptCompiler
  import org.ergoplatform.playground._

  def buyerContract(
    buyerParty: Party,
    token: TokenInfo,
    tokenPrice: Long,
    dexFeePerToken: Long
  ) = {

    val buyerPk = buyerParty.wallet.getAddress.pubKey

    val buyerContractEnv: ScriptEnv =
      Map("buyerPk" -> buyerPk, "tokenId" -> token.tokenId)

    val buyerScript = s"""buyerPk || {

      val tokenPrice = $tokenPrice
      val dexFeePerToken = $dexFeePerToken

      val returnBox = OUTPUTS.filter { (b: Box) => 
        b.R4[Coll[Byte]].isDefined && b.R4[Coll[Byte]].get == SELF.id && b.propositionBytes == buyerPk.propBytes
      }(0)

      val returnTokenData = returnBox.tokens(0)
      val returnTokenId = returnTokenData._1
      val returnTokenAmount = returnTokenData._2
      val maxReturnTokenErgValue = returnTokenAmount * tokenPrice
      val totalReturnErgValue = maxReturnTokenErgValue + returnBox.value
      val expectedDexFee = dexFeePerToken * returnTokenAmount
      
      val foundNewOrderBoxes = OUTPUTS.filter { (b: Box) => 
        b.R4[Coll[Byte]].isDefined && b.R4[Coll[Byte]].get == SELF.id && b.propositionBytes == SELF.propositionBytes
      }

      val coinsSecured = (SELF.value - expectedDexFee) == maxReturnTokenErgValue || {
        foundNewOrderBoxes.size == 1 && foundNewOrderBoxes(0).value >= (SELF.value - totalReturnErgValue - expectedDexFee)
      }

      val tokenIdIsCorrect = returnTokenId == tokenId
    
      allOf(Coll(
          tokenIdIsCorrect,
          returnTokenAmount >= 1,
          coinsSecured
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

    val sellerContractEnv: ScriptEnv =
      Map("sellerPk" -> sellerPk, "tokenId" -> token.tokenId)

    val sellerScript = s""" sellerPk || {
      val tokenPrice = $tokenPrice
      val dexFeePerToken = $dexFeePerToken

      val selfTokenAmount = SELF.tokens(0)._2

      val returnBox = OUTPUTS.filter { (b: Box) => 
        b.R4[Coll[Byte]].isDefined && b.R4[Coll[Byte]].get == SELF.id && b.propositionBytes == sellerPk.propBytes
      }(0)
      
      val foundNewOrderBoxes = OUTPUTS.filter { (b: Box) => 
        b.R4[Coll[Byte]].isDefined && b.R4[Coll[Byte]].get == SELF.id && b.propositionBytes == SELF.propositionBytes
      }

      (returnBox.value == selfTokenAmount * tokenPrice) || {
        foundNewOrderBoxes.size == 1 && {
          val newOrderBox = foundNewOrderBoxes(0)
          val newOrderTokenData = newOrderBox.tokens(0)
          val newOrderTokenAmount = newOrderTokenData._2
          val soldTokenAmount = selfTokenAmount - newOrderTokenAmount
          val minSoldTokenErgValue = soldTokenAmount * tokenPrice
          val expectedDexFee = dexFeePerToken * soldTokenAmount

          val newOrderTokenId = newOrderTokenData._1
          val tokenIdIsCorrect = newOrderTokenId == tokenId

          tokenIdIsCorrect && soldTokenAmount >= 1 && newOrderBox.value >= (SELF.value - minSoldTokenErgValue - expectedDexFee)
        }
      }

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

    val sellOrderTx = Transaction(
      inputs       = sellerBalanceBoxes,
      outputs      = List(sellOrderBox),
      fee          = sellOrderTxFee,
      sendChangeTo = sellerParty.wallet.getAddress
    )

    val sellOrderTxSigned = sellerParty.wallet.sign(sellOrderTx)

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

    val buyOrderAfterPartialMatching  = swapTxPartialMatchingSigned.outputs(1)
    val sellOrderAfterPartialMatching = swapTxPartialMatchingSigned.outputs(3)

    val sellerTokenAmountSoldInTotalMatching =
      sellOrderAfterPartialMatching.additionalTokens(0)._2
    val sellerDexFeeForTotalMatching = sellerDexFeePerToken * sellerTokenAmountSoldInTotalMatching
    val sellerOutBoxForTotalMatching = Box(
      value    = sellerTokenAmountSoldInTotalMatching * sellerAskTokenPrice,
      register = (R4 -> sellOrderAfterPartialMatching.id),
      script   = contract(sellerParty.wallet.getAddress.pubKey)
    )

    val buyerTokenAmountBoughtInTotalMatching = sellerTokenAmountSoldInTotalMatching
    val buyerDexFeeForTotalMatching           = buyerDexFeePerToken * buyerTokenAmountBoughtInTotalMatching
    val buyerOutBoxForTotalMatching = Box(
      value    = buyerSwapBoxValue,
      token    = (token -> buyerTokenAmountBoughtInTotalMatching),
      register = (R4 -> buyOrderAfterPartialMatching.id),
      script   = contract(buyerParty.wallet.getAddress.pubKey)
    )

    val swapTxFeeForTotalMatching = MinTxFee
    val dexFeeForTotalMatching    = sellerDexFeeForTotalMatching + buyerDexFeeForTotalMatching - swapTxFeeForTotalMatching - buyerSwapBoxValue

    val dexFeeOutBoxForTotalMatching = Box(
      value  = dexFeeForTotalMatching,
      script = contract(dexParty.wallet.getAddress.pubKey)
    )

    val swapTxForTotalMatching = Transaction(
      inputs = List(buyOrderAfterPartialMatching, sellOrderAfterPartialMatching),
      outputs = List(
        buyerOutBoxForTotalMatching,
        sellerOutBoxForTotalMatching,
        dexFeeOutBoxForTotalMatching
      ),
      fee = swapTxFeeForTotalMatching
    )

    val swapTxForTotalMatchingSigned = dexParty.wallet.sign(swapTxForTotalMatching)

    blockchainSim.send(swapTxForTotalMatchingSigned)

    sellerParty.printUnspentAssets()
    buyerParty.printUnspentAssets()
    dexParty.printUnspentAssets()

  }

  def cancelBuyOrderScenario = {

    val blockchainSim = newBlockChainSimulationScenario("CancelBuyOrder")

    val token = blockchainSim.newToken("TKN")
    // as a workaround for https://github.com/ScorexFoundation/sigmastate-interpreter/issues/628

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

    val buyOrderTxSigned = buyerParty.wallet.sign(buyOrderTransaction)

    blockchainSim.send(buyOrderTxSigned)

    buyerParty.printUnspentAssets()

    val cancelTxFee = MinTxFee

    val buyerReturnBox = Box(
      value = buyOrderBox.value - cancelTxFee,
      // as a workaround for https://github.com/ScorexFoundation/sigmastate-interpreter/issues/628
      token = (blockchainSim.newToken("DEXCNCL") -> 1L),
      // as a workaround for https://github.com/ScorexFoundation/sigmastate-interpreter/issues/628
      register = (R4 -> buyOrderTxSigned.outputs(0).id),
      script   = contract(buyerParty.wallet.getAddress.pubKey)
    )

    val cancelBuyTransaction = Transaction(
      inputs  = List(buyOrderTxSigned.outputs(0)),
      outputs = List(buyerReturnBox),
      fee     = cancelTxFee
    )

    val cancelBuyTransactionSigned = buyerParty.wallet.sign(cancelBuyTransaction)
    blockchainSim.send(cancelBuyTransactionSigned)

    buyerParty.printUnspentAssets()
  }

  def cancelSellOrderScenario = {

    val blockchainSim = newBlockChainSimulationScenario("CancelSellOrder")

    val token = blockchainSim.newToken("TKN")

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

    val sellOrderTx = Transaction(
      inputs       = sellerBalanceBoxes,
      outputs      = List(sellOrderBox),
      fee          = sellOrderTxFee,
      sendChangeTo = sellerParty.wallet.getAddress
    )

    val sellOrderTxSigned = sellerParty.wallet.sign(sellOrderTx)

    blockchainSim.send(sellOrderTxSigned)

    val cancelTxFee = MinTxFee

    val sellerReturnBox = Box(
      value = sellOrderBox.value - cancelTxFee,
      token = (token -> sellerAskTokenAmount),
      // as a workaround for https://github.com/ScorexFoundation/sigmastate-interpreter/issues/628
      register = (R4 -> sellOrderTxSigned.outputs(0).id),
      script   = contract(sellerParty.wallet.getAddress.pubKey)
    )

    val cancelSellTransaction = Transaction(
      inputs  = List(sellOrderTxSigned.outputs(0)),
      outputs = List(sellerReturnBox),
      fee     = cancelTxFee
    )

    val cancelSellTransactionSigned = sellerParty.wallet.sign(cancelSellTransaction)

    blockchainSim.send(cancelSellTransactionSigned)

    sellerParty.printUnspentAssets()
  }

  swapScenario
  cancelSellOrderScenario
  cancelBuyOrderScenario
}
