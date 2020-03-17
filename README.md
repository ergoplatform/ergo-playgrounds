# Ergo Playgrounds
Run contracts + off-chain code in the browser. 
1. Design and model a contract along with its off-chain counterpart in the same Scala environment. 
2. Share and discuss the contract. Explain how it works.
3. Check different scenarios of contract execution.

Enjoy:
- seamless on-chain < - > off-chain code integration;
- immediate feedback.

## Example:
### Assets Atomic Exchange (DEX) contract

[Run in Scastie](https://scastie.scala-lang.org/qx8LnkelR124gGkWzdn2wg)

```scala
  import org.ergoplatform.compiler.ErgoScalaCompiler._
  import org.ergoplatform.playground._

  def buyerOrder(
    buyerParty: Party,
    token: TokenInfo,
    tokenAmount: Long,
    ergAmount: Long,
    txFee: Long
  ) = {

    val buyerPk = buyerParty.wallet.getAddress.pubKey

    val BuyerContract = contract {
      buyerPk || {
        (OUTPUTS.nonEmpty && OUTPUTS(0).R4[Coll[Byte]].isDefined) && {
          val tokens = OUTPUTS(0).tokens
          val tokenDataCorrect = tokens.nonEmpty &&
            tokens(0)._1 == token.tokenId &&
            tokens(0)._2 >= tokenAmount

          val knownId = OUTPUTS(0).R4[Coll[Byte]].get == SELF.id
          tokenDataCorrect && OUTPUTS(0).propositionBytes == buyerPk.propBytes && knownId
        }
      }
    }

    val buyerBidBox = Box(value = ergAmount, script = BuyerContract)

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
    ergAmount: Long,
    dexFee: Long,
    txFee: Long
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
      toSpend       = dexFee + txFee,
      tokensToSpend = List(token -> tokenAmount)
    )

    val sellerAskBox = Box(
      value  = dexFee,
      token  = (token -> tokenAmount),
      script = SellerContract
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
    val buyersBidNanoErgs   = 50000000L
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
        buyersBidNanoErgs + buyerDexFee + buyerSwapBoxValue,
        buyOrderTxFee
      )

    val buyOrderTransactionSigned = buyerParty.wallet.sign(buyOrderTransaction)

    blockchainSim.send(buyOrderTransactionSigned)

    val sellerParty          = blockchainSim.newParty("seller")
    val sellerAskNanoErgs    = 50000000L
    val sellerAskTokenAmount = 100L
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
        sellerAskNanoErgs,
        sellerDexFee,
        sellOrderTxFee
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

  def refundBuyOrderScenario = {

    val blockchainSim = newBlockChainSimulationScenario("Refund buy order")

    val buyerParty          = blockchainSim.newParty("buyer")
    val buyerBidTokenAmount = 100
    val buyersBidNanoErgs   = 100000000
    val buyOrderTxFee       = MinTxFee
    val buyerDexFee         = 1000000L
    val buyerSwapBoxValue   = MinErg
    val cancelTxFee         = MinTxFee

    buyerParty
      .generateUnspentBoxes(
        toSpend = buyersBidNanoErgs + buyerDexFee + buyOrderTxFee + buyerSwapBoxValue + cancelTxFee
      )
    val token = blockchainSim.newToken("TKN")

    val buyOrderTransaction =
      buyerOrder(
        buyerParty,
        token,
        buyerBidTokenAmount,
        buyersBidNanoErgs + buyerDexFee + buyerSwapBoxValue,
        buyOrderTxFee
      )

    val buyOrderTransactionSigned = buyerParty.wallet.sign(buyOrderTransaction)

    blockchainSim.send(buyOrderTransactionSigned)

    val buyerRefundBox =
      Box(
        value  = buyersBidNanoErgs,
        token  = (blockchainSim.newToken("DEXCNCL") -> 1L),
        script = contract(buyerParty.wallet.getAddress.pubKey)
      )

    val cancelBuyTransaction = Transaction(
      inputs       = List(buyOrderTransactionSigned.outputs(0)),
      outputs      = List(buyerRefundBox),
      fee          = cancelTxFee,
      sendChangeTo = buyerParty.wallet.getAddress
    )

    val cancelBuyTransactionSigned = buyerParty.wallet.sign(cancelBuyTransaction)
    blockchainSim.send(cancelBuyTransactionSigned)

    buyerParty.printUnspentAssets()
  }

  def refundSellOrderScenario = {

    val blockchainSim = newBlockChainSimulationScenario("Refund sell order")

    val token                = blockchainSim.newToken("TKN")
    val sellerParty          = blockchainSim.newParty("seller")
    val sellerAskNanoErgs    = 50000000L
    val sellerAskTokenAmount = 100L
    val sellerDexFee         = 1000000L
    val sellOrderTxFee       = MinTxFee
    val cancelTxFee          = MinTxFee

    sellerParty.generateUnspentBoxes(
      toSpend       = sellerDexFee + sellOrderTxFee + cancelTxFee,
      tokensToSpend = List(token -> sellerAskTokenAmount)
    )

    val sellOrderTransaction =
      sellerOrder(
        sellerParty,
        token,
        sellerAskTokenAmount,
        sellerAskNanoErgs,
        sellerDexFee,
        sellOrderTxFee
      )

    val sellOrderTransactionSigned = sellerParty.wallet.sign(sellOrderTransaction)

    blockchainSim.send(sellOrderTransactionSigned)
    val sellerRefundBox =
      Box(
        value  = sellerDexFee,
        token  = (token -> sellerAskTokenAmount),
        script = contract(sellerParty.wallet.getAddress.pubKey)
      )

    val cancelSellTransaction = Transaction(
      inputs = List(sellOrderTransactionSigned.outputs(0)) ++ sellerParty
          .selectUnspentBoxes(cancelTxFee),
      outputs = List(sellerRefundBox),
      fee     = cancelTxFee
    )

    val cancelSellTransactionSigned = sellerParty.wallet.sign(cancelSellTransaction)

    blockchainSim.send(cancelSellTransactionSigned)

    sellerParty.printUnspentAssets()
  }

  swapScenario
  refundSellOrderScenario
  refundBuyOrderScenario
```
