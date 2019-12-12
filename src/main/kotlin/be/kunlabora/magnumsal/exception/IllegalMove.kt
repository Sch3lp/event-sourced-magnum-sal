package be.kunlabora.magnumsal.exception

import java.lang.RuntimeException

class IllegalMove(override val message: String) : RuntimeException(message)

fun String.requires(condition: String, requirement: () -> Boolean) {
    if (!requirement()) {
        throw IllegalMove("$this requires $condition")
    }
}
