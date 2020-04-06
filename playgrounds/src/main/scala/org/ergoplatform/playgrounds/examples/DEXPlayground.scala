package org.ergoplatform.playgrounds.examples

import org.ergoplatform.playgroundenv.models.ErgoScriptCompiler

object DEXPlayground {
  import org.ergoplatform.compiler.ErgoScalaCompiler._
  import org.ergoplatform.playground._
  import sigmastate.interpreter.Interpreter.ScriptEnv

  def buyerContract(
    buyerParty: Party,
    token: TokenInfo,
    tokenPrice: Long
  ) = {

    val buyerPk = buyerParty.wallet.getAddress.pubKey

    val buyerContractEnv: ScriptEnv = Map("pkA" -> buyerPk, "token1" -> token.tokenId)

    // TODO: fix failing value check
    println(s"buyerPk: $buyerPk")
    val buyerScript = s"""pkA || {
      |
      |  // val outIdx = getVar[Short](127).get
      |  val out = OUTPUTS(0)
      |  val tokenData = out.tokens(0)
      |  val tokenId = tokenData._1
      |  val tokenValue = tokenData._2
      |  val outValue = out.value
      |  val price = $tokenPrice
      |
      |  allOf(Coll(
      |      tokenId == token1,
      |      tokenValue >= 1,
      |      (SELF.value - outValue) <= tokenValue * price,
      |      out.propositionBytes == pkA.propBytes,
      |      out.R4[Coll[Byte]].get == SELF.id
      |  ))
      |}
      """.stripMargin

    ErgoScriptCompiler.compile(buyerContractEnv, buyerScript)
  }

  def sellerOrder(
    sellerParty: Party,
    token: TokenInfo,
    tokenAmount: Long,
    tokenPrice: Long,
    dexFee: Long
  ) = {

    val sellerPk = sellerParty.wallet.getAddress.pubKey

    println(s"sellerPk: $sellerPk")
    val sellerContractEnv: ScriptEnv = Map("pkB" -> sellerPk, "token1" -> token.tokenId)

    val sellerScript = s""" pkB || {
      |   // val outIdx = getVar[Short](127).get
      |   val out = OUTPUTS(1)
      |
      |   val tokenData = out.tokens(0)
      |   val tokenId = tokenData._1
      |   val tokenValue = tokenData._2
      |
      |   val selfTokenData = SELF.tokens(0)
      |   val selfTokenId = selfTokenData._1
      |   val selfTokenValue = selfTokenData._2
      |
      |   val selfValue = SELF.value
      |   val outValue = out.value
      |
      |   val sold = selfTokenValue - tokenValue
      |
      |   val price = $tokenPrice
      |   
      |   // val isTotalMatch = out.tokens.size == 0 && outValue >= selfTokenAmount * price  
      |   // val isPartialMatch = out.tokens.size == 1 && out.tokens(0)._1 == selfTokenId && out.tokens(0)._2 >= outValue
      |
      |   allOf(Coll(
      |        sold >= 1,
      |        (outValue - selfValue) >= sold*price,
      |        out.R4[Coll[Byte]].get == SELF.id,
      |        out.propositionBytes == pkB.propBytes
      |   ))
      | }""".stripMargin

    val sellerContract = ErgoScriptCompiler.compile(sellerContractEnv, sellerScript)
    Box(
      value  = dexFee,
      token  = (token -> tokenAmount),
      script = sellerContract
    )
  }

