package be.kunlabora.magnumsal

import be.kunlabora.magnumsal.MagnumSalEvent.PaymentEvent
import be.kunlabora.magnumsal.gamepieces.Zloty

data class ZlotyPerPlayer(private val eventStream: EventStream) {
    private val _zlotyPerPlayer: Map<PlayerColor, Zloty>
        get() = eventStream.filterEvents<PaymentEvent>()
                .groupBy(PaymentEvent::player)
                .mapValues { (_, payments) ->
                    payments.fold(0) { acc, payment ->
                        when (payment) {
                            is PaymentEvent.ZlotyPaid -> acc - payment.zloty
                            is PaymentEvent.ZlotyReceived -> acc + payment.zloty
                        }
                    }
                }

    fun all() = _zlotyPerPlayer
    fun forPlayer(player: PlayerColor) : Zloty = _zlotyPerPlayer[player] ?: 0
}
