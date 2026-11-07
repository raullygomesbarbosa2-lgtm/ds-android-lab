package lab.ds.core

/** Parses fixed NDS header metadata only; it does not execute cartridge code. */
class NdsHeaderParser {
    data class Program(val romOffset: Long, val entryAddress: Long, val loadAddress: Long, val size: Long)
    data class Header(val title: String, val gameCode: String, val arm9: Program, val arm7: Program)

    fun parse(headerBytes: ByteArray, romLength: Long): Header {
        require(romLength >= HEADER_SIZE) { "ROM is shorter than the NDS header" }
        require(headerBytes.size >= HEADER_SIZE) { "Incomplete NDS header (need 0x160 bytes)" }
        val title = ascii(headerBytes, 0, 12).trim()
        val gameCode = ascii(headerBytes, 0x0c, 4)
        require(title.isNotEmpty()) { "Header title is empty" }
        require(gameCode.length == 4 && gameCode.all { it in ' '..'~' }) { "Invalid game code" }
        return Header(title, gameCode, program(headerBytes, 0x20, romLength, "ARM9"), program(headerBytes, 0x30, romLength, "ARM7"))
    }

    private fun program(b: ByteArray, base: Int, romLength: Long, name: String): Program {
        val offset = u32(b, base); val entry = u32(b, base + 4); val load = u32(b, base + 8); val size = u32(b, base + 12)
        require(offset >= HEADER_SIZE) { "$name ROM offset points into the header" }
        require(size > 0) { "$name binary size is zero" }
        require(offset <= romLength && size <= romLength - offset) { "$name binary extends beyond the ROM file" }
        require(load <= UINT32_MAX - size) { "$name load range overflows 32-bit address space" }
        require(entry >= load && entry < load + size && entry % 4L == 0L) { "$name entry address is outside its aligned load range" }
        return Program(offset, entry, load, size)
    }
    private fun u32(b: ByteArray, i: Int): Long = (b[i].toLong() and 255) or ((b[i+1].toLong() and 255) shl 8) or ((b[i+2].toLong() and 255) shl 16) or ((b[i+3].toLong() and 255) shl 24)
    private fun ascii(b: ByteArray, start: Int, length: Int) = String(b, start, length, Charsets.US_ASCII).trimEnd('\u0000', ' ')
    companion object { const val HEADER_SIZE = 0x160; private const val UINT32_MAX = 0xffff_ffffL }
}
