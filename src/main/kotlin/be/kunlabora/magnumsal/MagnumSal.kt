package be.kunlabora.magnumsal

import be.kunlabora.magnumsal.MagnumSalEvent.PlayerAdded

sealed class MagnumSalEvent: Event {
    data class PlayerAdded(val name: String) : MagnumSalEvent()
}

class MagnumSal(private val eventStream: EventStream) {
    fun addPlayer(playerName: String) {
        eventStream.push(PlayerAdded(playerName))
    }
}
