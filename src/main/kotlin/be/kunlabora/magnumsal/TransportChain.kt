package be.kunlabora.magnumsal

import be.kunlabora.magnumsal.gamepieces.Salts

class TransportChain(startingFrom: PositionInMine, playerThatRequiresTransport: PlayerColor, eventStream: EventStream) {
    private val positionsTheSaltWillTravel = startingFrom.positionsUntilTheTop()
    private val transportChain: Map<PositionInMine, List<PlayerColor>> = Miners.from(eventStream).filter { it.at in positionsTheSaltWillTravel }
            .groupBy { it.at }
            .mapValues { (_, miners) -> miners.map { it.player } }
            .filterValues { players -> playerThatRequiresTransport !in players }

    fun containsAll(keys: Set<PlayerColor>) = transportChain.flatMap { (_, players) -> players }.containsAll(keys)
    fun transportersNeeded() = transportChain.count()
    fun transportCostFor(saltMined: Salts) = saltMined.size * transportersNeeded()

}
