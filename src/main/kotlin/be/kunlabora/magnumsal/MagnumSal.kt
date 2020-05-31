package be.kunlabora.magnumsal

import be.kunlabora.magnumsal.MagnumSalEvent.*
import be.kunlabora.magnumsal.MagnumSalEvent.PaymentEvent.ZlotyPaid
import be.kunlabora.magnumsal.MagnumSalEvent.PaymentEvent.ZlotyReceived
import be.kunlabora.magnumsal.MagnumSalEvent.PersonThatCanHandleZloty.Bank
import be.kunlabora.magnumsal.MagnumSalEvent.PersonThatCanHandleZloty.Player
import be.kunlabora.magnumsal.MagnumSalEvent.PlayerActionEvent.MinerMovementEvent.MinerPlaced
import be.kunlabora.magnumsal.MagnumSalEvent.PlayerActionEvent.MinerMovementEvent.MinerRemoved
import be.kunlabora.magnumsal.MagnumSalEvent.PlayerActionEvent.SaltMined
import be.kunlabora.magnumsal.MagnumSalEvent.PlayerActionEvent.WaterPumped
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
    // Game initialization events
    @JsonTypeName("PlayerJoined")
    data class PlayerJoined(val name: String, val color: PlayerColor) : MagnumSalEvent()

    @JsonTypeName("PlayerOrderDetermined")
    data class PlayerOrderDetermined(val player1: PlayerColor, val player2: PlayerColor, val player3: PlayerColor? = null, val player4: PlayerColor? = null) : MagnumSalEvent()


    // Player actions
    sealed class PlayerActionEvent(val player: PlayerColor) : MagnumSalEvent() {
        @JsonTypeName("PassedTurn")
        data class PassedAction(val _player: PlayerColor) : PlayerActionEvent(_player)

        sealed class MinerMovementEvent(_player: PlayerColor, val at: PositionInMine) : PlayerActionEvent(_player) {
            @JsonTypeName("MinerPlaced")
            data class MinerPlaced(private val __player: PlayerColor, private val _at: PositionInMine) : MinerMovementEvent(__player, _at)

            @JsonTypeName("MinerRemoved")
            data class MinerRemoved(private val __player: PlayerColor, private val _at: PositionInMine) : MinerMovementEvent(__player, _at)
        }

        @JsonTypeName("SaltMined")
        data class SaltMined(val _player: PlayerColor, val from: PositionInMine, val saltMined: Salts) : PlayerActionEvent(_player)

        @JsonTypeName("WaterPumped")
        data class WaterPumped(val _player: PlayerColor, val from: PositionInMine, val waterPumped: WaterCubes) : PlayerActionEvent(_player)
    }

    sealed class PersonThatCanHandleZloty {
        object Bank : PersonThatCanHandleZloty()
        data class Player(val player: PlayerColor) : PersonThatCanHandleZloty()
    }

    // Side-effect events, but therefor not necessarily less important, still contains domain language/concepts
    sealed class PaymentEvent(val person: PersonThatCanHandleZloty, val zloty: Zloty) : MagnumSalEvent() {
        @JsonTypeName("ZłotyReceived")
        data class ZlotyReceived(private val _person: PersonThatCanHandleZloty, private val _zloty: Zloty) : PaymentEvent(_person, _zloty)

        @JsonTypeName("ZlotyPaid")
        data class ZlotyPaid(private val _person: PersonThatCanHandleZloty, private val _zloty: Zloty) : PaymentEvent(_person, _zloty)
    }

    @JsonTypeName("MineChamberRevealed")
    data class MineChamberRevealed(val at: PositionInMine, val tile: MineChamberTile) : MagnumSalEvent()

    @JsonTypeName("MinersGotTired")
    data class MinersGotTired(val player: PlayerColor, val from: PositionInMine, val tiredMiners: Int) : MagnumSalEvent()
}