  def swapScenario = {

    val blockchainSim = newBlockChainSimulationScenario(
      "SwapWithPartialAndThenTotalMatching"
    )

    val token = blockchainSim.newToken("TKN")

    val buyerParty          = blockchainSim.newParty("buyer")
    val buyerBidTokenAmount = 100L
    val buyersBidTokenPrice = 500000L
    val buyersBidNanoErgs   = buyersBidTokenPrice * buyerBidTokenAmount
    val buyerDexFee         = 1000000L
    val buyOrderTxFee       = MinTxFee
    val buyerSwapBoxValue   = MinErg

    buyerParty
      .generateUnspentBoxes(
        toSpend = buyersBidNanoErgs + buyOrderTxFee + buyerDexFee + buyerSwapBoxValue
      )

    val buyOrderContract =
      buyerContract(
        buyerParty,
        token,
        buyersBidTokenPrice
      )

    val buyOrderBoxValue = buyersBidTokenPrice * buyerBidTokenAmount + buyerDexFee + buyerSwapBoxValue
    val buyOrderBox      = Box(value = buyOrderBoxValue, script = buyOrderContract)

    val buyOrderTransaction = Transaction(
      inputs       = buyerParty.selectUnspentBoxes(toSpend = buyOrderBoxValue + buyOrderTxFee),
      outputs      = List(buyOrderBox),
      fee          = buyOrderTxFee,
      sendChangeTo = buyerParty.wallet.getAddress
    )

    // TODO: pass context extension
    val buyOrderTransactionSigned = buyerParty.wallet.sign(buyOrderTransaction)

    blockchainSim.send(buyOrderTransactionSigned)

    val sellerParty          = blockchainSim.newParty("seller")
    val sellerAskTokenPrice  = 500000L
    val sellerAskTokenAmount = 100L
    val sellerAskNanoErgs    = sellerAskTokenPrice * sellerAskTokenAmount
    val sellerDexFee         = 1000000L
    val sellOrderTxFee       = MinTxFee

    sellerParty.generateUnspentBoxes(
      toSpend       = sellOrderTxFee + sellerDexFee,
      tokensToSpend = List(token -> sellerAskTokenAmount)
    )

    val sellOrderBox =
      sellerOrder(
        sellerParty,
        token,
        sellerAskTokenAmount,
        sellerAskTokenPrice,
        sellerDexFee
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

    val sellerOutBoxPartialMatch =
      Box(
        value    = sellerAskNanoErgs / 2,
        register = (R4 -> sellOrderTxSigned.outputs(0).id),
        script   = contract(sellerParty.wallet.getAddress.pubKey)
      )

    val newSellOrderBox =
      sellerOrder(
        sellerParty,
        token,
        sellerAskTokenAmount / 2,
        sellerAskTokenPrice,
        sellerDexFee
      )

    val buyerTokenAmountBought = buyerBidTokenAmount / 2
    val buyerOutBoxPartialMatch = Box(
      value    = buyOrderBox.value - buyerTokenAmountBought * buyersBidTokenPrice,
      token    = (token -> buyerTokenAmountBought),
      register = (R4 -> buyOrderTransactionSigned.outputs(0).id),
      script   = contract(buyerParty.wallet.getAddress.pubKey)
    )

    // reuse old contract, nothing is changed
    val newBuyOrderContract = buyOrderContract

    val newBuyOrderBoxValue = buyerOutBoxPartialMatch.value + buyerDexFee + buyerSwapBoxValue
    val newBuyOrderBox      = Box(value = newBuyOrderBoxValue, script = newBuyOrderContract)

    val dexParty = blockchainSim.newParty("DEX")

    val dexFee    = sellerDexFee + buyerDexFee
    val swapTxFee = MinTxFee

    val dexFeeOutBox = Box(
      value  = dexFee - swapTxFee,
      script = contract(dexParty.wallet.getAddress.pubKey)
    )

    val swapTransaction = Transaction(
      inputs = List(buyOrderTransactionSigned.outputs(0), sellOrderTxSigned.outputs(0)),
      outputs = List(
        buyerOutBoxPartialMatch,
        sellerOutBoxPartialMatch,
        newBuyOrderBox,
        newSellOrderBox,
        dexFeeOutBox
      ),
      fee = swapTxFee
    )

    val swapTransactionSigned = dexParty.wallet.sign(swapTransaction)

    blockchainSim.send(swapTransactionSigned)

    sellerParty.printUnspentAssets()
    buyerParty.printUnspentAssets()
    dexParty.printUnspentAssets()
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
