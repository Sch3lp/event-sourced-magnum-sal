package be.kunlabora.magnumsal

import be.kunlabora.magnumsal.MagnumSalEvent.*
import be.kunlabora.magnumsal.exception.requires

sealed class MagnumSalEvent : Event {
    data class PlayerAdded(val name: String) : MagnumSalEvent()
    data class MinerPlaced(val by: String, val at: MineShaftPosition) : MagnumSalEvent()
    data class MinerRemoved(val by: String, val at: MineShaftPosition) : MagnumSalEvent()
}

class MagnumSal(private val eventStream: EventStream) {
    fun addPlayer(playerName: String) {
        eventStream.push(PlayerAdded(playerName))
    }

    fun placeMiner(by: String, at: MineShaftPosition) {
        requiresPlayerInGame("Placing a miner", by)
        "Placing a miner at $at".requires("there to be a miner at ${at.previous()}.") {
            (at == MineShaftPosition(1))
                    || at.previous() in eventStream.filterIsInstance(MinerPlaced::class.java).map { it.at }
        }
        eventStream.push(MinerPlaced(by, at))
    }

    fun removeMiner(by: String, at: MineShaftPosition) {
        requiresPlayerInGame("Removing a miner", by)
        "Removing a miner at $at".requires("there to either be another miner there, or no miner at ${at.next()}") {
            val mineShaftOccupation = replayMinersPlacedAndRemoved() - at
            at in mineShaftOccupation
                    || at.next() !in mineShaftOccupation
        }
        eventStream.push(MinerRemoved(by, at))
    }

    private fun replayMinersPlacedAndRemoved(): List<MineShaftPosition> {
        return eventStream.filterIsInstance(MinerPlaced::class.java).map { it.at } - eventStream.filterIsInstance(MinerRemoved::class.java).map { it.at }
    }

    private fun requiresPlayerInGame(move: String, by: String) {
        move.requires("$by to be a player in the game.") {
            by in eventStream.filterIsInstance(PlayerAdded::class.java).map { it.name }
        }
    }
}

data class MineShaftPosition(private val _at: Int) {
    init {
        if (_at !in 1..6) throw IllegalArgumentException("MineShaftPosition $_at does not exist.")
    }

    fun previous(): MineShaftPosition = if (_at == 1) this else this.copy(_at = _at - 1)
    fun next(): MineShaftPosition = if (_at == 6) this else this.copy(_at = _at + 1)
    operator fun rangeTo(other: MineShaftPosition): List<MineShaftPosition> = (this._at..other._at).map { MineShaftPosition(it) }
    override fun toString(): String = "mineshaft[$_at]"

    init {
        if (_at !in 1..6) throw IllegalArgumentException("MineShaftPosition $_at does not exist.")
    }
}
