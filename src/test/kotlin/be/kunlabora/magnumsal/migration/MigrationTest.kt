package be.kunlabora.magnumsal.migration

import be.kunlabora.magnumsal.*
import be.kunlabora.magnumsal.MagnumSalEvent.*
import be.kunlabora.magnumsal.migration.read.readEventLog
import org.junit.jupiter.api.Test

class MigrationTest {
    @Test
    fun `migrate | returns a new EventStream with MinersGotTired events`() {
        val migratedEventStream = migrate(readEventLog())
        migratedEventStream.map {
            if (it is SaltMined || it is MinersGotTired) {
                println("${migratedEventStream.indexOf(it)} : $it")
            }
        }
//        assertThat migratedEventStream contains the correct MinersGotTired events
    }
}

fun migrate(eventStream: EventStream): EventStream {
    val nextUnprocessedSaltMinedEvent = nextSaltMinedEvent(eventStream)
    val indexOfNextSaltMinedEvent = eventStream.indexOf(nextUnprocessedSaltMinedEvent)
    val limitedEventStream = EventStream(eventStream.subList(0, indexOfNextSaltMinedEvent + 1).toMutableList())
    val tiredMiners = tiredWorkersAt(limitedEventStream, nextUnprocessedSaltMinedEvent.player, nextUnprocessedSaltMinedEvent.from)
    val migratedEventStream = eventStream.toMutableList()
    val newEvent = MinersGotTired(nextUnprocessedSaltMinedEvent.player, nextUnprocessedSaltMinedEvent.from, tiredMiners)
    migratedEventStream.add(indexOfNextSaltMinedEvent + 1, newEvent)
    return EventStream(migratedEventStream)
}

private fun nextSaltMinedEvent(eventStream: EventStream): SaltMined {
    val lastMinersGotTired = eventStream.lastEventOrNull<MinersGotTired>()
    return if (lastMinersGotTired != null) {
        val eventsAfterMinersGotTired = eventStream.subList(eventStream.indexOf(lastMinersGotTired), eventStream.size - 1)
        EventStream(eventsAfterMinersGotTired.toMutableList()).filterEvents<SaltMined>().first()
    } else {
        eventStream.filterEvents<SaltMined>().first()
    }
}

private fun tiredWorkersAt(limitedEventStream: EventStream, player: PlayerColor, at: PositionInMine): Int {
    val playersSaltMiningActions = limitedEventStream.filterEvents<SaltMined>()
            .filter { it.from == at && it.player == player }
    val minersTiredFromMining = playersSaltMiningActions
            .fold(0) { acc, it -> acc + it.saltMined.size }
    val waterRemainingInChamber = limitedEventStream.filterEvents<MineChamberRevealed>().single { it.at == at }.tile.waterCubes
    val minersTiredFromHoldingBackWater = playersSaltMiningActions
            .count() * waterRemainingInChamber
    return minersTiredFromMining + minersTiredFromHoldingBackWater
}
