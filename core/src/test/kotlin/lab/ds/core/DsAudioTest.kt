package lab.ds.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DsAudioTest {
    private fun only(kind: DsAudio.Kind, rate: Int = 8): DsAudio {
        val audio = DsAudio(rate)
        audio.channels.forEach { it.enabled = false }
        audio.channels[0] = DsAudio.Channel(kind).apply {
            enabled = true
            frequencyHz = 2.0
            volume = 1.0
            pan = 0.0
        }
        return audio
    }

    @Test fun squareDutyAndSampleClockAreDeterministic() {
        val audio = only(DsAudio.Kind.SQUARE)
        audio.channels[0].duty = 0.25
        val first = audio.render(4)
        assertTrue(first[0] > 0); assertTrue(first[2] < 0)
        audio.reset()
        assertTrue(first.contentEquals(audio.render(4)))
    }

    @Test fun waveTableAndNoiseProduceSamples() {
        val wave = only(DsAudio.Kind.WAVE)
        wave.channels[0].wave = doubleArrayOf(-1.0, 1.0)
        wave.channels[0].frequencyHz = 4.0
        val pcm = wave.render(4)
        assertTrue(pcm[0] < 0); assertTrue(pcm[2] > 0)
        val noise = only(DsAudio.Kind.NOISE)
        noise.channels[0].frequencyHz = 8.0
        val samples = noise.render(32)
        assertTrue(samples.any { it > 0 } && samples.any { it < 0 })
    }

    @Test fun panningAndMasterMuteApplyToStereoOutput() {
        val audio = only(DsAudio.Kind.SQUARE)
        audio.channels[0].pan = -1.0
        val leftOnly = audio.render(1)
        assertTrue(leftOnly[0] > 0); assertEquals(0, leftOnly[1].toInt())
        audio.masterEnabled = false
        assertTrue(audio.render(3).all { it == 0.toShort() })
    }

    @Test fun mixClipsAndMasterVolumeScales() {
        val audio = DsAudio(32768)
        audio.channels.forEach { it.volume = 1.0; it.pan = 0.0 }
        val loud = audio.render(1)
        assertTrue(loud[0] <= 32767 && loud[0] > 0)
        audio.reset(); audio.masterVolume = 0.0
        assertTrue(audio.render(4).all { it == 0.toShort() })
    }
}
