package com.example.model

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.R

enum class DanceGenre(
    val displayNameKo: String,
    val displayNameEn: String,
    val bpmMin: Int,
    val bpmMax: Int,
    val accentHex: Color,
    val secondaryHex: Color,
    @DrawableRes val defaultCoverRes: Int,
    val clapSnare: Boolean,
    val syncopatedKick: Boolean
) {
    NU_DISCO(
        displayNameKo = "뉴 디스코",
        displayNameEn = "NU-DISCO",
        bpmMin = 118,
        bpmMax = 124,
        accentHex = Color(0xFFFF4D79),
        secondaryHex = Color(0xFFFFB020),
        defaultCoverRes = R.drawable.img_cover_disco_sun,
        clapSnare = true,
        syncopatedKick = false
    ),
    TROPICAL_HOUSE(
        displayNameKo = "트로피컬 하우스",
        displayNameEn = "TROPICAL HOUSE",
        bpmMin = 114,
        bpmMax = 120,
        accentHex = Color(0xFF29E3D7),
        secondaryHex = Color(0xFFA3FF4E),
        defaultCoverRes = R.drawable.img_cover_tropical_beat,
        clapSnare = true,
        syncopatedKick = false
    ),
    ELECTRO_POP(
        displayNameKo = "일렉트로 팝",
        displayNameEn = "ELECTRO POP",
        bpmMin = 122,
        bpmMax = 128,
        accentHex = Color(0xFFFF7597),
        secondaryHex = Color(0xFF29E3D7),
        defaultCoverRes = R.drawable.img_cover_cyber_pulse,
        clapSnare = false,
        syncopatedKick = false
    ),
    SYNTHWAVE(
        displayNameKo = "신스웨이브",
        displayNameEn = "SYNTHWAVE",
        bpmMin = 118,
        bpmMax = 126,
        accentHex = Color(0xFFA56BFF),
        secondaryHex = Color(0xFFFF4D79),
        defaultCoverRes = R.drawable.img_cover_cyber_pulse,
        clapSnare = false,
        syncopatedKick = false
    ),
    FUTURE_FUNK(
        displayNameKo = "퓨처 펑크",
        displayNameEn = "FUTURE FUNK",
        bpmMin = 122,
        bpmMax = 128,
        accentHex = Color(0xFFFFB020),
        secondaryHex = Color(0xFFFF4D79),
        defaultCoverRes = R.drawable.img_cover_disco_sun,
        clapSnare = true,
        syncopatedKick = true
    ),
    EURODANCE(
        displayNameKo = "유로 댄스",
        displayNameEn = "EURODANCE",
        bpmMin = 128,
        bpmMax = 135,
        accentHex = Color(0xFF29E3D7),
        secondaryHex = Color(0xFFA56BFF),
        defaultCoverRes = R.drawable.img_cover_cyber_pulse,
        clapSnare = true,
        syncopatedKick = false
    ),
    LATIN_HOUSE(
        displayNameKo = "라틴 하우스",
        displayNameEn = "LATIN HOUSE",
        bpmMin = 122,
        bpmMax = 127,
        accentHex = Color(0xFFFFB020),
        secondaryHex = Color(0xFF29E3D7),
        defaultCoverRes = R.drawable.img_cover_tropical_beat,
        clapSnare = true,
        syncopatedKick = true
    )
}

enum class TrackSection(val labelKo: String, val labelEn: String) {
    INTRO("워밍업", "INTRO"),
    GROOVE("그루브", "GROOVE"),
    BUILD("빌드업", "BUILD"),
    DROP("하이라이트", "DROP!")
}

data class DanceTrack(
    val id: String,
    val date: DanceDate,
    val title: String,
    val subtitle: String,
    val genre: DanceGenre,
    val bpm: Int,
    val keyLabel: String,
    val rootMidi: Int,
    val scaleIntervals: List<Int>,
    val chordProgression: List<Int>, // scale degree offsets for 4 bars
    val bassPattern: List<Int>, // 16-step (-1 = rest, 0 = root, 12 = octave, 7 = fifth)
    val arpPattern: List<Int>, // 16-step (-1 = rest, 0..7 = scale degree index)
    val danceCue: String,
    val energyStars: Int,
    @DrawableRes val coverResId: Int
)

data class BeatTelemetry(
    val isPlaying: Boolean = false,
    val currentTrackId: String = "",
    val beatPhase: Float = 0f,
    val isKickHit: Boolean = false,
    val barNumber: Int = 0,
    val stepIndex: Int = 0,
    val section: TrackSection = TrackSection.INTRO,
    val spectrumBars: List<Float> = List(16) { 0.08f },
    val elapsedSeconds: Int = 0
)
