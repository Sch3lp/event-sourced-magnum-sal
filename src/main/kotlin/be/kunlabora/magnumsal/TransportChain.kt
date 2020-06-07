package be.kunlabora.magnumsal

import be.kunlabora.magnumsal.gamepieces.Salts
import be.kunlabora.magnumsal.gamepieces.Zloty

class TransportChain(startingFrom: PositionInMine, playerThatRequiresTransport: PlayerColor, eventStream: EventStream) {
    private val positionsTheSaltWillTravel = startingFrom.positionsUntilTheTop()
    private val transportChain: Map<PositionInMine, List<PlayerColor>> = Miners.from(eventStream).filter { it.at in positionsTheSaltWillTravel }
            .groupBy { it.at }
            .mapValues { (_, miners) -> miners.map { it.player } }
            .filterValues { players -> playerThatRequiresTransport !in players }

    fun containsAll(keys: Set<PlayerColor>) = transportChain.flatMap { (_, players) -> players }.containsAll(keys)
    fun transportCostFor(saltMined: Salts) = saltMined.size * positionsThatRequireTransport()
    fun maxTransportCostPerPlayer(saltMined: Salts): Map<PlayerColor, Zloty> = getPlayerTransportStrength()
            .mapValues { (_, transportStrength) -> transportStrength * saltMined.size }

    private fun getPlayerTransportStrength(): Map<PlayerColor, Int> {
        val playersPerPosition: Map<PositionInMine, Set<PlayerColor>> = transportChain.mapValues { (_, miners) -> miners.toSet() }
        return playersPerPosition.values.flatten().map { player ->
                val strength: Int = playersPerPosition.count { (_, playersAtPos) -> player in playersAtPos }
                player to strength
            }.toMap()
    }

    private fun positionsThatRequireTransport() = transportChain.count()

}
