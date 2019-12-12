package be.kunlabora.magnumsal

import be.kunlabora.magnumsal.MagnumSalEvent.*
import be.kunlabora.magnumsal.exception.IllegalMove
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MagnumSalTest {

    private lateinit var eventStream: EventStream
    private lateinit var magnumSal: MagnumSal

    @BeforeEach
    internal fun setUp() {
        eventStream = EventStream()
        magnumSal = MagnumSal(eventStream)
    }

    @Test
    fun `A player can be added`() {
        magnumSal.addPlayer("Snarf")

        assertThat(eventStream).containsExactly(PlayerAdded("Snarf"))
    }

    @Test
    fun `A Miner cannot be placed by a player that's not in the game`() {
        assertThatExceptionOfType(IllegalMove::class.java)
                .isThrownBy { magnumSal.placeMiner("Snarf", MineShaftPosition(1)) }
                .withMessage("Placing a miner requires Snarf to be a player in the game.")
    }

    @Test
    fun `A Miner can be placed in the mineshaft's first position`() {
        magnumSal.addPlayer("Snarf")

        magnumSal.placeMiner("Snarf", MineShaftPosition(1))

        assertThat(eventStream)
                .contains(MinerPlaced("Snarf", MineShaftPosition(1)))
    }
}
