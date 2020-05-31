package be.kunlabora.magnumsal

class TransportChain(startingFrom: PositionInMine, playerThatRequiresTransport: PlayerColor, eventStream: EventStream) {
    private val positionsTheSaltWillTravel = startingFrom.positionsUntilTheTop()
    private val transportChain: Map<PositionInMine, List<PlayerColor>> = Miners.from(eventStream).filter { it.at in positionsTheSaltWillTravel }
            .groupBy { it.at }
            .mapValues { (_, miners) -> miners.map { it.player } }
            .filterValues { players -> playerThatRequiresTransport !in players }

    fun containsAll(keys: Set<PlayerColor>) = transportChain.flatMap { (_, players) -> players }.containsAll(keys)

}
