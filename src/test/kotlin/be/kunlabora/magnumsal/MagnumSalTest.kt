package be.kunlabora.magnumsal

import be.kunlabora.magnumsal.MagnumSalEvent.*
import be.kunlabora.magnumsal.PositionInMine.Companion.at
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class MagnumSalTest {

    @Test
    fun `A player can be added`() {
        val eventStream = EventStream()
        val magnumSal = MagnumSal(eventStream)

        magnumSal.addPlayer("Snarf")

        assertThat(eventStream).containsExactly(PlayerWasAdded("Snarf"))
    }

    @Nested
    inner class `Placing miners` {
        @Test
        internal fun `A first worker cannot be placed in a position that is not the first place of the mineshaft`() {
            val eventStream = EventStream()
            val magnumSal = MagnumSal(eventStream)

            assertThatExceptionOfType(IllegalMove::class.java)
                    .isThrownBy {
                        magnumSal.placeWorker(at(2, 0))
                    }
                    .withMessage("The chain rule must be obeyed")
        }

        @Test
        internal fun `A first worker can be placed at the top of the mineshaft`() {
            val eventStream = EventStream()
            val magnumSal = MagnumSal(eventStream)

            magnumSal.placeWorker(at(1, 0))

            assertThat(eventStream).containsExactly(MinerWasPlaced(at(1, 0)))
        }

        @Test
        internal fun `A second worker can be placed at the next position when there's already a miner at the top of the mineshaft`() {
            val eventStream = EventStream()
            val magnumSal = MagnumSal(eventStream)

            magnumSal.placeWorker(at(1, 0))
            magnumSal.placeWorker(at(2, 0))

            assertThat(eventStream).containsExactly(MinerWasPlaced(at(1, 0)), MinerWasPlaced(at(2, 0)))
        }

        @Test
        internal fun `You can't place a miner when a gap would occur`() {
            val eventStream = EventStream()
            val magnumSal = MagnumSal(eventStream)

            magnumSal.placeWorker(at(1, 0))

            assertThatExceptionOfType(IllegalMove::class.java)
                    .isThrownBy {
                        magnumSal.placeWorker(at(2, 1))
                    }
                    .withMessage("The chain rule must be obeyed")
        }

        @Test
        internal fun `You can't place a miner when a gap would occur, when a miner was placed and removed from the previous position`() {
            val eventStream = EventStream()
            val magnumSal = MagnumSal(eventStream)

            magnumSal.placeWorker(at(1, 0))
            magnumSal.placeWorker(at(2, 0))
            magnumSal.removeWorker(at(2, 0))

            assertThatExceptionOfType(IllegalMove::class.java)
                    .isThrownBy {
                        magnumSal.placeWorker(at(2, 1))
                    }
                    .withMessage("The chain rule must be obeyed")
        }
    }

    @Nested
    inner class `Removing Miners` {

        @Test
        internal fun `You can't remove a miner from a mineshaft position where there was never a miner placed`() {
            val eventStream = EventStream()
            val magnumSal = MagnumSal(eventStream)

            magnumSal.placeWorker(at(1, 0))

            assertThatExceptionOfType(IllegalMove::class.java)
                    .isThrownBy {
                        magnumSal.removeWorker(at(2, 0))
                    }
                    .withMessage("There is no miner to be removed at ${at(2,0)}")
        }

        @Test
        internal fun `You can only remove miners when there are still miners present`() {
            val eventStream = EventStream()
            val magnumSal = MagnumSal(eventStream)

            magnumSal.placeWorker(at(1, 0))
            magnumSal.placeWorker(at(2, 0))
            magnumSal.placeWorker(at(2, 0))
            magnumSal.removeWorker(at(2, 0))
            magnumSal.removeWorker(at(2, 0))

            assertThatExceptionOfType(IllegalMove::class.java)
                    .isThrownBy {
                        magnumSal.removeWorker(at(2, 0))
                    }
                    .withMessage("There is no miner to be removed at ${at(2,0)}")
        }

        @Test
        internal fun `You can't remove a miner when a gap would occur`() {
            val eventStream = EventStream()
            val magnumSal = MagnumSal(eventStream)

            magnumSal.placeWorker(at(1, 0))
            magnumSal.placeWorker(at(2, 0))
            magnumSal.placeWorker(at(2, 1))

            assertThatExceptionOfType(IllegalMove::class.java)
                    .isThrownBy {
                        magnumSal.removeWorker(at(2, 0))
                    }
                    .withMessage("The chain rule must be obeyed")
        }

        @Test
        internal fun `You can remove a miner when they are at the top of the mineshaft`() {
            val eventStream = EventStream()
            val magnumSal = MagnumSal(eventStream)

            magnumSal.placeWorker(at(1, 0))
            magnumSal.removeWorker(at(1, 0))

            assertThat(eventStream).contains(MinerWasRemoved(at(1,0)))
        }

        @Test
        internal fun `You can remove a miner when they are the last in the chain`() {
            val eventStream = EventStream()
            val magnumSal = MagnumSal(eventStream)

            magnumSal.placeWorker(at(1, 0))
            magnumSal.placeWorker(at(2, 0))
            magnumSal.placeWorker(at(2, 1))
            magnumSal.placeWorker(at(2, 2))

            magnumSal.removeWorker(at(2, 2))

            assertThat(eventStream).contains(MinerWasRemoved(at(2,2)))
        }

        @Test
        internal fun `You also can remove a miner when there would still be another miner left after removal`() {
            val eventStream = EventStream()
            val magnumSal = MagnumSal(eventStream)

            magnumSal.placeWorker(at(1, 0))
            magnumSal.placeWorker(at(2, 0))
            magnumSal.placeWorker(at(2, 0))
            magnumSal.placeWorker(at(2, 1))

            magnumSal.removeWorker(at(2, 0))

            assertThat(eventStream).contains(MinerWasRemoved(at(2,0)))
        }
    }

    @Nested
    inner class `Scenario Test` {
        @Test
        internal fun `quick-start scenario`() {
            /*
            Player 1 places a miner in the mineshaft's first spot.
            Player 1 removes a miner in the mineshaft's first spot.
            Player 1 places a miner in the mineshaft's first spot again.
            Player 1 places a miner in the mineshaft's second spot.
            Player 1 removes their miner in the first spot. <-- this should be an illegal move, because the first spot should be occupied according to the chain rule.
             */
            val eventStream = EventStream()
            val magnumSal = MagnumSal(eventStream)

            magnumSal.placeWorker(at(1,0))
            magnumSal.removeWorker(at(1,0))
            magnumSal.placeWorker(at(1,0))
            magnumSal.placeWorker(at(2,0))

            assertThatExceptionOfType(IllegalMove::class.java)
                    .isThrownBy { magnumSal.removeWorker(at(1,0)) }
                    .withMessage("The chain rule must be obeyed")
        }
    }
}
