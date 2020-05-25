package be.kunlabora.magnumsal

import be.kunlabora.magnumsal.MagnumSalEvent.*
import be.kunlabora.magnumsal.exception.transitionRequires

class TurnOrderRule(private val eventStream: EventStream) {

    private val players
        get() = eventStream.filterEvents<PlayerJoined>()

    private val amountOfPlayers
        get() = players.count()

    private val turnOrder
        get() =
            eventStream.filterEvents<PlayerOrderDetermined>().single()
                    .let { listOfNotNull(it.player1, it.player2, it.player3, it.player4) }

    private val playerActions
        get() = eventStream.filterEvents<PlayerActionEvent>().map { it.player }

    private fun itIsTheTurnOf(player: PlayerColor): Boolean {
        return if (inFirstRound()) {
            playerActions.count() % amountOfPlayers == turnOrder.indexOf(player)
        } else {
            val playerActionsExcludingFirstRound = playerActions.count() - amountOfPlayers
            val actionsPerTurn = 2
            (playerActionsExcludingFirstRound % (amountOfPlayers * actionsPerTurn)).div(actionsPerTurn) == turnOrder.indexOf(player)
        }
    }

    private fun inFirstRound() = playerActions.count() <= amountOfPlayers

    fun onlyInPlayersTurn(player: PlayerColor, block: () -> Unit): Any {
        transitionRequires("it to be your turn") {
            itIsTheTurnOf(player)
        }
        return block()
    }
}
