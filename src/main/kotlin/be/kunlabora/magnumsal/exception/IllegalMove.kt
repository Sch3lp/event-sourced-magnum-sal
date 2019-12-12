package be.kunlabora.magnumsal.exception

import java.lang.RuntimeException

class IllegalMove(override val message: String) : RuntimeException(message)

fun String.requires(condition: String? = null, requirement: () -> Boolean) {
    if (!requirement()) {
        val message = condition?.let { "$this requires $it" } ?: this
        throw IllegalMove(message)
    }
}
