package de.mineking.hexo.web.audio

import com.varabyte.kobweb.navigation.BasePath
import org.w3c.dom.HTMLAudioElement
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

interface PlayableSoundEffect {
    fun play(context: AudioContext)
}

sealed interface SoundEffectSource {
    fun load(): PlayableSoundEffect

    class AssetSoundEffectSource(val path: String) : SoundEffectSource {
        override fun load(): PlayableSoundEffect = AssetSoundEffect(path)

        private class AssetSoundEffect(path: String) : PlayableSoundEffect {
            val template = createAudio(BasePath.prependTo("/sounds/$path"))

            private fun createAudio(@Suppress("UNUSED") sourceUrl: String): HTMLAudioElement {
                val audio = js("new Audio(sourceUrl)") as HTMLAudioElement
                audio.preload = "auto"
                return audio
            }

            override fun play(context: AudioContext) {
                template.currentTime = 0.0
                template.play()
            }
        }
    }

    class SynthesizerSoundEffectSource(
        val frequency: Float,
        val duration: Duration,
        val gain: Float,
        val type: OscillatorType,
    ) : SoundEffectSource {
        override fun load(): PlayableSoundEffect = SynthesizerSoundEffect(frequency, duration, gain, type)

        private class SynthesizerSoundEffect(
            val frequency: Float,
            val duration: Duration,
            val gain: Float,
            val type: OscillatorType,
        ) : PlayableSoundEffect {
            override fun play(context: AudioContext) {
                val oscillator = context.createOscillator()
                val gainNode = context.createGain()
                val toneStartTime = context.currentTime + 0.01
                val toneEndTime = toneStartTime + duration.inWholeMilliseconds / 1000.0

                oscillator.type = type.type
                oscillator.frequency.setValueAtTime(frequency, toneStartTime)

                gainNode.gain.setValueAtTime(0.0001f, toneStartTime)
                gainNode.gain.exponentialRampToValueAtTime(gain, toneStartTime + 0.01)
                gainNode.gain.exponentialRampToValueAtTime(0.0001f, toneEndTime)

                oscillator.connect(gainNode)
                gainNode.connect(context.destination)

                oscillator.start(toneStartTime)
                oscillator.stop(toneEndTime + 0.02)
            }
        }
    }
}

enum class OscillatorType(val type: String) {
    SawTooth("sawtooth"),
    Sine("sine"),
    Square("square"),
    Triangle("triangle"),
}

enum class SoundEffect(val source: SoundEffectSource) {
    GameStart(SoundEffectSource.AssetSoundEffectSource("game-start.aac")),
    GameWin(SoundEffectSource.AssetSoundEffectSource("game-win.aac")),
    TilePlaced(SoundEffectSource.SynthesizerSoundEffectSource(
        frequency = 659.25f,
        duration = 70.milliseconds,
        gain = 0.25f,
        type = OscillatorType.Triangle,
    )),
    CountdownWarning(SoundEffectSource.SynthesizerSoundEffectSource(
        frequency = 880f,
        duration = 85.milliseconds,
        gain = 0.25f,
        type = OscillatorType.Square,
    )),
}

class SoundPlayer {
    private var audioContext: AudioContext? = null
    private val sounds = SoundEffect.entries.associateWith { it.source.load() }

    fun play(sound: SoundEffect) {
        val context = audioContext ?: AudioContext().also { audioContext = it }
        context.resume()

        sounds[sound]?.play(context)
    }
}
