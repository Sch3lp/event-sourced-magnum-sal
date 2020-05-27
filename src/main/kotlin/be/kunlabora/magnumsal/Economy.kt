package be.kunlabora.magnumsal

import be.kunlabora.magnumsal.MagnumSalEvent.PaymentEvent
import be.kunlabora.magnumsal.MagnumSalEvent.PersonThatCanHandleZloty
import be.kunlabora.magnumsal.MagnumSalEvent.PersonThatCanHandleZloty.Player
import be.kunlabora.magnumsal.gamepieces.Zloty

data class ZlotyPerPlayer(private val eventStream: EventStream) {
    private val _zlotyPerPersonThatCanHandleZloty: Map<PersonThatCanHandleZloty, Zloty>
        get() = eventStream.filterEvents<PaymentEvent>()
                .groupBy(PaymentEvent::person)
                .mapValues { (_, payments) ->
                    payments.fold(0) { acc, payment ->
                        when (payment) {
                            is PaymentEvent.ZlotyPaid -> acc - payment.zloty
                            is PaymentEvent.ZlotyReceived -> acc + payment.zloty
                        }
                    }
                }

    fun all() = _zlotyPerPersonThatCanHandleZloty
    //TODO fix signature: player should be a Player not a PlayerColor
    fun forPlayer(player: PlayerColor) : Zloty = _zlotyPerPersonThatCanHandleZloty
            .filterKeys { it is Player }[Player(player)] ?: 0
}
