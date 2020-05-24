package be.kunlabora.magnumsal

import be.kunlabora.magnumsal.MagnumSalEvent.*
import be.kunlabora.magnumsal.MagnumSalEvent.MinerMovementEvent.MinerPlaced
import be.kunlabora.magnumsal.MagnumSalEvent.MinerMovementEvent.MinerRemoved
import be.kunlabora.magnumsal.MagnumSalEvent.PaymentEvent.ZlotyPaid
import be.kunlabora.magnumsal.MagnumSalEvent.PaymentEvent.ZlotyReceived
import be.kunlabora.magnumsal.PlayerColor.*
import be.kunlabora.magnumsal.PositionInMine.Companion.at
import be.kunlabora.magnumsal.TransportCostDistribution.TransportCosts.transportCostDistribution
import be.kunlabora.magnumsal.exception.IllegalTransitionException
import be.kunlabora.magnumsal.gamepieces.*
import be.kunlabora.magnumsal.gamepieces.Salt.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class MagnumSalTest {

    private lateinit var eventStream: EventStream

    @BeforeEach
    internal fun setUp() {
        eventStream = EventStream()
    }

    @Nested
    inner class AddingPlayers {
        @Test
        fun `Cannot add two players with the same color`() {
            val magnumSal = TestMagnumSal(eventStream).build()
            magnumSal.addPlayer("Tim", Black)

            assertThatExceptionOfType(IllegalTransitionException::class.java)
                    .isThrownBy { magnumSal.addPlayer("Bruno", Black) }

            assertThat(eventStream)
                    .containsExactly(PlayerJoined("Tim", Black))
                    .doesNotContain(PlayerJoined("Bruno", Black))
        }

        @Test
        fun `Cannot add a fifth player`() {
            val magnumSal = TestMagnumSal(eventStream)
                    .withPlayersInOrder(
                            "Bruno" using White,
                            "Tim" using Black,
                            "Nele" using Orange,
                            "Jan" using Purple)
                    .build()

            assertThatExceptionOfType(IllegalTransitionException::class.java)
                    .isThrownBy { magnumSal.addPlayer("Snarf", Orange) }

            assertThat(eventStream)
                    .doesNotContain(PlayerJoined("Snarf", Orange))
        }
    }

    @Nested
    inner class DeterminingPlayOrder {
        @Test
        fun `Cannot determine a player order when only one player joined`() {
            val magnumSal = TestMagnumSal(eventStream).build()
            magnumSal.addPlayer("Tim", Black)

            assertThatExceptionOfType(IllegalTransitionException::class.java)
                    .isThrownBy { magnumSal.determinePlayOrder(White, Black) }

            assertThat(eventStream).containsExactly(PlayerJoined("Tim", Black))
        }

        @Test
        fun `Cannot determine a player order with colors that players didn't choose`() {
            val magnumSal = TestMagnumSal(eventStream)
                    .withPlayers("Bruno" using White, "Tim" using Black)
                    .build()

            assertThatExceptionOfType(IllegalTransitionException::class.java)
                    .isThrownBy { magnumSal.determinePlayOrder(Orange, Black) }

            assertThat(eventStream.filterEvents<PlayerOrderDetermined>()).isEmpty()
        }

        @Test
        fun `Cannot determine a player order with two same colors`() {
            val magnumSal = TestMagnumSal(eventStream)
                    .withPlayers("Bruno" using White, "Tim" using Black)
                    .build()

            assertThatExceptionOfType(IllegalTransitionException::class.java)
                    .isThrownBy { magnumSal.determinePlayOrder(Black, Black) }
                    .withMessage("Transition requires player colors to be unique")

            assertThat(eventStream.filterEvents<PlayerOrderDetermined>()).isEmpty()
        }

        @Test
        fun `Can determine a player order when at least two players joined`() {
            val magnumSal = TestMagnumSal(eventStream)
                    .withPlayers("Bruno" using White, "Tim" using Black)
                    .build()

            magnumSal.determinePlayOrder(White, Black)

            assertThat(eventStream).contains(PlayerOrderDetermined(White, Black))
        }

        @Test
        fun `Can determine a player order for three players`() {
            val magnumSal = TestMagnumSal(eventStream)
                    .withPlayers("Bruno" using White,
                            "Tim" using Black,
                            "Snarf" using Purple)
                    .build()

            magnumSal.determinePlayOrder(Black, Purple, White)

            assertThat(eventStream).contains(PlayerOrderDetermined(Black, Purple, White))
        }

        @Test
        fun `Can determine a player order for four players`() {
            val magnumSal = TestMagnumSal(eventStream)
                    .withPlayers("Bruno" using White,
                            "Tim" using Black,
                            "Gargamel" using Orange,
                            "Snarf" using Purple)
                    .build()

            magnumSal.determinePlayOrder(Orange, Black, Purple, White)

            assertThat(eventStream).contains(PlayerOrderDetermined(Orange, Black, Purple, White))
        }

        @Test
        fun `Starting zł are doled out after order is determined`() {
            val magnumSal = TestMagnumSal(eventStream)
                    .withPlayers("Bruno" using White,
                            "Tim" using Black,
                            "Gargamel" using Orange,
                            "Snarf" using Purple)
                    .build()

            magnumSal.determinePlayOrder(Orange, Black, Purple, White)

            assertThat(eventStream)
                    .containsExactly(
                            PlayerJoined("Bruno", White),
                            PlayerJoined("Tim", Black),
                            PlayerJoined("Gargamel", Orange),
                            PlayerJoined("Snarf", Purple),
                            PlayerOrderDetermined(Orange, Black, Purple, White),
                            ZlotyReceived(Orange, 10),
                            ZlotyReceived(Black, 12),
                            ZlotyReceived(Purple, 14),
                            ZlotyReceived(White, 16)
                    )
        }

        @Test
        fun `Starting zł are doled out only to participating players after order is determined`() {
            val magnumSal = TestMagnumSal(eventStream)
                    .withPlayers("Bruno" using White,
                            "Tim" using Black)
                    .build()

            magnumSal.determinePlayOrder(Black, White)

            assertThat(eventStream)
                    .containsExactly(
                            PlayerJoined("Bruno", White),
                            PlayerJoined("Tim", Black),
                            PlayerOrderDetermined(Black, White),
                            ZlotyReceived(Black, 10),
                            ZlotyReceived(White, 12)
                    )
        }
    }

    @Nested
    inner class PlacingWorkersInTheMine {
        @Test
        fun `Can place a worker in Shaft 1`() {
            val magnumSal = TestMagnumSal(eventStream)
                    .withPlayersInOrder("Bruno" using White, "Tim" using Black)
                    .build()

            magnumSal.placeWorkerInMine(White, at(1, 0))

            assertThat(eventStream).contains(MinerPlaced(White, at(1, 0)))
        }

        @Test
        fun `Cannot place a worker in Shaft 2 when Shaft 1 is unoccupied`() {
            val magnumSal = TestMagnumSal(eventStream)
                    .withPlayersInOrder("Bruno" using White, "Tim" using Black)
                    .build()

            assertThatExceptionOfType(IllegalTransitionException::class.java)
                    .isThrownBy { magnumSal.placeWorkerInMine(White, at(2, 0)) }

            assertThat(eventStream).doesNotContain(MinerPlaced(White, at(2, 0)))
        }

        @Test
        fun `Second player can also place a worker in shaft 1`() {
            val magnumSal = TestMagnumSal(eventStream)
                    .withPlayersInOrder("Bruno" using White, "Tim" using Black)
                    .build()
            magnumSal.placeWorkerInMine(White, at(1, 0))

            magnumSal.placeWorkerInMine(Black, at(1, 0))

            assertThat(eventStream).contains(MinerPlaced(Black, at(1, 0)))
        }

        @Test
        fun `Second player can place a worker in shaft 2`() {
            val magnumSal = TestMagnumSal(eventStream)
                    .withPlayersInOrder("Bruno" using White, "Tim" using Black)
                    .build()
            magnumSal.placeWorkerInMine(White, at(1, 0))

            magnumSal.placeWorkerInMine(Black, at(2, 0))

            assertThat(eventStream).contains(MinerPlaced(Black, at(2, 0)))
        }

        @Test
        fun `Second player cannot place a worker in shaft 3`() {
            val magnumSal = TestMagnumSal(eventStream)
                    .withPlayersInOrder("Bruno" using White, "Tim" using Black)
                    .build()
            magnumSal.placeWorkerInMine(White, at(1, 0))
            assertThatExceptionOfType(IllegalTransitionException::class.java)
                    .isThrownBy { magnumSal.placeWorkerInMine(Black, at(3, 0)) }

            assertThat(eventStream).doesNotContain(MinerPlaced(Black, at(3, 0)))
        }

        @Test
        fun `Cannot place two workers in one turn`() {
            val magnumSal = TestMagnumSal(eventStream)
                    .withPlayersInOrder("Bruno" using White, "Tim" using Black)
                    .build()

            magnumSal.placeWorkerInMine(White, at(1, 0))
            assertThatExceptionOfType(IllegalTransitionException::class.java)
                    .isThrownBy { magnumSal.placeWorkerInMine(White, at(2, 0)) }

            assertThat(eventStream)
                    .contains(MinerPlaced(White, at(1, 0)))
                    .doesNotContain(MinerPlaced(White, at(2, 0)))
        }

        @Test
        fun `Cannot place a worker when it's not your turn`() {
            val magnumSal = TestMagnumSal(eventStream)
                    .withPlayersInOrder("Bruno" using White, "Tim" using Black)
                    .build()

            assertThatExceptionOfType(IllegalTransitionException::class.java)
                    .isThrownBy { magnumSal.placeWorkerInMine(Black, at(1, 0)) }

            assertThat(eventStream)
                    .doesNotContain(MinerPlaced(Black, at(1, 0)))
        }

        @Test
        fun `Cannot place a worker when you're out of workers`() {
            val magnumSal = TestMagnumSal(eventStream)
                    .withPlayersInOrder("Bruno" using White, "Tim" using Black)
                    .distributeWorkersInTheMineShaft(5, listOf(White, Black))
                    .build()

            magnumSal.visualizeMiners()

            assertThatExceptionOfType(IllegalTransitionException::class.java)
                    .isThrownBy { magnumSal.placeWorkerInMine(White, at(6, 0)) }
                    .withMessage("Transition requires you to have enough available workers")

            assertThat(eventStream).doesNotContain(
                    MinerPlaced(White, at(6, 0))
            )
        }

        @Test
        fun `Players can fill up the shaft`() {
            val magnumSal = TestMagnumSal(eventStream)
                    .withPlayersInOrder("Bruno" using White, "Tim" using Black)
                    .build()

            magnumSal.placeWorkerInMine(White, at(1, 0))
            magnumSal.placeWorkerInMine(Black, at(2, 0))
            magnumSal.placeWorkerInMine(White, at(3, 0))
            magnumSal.placeWorkerInMine(White, at(4, 0))
            magnumSal.placeWorkerInMine(Black, at(5, 0))
            magnumSal.placeWorkerInMine(Black, at(6, 0))

            assertThat(eventStream).contains(
                    MinerPlaced(White, at(1, 0)),
                    MinerPlaced(Black, at(2, 0)),
                    MinerPlaced(White, at(3, 0)),
                    MinerPlaced(White, at(4, 0)),
                    MinerPlaced(Black, at(5, 0)),
                    MinerPlaced(Black, at(6, 0))
            )
        }
    }

    @Nested
    inner class RemovingMinersFromTheMine {
        @Test
        fun `can remove worker if it does not break the chain`() {
            val magnumSal = TestMagnumSal(eventStream)
                    .withPlayersInOrder("Bruno" using White, "Tim" using Black)
                    .build()
            magnumSal.placeWorkerInMine(White, at(1, 0))
            magnumSal.placeWorkerInMine(Black, at(1, 0))

            magnumSal.removeWorkerFromMine(White, at(1, 0))

            assertThat(eventStream).contains(MinerRemoved(White, at(1, 0)))
        }

        @Test
        fun `cannot remove worker if it would break the chain 2nd scenario`() {
            val magnumSal = TestMagnumSal(eventStream)
                    .withPlayersInOrder("Bruno" using White, "Tim" using Black)
                    .build()
            magnumSal.placeWorkerInMine(White, at(1, 0))
            magnumSal.placeWorkerInMine(Black, at(1, 0))
            magnumSal.placeWorkerInMine(White, at(2, 0))
            magnumSal.removeWorkerFromMine(White, at(1, 0))
            magnumSal.placeWorkerInMine(Black, at(2, 0))

            assertThatExceptionOfType(IllegalTransitionException::class.java)
                    .isThrownBy { magnumSal.removeWorkerFromMine(Black, at(1, 0)) }

            assertThat(eventStream).doesNotContain(MinerRemoved(Black, at(1, 0)))
        }

        @Test
        fun `cannot remove worker if it's not your turn`() {
            val magnumSal = TestMagnumSal(eventStream)
                    .withPlayersInOrder("Bruno" using White, "Tim" using Black)
                    .build()
            magnumSal.placeWorkerInMine(White, at(1, 0))
            magnumSal.placeWorkerInMine(Black, at(1, 0))
            magnumSal.removeWorkerFromMine(White, at(1, 0))

            assertThatExceptionOfType(IllegalTransitionException::class.java)
                    .isThrownBy { magnumSal.removeWorkerFromMine(White, at(1, 0)) }

            assertThat(eventStream).containsOnlyOnce(MinerRemoved(White, at(1, 0)))
        }

        @Test
        fun `cannot remove worker if there's no worker`() {
            val magnumSal = TestMagnumSal(eventStream)
                    .withPlayersInOrder("Bruno" using White, "Tim" using Black)
                    .build()
            magnumSal.placeWorkerInMine(White, at(1, 0))
            magnumSal.placeWorkerInMine(Black, at(1, 0))

            assertThatExceptionOfType(IllegalTransitionException::class.java)
                    .isThrownBy { magnumSal.removeWorkerFromMine(White, at(2, 0)) }

            assertThat(eventStream).doesNotContain(MinerRemoved(White, at(2, 0)))
        }

        @Test
        fun `cannot remove worker if it's not your worker`() {
            val magnumSal = TestMagnumSal(eventStream)
                    .withPlayersInOrder("Bruno" using White, "Tim" using Black)
                    .build()
            magnumSal.placeWorkerInMine(White, at(1, 0))

            assertThatExceptionOfType(IllegalTransitionException::class.java)
                    .isThrownBy { magnumSal.removeWorkerFromMine(Black, at(1, 0)) }

            assertThat(eventStream).doesNotContain(MinerRemoved(Black, at(1, 0)))
        }
    }

    @Nested
    inner class ExploringTheMine {

        @Test
        fun `Placing a Miner on an undiscovered mine chamber tile, reveals the contents of the mine chamber`() {
            val magnumSal = TestMagnumSal(eventStream)
                    .withPlayersInOrder("Bruno" using White, "Tim" using Black)
                    .build()
            magnumSal.placeWorkerInMine(White, at(1, 0))
            magnumSal.placeWorkerInMine(Black, at(2, 0))
            magnumSal.placeWorkerInMine(White, at(2, 1))

            assertThat(eventStream.filterEvents<MineChamberRevealed>().map { it.at })
                    .containsOnlyOnce(at(2, 1))
        }

        @Test
        fun `Placing a second Miner on a discovered mine chamber tile, does not reveal the contents of the mine chamber again`() {
            val magnumSal = TestMagnumSal(eventStream)
                    .withPlayersInOrder("Bruno" using White, "Tim" using Black)
                    .build()
            magnumSal.placeWorkerInMine(White, at(1, 0))
            magnumSal.placeWorkerInMine(Black, at(2, 0))
            magnumSal.placeWorkerInMine(White, at(2, 1))
            magnumSal.placeWorkerInMine(White, at(2, 1))

            assertThat(eventStream.filterEvents<MineChamberRevealed>().map { it.at })
                    .containsOnlyOnce(at(2, 1))
        }

        @Test
        fun `Uncovering all mine chamber tiles at depth 2, should only reveal level I mine chambers`() {
            TestMagnumSal(eventStream).revealAllLevelIMineChambers()

            val uncoveredLevelIChambers = eventStream.filterEvents<MineChamberRevealed>().map { it.tile }
            assertThat(uncoveredLevelIChambers)
                    .usingElementComparatorIgnoringFields("at")
                    .containsAll(AllMineChamberTiles.filter { it.level == Level.I })
                    .doesNotContainAnyElementsOf(AllMineChamberTiles.filter { it.level == Level.II })
                    .doesNotContainAnyElementsOf(AllMineChamberTiles.filter { it.level == Level.III })
        }

        @Test
        fun `Uncovering all mine chamber tiles at depth 4, should only reveal level II mine chambers`() {
            TestMagnumSal(eventStream).revealAllLevelIIMineChambers()

            val uncoveredLevelIChambers = eventStream.filterEvents<MineChamberRevealed>().map { it.tile }
            assertThat(uncoveredLevelIChambers)
                    .usingElementComparatorIgnoringFields("at")
                    .containsAll(AllMineChamberTiles.filter { it.level == Level.II })
                    .doesNotContainAnyElementsOf(AllMineChamberTiles.filter { it.level == Level.I })
                    .doesNotContainAnyElementsOf(AllMineChamberTiles.filter { it.level == Level.III })
        }

        @Test
        fun `Uncovering all mine chamber tiles at depth 6, should only reveal level III mine chambers`() {
            TestMagnumSal(eventStream).revealAllLevelIIIMineChambers()

            val uncoveredLevelIChambers = eventStream.filterEvents<MineChamberRevealed>().map { it.tile }
            assertThat(uncoveredLevelIChambers)
                    .usingElementComparatorIgnoringFields("at")
                    .containsAll(AllMineChamberTiles.filter { it.level == Level.III })
                    .doesNotContainAnyElementsOf(AllMineChamberTiles.filter { it.level == Level.I })
                    .doesNotContainAnyElementsOf(AllMineChamberTiles.filter { it.level == Level.II })
        }
    }

    @Nested
    inner class MiningFromTheMine {
        @Test
        fun `Cannot mine when it's not your turn`() {
            val magnumSal = TestMagnumSal(eventStream)
                    .withPlayersInOrder("Bruno" using White, "Tim" using Black)
                    .build()
            magnumSal.placeWorkerInMine(White, at(1, 0))

            assertThatExceptionOfType(IllegalTransitionException::class.java)
                    .isThrownBy { magnumSal.mine(White, at(1, 0), Salts(BROWN)) }
                    .withMessage("Transition requires it to be your turn")
        }

        @Test
        fun `Cannot mine from the mineshaft`() {
            val magnumSal = TestMagnumSal(eventStream)
                    .withPlayersInOrder("Bruno" using White, "Tim" using Black)
                    .build()
            magnumSal.placeWorkerInMine(White, at(1, 0))
            magnumSal.placeWorkerInMine(Black, at(2, 0))

            assertThatExceptionOfType(IllegalTransitionException::class.java)
                    .isThrownBy { magnumSal.mine(White, at(1, 0), Salts(BROWN)) }
                    .withMessage("Transition requires you to mine from a MineChamber")
        }

        @Test
        fun `Cannot mine from a Mine Chamber when no own miners present`() {
            val magnumSal = TestMagnumSal(eventStream)
                    .withPlayersInOrder("Bruno" using White, "Tim" using Black)
                    .build()
            magnumSal.placeWorkerInMine(White, at(1, 0))
            magnumSal.placeWorkerInMine(Black, at(2, 0))
            magnumSal.placeWorkerInMine(White, at(2, 1))
            magnumSal.placeWorkerInMine(White, at(2, 2))

            assertThatExceptionOfType(IllegalTransitionException::class.java)
                    .isThrownBy { magnumSal.mine(Black, at(2, 1), Salts(BROWN)) }
                    .withMessage("Transition requires you to have a miner at ${at(2, 1)}")
        }

        @Test
        fun `Cannot mine salt that is not in the Mine Chamber`() {
            val magnumSal = TestMagnumSal(eventStream)
                    .withOnlyMineChamberTilesOf(MineChamberTile(Level.I, Salts(BROWN), 0))
                    .withPlayersInOrder("Bruno" using White, "Tim" using Black)
                    .build()
            magnumSal.placeWorkerInMine(White, at(1, 0))
            magnumSal.placeWorkerInMine(Black, at(2, 0))
            magnumSal.placeWorkerInMine(White, at(2, 1))
            magnumSal.placeWorkerInMine(White, at(2, 2))
            magnumSal.placeWorkerInMine(Black, at(2, 1))
            magnumSal.placeWorkerInMine(Black, at(2, 1))
            magnumSal.placeWorkerInMine(White, at(2, 2))
            magnumSal.placeWorkerInMine(White, at(2, 2))
            magnumSal.placeWorkerInMine(Black, at(2, 1))

            assertThatExceptionOfType(IllegalTransitionException::class.java)
                    .isThrownBy { magnumSal.mine(Black, at(2, 1), Salts(GREEN, GREEN, WHITE)) }
                    .withMessage("Transition requires there to be 2 Green salt, 1 White salt in ${at(2, 1)}")
        }

        @Test
        fun `Mining salt after the chamber was mined twice for some but not all salt`() {
            val magnumSal = TestMagnumSal(eventStream)
                    .withOnlyMineChamberTilesOf(MineChamberTile(Level.I, Salts(BROWN, GREEN, WHITE), 0))
                    .withPlayersInOrder("Bruno" using White, "Tim" using Black)
                    .build()
            magnumSal.placeWorkerInMine(White, at(1, 0))
            magnumSal.placeWorkerInMine(Black, at(2, 0))
            magnumSal.placeWorkerInMine(White, at(2, 1))
            magnumSal.placeWorkerInMine(White, at(2, 2))
            magnumSal.placeWorkerInMine(Black, at(2, 1))
            magnumSal.placeWorkerInMine(Black, at(2, 1))
            magnumSal.placeWorkerInMine(White, at(2, 1))
            magnumSal.placeWorkerInMine(White, at(2, 1))
            magnumSal.placeWorkerInMine(Black, at(2, 1))

            magnumSal.mine(Black, at(2, 1), Salts(BROWN, WHITE))
            magnumSal.mine(White, at(2, 1), Salts(GREEN))

            assertThatExceptionOfType(IllegalTransitionException::class.java)
                    .isThrownBy { magnumSal.mine(White, at(2, 1), Salts(BROWN)) }
                    .withMessage("Transition requires there to be 1 Brown salt in ${at(2, 1)}")

            assertThat(eventStream.filterEvents<SaltMined>())
                    .doesNotContain(SaltMined(White, at(2, 1), Salts(BROWN)))
        }

        @Test
        fun `Cannot mine the same salt from the same Mine Chamber twice`() {
            val magnumSal = TestMagnumSal(eventStream)
                    .withOnlyMineChamberTilesOf(MineChamberTile(Level.I, Salts(GREEN, GREEN, WHITE), 1))
                    .withFourWhiteMinersAtFirstRightMineChamber()
                    .build()

            magnumSal.mine(White, at(2, 1), Salts(WHITE))

            assertThatExceptionOfType(IllegalTransitionException::class.java)
                    .isThrownBy { magnumSal.mine(White, at(2, 1), Salts(WHITE)) }
                    .withMessage("Transition requires there to be 1 White salt in ${at(2, 1)}")
        }

        @Nested
        inner class `Mining requires strength` {
            @Test
            fun `Cannot mine from a Mine Chamber with water when not enough own miners present`() {
                val magnumSal = TestMagnumSal(eventStream)
                        .withOnlyMineChamberTilesOf(MineChamberTile(Level.I, Salts(BROWN), 1))
                        .withPlayersInOrder("Bruno" using White, "Tim" using Black)
                        .build()
                magnumSal.placeWorkerInMine(White, at(1, 0))
                magnumSal.placeWorkerInMine(Black, at(2, 0))
                magnumSal.placeWorkerInMine(White, at(2, 1))

                assertThatExceptionOfType(IllegalTransitionException::class.java)
                        .isThrownBy { magnumSal.mine(White, at(2, 1), Salts(BROWN)) }
                        .withMessage("Transition requires you to have enough rested miners at ${at(2, 1)}")
            }

            @Test
            fun `Cannot mine from a Mine Chamber with as many water as there are own miners`() {
                val magnumSal = TestMagnumSal(eventStream)
                        .withOnlyMineChamberTilesOf(MineChamberTile(Level.I, Salts(BROWN), 4))
                        .withFourWhiteMinersAtFirstRightMineChamber()
                        .build()

                assertThatExceptionOfType(IllegalTransitionException::class.java)
                        .isThrownBy { magnumSal.mine(White, at(2, 1), Salts(BROWN)) }
                        .withMessage("Transition requires you to have enough rested miners at ${at(2, 1)}")
            }

            @Test
            fun `Mining from a Mine Chamber without water and with own miners present allows to mine an amount of salt equal to own miners`() {
                val magnumSal = TestMagnumSal(eventStream)
                        .withOnlyMineChamberTilesOf(MineChamberTile(Level.I, Salts(GREEN, GREEN, WHITE), 1))
                        .withFourWhiteMinersAtFirstRightMineChamber()
                        .build()

                assertThat(Miners.from(eventStream).filter { it.player == White && it.at == at(2, 1) }).hasSize(4)

                magnumSal.mine(White, at(2, 1), Salts(GREEN, GREEN, WHITE))

                assertThat(eventStream).containsOnlyOnce(SaltMined(White, at(2, 1), Salts(GREEN, GREEN, WHITE)))
            }
        }

        @Nested
        inner class `Mining Tires Miners` {
            @Test
            fun `Mining from a Mine Chamber without water, tires miners that mined salt`() {
                val magnumSal = TestMagnumSal(eventStream)
                        .withOnlyMineChamberTilesOf(MineChamberTile(Level.I, Salts(BROWN, BROWN, GREEN, GREEN, WHITE, WHITE), 0))
                        .withFourWhiteMinersAtFirstRightMineChamber()
                        .withDebugger()
                        .build()

                magnumSal.mine(White, at(2, 1), Salts(BROWN, BROWN, GREEN, GREEN))

                assertThatExceptionOfType(IllegalTransitionException::class.java)
                        .isThrownBy { magnumSal.mine(White, at(2, 1), Salts(WHITE, WHITE)) }
                        .withMessage("Transition requires you to have enough rested miners at ${at(2, 1)}")
            }

            @Test
            fun `Mining from a Mine Chamber with water, tires miners that mined salt and had to hold back the water`() {
                val magnumSal = TestMagnumSal(eventStream)
                        .withOnlyMineChamberTilesOf(MineChamberTile(Level.I, Salts(BROWN, BROWN, GREEN, GREEN, WHITE, WHITE), 2))
                        .withFourWhiteMinersAtFirstRightMineChamber()
                        .build()

                // tire 3 workers: 1 from mining and 2 from holding back water
                magnumSal.mine(White, at(2, 1), Salts(BROWN))

                // since the water wasn't pumped out it remains, so strength required is 2 water + 1 salt = 3,
                // but there should only be 1 rested miner left
                assertThatExceptionOfType(IllegalTransitionException::class.java)
                        .isThrownBy { magnumSal.mine(White, at(2, 1), Salts(WHITE)) }
                        .withMessage("Transition requires you to have enough rested miners at ${at(2, 1)}")
            }
        }

        @Nested
        inner class `Mining costs złoty` {
            @Test
            fun `Cannot mine from a Mine Chamber when player does not have enough money to pay the chain`() {
                val magnumSal = TestMagnumSal(eventStream)
                        .withOnlyMineChamberTilesOf(MineChamberTile(Level.I, Salts(BROWN, BROWN, GREEN, GREEN, WHITE, WHITE), 0))
                        .withFourWhiteMinersAtFirstRightMineChamber()
                        .withPlayerHaving(White, 0)
                        .withDebugger()
                        .build()
                magnumSal.visualizeZloty()

                assertThatExceptionOfType(IllegalTransitionException::class.java)
                        .isThrownBy {
                            magnumSal.mine(White, at(2, 1), Salts(WHITE))
                        }
                        .withMessage("Transition requires you to have enough złoty to pay for salt transport bringing your mined salt out of the mine")
            }

            @Test
            fun `Can mine from a Mine Chamber when player has just enough money to pay the chain`() {
                val testMagnumSal = TestMagnumSal(eventStream)
                        .withOnlyMineChamberTilesOf(MineChamberTile(Level.I, Salts(BROWN, BROWN, GREEN, GREEN, WHITE, WHITE), 0))
                        .withFourWhiteMinersAtFirstRightMineChamber()
                        .withPlayerHaving(White, 2) //White is nor at(1,0) or at(2,0)
                        .withDebugger()
                val magnumSal = testMagnumSal.build()
                magnumSal.removeWorkerFromMine(White, at(1, 0))
                magnumSal.visualizeMiners()
                magnumSal.visualizeZloty()

                magnumSal.mine(White, at(2, 1), Salts(WHITE), with(transportCostDistribution(White)) {
                    pay(Black, 1)
                    pay(Black, 1)
                })

                assertThat(testMagnumSal.filterEvents<PaymentEvent>())
                        .containsExactlyInAnyOrder(ZlotyPaid(White, 2), ZlotyReceived(Black, 2))
                magnumSal.visualizeZloty()
            }

            @Test
            fun `Can't pay more for salt transport than is required`() {
                val magnumSal = TestMagnumSal(eventStream)
                        .withOnlyMineChamberTilesOf(MineChamberTile(Level.I, Salts(BROWN, BROWN, GREEN, GREEN, WHITE, WHITE), 0))
                        .withFourWhiteMinersAtFirstRightMineChamber()
                        .withPlayerHaving(White, 6) //White is nor at(1,0) or at(2,0)
                        .withDebugger()
                        .build()
                magnumSal.removeWorkerFromMine(White, at(1, 0))
                magnumSal.visualizeMiners()
                magnumSal.visualizeZloty()

                assertThatExceptionOfType(IllegalTransitionException::class.java)
                        .isThrownBy {
                            magnumSal.mine(White, at(2, 1), Salts(WHITE, WHITE), with(transportCostDistribution(White)) {
                                pay(Black, 3)
                                pay(Black, 3)
                            })
                            magnumSal.visualizeZloty()
                        }
                        .withMessage("Transition requires you not to pay more złoty for salt transport than is required")

                assertThat(eventStream).doesNotContain(ZlotyPaid(White, 6), ZlotyReceived(Black, 6))
            }

            @Test
            fun `Can't bring up multiple salt blocks when player does not have enough money to pay for transport`() {
                val magnumSal = TestMagnumSal(eventStream)
                        .withOnlyMineChamberTilesOf(MineChamberTile(Level.I, Salts(BROWN, BROWN, GREEN, GREEN, WHITE, WHITE), 0))
                        .withFourWhiteMinersAtFirstRightMineChamber()
                        .withPlayerHaving(White, 4)
                        .withDebugger()
                        .build()
                magnumSal.removeWorkerFromMine(White, at(1, 0))
                magnumSal.visualizeMiners()
                magnumSal.visualizeZloty()

                assertThatExceptionOfType(IllegalTransitionException::class.java)
                        .isThrownBy {
                            magnumSal.mine(White, at(2, 1), Salts(WHITE, WHITE, GREEN), with(transportCostDistribution(White)) {
                                pay(Black, 3)
                                pay(Black, 3)
                            })
                            magnumSal.visualizeZloty()
                        }
                        .withMessage("Transition requires you to have enough złoty to pay for salt transport bringing your mined salt out of the mine")

                assertThat(eventStream).doesNotContain(ZlotyPaid(White, 4), ZlotyReceived(Black, 4))
            }

            @Test
            fun `Can't pay more for salt transport than is required, when paying multiple players`() {
                val magnumSal = TestMagnumSal(eventStream)
                        .withOnlyMineChamberTilesOf(MineChamberTile(Level.I, Salts(BROWN, BROWN, GREEN, GREEN, WHITE, WHITE), 0))
                        .withTwoWhiteMinersAtFirstRightMineChamberWithThreePlayers()
                        .withPlayerHaving(White, 6) //White is nor at(1,0) or at(2,0)
                        .withDebugger()
                        .build()
                magnumSal.removeWorkerFromMine(White, at(1, 0))
                magnumSal.visualizeMiners()
                magnumSal.visualizeZloty()

                assertThatExceptionOfType(IllegalTransitionException::class.java)
                        .isThrownBy {
                            magnumSal.mine(White, at(2, 1), Salts(WHITE, WHITE), with(transportCostDistribution(White)) {
                                pay(Black, 3)
                                pay(Orange, 3)
                            })
                            magnumSal.visualizeZloty()
                        }
                        .withMessage("Transition requires you not to pay more złoty for salt transport than is required")

                assertThat(eventStream).doesNotContain(ZlotyPaid(White, 3), ZlotyPaid(White, 3), ZlotyReceived(Black, 3), ZlotyReceived(Orange, 3))
            }

            @Test
            fun `Can't bring up multiple salt blocks when player does not have enough money to pay for transport, when paying multiple players`() {
                val testMagnumSal = TestMagnumSal(eventStream)
                        .withOnlyMineChamberTilesOf(MineChamberTile(Level.I, Salts(BROWN, BROWN, GREEN, GREEN, WHITE, WHITE), 0))
                        .withTwoWhiteMinersAtFirstRightMineChamberWithThreePlayers()
                        .withPlayerHaving(White, 3)
                        .withDebugger()
                val magnumSal = testMagnumSal
                        .build()
                magnumSal.removeWorkerFromMine(White, at(1, 0))
                magnumSal.visualizeMiners()
                magnumSal.visualizeZloty()

                assertThatExceptionOfType(IllegalTransitionException::class.java)
                        .isThrownBy {
                            magnumSal.mine(White, at(2, 1), Salts(WHITE, WHITE), with(transportCostDistribution(White)) {
                                pay(Black, 2)
                                pay(Orange, 2)
                            })
                            magnumSal.visualizeZloty()
                        }
                        .withMessage("Transition requires you to have enough złoty to pay for salt transport bringing your mined salt out of the mine")

                assertThat(testMagnumSal.filterEvents<PaymentEvent>())
                        .doesNotContain(ZlotyPaid(White, 2), ZlotyPaid(White, 2), ZlotyReceived(Black, 2), ZlotyReceived(Orange, 2))
            }

            @Test
            fun `Paying multiple players for 2 salts, and one position`() {
                val testMagnumSal = TestMagnumSal(eventStream)
                        .withOnlyMineChamberTilesOf(MineChamberTile(Level.I, Salts(BROWN, BROWN, GREEN, GREEN, WHITE, WHITE), 0))
                        .withTwoWhiteMinersAtFirstRightMineChamberWithThreePlayers()
                        .withPlayerHaving(White, 4)
                        .withDebugger()
                val magnumSal = testMagnumSal.build()
                magnumSal.visualizeMiners()
                magnumSal.visualizeZloty()

                magnumSal.mine(White, at(2, 1), Salts(WHITE, WHITE), with(transportCostDistribution(White)) {
                    pay(Black, 1)
                    pay(Orange, 1)
                })
                magnumSal.visualizeZloty()

                assertThat(testMagnumSal.filterEvents<PaymentEvent>())
                        .containsExactlyInAnyOrder(
                                ZlotyPaid(White, 1), ZlotyReceived(Black, 1),
                                ZlotyPaid(White, 1), ZlotyReceived(Orange, 1)
                        )
            }
        }
    }
}
