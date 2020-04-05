package org.ergoplatform.playgrounds.examples

import org.ergoplatform.playgroundenv.models.ErgoScriptCompiler

object DEXPlayground {
  import org.ergoplatform.compiler.ErgoScalaCompiler._
  import org.ergoplatform.playground._
  import sigmastate.interpreter.Interpreter.ScriptEnv

  def buyerOrder(
    buyerParty: Party,
    token: TokenInfo,
    tokenAmount: Long,
    tokenPrice: Long,
    dexFee: Long,
    txFee: Long
  ) = {

    val buyerPk = buyerParty.wallet.getAddress.pubKey

    val buyerContractEnv: ScriptEnv = Map("pkA" -> buyerPk, "token1" -> token.tokenId)

    val buyerScript = s"""pkA || {
      |
      |  val outIdx = getVar[Short](127).get
      |  val out = OUTPUTS(outIdx)
      |  val tokenData = out.R2[Coll[(Coll[Byte], Long)]].get(0)
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

    val ergAmount = tokenPrice * tokenAmount + dexFee

    val buyerContract = ErgoScriptCompiler.compile(buyerScript, buyerContractEnv)
    val buyerBidBox   = Box(value = ergAmount, script = buyerContract)

    Transaction(
      inputs       = buyerParty.selectUnspentBoxes(toSpend = ergAmount + txFee),
      outputs      = List(buyerBidBox),
      fee          = txFee,
      sendChangeTo = buyerParty.wallet.getAddress
    )
  }

  def sellerOrder(
    sellerParty: Party,
    token: TokenInfo,
    tokenAmount: Long,
    tokenPrice: Long,
    dexFee: Long,
    txFee: Long
  ) = {

    val sellerPk = sellerParty.wallet.getAddress.pubKey

    val sellerContractEnv: ScriptEnv = Map("pkB" -> sellerPk, "token1" -> token.tokenId)

    val sellerScript = s""" pkB || {
      |   val outIdx = getVar[Short](127).get
      |   val out = OUTPUTS(outIdx)
      |
      |   val tokenData = out.R2[Coll[(Coll[Byte], Long)]].get(0)
      |   val tokenId = tokenData._1
      |   val tokenValue = tokenData._2
      |
      |   val selfTokenData = SELF.R2[Coll[(Coll[Byte], Long)]].get(0)
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
      |   allOf(Coll(
      |        sold >= 1,
      |        (outValue - selfValue) >= sold*price,
      |        out.R4[Coll[Byte]].get == SELF.id,
      |        out.propositionBytes == pkB.propBytes
      |   ))
      | }""".stripMargin

    val sellerContract = ErgoScriptCompiler.compile(sellerScript, sellerContractEnv)
    val sellerBalanceBoxes = sellerParty.selectUnspentBoxes(
      toSpend       = dexFee + txFee,
      tokensToSpend = List(token -> tokenAmount)
    )

    val sellerAskBox = Box(
      value  = dexFee,
      token  = (token -> tokenAmount),
      script = sellerContract
    )

    Transaction(
      inputs       = sellerBalanceBoxes,
      outputs      = List(sellerAskBox),
      fee          = txFee,
      sendChangeTo = sellerParty.wallet.getAddress
    )
  }

  def swapScenario = {

    val blockchainSim = newBlockChainSimulationScenario("Swap")

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

    val buyOrderTransaction =
      buyerOrder(
        buyerParty,
        token,
        buyerBidTokenAmount,
        buyersBidTokenPrice,
        buyerDexFee,
        buyOrderTxFee
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

    val sellOrderTransaction =
      sellerOrder(
        sellerParty,
        token,
        sellerAskTokenAmount,
        sellerAskTokenPrice,
        sellerDexFee,
        sellOrderTxFee
      )

    // TODO: pass context extension
    val sellOrderTransactionSigned = sellerParty.wallet.sign(sellOrderTransaction)

    blockchainSim.send(sellOrderTransactionSigned)

    val sellerOutBox =
      Box(
        value    = sellerAskNanoErgs,
        register = (R4 -> sellOrderTransactionSigned.outputs(0).id),
        script   = contract(sellerParty.wallet.getAddress.pubKey)
      )

    val buyerOutBox = Box(
      value    = buyerSwapBoxValue,
      token    = (token -> buyerBidTokenAmount),
      register = (R4 -> buyOrderTransactionSigned.outputs(0).id),
      script   = contract(buyerParty.wallet.getAddress.pubKey)
    )

    val dexParty = blockchainSim.newParty("DEX")

    val dexFee    = sellerDexFee + buyerDexFee
    val swapTxFee = MinTxFee

    val dexFeeOutBox = Box(
      value  = dexFee - swapTxFee,
      script = contract(dexParty.wallet.getAddress.pubKey)
    )

    val swapTransaction = Transaction(
      inputs =
        List(buyOrderTransactionSigned.outputs(0), sellOrderTransactionSigned.outputs(0)),
      outputs = List(buyerOutBox, sellerOutBox, dexFeeOutBox),
      fee     = swapTxFee
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
