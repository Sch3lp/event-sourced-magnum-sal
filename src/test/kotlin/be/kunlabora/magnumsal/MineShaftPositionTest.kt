package be.kunlabora.magnumsal

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MineShaftPositionTest {
    @Test
    fun `previous | MineShaftPosition 1 returns itself`() {
        assertThat(MineShaftPosition(1).previous()).isEqualTo(MineShaftPosition(1))
    }

    @Test
    fun `previous | MineShaftPosition other than 1 returns previous position`() {
        assertThat(MineShaftPosition(2).previous()).isEqualTo(MineShaftPosition(1))
        assertThat(MineShaftPosition(3).previous()).isEqualTo(MineShaftPosition(2))
        assertThat(MineShaftPosition(4).previous()).isEqualTo(MineShaftPosition(3))
        assertThat(MineShaftPosition(5).previous()).isEqualTo(MineShaftPosition(4))
        assertThat(MineShaftPosition(6).previous()).isEqualTo(MineShaftPosition(5))
    }

    @Test
    fun `toString | just returns the internal int value`() {
        assertThat(MineShaftPosition(1).toString()).isEqualTo("mineshaft[1]")
    }
}
