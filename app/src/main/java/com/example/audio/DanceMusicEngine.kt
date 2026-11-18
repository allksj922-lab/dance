package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import com.example.model.BeatTelemetry
import com.example.model.DanceGenre
import com.example.model.DanceTrack
import com.example.model.TrackSection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Real-time 44.1kHz stereo PCM dance music synthesizer powered by AudioTrack.
 * Synthesizes multi-layered EDM/Disco/House/Pop tracks parameterized by each day's DanceTrack.
 */
class DanceMusicEngine {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var playbackJob: Job? = null

    private val _telemetry = MutableStateFlow(BeatTelemetry())
    val telemetry: StateFlow<BeatTelemetry> = _telemetry.asStateFlow()

    @Volatile
    private var activeTrack: DanceTrack? = null

    @Volatile
    private var masterVolume: Float = 0.85f

    @Volatile
    private var fadeInTotalSamples: Long = 0L

    fun start(track: DanceTrack, fadeInSeconds: Int = 0) {
        val alreadyPlayingSame = playbackJob?.isActive == true && activeTrack?.id == track.id
        if (alreadyPlayingSame) return

        stop()
        activeTrack = track
        fadeInTotalSamples = if (fadeInSeconds > 0) fadeInSeconds * SAMPLE_RATE.toLong() else 0L

        playbackJob = scope.launch {
            runSynthLoop(track)
        }
    }

    fun switchTrack(track: DanceTrack) {
        val wasPlaying = playbackJob?.isActive == true
        if (wasPlaying) {
            start(track, fadeInSeconds = 0)
        } else {
            activeTrack = track
            _telemetry.value = BeatTelemetry(
                isPlaying = false,
                currentTrackId = track.id
            )
        }
    }

    fun setVolume(volume: Float) {
        masterVolume = volume.coerceIn(0f, 1f)
    }

    fun stop() {
        playbackJob?.cancel()
        playbackJob = null
        val currentId = activeTrack?.id.orEmpty()
        _telemetry.value = BeatTelemetry(
            isPlaying = false,
            currentTrackId = currentId
        )
    }

    fun isPlaying(): Boolean = playbackJob?.isActive == true

    private fun CoroutineScope.runSynthLoop(track: DanceTrack) {
        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
        } catch (_: Throwable) {
            // Safe fallback in unit tests / restricted environments
        }

        val minBufBytes = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(FRAMES_PER_BUFFER * 4)

