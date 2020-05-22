package be.kunlabora.magnumsal

import be.kunlabora.magnumsal.MagnumSalEvent.*
import be.kunlabora.magnumsal.MinerMovement.PlaceMiner
import be.kunlabora.magnumsal.MinerMovement.RemoveMiner
import be.kunlabora.magnumsal.exception.transitionRequires
import be.kunlabora.magnumsal.gamepieces.*
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter.ofPattern

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
sealed class MagnumSalEvent : Event {
    @JsonTypeName("PlayerJoined")
    data class PlayerJoined(val name: String, val color: PlayerColor) : MagnumSalEvent()
    @JsonTypeName("PlayerOrderDetermined")
    data class PlayerOrderDetermined(val player1: PlayerColor, val player2: PlayerColor, val player3: PlayerColor? = null, val player4: PlayerColor? = null) : MagnumSalEvent()
    @JsonTypeName("ZłotyReceived")
    data class ZłotyReceived(val player: PlayerColor, val złoty: Złoty) : MagnumSalEvent()
    @JsonTypeName("ZlotyPaid")
    data class ZlotyPaid(val player: PlayerColor, val złoty: Złoty) : MagnumSalEvent()
    @JsonTypeName("MinerPlaced")
    data class MinerPlaced(val player: PlayerColor, val at: PositionInMine) : MagnumSalEvent()
    @JsonTypeName("MineChamberRevealed")
    data class MineChamberRevealed(val at: PositionInMine, val tile: MineChamberTile) : MagnumSalEvent()
    @JsonTypeName("MinerRemoved")
    data class MinerRemoved(val player: PlayerColor, val at: PositionInMine) : MagnumSalEvent()
    @JsonTypeName("SaltMined")
    data class SaltMined(val player: PlayerColor, val from: PositionInMine, val saltMined: Salts) : MagnumSalEvent()
    @JsonTypeName("MinersGotTired")
    data class MinersGotTired(val player: PlayerColor, val from: PositionInMine, val tiredMiners: Int) : MagnumSalEvent()
}

