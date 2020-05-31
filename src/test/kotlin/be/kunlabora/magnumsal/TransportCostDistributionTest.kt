package be.kunlabora.magnumsal

import be.kunlabora.magnumsal.PlayerColor.*
import be.kunlabora.magnumsal.PositionInMine.Companion.at
import be.kunlabora.magnumsal.TransportCostDistribution.TransportCosts.transportCostDistribution
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TransportCostDistributionTest {

    @Test
    fun `canCover | Can cover the transport chain`() {
        val distribution = with(transportCostDistribution(White)) {
            pay(Orange, 2)
            pay(Purple, 3)
        }

        val transportChain = TransportChainTestBuilder(startsAt = at(6, 0),
                playerThatRequiresTransport = White,
                chain = mapOf(
                        at(1, 0) to listOf(Orange),
                        at(2, 0) to listOf(Orange),
                        at(3, 0) to listOf(Purple),
                        at(4, 0) to listOf(Purple),
                        at(5, 0) to listOf(Purple),
                        at(6, 0) to listOf(White)
                )
        )

        assertThat(distribution.canCover(transportChain.build())).isTrue()
    }

    @Test
    fun `canCover | Distribution holds player that's not in the transport chain`() {
        val distribution = with(transportCostDistribution(White)) {
            pay(Orange, 1)
            pay(Purple, 1)
        }

        val transportChain = TransportChainTestBuilder(startsAt = at(6, 0),
                playerThatRequiresTransport = White,
                chain = mapOf(
                        at(1, 0) to listOf(Purple, White),
                        at(2, 0) to listOf(Purple)
                )
        )

        assertThat(distribution.canCover(transportChain.build())).isFalse()
    }

    @Test
    fun `canCover | Distribution holds player that's in the transport chain, but at a position occupied by the paying player`() {
        val distribution = with(transportCostDistribution(White)) {
            pay(Orange, 1)
            pay(Purple, 1)
        }

        val transportChain = TransportChainTestBuilder(startsAt = at(6, 0),
                playerThatRequiresTransport = White,
                chain = mapOf(
                        at(1, 0) to listOf(Purple, White, Orange),
                        at(2, 0) to listOf(Purple)
                )
        )

        assertThat(distribution.canCover(transportChain.build())).isFalse()
    }
}
