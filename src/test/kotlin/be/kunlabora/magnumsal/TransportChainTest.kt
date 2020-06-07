package be.kunlabora.magnumsal

import be.kunlabora.magnumsal.PlayerColor.*
import be.kunlabora.magnumsal.PositionInMine.Companion.at
import be.kunlabora.magnumsal.gamepieces.Salt.WHITE
import be.kunlabora.magnumsal.gamepieces.Salts
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.jupiter.api.Test

class TransportChainTest {

    private val oneSalt = Salts(WHITE)

    @Test
    fun `maxAmountOfSaltPerPlayer | player at 1,0 and at 2,1 still has 2 transportStrength`() {
        val transportChain = TransportChainTestBuilder(startsAt = at(2, 2),
                playerThatRequiresTransport = White,
                chain = mapOf(
                        at(1, 0) to listOf(Purple),
                        at(2, 0) to listOf(Orange),
                        at(2, 1) to listOf(Purple)
                )).build()

        assertThat(transportChain.maxTransportCostPerPlayer(oneSalt))
                .containsOnly(entry(Purple, 2), entry(Orange, 1))
    }

    @Test
    fun `maxAmountOfSaltPerPlayer | Takes into account multiple players at the same position`() {
        val transportChain = TransportChainTestBuilder(startsAt = at(2, 2),
                playerThatRequiresTransport = White,
                chain = mapOf(
                        at(1, 0) to listOf(Purple),
                        at(2, 0) to listOf(Orange, Purple),
                        at(2, 1) to listOf(Purple)
                )).build()

        assertThat(transportChain.maxTransportCostPerPlayer(oneSalt))
                .containsOnly(entry(Purple, 3), entry(Orange, 1))
    }

    @Test
    fun `maxAmountOfSaltPerPlayer | Excludes positions where the mining player has transportstrength`() {
        val miningPlayer = White
        val transportChain = TransportChainTestBuilder(startsAt = at(2, 2),
                playerThatRequiresTransport = miningPlayer,
                chain = mapOf(
                        at(1, 0) to listOf(Purple, miningPlayer),
                        at(2, 0) to listOf(Orange, Purple),
                        at(2, 1) to listOf(Purple, miningPlayer),
                        at(2, 2) to listOf(Purple, miningPlayer)
                )).build()

        assertThat(transportChain.maxTransportCostPerPlayer(oneSalt))
                .containsOnly(entry(Purple, 1), entry(Orange, 1))
    }

    @Test
    fun `maxAmountOfSaltPerPlayer | Limits by amount of salt being transported`() {
        val miningPlayer = White
        val transportChain = TransportChainTestBuilder(startsAt = at(2, 2),
                playerThatRequiresTransport = miningPlayer,
                chain = mapOf(
                        at(1, 0) to listOf(Purple),
                        at(2, 0) to listOf(Orange, Purple),
                        at(2, 1) to listOf(Purple)
                )).build()

        assertThat(transportChain.maxTransportCostPerPlayer(Salts(WHITE, WHITE)))
                .containsOnly(entry(Purple, 6), entry(Orange, 2))
    }
}
