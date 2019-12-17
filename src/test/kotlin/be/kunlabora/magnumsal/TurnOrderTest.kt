package be.kunlabora.magnumsal

import be.kunlabora.magnumsal.MineShaftPosition.Companion.at
import be.kunlabora.magnumsal.PlayerColor.*
import be.kunlabora.magnumsal.TurnOrder.Companion.onlyInPlayersTurn
import be.kunlabora.magnumsal.exception.IllegalTransitionException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class TurnOrderTest {
    private lateinit var eventStream: EventStream

    @BeforeEach
    internal fun setUp() {
        eventStream = EventStream()
    }

    @Test
    fun `Illegal case in a game with 2 players, first player goes twice after one round`() {
        val magnumSal = MagnumSal(eventStream)
                .withPlayersInOrder("Bruno" using White, "Tim" using Black)

        magnumSal.placeWorkerInMine(White, at(1))
        magnumSal.placeWorkerInMine(Black, at(1))
        magnumSal.placeWorkerInMine(White, at(2))

        assertThatExceptionOfType(IllegalTransitionException::class.java)
                .isThrownBy {
                    onlyInPlayersTurn(eventStream, White) {
                        fail { "this should not be executed, because it's not $White's turn" }
                    }
                }
                .withMessage("Transition requires it to be your turn")
    }

    @Test
    fun `Illegal case in a game with 3 players, third player goes twice`() {
        val magnumSal = MagnumSal(eventStream)
                .withPlayersInOrder("Bruno" using White, "Tim" using Black, "Snarf" using Orange)

        magnumSal.placeWorkerInMine(White, at(1))
        magnumSal.placeWorkerInMine(Black, at(1))
        magnumSal.placeWorkerInMine(Orange, at(1))

        assertThatExceptionOfType(IllegalTransitionException::class.java)
                .isThrownBy {
                    onlyInPlayersTurn(eventStream, Orange) {
                        fail { "this should not be executed, because it's not $Orange's turn" }
                    }
                }
                .withMessage("Transition requires it to be your turn")

        assertThat(eventStream).containsOnlyOnce(MagnumSalEvent.MinerPlaced(Orange, at(1)))
    }

    @Test
    fun `Illegal case in a game with 4 players, fourth player goes twice`() {
        val magnumSal = MagnumSal(eventStream)
                .withPlayersInOrder("Bruno" using White, "Tim" using Black, "Snarf" using Orange, "Gargamel" using Purple)

        magnumSal.placeWorkerInMine(White, at(1))
        magnumSal.placeWorkerInMine(Black, at(1))
        magnumSal.placeWorkerInMine(Orange, at(1))
        magnumSal.placeWorkerInMine(Purple, at(1))

        assertThatExceptionOfType(IllegalTransitionException::class.java)
                .isThrownBy {
                    onlyInPlayersTurn(eventStream, Orange) {
                        fail { "this should not be executed, because it's not $Purple's turn" }
                    }
                }
                .withMessage("Transition requires it to be your turn")

        assertThat(eventStream).containsOnlyOnce(MagnumSalEvent.MinerPlaced(Purple, at(1)))
    }
}
