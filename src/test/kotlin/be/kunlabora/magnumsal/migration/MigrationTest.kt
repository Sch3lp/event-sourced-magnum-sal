package be.kunlabora.magnumsal.migration

import be.kunlabora.magnumsal.*
import be.kunlabora.magnumsal.MagnumSalEvent.*
import be.kunlabora.magnumsal.PlayerColor.*
import be.kunlabora.magnumsal.PositionInMine.Companion.at
import be.kunlabora.magnumsal.gamepieces.Salt.BROWN
import be.kunlabora.magnumsal.gamepieces.Salt.GREEN
import be.kunlabora.magnumsal.gamepieces.Salts
import be.kunlabora.magnumsal.migration.read.readEventLog
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class MigrationTest {
    @Disabled
    @Test
    fun `migrate | returns a new EventStream with MinersGotTired events`() {
        val migratedEventStream = migrate(readEventLog())
        migratedEventStream.map {
            if (it is SaltMined || it is MinersGotTired) {
                println("${migratedEventStream.indexOf(it)} :        $it")
            }
        }
        assertThat(migratedEventStream)
                .containsOnlyOnce(SaltMined(White, at(2, -1), Salts(BROWN, GREEN)))
                .containsOnlyOnce(MinersGotTired(White, at(2, -1), 3))
                .containsOnlyOnce(SaltMined(Black, at(2, -2), Salts(BROWN, BROWN)))
                .containsOnlyOnce(MinersGotTired(Black, at(2, -2), 3))
                .containsOnlyOnce(SaltMined(Orange, at(2, 1), Salts(BROWN, BROWN)))
                .containsOnlyOnce(MinersGotTired(Orange, at(2, 1), 3))
                .containsOnlyOnce(SaltMined(Purple, at(2, 2), Salts(BROWN, BROWN)))
                .containsOnlyOnce(MinersGotTired(Purple, at(2, 2), 2)) //tile has 0 water cubes
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
