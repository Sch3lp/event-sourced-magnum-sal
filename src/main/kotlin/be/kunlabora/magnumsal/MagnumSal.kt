package be.kunlabora.magnumsal

import be.kunlabora.magnumsal.MagnumSalEvent.MinerPlaced
import be.kunlabora.magnumsal.MagnumSalEvent.PlayerAdded
import be.kunlabora.magnumsal.exception.requires

sealed class MagnumSalEvent: Event {
    data class PlayerAdded(val name: String) : MagnumSalEvent()
    data class MinerPlaced(val by: String, val at: MineShaftPosition) : MagnumSalEvent()
}

class MagnumSal(private val eventStream: EventStream) {
    fun addPlayer(playerName: String) {
        eventStream.push(PlayerAdded(playerName))
    }

    fun placeMiner(by: String, at: MineShaftPosition) {
        "Placing a miner".requires("$by to be a player in the game.") {
            by in eventStream.filterIsInstance(PlayerAdded::class.java).map { it.name }
        }
        eventStream.push(MinerPlaced(by, at))
    }
}

data class MineShaftPosition(private val _pos: Int)
