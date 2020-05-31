package be.kunlabora.magnumsal

import be.kunlabora.magnumsal.MagnumSalEvent.PlayerActionEvent.MinerMovementEvent.MinerPlaced

class TransportChainTestBuilder(
        private val _eventStream: EventStream = EventStream(),
        val startsAt: PositionInMine,
        val playerThatRequiresTransport: PlayerColor,
        val chain: Map<PositionInMine, List<PlayerColor>>
) {
    fun build(): TransportChain {
        chain.flatMap { (at, players) -> players.map { p -> at to p } }
                .forEach { (at, player) ->
                    _eventStream.push(MinerPlaced(player, at))
                }
        return TransportChain(startsAt, playerThatRequiresTransport, _eventStream)
    }
}
