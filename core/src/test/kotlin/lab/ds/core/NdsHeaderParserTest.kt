package lab.ds.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NdsHeaderParserTest {
    private val parser = NdsHeaderParser()
    private val romSize = 0x5000L

    @Test fun parsesTitleCodeAndCpuRegions() {
        val h = parser.parse(validHeader(), romSize)
        assertEquals("LAB HOME", h.title); assertEquals("ABCD", h.gameCode)
        assertEquals(NdsHeaderParser.Program(0x4000, 0x02000000, 0x02000000, 0x100), h.arm9)
        assertEquals(NdsHeaderParser.Program(0x4100, 0x03800000, 0x03800000, 0x80), h.arm7)
    }
    @Test fun rejectsShortHeaderAndRom() {
        assertFailsWith<IllegalArgumentException> { parser.parse(ByteArray(0x15f), romSize) }
        assertFailsWith<IllegalArgumentException> { parser.parse(validHeader(), 0x15f) }
    }
    @Test fun rejectsBinaryBoundsAndZeroSize() {
        assertFailsWith<IllegalArgumentException> { parser.parse(validHeader().also { put32(it, 0x20, 0x4fff) }, romSize) }
        assertFailsWith<IllegalArgumentException> { parser.parse(validHeader().also { put32(it, 0x3c, 0) }, romSize) }
        assertFailsWith<IllegalArgumentException> { parser.parse(validHeader().also { put32(it, 0x30, 0x5000) }, romSize) }
    }
    @Test fun rejectsInvalidCodeAndEntryOutsideRange() {
        assertFailsWith<IllegalArgumentException> { parser.parse(validHeader().also { it[0x0c] = 0 }, romSize) }
        assertFailsWith<IllegalArgumentException> { parser.parse(validHeader().also { put32(it, 0x24, 0x02000100) }, romSize) }
    }
    @Test fun rejectsAddressOverflow() {
        val b = validHeader().also { put32(it, 0x28, 0xfffffff0L); put32(it, 0x24, 0xfffffff0L); put32(it, 0x2c, 0x100) }
        assertFailsWith<IllegalArgumentException> { parser.parse(b, romSize) }
    }
    private fun validHeader() = ByteArray(NdsHeaderParser.HEADER_SIZE).also { b ->
        "LAB HOME".toByteArray().copyInto(b, 0); "ABCD".toByteArray().copyInto(b, 0x0c)
        put32(b,0x20,0x4000); put32(b,0x24,0x02000000); put32(b,0x28,0x02000000); put32(b,0x2c,0x100)
        put32(b,0x30,0x4100); put32(b,0x34,0x03800000); put32(b,0x38,0x03800000); put32(b,0x3c,0x80)
    }
    private fun put32(b: ByteArray, i: Int, v: Long) { repeat(4) { b[i+it] = (v shr (8*it)).toByte() } }
}
