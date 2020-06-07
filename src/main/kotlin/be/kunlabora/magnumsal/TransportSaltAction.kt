package be.kunlabora.magnumsal

import be.kunlabora.magnumsal.exception.transitionRequires
import be.kunlabora.magnumsal.gamepieces.Salts
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TransportSaltAction(at: PositionInMine,
                          eventStream: EventStream,
                          private val saltMined: Salts,
                          private val player: PlayerColor,
                          private val debugEnabled: Boolean = false) {

    private val transportChain = TransportChain(at, player, eventStream)
    private val zlotyPerPlayer = ZlotyPerPlayer(eventStream)


    fun whenCoveredBy(transportCostDistribution: TransportCostDistribution?, block: (PaymentTransaction) -> Unit) {
        transitionRequires("you to have enough złoty to pay for salt transport bringing your mined salt out of the mine") {
            zlotyPerPlayer.forPlayer(player).debug { "$player currently has $it zł" } >= transportCost(saltMined)
        }
        if (transportCostDistribution == null) {
            transitionRequires("you to pay for salt transport when your miners can't cover the complete transport chain") {
                transportCost(saltMined) == 0
            }
        } else {
            transitionRequires("you not to pay more złoty for salt transport than is required") {
                transportCostDistribution.totalToPay().debug { "$player intends to pay $it in total for transport" } <= transportCost(saltMined)
            }
            transitionRequires("you to only pay miners in the transport chain") {
                transportCanBeCoveredBy(transportCostDistribution, saltMined)
            }
            transportCostDistribution.executeTransactions().forEach(block)
        }
    }

    private fun transportCost(saltMined: Salts) = transportChain.transportCostFor(saltMined)

    private fun transportCanBeCoveredBy(transportCostDistribution: TransportCostDistribution, saltMined: Salts) : Boolean {
        return transportCostDistribution.canCover(transportChain, saltMined)
    }

    //TODO: figure out a way to remove the duplication with debug() in MagnumSal (maybe introduce Debuggable interface?)
    private inline fun <T> T.debug(block: (T) -> String): T {
        if (debugEnabled) {
            val formattedTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))
            println("[debug ~ $formattedTimestamp] ${block(this)}")
        }
        return this
    }
}
