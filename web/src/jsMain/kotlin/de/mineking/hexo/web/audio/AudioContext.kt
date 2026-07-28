package de.mineking.hexo.web.audio

@JsName("AudioContext")
external class AudioContext {
    val currentTime: Double
    val destination: AudioNode

    fun createOscillator(): OscillatorNode
    fun createGain(): GainNode
    fun resume()
}

open external class AudioNode {
    @IgnorableReturnValue
    fun connect(destination: AudioNode): AudioNode
}

external class OscillatorNode : AudioNode {
    val frequency: AudioParam
    var type: String

    fun start(whenTime: Double = definedExternally)
    fun stop(whenTime: Double = definedExternally)
}

external class GainNode : AudioNode {
    val gain: AudioParam
}

external class AudioParam {
    var value: Float

    fun setValueAtTime(value: Float, startTime: Double)
    fun exponentialRampToValueAtTime(value: Float, endTime: Double)
}
