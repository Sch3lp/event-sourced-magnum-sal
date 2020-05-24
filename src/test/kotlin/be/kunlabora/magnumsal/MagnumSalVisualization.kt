package be.kunlabora.magnumsal

private fun visualize(miners: Miners) {
    println("${"#".repeat(25)} MineShaft Top ${"#".repeat(25)}")
    miners.groupBy { it.at }
            .forEach { (at, miners) ->
                val amountOfMinersPerPlayer = miners.groupBy(Miner::player).mapValues { (player, pMiners) -> player.icon(pMiners.size) }.values.joinToString(separator = "")
                println("$at: $amountOfMinersPerPlayer")
            }
    println("${"#".repeat(25)} MineShaft End ${"#".repeat(25)}")
}

private fun visualizeZloty(eventStream: EventStream) {
    println("Złoty per player: " + ZlotyPerPlayer(eventStream).all().mapValues { (player, zloty) -> "${player.icon()}: $zloty zł" }.values.joinToString())
}

private fun PlayerColor.icon(n: Int = 1): String = when (this) {
    PlayerColor.White -> "💛"
    PlayerColor.Black -> "🖤"
    PlayerColor.Orange -> "🧡"
    PlayerColor.Purple -> "💜"
}.repeat(n)

fun MagnumSal.visualizeMiners() = this.visualize { eventStream -> visualize(Miners.from(eventStream)) }
fun MagnumSal.visualizeZloty() = this.visualize { eventStream -> visualizeZloty(eventStream) }
