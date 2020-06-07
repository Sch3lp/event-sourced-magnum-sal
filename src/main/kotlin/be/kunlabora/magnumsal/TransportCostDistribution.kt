package be.kunlabora.magnumsal

import be.kunlabora.magnumsal.gamepieces.Salts
import be.kunlabora.magnumsal.gamepieces.Zloty

class TransportCostDistribution
private constructor(private val from: PlayerColor,
                    private val paymentPerPlayer: MutableMap<PlayerColor, Zloty> = mutableMapOf()
) {
    fun pay(player: PlayerColor, zloty: Zloty): TransportCostDistribution {
        paymentPerPlayer.merge(player, zloty, Zloty::plus)
        return this
    }

    fun executeTransactions() = paymentPerPlayer.map { (to, amount) -> PaymentTransaction.transaction(from, to, amount) }
    fun totalToPay(): Zloty = paymentPerPlayer.values.sum()
    fun canCover(transportChain: TransportChain, saltMined: Salts) = allPlayersInDistributionAreIn(transportChain)
            && playersWereNotOverpaidAccordingToTheirMaxTransportCost(transportChain, saltMined)

    private fun playersWereNotOverpaidAccordingToTheirMaxTransportCost(transportChain: TransportChain, saltMined: Salts): Boolean {
        val maxTransportCostPerPlayer: Map<PlayerColor, Zloty> = transportChain.maxTransportCostPerPlayer(saltMined)
        return paymentPerPlayer.all { (player, payment) ->
            payment <= (maxTransportCostPerPlayer[player]?: 0)
        }
    }

    private fun allPlayersInDistributionAreIn(transportChain: TransportChain) =
            transportChain.containsAll(paymentPerPlayer.keys)

    companion object TransportCosts {
        fun transportCostDistribution(from: PlayerColor): TransportCostDistribution = TransportCostDistribution(from)
    }
}