        val audioTrack = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(minBufBytes * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (_: Throwable) {
            return
        }

        val shortBuffer = ShortArray(FRAMES_PER_BUFFER * 2)

        try {
            audioTrack.play()

            val samplesPerBeat = (SAMPLE_RATE * 60.0 / track.bpm)
            val samplesPerStep = samplesPerBeat / 4.0 // 16th note

            var totalSamples = 0L
            var currentStepGlobal = -1L

            // Synth voice states
            var kickTimer = 10.0f
            var kickPhase = 0.0

            var snareTimer = 10.0f
            var snareBodyPhase = 0.0

            var hatTimer = 10.0f
            var hatIsOpen = false
            var lastNoise = 0f
            var hpNoise = 0f

            var bassPhase = 0.0
            var bassFreq = 55.0
            var bassTimer = 10.0f
            var bassLp = 0f

            var leadPhase = 0.0
            var leadModPhase = 0.0
            var leadFreq = 440.0
            var leadTimer = 10.0f

            val chordPhases = DoubleArray(3)
            val chordFreqs = DoubleArray(3) { 261.63 }
            var chordTimer = 10.0f
            var chordLpL = 0f
            var chordLpR = 0f

            // Fast xorshift PRNG state
            var rngState = (track.date.toEpochDay().toInt() xor 0x5F3759DF).let {
                if (it == 0) 1234567 else it
            }

            var kickHitFlag = false

            while (isActive) {
                var peakBass = 0f
                var peakMid = 0f
                var peakHigh = 0f

                for (frame in 0 until FRAMES_PER_BUFFER) {
                    val stepGlobal = (totalSamples / samplesPerStep).toLong()
                    val stepInBar = (stepGlobal % 16).toInt()
                    val barNumber = (stepGlobal / 16).toInt()
                    val phraseBar = barNumber % 16

                    val section = when {
                        phraseBar < 2 -> TrackSection.INTRO
                        phraseBar < 6 -> TrackSection.GROOVE
                        phraseBar < 8 -> TrackSection.BUILD
                        else -> TrackSection.DROP
                    }

                    if (stepGlobal != currentStepGlobal) {
                        currentStepGlobal = stepGlobal

                        val chordDegree = track.chordProgression[barNumber % track.chordProgression.size]
                        val chordRootInterval = track.scaleIntervals[chordDegree % track.scaleIntervals.size]
                        val chordBaseMidi = track.rootMidi + chordRootInterval

                        // Trigger Kick
                        val isFourOnFloor = stepInBar % 4 == 0
                        val isSyncoKick = track.genre.syncopatedKick && (stepInBar == 10 || stepInBar == 14)
                        val isBuildRoll = section == TrackSection.BUILD && phraseBar == 7 && stepInBar % 2 == 0
                        val playKick = when (section) {
                            TrackSection.INTRO -> stepInBar == 0 || stepInBar == 8
                            TrackSection.BUILD -> if (phraseBar == 7) isBuildRoll else isFourOnFloor
                            else -> isFourOnFloor || isSyncoKick
                        }
                        if (playKick) {
                            kickTimer = 0f
                            kickPhase = 0.0
                            if (stepInBar % 4 == 0) kickHitFlag = true
                        }

                        // Trigger Snare / Clap
                        val isBackbeat = stepInBar == 4 || stepInBar == 12
                        val isBuildSnare = section == TrackSection.BUILD &&
                            ((phraseBar == 6 && stepInBar % 4 == 0) || (phraseBar == 7 && stepInBar % 2 == 0))
                        if ((section != TrackSection.INTRO && isBackbeat) || isBuildSnare) {
                            snareTimer = 0f
                            snareBodyPhase = 0.0
                        }

                        // Trigger Hi-Hat
                        val isOffbeatHat = stepInBar % 4 == 2
                        val isClosedHat = when (track.genre) {
                            DanceGenre.EURODANCE, DanceGenre.ELECTRO_POP -> stepInBar % 2 == 0 || isOffbeatHat
                            else -> isOffbeatHat || (stepInBar % 4 == 3 && section == TrackSection.DROP)
                        }
                        if (isOffbeatHat || isClosedHat) {
                            hatTimer = 0f
                            hatIsOpen = isOffbeatHat && section != TrackSection.INTRO
                        }

                        // Trigger Bassline
                        val bassStep = track.bassPattern[stepInBar]
                        if (bassStep >= 0 && section != TrackSection.BUILD) {
                            val bassMidi = chordBaseMidi - 12 + bassStep
                            bassFreq = midiToFreq(bassMidi)
                            bassTimer = 0f
                        }

                        // Trigger Chords on beat 0 and syncopated upbeat
                        val triggerChord = stepInBar == 0 || (stepInBar == 6 && section != TrackSection.INTRO)
                        if (triggerChord) {
                            val intervalThird = track.scaleIntervals[(chordDegree + 2) % track.scaleIntervals.size] +
                                if (chordDegree + 2 >= track.scaleIntervals.size) 12 else 0
                            val intervalFifth = track.scaleIntervals[(chordDegree + 4) % track.scaleIntervals.size] +
                                if (chordDegree + 4 >= track.scaleIntervals.size) 12 else 0
                            chordFreqs[0] = midiToFreq(track.rootMidi + chordRootInterval + 12)
                            chordFreqs[1] = midiToFreq(track.rootMidi + intervalThird + 12)
                            chordFreqs[2] = midiToFreq(track.rootMidi + intervalFifth + 12)
                            chordTimer = 0f
                        }

                        // Trigger Arp / Lead Melody
                        val arpDegree = track.arpPattern[stepInBar]
                        val playLead = arpDegree >= 0 && (section == TrackSection.GROOVE || section == TrackSection.DROP ||
                            (section == TrackSection.INTRO && stepInBar % 4 == 0))
                        if (playLead) {
                            val scaleNote = track.scaleIntervals[arpDegree % track.scaleIntervals.size]
                            // Subtle bar-to-bar variation on bar 4 of phrase
                            val barLift = if (barNumber % 4 == 3 && stepInBar >= 8) 2 else 0
                            val leadMidi = track.rootMidi + 24 + scaleNote + barLift
                            leadFreq = midiToFreq(leadMidi)
                            leadTimer = 0f
                        }
                    }

                    // Advance envelopes (dt = 1 / SAMPLE_RATE)
                    val dt = INV_SAMPLE_RATE
                    kickTimer += dt
                    snareTimer += dt
                    hatTimer += dt
                    bassTimer += dt
                    chordTimer += dt
                    leadTimer += dt

                    // 1. Kick Synthesis
                    var kickSample = 0f
                    if (kickTimer < 0.28f) {
                        val freqEnv = 44.0 + 115.0 * exp((-kickTimer * 28.0f).toDouble())
                        kickPhase += TWO_PI * freqEnv * dt
                        val ampEnv = exp((-kickTimer * 10.5f).toDouble()).toFloat()
                        val clickEnv = exp((-kickTimer * 90.0f).toDouble()).toFloat()
                        kickSample = tanh((sin(kickPhase).toFloat() * 1.4f + clickEnv * 0.35f) * ampEnv) * 0.85f
                    }

                    // Noise generator (xorshift32)
                    rngState = rngState xor (rngState shl 13)
                    rngState = rngState xor (rngState ushr 17)
                    rngState = rngState xor (rngState shl 5)
                    val white = (rngState ushr 1).toFloat() / Int.MAX_VALUE.toFloat() * 2f - 1f
                    // High-pass filter for crisp cymbals/claps
                    hpNoise = 0.85f * (hpNoise + white - lastNoise)
                    lastNoise = white

                    // 2. Snare / Clap Synthesis
                    var snareSample = 0f
                    if (snareTimer < 0.20f) {
                        if (track.genre.clapSnare) {
                            // Multi-burst clap envelope
                            val b1 = exp((-snareTimer * 45f)).coerceIn(0f, 1f)
                            val b2 = if (snareTimer > 0.011f) exp((-(snareTimer - 0.011f) * 45f)) else 0f
                            val b3 = if (snareTimer > 0.022f) exp((-(snareTimer - 0.022f) * 18f)) else 0f
                            snareSample = hpNoise * (b1 * 0.4f + b2 * 0.45f + b3 * 0.65f) * 0.55f
                        } else {
                            snareBodyPhase += TWO_PI * 185.0 * dt
                            val body = sin(snareBodyPhase).toFloat() * exp(-snareTimer * 24f) * 0.45f
                            val snap = hpNoise * exp(-snareTimer * 18f) * 0.55f
                            snareSample = body + snap
                        }
                    }

                    // 3. Hi-Hat Synthesis
                    var hatSample = 0f
                    val hatMax = if (hatIsOpen) 0.19f else 0.055f
                    if (hatTimer < hatMax) {
                        val decay = if (hatIsOpen) 16f else 58f
                        hatSample = hpNoise * exp(-hatTimer * decay) * 0.24f
                    }

                    // Sidechain ducking envelope synced to kick
                    val sidechain = if (kickTimer < 0.16f) {
                        0.28f + 0.72f * (kickTimer / 0.16f).pow(1.4f)
                    } else {
                        1.0f
                    }

                    // 4. Bass Synthesis
                    var bassSample = 0f
                    if (bassTimer < 0.30f) {
                        bassPhase = (bassPhase + bassFreq * dt) % 1.0
                        val saw = (bassPhase.toFloat() * 2f - 1f)
                        val sub = sin(TWO_PI * bassPhase).toFloat()
                        val rawBass = saw * 0.55f + sub * 0.55f
                        val cutoff = 0.09f + 0.22f * exp(-bassTimer * 11f)
                        bassLp += cutoff * (rawBass - bassLp)
                        val amp = exp(-bassTimer * 5.2f)
                        bassSample = bassLp * amp * sidechain * 0.62f
                    }

                    // 5. Polyphonic Chords (Stereo spread)
                    var chordL = 0f
                    var chordR = 0f
                    if (chordTimer < 0.65f) {
                        for (i in 0 until 3) {
                            chordPhases[i] = (chordPhases[i] + chordFreqs[i] * dt) % 1.0
                            val saw = chordPhases[i].toFloat() * 2f - 1f
                            val tri = 1f - 4f * kotlin.math.abs(chordPhases[i].toFloat() - 0.5f)
                            val voice = saw * 0.4f + tri * 0.6f
                            when (i) {
                                0 -> { chordL += voice * 0.6f; chordR += voice * 0.6f }
                                1 -> { chordL += voice * 0.8f; chordR += voice * 0.35f }
                                else -> { chordL += voice * 0.35f; chordR += voice * 0.8f }
                            }
                        }
                        val chordEnv = exp(-chordTimer * 2.8f) * sidechain * 0.22f
                        chordLpL += 0.18f * (chordL * chordEnv - chordLpL)
                        chordLpR += 0.18f * (chordR * chordEnv - chordLpR)
                    }

                    // 6. Lead / Arpeggio Pluck
                    var leadL = 0f
                    var leadR = 0f
                    if (leadTimer < 0.28f) {
                        leadPhase = (leadPhase + leadFreq * dt) % 1.0
                        leadModPhase = (leadModPhase + leadFreq * 2.0 * dt) % 1.0
                        val leadWave = if (track.genre == DanceGenre.TROPICAL_HOUSE || track.genre == DanceGenre.LATIN_HOUSE) {
                            // Sunny marimba-like FM pluck
                            val modIndex = exp(-leadTimer * 18f) * 1.8f
                            sin(TWO_PI * leadPhase + modIndex * sin(TWO_PI * leadModPhase)).toFloat()
                        } else {
                            // Bright supersaw/square pluck
                            val saw = leadPhase.toFloat() * 2f - 1f
                            val sq = if (leadPhase < 0.5) 0.7f else -0.7f
                            saw * 0.6f + sq * 0.4f
                        }
                        val amp = exp(-leadTimer * 11f) * 0.26f
                        val pan = if ((currentStepGlobal % 2L) == 0L) 0.65f else 0.35f
                        leadL = leadWave * amp * pan
                        leadR = leadWave * amp * (1f - pan)
                    }

                    // Fade-in ramp for morning alarm
                    val fadeGain = if (fadeInTotalSamples > 0L && totalSamples < fadeInTotalSamples) {
                        (totalSamples.toFloat() / fadeInTotalSamples.toFloat()).coerceIn(0.08f, 1f)
                    } else {
                        1f
                    }

                    val gain = masterVolume * fadeGain
                    val mixL = (kickSample + snareSample * 0.9f + hatSample * 0.85f + bassSample + chordLpL + leadL) * gain
                    val mixR = (kickSample + snareSample * 0.9f + hatSample * 1.15f + bassSample + chordLpR + leadR) * gain

                    // Soft club limiter (no digital clipping)
                    val outL = tanh(mixL * 0.88f)
                    val outR = tanh(mixR * 0.88f)

                    shortBuffer[frame * 2] = (outL * 32767f).toInt().coerceIn(-32767, 32767).toShort()
                    shortBuffer[frame * 2 + 1] = (outR * 32767f).toInt().coerceIn(-32767, 32767).toShort()

                    peakBass = max(peakBass, kotlin.math.abs(kickSample + bassSample * 0.6f))
                    peakMid = max(peakMid, kotlin.math.abs(chordLpL + leadL + snareSample * 0.5f))
                    peakHigh = max(peakHigh, kotlin.math.abs(hatSample + snareSample * 0.6f))

                    totalSamples++
                }

                audioTrack.write(shortBuffer, 0, shortBuffer.size)

                // Emit beat telemetry once per buffer (~21 Hz)
                val beatPos = (totalSamples / samplesPerBeat)
                val beatPhase = (beatPos - beatPos.toLong()).toFloat()
                val stepGlobal = (totalSamples / samplesPerStep).toLong()
                val stepInBar = (stepGlobal % 16).toInt()
                val barNumber = (stepGlobal / 16).toInt()
                val phraseBar = barNumber % 16
                val section = when {
                    phraseBar < 2 -> TrackSection.INTRO
                    phraseBar < 6 -> TrackSection.GROOVE
                    phraseBar < 8 -> TrackSection.BUILD
                    else -> TrackSection.DROP
                }

                val bars = List(16) { i ->
                    val bandEnergy = when {
                        i < 4 -> peakBass * (1f - i * 0.12f)
                        i < 11 -> peakMid * (0.75f + 0.25f * sin((i + stepInBar) * 0.7f))
                        else -> peakHigh * (0.8f + 0.2f * sin((i * 1.3f + beatPhase * 6.28f)))
                    }
                    min(1f, max(0.08f, bandEnergy * 1.15f))
                }

                _telemetry.value = BeatTelemetry(
                    isPlaying = true,
                    currentTrackId = track.id,
                    beatPhase = beatPhase,
                    isKickHit = kickHitFlag,
                    barNumber = barNumber,
                    stepIndex = stepInBar,
                    section = section,
                    spectrumBars = bars,
                    elapsedSeconds = (totalSamples / SAMPLE_RATE).toInt()
                )
                kickHitFlag = false
            }
        } finally {
            try {
                audioTrack.stop()
            } catch (_: Throwable) {
            }
            audioTrack.release()
        }
    }

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val INV_SAMPLE_RATE = 1f / SAMPLE_RATE
        private const val FRAMES_PER_BUFFER = 2048
        private const val TWO_PI = 2.0 * PI

        private fun midiToFreq(midi: Int): Double {
            return 440.0 * 2.0.pow((midi - 69) / 12.0)
        }
    }
}
