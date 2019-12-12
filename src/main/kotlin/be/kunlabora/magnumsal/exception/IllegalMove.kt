package be.kunlabora.magnumsal.exception

import java.lang.RuntimeException

class IllegalMove(override val message: String) : RuntimeException(message)
