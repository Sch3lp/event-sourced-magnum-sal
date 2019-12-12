package be.kunlabora.magnumsal

import be.kunlabora.magnumsal.MagnumSalEvent.PlayerAdded

sealed class MagnumSalEvent: Event {
    data class PlayerAdded(val name: String) : MagnumSalEvent()
    data class MinerPlaced(val by: String, val at: MineShaftPosition) : MagnumSalEvent()
}

class MagnumSal(private val eventStream: EventStream) {
    fun addPlayer(playerName: String) {
        eventStream.push(PlayerAdded(playerName))
    }

    fun placeMiner(by: String, at: MineShaftPosition) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

data class MineShaftPosition(private val _pos: Int)
