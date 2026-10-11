package dev.dreamerslab.media3

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import androidx.media3.common.C
import androidx.media3.common.audio.ChannelMixingAudioProcessor
import androidx.media3.common.audio.ChannelMixingMatrix

internal class AudioEffects {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var loudness: LoudnessEnhancer? = null

    private val bandLevels = mutableMapOf<Int, Short>()
    private var presetName: String? = null

    val channelMixer = ChannelMixingAudioProcessor().apply {
        (1..MAX_CHANNELS).forEach { putChannelMixingMatrix(ChannelMixingMatrix.createForConstantGain(it, it)) }
    }

    var equalizerEnabled = false
        set(value) {
            field = value
            equalizer?.enabled = value
        }

    var bassBoostPercent = 0
        set(value) {
            field = value
            bassBoost?.apply {
                enabled = value > 0
                if (strengthSupported) setStrength((value * 10).toShort())
            }
        }

    var loudnessGainDb = 0
        set(value) {
            field = value
            loudness?.apply {
                enabled = value > 0
                setTargetGain(value * 100)
            }
        }

    var mono = false
        set(value) {
            field = value
            updateStereoMix()
        }

    var balance = 0
        set(value) {
            field = value
            updateStereoMix()
        }

    val presets: List<String>
        get() = equalizer?.let { eq -> (0 until eq.numberOfPresets).map { presetName(eq, it) } }.orEmpty()

    val currentPreset: String
        get() = presetName ?: ""

    val bandFrequencies: List<Int>
        get() = equalizer?.let { eq -> (0 until eq.numberOfBands).map { eq.getCenterFreq(it.toShort()) / 1000 } }.orEmpty()

    val levelRangeDb: Pair<Int, Int>
        get() = equalizer?.bandLevelRange?.let { it[0] / 100 to it[1] / 100 } ?: (0 to 0)

    fun attach(audioSessionId: Int) {
        release()
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET) return
        equalizer = runCatching { Equalizer(0, audioSessionId) }.getOrNull()
        bassBoost = runCatching { BassBoost(0, audioSessionId) }.getOrNull()
        loudness = runCatching { LoudnessEnhancer(audioSessionId) }.getOrNull()
        presetName?.let(::applyPreset)
        bandLevels.forEach { (band, level) -> equalizer?.setBandLevel(band.toShort(), level) }
        equalizerEnabled = equalizerEnabled
        bassBoostPercent = bassBoostPercent
        loudnessGainDb = loudnessGainDb
    }

    fun usePreset(name: String): Boolean {
        if (equalizer != null && name !in presets) return false
        presetName = name
        bandLevels.clear()
        applyPreset(name)
        return true
    }

    fun setBandLevel(band: Int, levelDb: Int) {
        val level = (levelDb * 100).toShort()
        presetName = null
        bandLevels[band] = level
        equalizer?.setBandLevel(band.toShort(), level)
    }

    fun bandLevelDb(band: Int): Int = (equalizer?.getBandLevel(band.toShort()) ?: 0) / 100

    fun release() {
        listOf(equalizer, bassBoost, loudness).forEach { it?.release() }
        equalizer = null
        bassBoost = null
        loudness = null
    }

    private fun presetName(equalizer: Equalizer, index: Int): String =
        equalizer.getPresetName(index.toShort()).takeWhile { !it.isISOControl() && it != '\uFFFD' }.trim()

    private fun applyPreset(name: String) {
        val index = presets.indexOf(name).takeIf { it >= 0 } ?: return
        equalizer?.usePreset(index.toShort())
    }

    private fun updateStereoMix() {
        val left = (1f - balance / 100f).coerceAtMost(1f)
        val right = (1f + balance / 100f).coerceAtMost(1f)
        val mix = if (mono) floatArrayOf(0.5f, 0.5f, 0.5f, 0.5f) else floatArrayOf(1f, 0f, 0f, 1f)
        channelMixer.putChannelMixingMatrix(
            ChannelMixingMatrix(2, 2, floatArrayOf(mix[0] * left, mix[1] * right, mix[2] * left, mix[3] * right))
        )
    }

    private companion object {
        const val MAX_CHANNELS = 8
    }
}
