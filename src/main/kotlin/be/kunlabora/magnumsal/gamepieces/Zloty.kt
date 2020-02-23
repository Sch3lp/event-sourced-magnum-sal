package be.kunlabora.magnumsal.gamepieces

class Zloty private constructor(private val _val: Int) {
    init {
        require(_val >= 0) { "Zloty must always be positive" }
    }
    override fun toString() = "$_val zł"
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Zloty

        if (_val != other._val) return false

        return true
    }
    override fun hashCode(): Int {
        return _val
    }

    companion object {
        fun zł(_val : Int) = Zloty(_val)
    }
}
