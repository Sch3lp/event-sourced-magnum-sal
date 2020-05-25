package be.kunlabora.magnumsal

import be.kunlabora.magnumsal.MagnumSalEvent.PlayerActionEvent.MinerMovementEvent


class Miners private constructor(private val _miners: List<Miner>) : List<Miner> by _miners {

    companion object {
        fun from(eventStream: EventStream): Miners {
            val miners: List<Miner> = eventStream.filterEvents<MinerMovementEvent>().fold(emptyList()) { acc, event ->
                when (event) {
                    is MinerMovementEvent.MinerRemoved -> Miner.from(event)?.let { acc - it } ?: acc
                    is MinerMovementEvent.MinerPlaced -> Miner.from(event)?.let { acc + it } ?: acc
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
            is MinerMovementEvent.MinerRemoved -> Miner(event.player, event.at)
            is MinerMovementEvent.MinerPlaced -> Miner(event.player, event.at)
        }
    }
}
