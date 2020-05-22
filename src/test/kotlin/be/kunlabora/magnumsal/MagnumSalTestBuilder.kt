package be.kunlabora.magnumsal

import be.kunlabora.magnumsal.MagnumSalEvent.PaymentEvent
import be.kunlabora.magnumsal.MagnumSalEvent.PaymentEvent.ZlotyPaid
import be.kunlabora.magnumsal.MagnumSalEvent.PaymentEvent.ZlotyReceived
import be.kunlabora.magnumsal.PlayerColor.*
import be.kunlabora.magnumsal.PositionInMine.Companion.at
import be.kunlabora.magnumsal.gamepieces.AllMineChamberTiles
import be.kunlabora.magnumsal.gamepieces.MineChamberTile
import be.kunlabora.magnumsal.gamepieces.Zloty

data class Player(val name: PlayerName, val color: PlayerColor)

infix fun PlayerName.using(color: PlayerColor): Player = Player(this, color)
typealias PlayerName = String

/**
 * Use TestMagnumSal for testing MagnumSal.
 * Has a bunch of insta-setup scenario's for all your most longed after testing purposes.
 * Internally uses an actual MagnumSal instance, so only _legal_ actions can be executed.
 * Comes with a fun build() that returns the built MagnumSal instance, so you can continue executing _legal_ actions necessary for your tests.
 */
class TestMagnumSal(val eventStream: EventStream) {
    var magnumSal = MagnumSal(eventStream)

    var _debugEnabled: Boolean = false
    var debugEnabled: Boolean = false
        set(value) {
            _debugEnabled = value
            magnumSal = MagnumSal(eventStream, _tiles, value)
        }
    var _tiles: List<MineChamberTile> = AllMineChamberTiles
    var tiles: List<MineChamberTile> = AllMineChamberTiles
        set(value) {
            _tiles = value
            magnumSal = MagnumSal(eventStream, value, _debugEnabled)
        }

    fun build(): MagnumSal {
        return MagnumSal(eventStream, _tiles, _debugEnabled)
    }
}

fun TestMagnumSal.withPlayers(player1: Player,
                              player2: Player,
                              player3: Player? = null,
                              player4: Player? = null): TestMagnumSal {
    listOfNotNull(player1, player2, player3, player4).forEach {
        magnumSal.addPlayer(it.name, it.color)
    }
    return this
}

fun TestMagnumSal.withPlayerOrder(player1: PlayerColor,
                                  player2: PlayerColor,
                                  player3: PlayerColor?,
                                  player4: PlayerColor?): TestMagnumSal {
    magnumSal.determinePlayOrder(player1, player2, player3, player4)
    return this
}

fun TestMagnumSal.withPlayersInOrder(player1: Player,
                                     player2: Player,
                                     player3: Player? = null,
                                     player4: Player? = null): TestMagnumSal {
    return withPlayers(player1, player2, player3, player4)
            .withPlayerOrder(player1.color, player2.color, player3?.color, player4?.color)
}

fun TestMagnumSal.distributeWorkersInTheMineShaft(amountOfWorkersToUse: Int, playerOrder: List<PlayerColor>): TestMagnumSal {
    require(amountOfWorkersToUse > 0) { "Distributing the mine shaft with 0 workers does not seem logical now does it?" }
    for (player in playerOrder) {
        magnumSal.placeWorkerInMine(player, at(1, 0))
    }
    for (pos in IntProgression.fromClosedRange(2, amountOfWorkersToUse, 2)) {
        for (player in playerOrder) {
            magnumSal.placeWorkerInMine(player, at(pos, 0))
            magnumSal.placeWorkerInMine(player, at(pos + 1, 0))
        }
    }
    return this
}

/**
 * Two players White and Black. It's now White's turn.
 */
fun TestMagnumSal.withFourWhiteMinersAtFirstRightMineChamber(): TestMagnumSal {
    this.withPlayersInOrder("Bruno" using White, "Tim" using Black)
    magnumSal.placeWorkerInMine(White, at(1, 0))
    magnumSal.placeWorkerInMine(Black, at(1, 0))

    magnumSal.placeWorkerInMine(White, at(2, 0))
    magnumSal.placeWorkerInMine(White, at(2, 1)) // 1

    magnumSal.placeWorkerInMine(Black, at(2, 0))
    magnumSal.placeWorkerInMine(Black, at(2, 0))

    magnumSal.placeWorkerInMine(White, at(2, 1)) // 2
    magnumSal.placeWorkerInMine(White, at(2, 1)) // 3

    magnumSal.placeWorkerInMine(Black, at(2, 0))
    magnumSal.removeWorkerFromMine(Black, at(2, 0))

    magnumSal.removeWorkerFromMine(White, at(2, 0))
    magnumSal.placeWorkerInMine(White, at(2, 1)) // 4

    magnumSal.placeWorkerInMine(Black, at(2, 0))
    magnumSal.removeWorkerFromMine(Black, at(2, 0))

    return this
}

