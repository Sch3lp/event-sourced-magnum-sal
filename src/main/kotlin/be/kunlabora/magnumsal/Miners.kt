package be.kunlabora.magnumsal

import be.kunlabora.magnumsal.MagnumSalEvent.MinerMovementEvent
import be.kunlabora.magnumsal.MagnumSalEvent.MinerMovementEvent.MinerPlaced
import be.kunlabora.magnumsal.MagnumSalEvent.MinerMovementEvent.MinerRemoved

class Miners private constructor(private val _miners: List<Miner>) : List<Miner> by _miners {

    companion object {
        fun from(eventStream: EventStream): Miners {
            val miners: List<Miner> = eventStream.filterEvents<MinerMovementEvent>().fold(emptyList()) { acc, event ->
                when (event) {
                    is MinerRemoved -> Miner.from(event)?.let { acc - it } ?: acc
                    is MinerPlaced -> Miner.from(event)?.let { acc + it } ?: acc
                }
            }
            return Miners(miners)
        }
    }
}

data class Miner(val player: PlayerColor, val at: PositionInMine) {
    override fun toString(): String = "$player at $at"

    companion object {
        fun from(event: MinerMovementEvent): Miner? = when (event) {
            is MinerRemoved -> Miner(event.player, event.at)
            is MinerPlaced -> Miner(event.player, event.at)
        }
    }
}
