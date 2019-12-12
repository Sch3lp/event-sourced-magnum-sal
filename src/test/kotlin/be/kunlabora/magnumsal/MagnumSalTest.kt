package be.kunlabora.magnumsal

import be.kunlabora.magnumsal.MagnumSalEvent.*
import be.kunlabora.magnumsal.exception.IllegalMove
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class MagnumSalTest {

    private lateinit var eventStream: EventStream
    private lateinit var magnumSal: MagnumSal

    @BeforeEach
    internal fun setUp() {
        eventStream = EventStream()
        magnumSal = MagnumSal(eventStream)
    }

    @Nested
    inner class AddingPlayers {
        @Test
        fun `A player can be added`() {
            magnumSal.addPlayer("Snarf")

            assertThat(eventStream).containsExactly(PlayerAdded("Snarf"))
        }
    }

    @Nested
    inner class PlacingMiners {
        @Test
        fun `A Miner can be placed in the mineshaft's first position`() {
            magnumSal.addPlayer("Snarf")

            magnumSal.placeMiner("Snarf", MineShaftPosition(1))

            assertThat(eventStream)
                    .contains(MinerPlaced("Snarf", MineShaftPosition(1)))
        }

        @Test
        fun `A Miner cannot be placed by a player that's not in the game`() {
            assertThatExceptionOfType(IllegalMove::class.java)
                    .isThrownBy { magnumSal.placeMiner("Snarf", MineShaftPosition(1)) }
                    .withMessage("Placing a miner requires Snarf to be a player in the game.")

            assertThat(eventStream)
                    .doesNotContain(MinerPlaced("Snarf", MineShaftPosition(1)))
        }

        @Test
        fun `A Miner cannot be placed in the mineshaft's second position without there being a miner in the first position`() {
            magnumSal.addPlayer("Snarf")

            assertThatExceptionOfType(IllegalMove::class.java)
                    .isThrownBy { magnumSal.placeMiner("Snarf", MineShaftPosition(2)) }
                    .withMessage("Placing a miner at mineshaft[2] requires there to be a miner at mineshaft[1].")

            assertThat(eventStream)
                    .doesNotContain(MinerPlaced("Snarf", MineShaftPosition(2)))
        }

        @Test
        fun `A Miner cannot be placed in the mineshaft's 7th position because there is no 7th position`() {
            magnumSal.addPlayer("Snarf")

            assertThatExceptionOfType(IllegalMove::class.java)
                    .isThrownBy { magnumSal.placeMiner("Snarf", MineShaftPosition(7)) }
                    .withMessage("Placing a miner in a non existing position is impossible")
        }
    }

    @Nested
    inner class RemovingMiners {
        @Test
        fun `A Miner can be removed out of the mineshaft if it's the last one in the chain`() {
            magnumSal.addPlayer("Snarf")
            magnumSal.placeMiner("Snarf", MineShaftPosition(1))
            magnumSal.placeMiner("Snarf", MineShaftPosition(2))

            magnumSal.removeMiner("Snarf", MineShaftPosition(2))

            assertThat(eventStream)
                    .contains(MinerRemoved("Snarf", MineShaftPosition(2)))
        }

        @Test
        fun `A Miner can be removed out of the mineshaft if there's another one at its spot in the chain`() {
            magnumSal.addPlayer("Snarf")
            magnumSal.placeMiner("Snarf", MineShaftPosition(1))
            magnumSal.placeMiner("Snarf", MineShaftPosition(1))
            magnumSal.placeMiner("Snarf", MineShaftPosition(2))

            magnumSal.removeMiner("Snarf", MineShaftPosition(1))

            assertThat(eventStream)
                    .contains(MinerRemoved("Snarf", MineShaftPosition(1)))
        }

        @Test
        fun `A Miner cannot be removed out of the mineshaft if it breaks the chain rule`() {
            magnumSal.addPlayer("Snarf")
            magnumSal.placeMiner("Snarf", MineShaftPosition(1))
            magnumSal.placeMiner("Snarf", MineShaftPosition(2))

            assertThatExceptionOfType(IllegalMove::class.java)
                    .isThrownBy { magnumSal.removeMiner("Snarf", MineShaftPosition(1)) }
                    .withMessage("Removing a miner at mineshaft[1] requires there to either be another miner there, or no miner at mineshaft[2]")

            assertThat(eventStream)
                    .doesNotContain(MinerRemoved("Snarf", MineShaftPosition(1)))
        }

        @Test
        fun `A Miner cannot be removed by a player that's not in the game`() {
            assertThatExceptionOfType(IllegalMove::class.java)
                    .isThrownBy { magnumSal.removeMiner("Snarf", MineShaftPosition(1)) }
                    .withMessage("Removing a miner requires Snarf to be a player in the game.")

            assertThat(eventStream)
                    .doesNotContain(MinerRemoved("Snarf", MineShaftPosition(1)))
        }
    }

}