class MagnumSal(private val eventStream: EventStream,
                private val allMineChamberTiles: List<MineChamberTile> = AllMineChamberTiles,
                private val debugEnabled: Boolean = false) {

    private val turnOrderRule = TurnOrderRule(eventStream)
    private val chainRule = ChainRule(eventStream)
    private val workerLimitRule = WorkerLimitRule(eventStream)

    private val zlotyPerPlayer = ZlotyPerPlayer(eventStream)

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

        eventStream.push(ZlotyReceived(Player(player1), 10))
        eventStream.push(ZlotyReceived(Player(player2), 12))
        player3?.let { eventStream.push(ZlotyReceived(Player(it), 14)) }
        player4?.let { eventStream.push(ZlotyReceived(Player(it), 16)) }
    }

    fun pass(player: PlayerColor) {
        eventStream.push(PlayerActionEvent.PassedAction(player))
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
            requiresYouToHaveAMinerAt(at, player)
            eventStream.push(MinerRemoved(player, at))
        }
    }

    fun mine(player: PlayerColor, at: PositionInMine, saltToMine: Salts, transportCostDistribution: TransportCostDistribution? = null) = onlyInPlayersTurn(player) {
        orderMinersToMine(player, at, saltToMine)
        handleSaltTransport(player, at, saltToMine, transportCostDistribution)
        eventStream.push(SaltMined(player, at, saltToMine))
    }

    fun usePumphouse(player: PlayerColor, at: PositionInMine, waterToPump: WaterCubes) = onlyInPlayersTurn(player) {
        transitionRequires("you to want to pump at least SOME water") { waterToPump > 0 }
        transitionRequires("you to not want to pump more than the pumphouse allows") { waterToPump <= 4 }
        gather(at, player, "pump water", waterToPump, this::waterIsAvailableAt)
        transitionRequires("you to have enough złoty to pay for using the pumphouse") {
            zlotyForPlayer(player) >= waterPumpCost(waterToPump) ?: 0
        }
        payWaterPump(waterToPump, player)
        eventStream.push(WaterPumped(player, at, waterToPump))
    }

    private fun payWaterPump(waterToPump: WaterCubes, player: PlayerColor) {
        waterPumpCost(waterToPump)?.let {
            with(eventStream) {
                push(ZlotyPaid(Player(player), it))
                push(ZlotyReceived(Bank, it))
            }
        }
    }

    private fun waterPumpCost(waterToPump: WaterCubes): Zloty? = when (waterToPump) {
        1 -> null
        2 -> 2
        3 -> 5
        4 -> 9
        else -> null
    }

    private fun orderMinersToMine(player: PlayerColor, at: PositionInMine, saltToMine: Salts) {
        gather(at, player, "mine", saltToMine, this::saltIsAvailableAt)

        transitionRequires("you to have enough rested miners at $at") {
            strengthAt(player, at) >= saltToMine.size
        }
        val minersThatWillGetTired = strengthAt(player, at)
        eventStream.push(MinersGotTired(player, at, minersThatWillGetTired))
    }

    //TODO: transportCostDistribution, transportChain, and transportCost are coupled but there's low cohesion
    private fun handleSaltTransport(player: PlayerColor, at: PositionInMine, saltMined: Salts, transportCostDistribution: TransportCostDistribution?) {
        TransportSaltAction(saltMined, player, at, eventStream)
                .whenCoveredBy(transportCostDistribution) {
                    eventStream.push(it.from); eventStream.push(it.to)
                }
    }


    private fun <T> gather(at: PositionInMine,
                           player: PlayerColor,
                           action: String,
                           presence: T,
                           presenceRequirementChecker: (presence: T, at: PositionInMine) -> Boolean) {
        requiresYouToHaveAMinerAt(at, player)
        requiresYouToBeInAMineChamber(action, at)
        requiresPresenceOf(presence, at, presenceRequirementChecker)
    }

    private fun <T> requiresPresenceOf(presenceType: T, at: PositionInMine,
                                       presenceRequirementChecker: (presence: T, at: PositionInMine) -> Boolean) {
        transitionRequires("there to be $presenceType in $at") {
            presenceRequirementChecker(presenceType, at)
        }
    }

    private fun requiresYouToBeInAMineChamber(action: String, at: PositionInMine) {
        transitionRequires("you to $action from a MineChamber") {
            at.isInACorridor()
        }
    }

    private fun requiresYouToHaveAMinerAt(at: PositionInMine, player: PlayerColor) {
        transitionRequires("you to have a miner at $at") {
            hasWorkerAt(player, at)
        }
    }


    private fun zlotyForPlayer(player: PlayerColor): Zloty {
        return zlotyPerPlayer.forPlayer(player).debug { "$player currently has $it zł" }
    }

    private fun saltIsAvailableAt(saltToMine: Salts, at: PositionInMine): Boolean =
            saltToMine.canBeMinedFrom(saltLeftInMineChamber(at).debug { "Checking if $saltToMine can be mined... Salt left in $at: $it." })

    private fun saltLeftInMineChamber(at: PositionInMine): Salts {
        val saltsOnTile = Salts(revealedMineChambers.single { it.at == at }.tile.salt)
        return eventStream.filterEvents<SaltMined>().filter { it.from == at }.fold(saltsOnTile) { acc, it -> acc - it.saltMined }
    }

    private fun waterIsAvailableAt(waterToPump: WaterCubes, at: PositionInMine): Boolean =
            waterToPump <= waterLeftInMineChamber(at).debug { "Checking if $waterToPump can be pumped... Water left in $at: $it." }

    private fun waterLeftInMineChamber(at: PositionInMine): WaterCubes {
        val waterOnTile = revealedMineChambers.single { it.at == at }.tile.waterCubes
        return eventStream.filterEvents<WaterPumped>().filter { it.from == at }.fold(waterOnTile) { acc, it -> acc - it.waterPumped }
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

    internal fun visualize(visualizer: (EventStream) -> Unit) = visualizer(eventStream)
}


enum class PlayerColor {
    Black,
    White,
    Orange,
    Purple;
}

class PaymentTransaction private constructor(val from: ZlotyPaid, val to: ZlotyReceived) {
    init {
        require(from.person != to.person) { "Can't create a payment transaction from and to the same player" }
    }

    companion object {
        fun transaction(from: PlayerColor, to: PlayerColor, amount: Zloty): PaymentTransaction {
            return PaymentTransaction(ZlotyPaid(Player(from), amount), ZlotyReceived(Player(to), amount))
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PaymentTransaction

        if (from != other.from) return false
        if (to != other.to) return false

        return true
    }

    override fun hashCode(): Int {
        var result = from.hashCode()
        result = 31 * result + to.hashCode()
        return result
    }

    override fun toString(): String {
        return "PaymentTransaction(from=$from, to=$to)"
    }
}