class MagnumSal(private val eventStream: EventStream,
                private val allMineChamberTiles: List<MineChamberTile> = AllMineChamberTiles,
                private val debugEnabled: Boolean = false) {

    private val turnOrderRule = TurnOrderRule(eventStream)
    private val chainRule = ChainRule(eventStream)
    private val workerLimitRule = WorkerLimitRule(eventStream)

    private val players
        get() = eventStream.filterEvents<PlayerJoined>()

    private val amountOfPlayers
        get() = players.count()

    private val miners: Miners
        get() = Miners.from(eventStream)

    private val revealedMineChambers
        get() = eventStream.filterEvents<MineChamberRevealed>()

    fun addPlayer(name: String, color: PlayerColor) {
        transitionRequires("the same color not to have been picked already") {
            color !in players.map { it.color }
        }
        eventStream.push(PlayerJoined(name, color))
    }

    fun determinePlayOrder(player1: PlayerColor,
                           player2: PlayerColor,
                           player3: PlayerColor? = null,
                           player4: PlayerColor? = null) {
        val colors = setOf(player1, player2, player3, player4).filterNotNull()
        transitionRequires("player colors to be unique") {
            colors.size == listOfNotNull(player1, player2, player3, player4).size
        }
        transitionRequires("at least 2 players") {
            amountOfPlayers >= 2
        }
        transitionRequires("player colors to have been picked") {
            val players = players.map { it.color }.toSet()
            colors.intersect(players).size == players.size
        }
        eventStream.push(PlayerOrderDetermined(player1, player2, player3, player4))
        eventStream.push(ZłotyReceived(player1, 10))
        eventStream.push(ZłotyReceived(player2, 12))
        player3?.let { eventStream.push(ZłotyReceived(it, 14)) }
        player4?.let { eventStream.push(ZłotyReceived(it, 16)) }
    }

    fun placeWorkerInMine(player: PlayerColor, at: PositionInMine) = onlyInPlayersTurn(player) {
        withoutBreakingTheChain(PlaceMiner(player, at)) {
            requirePlayerToHaveEnoughWorkers(player)
            eventStream.push(MinerPlaced(player, at))
            revealMineChamberIfPossible(at)
        }
    }

    fun removeWorkerFromMine(player: PlayerColor, at: PositionInMine) = onlyInPlayersTurn(player) {
        withoutBreakingTheChain(RemoveMiner(player, at)) {
            transitionRequires("you to have a miner at $at") {
                hasWorkerAt(player, at)
            }
            eventStream.push(MinerRemoved(player, at))
        }
    }

    fun mine(player: PlayerColor, at: PositionInMine, saltToMine: Salts) = onlyInPlayersTurn(player) {
        transitionRequires("you to mine from a MineChamber") {
            at.isInACorridor()
        }
        transitionRequires("there to be $saltToMine in $at") {
            saltIsAvailableAt(saltToMine, at)
        }
        transitionRequires("you to have a miner at $at") {
            hasWorkerAt(player, at)
        }
        transitionRequires("you to have enough rested miners at $at") {
            strengthAt(player, at) >= saltToMine.size
        }
        transitionRequires("you to have enough złoty to pay for transport bringing your mined salt out of the mine") {
            złotyForPlayer(player) >= transportersNeeded(player, at)
        }
        val minersThatWillGetTired = strengthAt(player, at)
        eventStream.push(SaltMined(player, at, saltToMine))
        eventStream.push(MinersGotTired(player, at, minersThatWillGetTired))
    }

    private fun transportersNeeded(player: PlayerColor, at: PositionInMine): Int {
        val positionsTheSaltWillTravel = at.positionsUntilTheTop()
        return miners.filter { it.at in positionsTheSaltWillTravel }
                .debug { "${player}'s mine action requires transport across $it" }
                .groupBy { it.at }
                .count { (_, miners) -> player !in miners.map { it.player } }
    }

    private fun złotyForPlayer(player: PlayerColor): Złoty {
        val złotyReceived = eventStream.filterEvents<ZłotyReceived>()
                .filter { it.player == player }
                .sumBy { it.złoty }
        val złotyPaid = eventStream.filterEvents<ZlotyPaid>()
                .filter { it.player == player }
                .sumBy { it.złoty }
        return (złotyReceived - złotyPaid).debug { "$player currently has $it zł" }
    }

    private fun saltIsAvailableAt(saltToMine: Salts, at: PositionInMine): Boolean =
            saltToMine.canBeMinedFrom(saltLeftInMineChamber(at).debug { "Checking if $saltToMine can be mined... Salt left in $at: $it." })

    private fun saltLeftInMineChamber(at: PositionInMine): Salts {
        val saltsOnTile = Salts(revealedMineChambers.single { it.at == at }.tile.salt)
        return eventStream.filterEvents<SaltMined>().filter { it.from == at }.fold(saltsOnTile) { acc, it -> acc - it.saltMined }
    }

    private fun strengthAt(player: PlayerColor, at: PositionInMine): Int {
        val playerMiners = miners.count { it == Miner(player, at) }
        val tiredWorkers: Int = tiredWorkersAt(player, at)
        val waterRemainingInChamber = waterRemainingInChamber(at)
        return (playerMiners - tiredWorkers - waterRemainingInChamber)
                .debug { "Strength at $at = $it: $player has $playerMiners miners, of which $tiredWorkers are tired, and there are $waterRemainingInChamber water cubes still in the chamber." }
    }

    private fun waterRemainingInChamber(at: PositionInMine) =
            revealedMineChambers.single { it.at == at }.tile.waterCubes

    private fun tiredWorkersAt(player: PlayerColor, at: PositionInMine) =
            eventStream.filterEvents<MinersGotTired>()
                    .filter { it.player == player && it.from == at }
                    .sumBy { it.tiredMiners }

    private fun revealMineChamberIfPossible(at: PositionInMine) {
        if (at.isInACorridor() && isNotRevealed(at)) {
            revealNewMineChamber(at)
        }
    }

    private fun isNotRevealed(at: PositionInMine) = at !in revealedMineChambers.map { it.at }

    private fun revealNewMineChamber(at: PositionInMine) {
        val level = Level.from(at)
        val revealedMineChamberTiles = revealedMineChambers.map { it.tile }
        val unrevealedMineChamberTiles = allMineChamberTiles.shuffled() - revealedMineChamberTiles
        val tile = unrevealedMineChamberTiles.filter { it.level == level }[0]
        eventStream.push(MineChamberRevealed(at, tile))
    }

    private fun hasWorkerAt(player: PlayerColor, at: PositionInMine): Boolean = miners.any { it == Miner(player, at) }

    private fun requirePlayerToHaveEnoughWorkers(player: PlayerColor) = workerLimitRule.requirePlayerToHaveEnoughWorkers(player)
    private fun onlyInPlayersTurn(player: PlayerColor, block: () -> Unit): Any = turnOrderRule.onlyInPlayersTurn(player, block)
    private fun withoutBreakingTheChain(minerMovement: MinerMovement, block: () -> Unit): Any = chainRule.withoutBreakingTheChain(minerMovement, block)

    // Util
    private inline fun <T> T.debug(block: (T) -> String): T {
        if (debugEnabled) {
            val formattedTimestamp = LocalDateTime.now().format(ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))
            println("[debug ~ $formattedTimestamp] ${block(this)}")
        }
        return this
    }

    internal fun currentState(visualizer: (EventStream) -> Unit) = visualizer(eventStream)
}


enum class PlayerColor {
    Black,
    White,
    Orange,
    Purple;
}

