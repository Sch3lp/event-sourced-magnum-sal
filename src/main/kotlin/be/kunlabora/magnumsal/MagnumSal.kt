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
        eventStream.push(MinerRemoved(by, at))
    }

    private fun requiresPlayerInGame(move: String, by: String) {
        move.requires("$by to be a player in the game.") {
            by in eventStream.filterIsInstance(PlayerAdded::class.java).map { it.name }
        }
    }
}

data class MineShaftPosition(private val _at: Int) {
    fun previous(): MineShaftPosition = if (_at == 1) this else this.copy(_at = _at - 1)
    override fun toString(): String = "mineshaft[$_at]"
}