fun TestMagnumSal.revealAllLevelIMineChambers(): TestMagnumSal {
    this.withPlayersInOrder("Bruno" using White, "Tim" using Black, "Snarf" using Orange, "Azrael" using Purple)
    magnumSal.placeWorkerInMine(White, at(1, 0))
    magnumSal.placeWorkerInMine(Black, at(2, 0))
    magnumSal.placeWorkerInMine(Orange, at(2, 1))
    magnumSal.placeWorkerInMine(Purple, at(2, 2))
    magnumSal.placeWorkerInMine(White, at(2, 3))
    magnumSal.placeWorkerInMine(White, at(2, 4))
    magnumSal.placeWorkerInMine(Black, at(2, 1))
    magnumSal.placeWorkerInMine(Black, at(2, -1))
    magnumSal.placeWorkerInMine(Orange, at(2, -2))
    magnumSal.placeWorkerInMine(Orange, at(2, -3))
    magnumSal.placeWorkerInMine(Purple, at(2, -4))
    return this
}

fun TestMagnumSal.revealAllLevelIIMineChambers(): TestMagnumSal {
    this.withPlayersInOrder("Bruno" using White, "Tim" using Black, "Snarf" using Orange)
    magnumSal.placeWorkerInMine(White, at(1, 0))
    magnumSal.placeWorkerInMine(Black, at(2, 0))
    magnumSal.placeWorkerInMine(Orange, at(3, 0))
    magnumSal.placeWorkerInMine(White, at(4, 0))
    magnumSal.placeWorkerInMine(White, at(4, 1))
    magnumSal.placeWorkerInMine(Black, at(4, 2))
    magnumSal.placeWorkerInMine(Black, at(4, 3))
    magnumSal.placeWorkerInMine(Orange, at(4, -1))
    magnumSal.placeWorkerInMine(Orange, at(4, -2))
    magnumSal.placeWorkerInMine(White, at(4, -3))
    return this
}

fun TestMagnumSal.revealAllLevelIIIMineChambers(): TestMagnumSal {
    this.withPlayersInOrder("Bruno" using White, "Tim" using Black, "Snarf" using Orange)
    magnumSal.placeWorkerInMine(White, at(1, 0))
    magnumSal.placeWorkerInMine(Black, at(2, 0))
    magnumSal.placeWorkerInMine(Orange, at(3, 0))
    magnumSal.placeWorkerInMine(White, at(4, 0))
    magnumSal.placeWorkerInMine(White, at(5, 0))
    magnumSal.placeWorkerInMine(Black, at(6, 0))
    magnumSal.placeWorkerInMine(Black, at(6, 1))
    magnumSal.placeWorkerInMine(Orange, at(6, 2))
    magnumSal.placeWorkerInMine(Orange, at(6, -1))
    magnumSal.placeWorkerInMine(White, at(6, -2))
    return this
}

fun TestMagnumSal.withOnlyMineChamberTilesOf(tile: MineChamberTile): TestMagnumSal {
    this.tiles = AllMineChamberTiles
            .filter { it.level == tile.level }
            .map { it.copy(salt = tile.salt, waterCubes = tile.waterCubes) }
    return this
}

fun TestMagnumSal.withPlayerHaving(player: PlayerColor, zł: Zloty): TestMagnumSal {
    val currentTotalZł = this.eventStream.filterEvents<ZlotyReceived>()
            .filter { it.player == player }
            .sumBy { it.zloty }
    when {
        currentTotalZł > zł -> this.eventStream.push(ZlotyPaid(player, currentTotalZł - zł))
        currentTotalZł < zł -> this.eventStream.push(ZlotyReceived(player, currentTotalZł - zł))
    }
    return this
}

// Util
fun TestMagnumSal.withDebugger(): TestMagnumSal {
    this.debugEnabled = true
    return this
}

fun visualize(miners: Miners) {
    println("${"#".repeat(25)} MineShaft Top ${"#".repeat(25)}")
    miners.groupBy { it.at }
            .forEach { (at, miners) ->
                val amountOfMinersPerPlayer = miners.groupBy(Miner::player).mapValues { (player, pMiners) -> player.icon(pMiners.size) }.values.joinToString(separator = "")
                println("$at: $amountOfMinersPerPlayer")
            }
    println("${"#".repeat(25)} MineShaft End ${"#".repeat(25)}")
}

fun visualizeZloty(eventStream: EventStream) {
    println("Złoty per player: " + ZlotyPerPlayer(eventStream).all().mapValues { (player, zloty) -> "${player.icon()}: $zloty zł" }.values.joinToString())
}

fun MagnumSal.visualizeMiners() = this.visualize { eventStream -> visualize(Miners.from(eventStream)) }
fun MagnumSal.visualizeZloty() = this.visualize { eventStream -> visualizeZloty(eventStream) }

fun PlayerColor.icon(n: Int = 1): String = when (this) {
    White -> "💛"
    Black -> "🖤"
    Orange -> "🧡"
    Purple -> "💜"
}.repeat(n)


