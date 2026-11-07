package lab.ds.core

/** Standalone DS-inspired PSG and stereo mixer experiment; not register-accurate hardware emulation. */
class DsAudio(private val sampleRate: Int = 32768) {
    init { require(sampleRate > 0) }
    enum class Kind { SQUARE, WAVE, NOISE }
    data class Channel(
        val kind: Kind, var frequencyHz: Double = 440.0, var volume: Double = 1.0,
        var pan: Double = 0.0, var enabled: Boolean = true, var duty: Double = 0.5,
        var wave: DoubleArray = DoubleArray(32) { if (it < 16) 1.0 else -1.0 },
        var noiseSeed: Int = 0x7fff
    ) {
        internal var phase = 0.0
        internal var waveIndex = 0.0
        internal var lfsr = noiseSeed and 0x7fff
    }
    val channels = MutableList(8) { Channel(Kind.SQUARE) }
    var masterVolume = 1.0
        set(value) { field = value.coerceIn(0.0, 1.0) }
    var masterEnabled = true

    /** Returns signed 16-bit interleaved stereo PCM (L,R), with persistent per-channel phase. */
    fun render(frames: Int): ShortArray {
        require(frames >= 0)
        val out = ShortArray(frames * 2)
        if (!masterEnabled) return out
        for (frame in 0 until frames) {
            var left = 0.0; var right = 0.0
            for (ch in channels) {
                if (!ch.enabled || ch.volume <= 0.0) continue
                require(ch.frequencyHz >= 0.0 && ch.frequencyHz.isFinite())
                val s = sample(ch)
                val gain = ch.volume.coerceIn(0.0, 1.0) * masterVolume / channels.size
                val pan = ch.pan.coerceIn(-1.0, 1.0)
                left += s * gain * (1.0 - pan) * 0.5
                right += s * gain * (1.0 + pan) * 0.5
            }
            out[frame * 2] = pcm(left); out[frame * 2 + 1] = pcm(right)
        }
        return out
    }
    private fun sample(ch: Channel): Double = when (ch.kind) {
        Kind.SQUARE -> {
            val value = if (ch.phase < ch.duty.coerceIn(0.0, 0.999999)) 1.0 else -1.0
            ch.phase = (ch.phase + ch.frequencyHz / sampleRate) % 1.0
            value
        }
        Kind.WAVE -> {
            if (ch.wave.isEmpty()) 0.0 else {
                val value = ch.wave[ch.waveIndex.toInt() % ch.wave.size].coerceIn(-1.0, 1.0)
                ch.waveIndex = (ch.waveIndex + ch.frequencyHz * ch.wave.size / sampleRate) % ch.wave.size
                value
            }
        }
        Kind.NOISE -> {
            ch.phase += ch.frequencyHz / sampleRate
            while (ch.phase >= 1.0) {
                val feedback = (ch.lfsr xor (ch.lfsr ushr 1)) and 1
                ch.lfsr = (ch.lfsr ushr 1) or (feedback shl 14)
                if (ch.lfsr == 0) ch.lfsr = 0x7fff
                ch.phase -= 1.0
            }
            if (ch.lfsr and 1 == 0) 1.0 else -1.0
        }
    }
    private fun pcm(x: Double) = (x.coerceIn(-1.0, 1.0) * 32767.0).toInt().toShort()
    fun reset() { channels.forEach { it.phase = 0.0; it.waveIndex = 0.0; it.lfsr = (it.noiseSeed and 0x7fff).let { seed -> if (seed == 0) 0x7fff else seed } } }
}
