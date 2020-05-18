package be.kunlabora.magnumsal

import be.kunlabora.magnumsal.MagnumSalEvent.*
import be.kunlabora.magnumsal.PositionInMine.Companion.at

sealed class MagnumSalEvent : Event {
    data class PlayerWasAdded(val name: String) : MagnumSalEvent()
    data class MinerWasPlaced(val positionInMine: PositionInMine) : MagnumSalEvent()
    data class MinerWasRemoved(val positionInMine: PositionInMine) : MagnumSalEvent()
}

data class MinersAtPosition(val positionInMine: PositionInMine, val miners: Int)

class MagnumSal(private val eventStream: EventStream) {
    /**
     * Adds a player with the given name to a game of MagnumSal.
     * This is merely an example that works in tandem with the already existing PlayerAdded event.
     * When you've managed to implement this and get the existing test green,
     * you should feel confident enough to continue on your own.
     * If that's not the case, take a look at the hints in the README.md and/or read the assignment (actual exercise) again.
     */

    private val minersInTheMineShaft: List<MinersAtPosition>
        get() {
            val minersThatWerePlaced = eventStream.filterEvents<MinerWasPlaced>()
                    .groupBy { it.positionInMine }
                    .mapValues { (pos, placedEvents) -> MinersAtPosition(pos, placedEvents.size) }
                    .values.toList()
            val minersThatWereRemoved = eventStream.filterEvents<MinerWasRemoved>()
                    .groupBy { it.positionInMine }
                    .mapValues { (pos, removedEvents) -> MinersAtPosition(pos, removedEvents.size) }
                    .values.toList()
            return minersThatWerePlaced.map { minersPlaced ->
                minersThatWereRemoved.firstOrNull { it.positionInMine == minersPlaced.positionInMine }
                        ?.let { minersRemoved -> minersPlaced.copy(miners = minersPlaced.miners - minersRemoved.miners) }
                        ?: minersPlaced
            }
        }

    fun addPlayer(playerName: String) {
        eventStream.push(PlayerWasAdded(playerName))
    }

    fun placeWorker(pos: PositionInMine) {
        if (pos != at(1, 0) && noMinersAtPosition(pos.previous())) throw IllegalMove("The chain rule must be obeyed")
        eventStream.push(MinerWasPlaced(pos))
    }

    fun removeWorker(pos: PositionInMine) {
        val minersAtPosition = minersInTheMineShaft.firstOrNull { it.positionInMine == pos }?.miners ?: 0
        if (noMinersAtPosition(pos)) throw IllegalMove("There is no miner to be removed at $pos")
        if (isNotLastMinerInTheChain(pos) && minersAtPosition == 1) throw IllegalMove("The chain rule must be obeyed")

        eventStream.push(MinerWasRemoved(pos))
    }

    private fun noMinersAtPosition(pos: PositionInMine) = minersInTheMineShaft.firstOrNull { it.positionInMine == pos }?.miners ?: 0 == 0

    private fun isLastMinerInTheChain(pos: PositionInMine) =
            pos.isAnEnd() || minersInTheMineShaft.none { it.positionInMine in pos.nearestFurtherPositions() }
    private fun isNotLastMinerInTheChain(pos: PositionInMine) = !isLastMinerInTheChain(pos)
}
