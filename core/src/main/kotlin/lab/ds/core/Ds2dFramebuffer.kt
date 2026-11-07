package lab.ds.core

/**
 * Small, original DS 2D-engine bitmap-mode experiment (mode 3 only).
 * Reads supplied VRAM; does not execute a ROM or model DS registers.
 */
class Ds2dFramebuffer {
    companion object {
        const val WIDTH = 256
        const val HEIGHT = 192
        const val BYTES_PER_PIXEL = 2
        const val VRAM_SIZE = WIDTH * HEIGHT * BYTES_PER_PIXEL
    }

    /** Render RGB555 pixels, little-endian, into opaque ARGB pixels. */
    fun renderMode3(vram: ByteArray, offset: Int = 0): IntArray {
        require(offset >= 0 && offset <= vram.size - VRAM_SIZE) { "mode-3 bitmap does not fit VRAM" }
        return IntArray(WIDTH * HEIGHT) { pixel ->
            val at = offset + pixel * 2
            val color = (vram[at].toInt() and 0xff) or ((vram[at + 1].toInt() and 0xff) shl 8)
            val r5 = color and 0x1f
            val g5 = (color ushr 5) and 0x1f
            val b5 = (color ushr 10) and 0x1f
            val r = (r5 shl 3) or (r5 ushr 2)
            val g = (g5 shl 3) or (g5 ushr 2)
            val b = (b5 shl 3) or (b5 ushr 2)
            (0xff shl 24) or (r shl 16) or (g shl 8) or b
        }
    }
}
