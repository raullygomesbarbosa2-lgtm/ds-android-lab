package lab.ds.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class Ds2dFramebufferTest {
    @Test fun convertsRgb555PrimaryColorsAndAlpha() {
        val vram = ByteArray(Ds2dFramebuffer.VRAM_SIZE)
        vram[0] = 0x1f
        vram[2] = (0x03e0 and 0xff).toByte(); vram[3] = (0x03e0 ushr 8).toByte()
        vram[4] = (0x7c00 and 0xff).toByte(); vram[5] = (0x7c00 ushr 8).toByte()
        val pixels = Ds2dFramebuffer().renderMode3(vram)
        assertEquals(0xffff0000.toInt(), pixels[0])
        assertEquals(0xff00ff00.toInt(), pixels[1])
        assertEquals(0xff0000ff.toInt(), pixels[2])
        assertEquals(256 * 192, pixels.size)
    }

    @Test fun rejectsShortVramAndBadOffset() {
        val renderer = Ds2dFramebuffer()
        assertFailsWith<IllegalArgumentException> { renderer.renderMode3(ByteArray(12)) }
        assertFailsWith<IllegalArgumentException> { renderer.renderMode3(ByteArray(Ds2dFramebuffer.VRAM_SIZE), 1) }
    }
}
