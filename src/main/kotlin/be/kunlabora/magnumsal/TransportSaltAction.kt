package be.kunlabora.magnumsal

import be.kunlabora.magnumsal.exception.transitionRequires
import be.kunlabora.magnumsal.gamepieces.Salts
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TransportSaltAction(private val saltMined: Salts,
                          private val player: PlayerColor,
                          private val at: PositionInMine,
                          private val eventStream: EventStream,
                          private val debugEnabled: Boolean = false) {

    private val zlotyPerPlayer = ZlotyPerPlayer(eventStream)


    fun whenCoveredBy(transportCostDistribution: TransportCostDistribution?, block: (PaymentTransaction) -> Unit) {
        transitionRequires("you to have enough złoty to pay for salt transport bringing your mined salt out of the mine") {
            zlotyPerPlayer.forPlayer(player).debug { "$player currently has $it zł" } >= transportCost(saltMined, player, at)
        }
        if (transportCostDistribution == null) {
            transitionRequires("you to pay for salt transport when your miners can't cover the complete transport chain") {
                transportCost(saltMined, player, at) == 0
            }
        } else {
            transitionRequires("you not to pay more złoty for salt transport than is required") {
                transportCostDistribution.totalToPay().debug { "$player intends to pay $it in total for transport" } <= transportCost(saltMined, player, at)
            }
            transitionRequires("you to only pay miners in the transport chain") {
                everyPlayerInTheDistributionShouldHaveAtLeastOneMinerInTheTransportChain(transportCostDistribution, at, player, eventStream)
            }
            transportCostDistribution.executeTransactions().forEach(block)
        }
    }

    private fun transportCost(saltMined: Salts, player: PlayerColor, at: PositionInMine) =
            saltMined.size.debug { "Amount of transported salt to pay for: $it" } * transportersNeeded(player, at)

    private fun transportersNeeded(player: PlayerColor, fromMineChamber: PositionInMine): Int =
            TransportChain(fromMineChamber, player, eventStream).transportersNeeded().debug { "Miners to pay for transport: $it" }

    private fun everyPlayerInTheDistributionShouldHaveAtLeastOneMinerInTheTransportChain(transportCostDistribution: TransportCostDistribution?, fromMineChamber: PositionInMine, playerThatRequiresTransport: PlayerColor, events: EventStream) : Boolean {
        // does not take into account a player having multiple miners and being paid for that
        // so we could check that a player has at least the same amount of miners than they're being paid for in the transportCostDistribution
        val transportChain = TransportChain(startingFrom = fromMineChamber, playerThatRequiresTransport = playerThatRequiresTransport, eventStream = events)
        return transportCostDistribution?.canCover(transportChain) ?: true
    }

    private inline fun <T> T.debug(block: (T) -> String): T {
        if (debugEnabled) {
            val formattedTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))
            println("[debug ~ $formattedTimestamp] ${block(this)}")
        }
        return this
    }
}
