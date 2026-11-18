package com.example.model

import kotlin.math.abs
import kotlin.random.Random

/**
 * Deterministically generates a unique, energetic dance music track for every single calendar day.
 * Every day rotates genres and synthesizes a distinct key, BPM, chord progression, bassline,
 * arpeggio hook, title, and choreography cue.
 */
object DailyTrackCatalog {

    private data class KeySpec(val label: String, val rootMidi: Int, val isMinor: Boolean)

    private val keys = listOf(
        KeySpec("C Major", 48, false),
        KeySpec("D Minor", 50, true),
        KeySpec("Eb Major", 51, false),
        KeySpec("E Minor", 52, true),
        KeySpec("F Major", 53, false),
        KeySpec("F# Minor", 54, true),
        KeySpec("G Major", 55, false),
        KeySpec("A Minor", 57, true),
        KeySpec("Bb Major", 58, false)
    )

    private val majorDanceScale = listOf(0, 2, 4, 7, 9, 12, 14, 16)
    private val minorDanceScale = listOf(0, 2, 3, 5, 7, 10, 12, 15)

    private val chordProgressionsMajor = listOf(
        listOf(0, 4, 2, 3), // I - V - iii - IV-ish bright bounce
        listOf(0, 3, 4, 2),
        listOf(3, 4, 0, 2),
        listOf(0, 2, 3, 4)
    )

    private val chordProgressionsMinor = listOf(
        listOf(0, 5, 3, 4), // i - VI - iv - v
        listOf(0, 3, 5, 4),
        listOf(0, 4, 5, 3),
        listOf(5, 3, 0, 4)
    )

    private val bassGrooves = listOf(
        // Classic Octave Disco
        listOf(0, -1, 12, -1, 0, -1, 12, -1, 0, -1, 12, -1, 0, 7, 12, -1),
        // Pumping Offbeat House
        listOf(-1, -1, 0, -1, -1, -1, 0, -1, -1, -1, 0, -1, -1, 0, 7, -1),
        // Funky Syncopated
        listOf(0, -1, -1, 0, -1, 12, 0, -1, 0, -1, 7, -1, 12, -1, 7, 0),
        // Driving 8th Synthwave
        listOf(0, 0, 0, 0, 0, 0, 7, 0, 0, 0, 0, 0, 12, 0, 7, 0),
        // Bouncy Latin/Tropical
        listOf(0, -1, -1, 7, -1, -1, 0, -1, 0, -1, -1, 7, -1, 12, -1, 7)
    )

    private val titlePrefixes = listOf(
        "선라이즈", "네온", "스파클", "에너지", "골든", "모닝", "일렉트릭",
        "비타민", "트윙클", "하이퍼", "브라이트", "선샤인", "코랄", "스카이",
        "비비드", "리듬", "글로우", "스타라이트", "프레시", "부스트"
    )

    private val titleSuffixes = listOf(
        "피버", "바운스", "웨이브", "그루브", "스텝", "쇼크", "플로우",
        "비트", "스파크", "드라이브", "대시", "무브", "러시", "믹스",
        "나이트", "퍼레이드", "블라스트", "점프", "댄스", "펄스"
    )

    private val genreSubtitles = mapOf(
        DanceGenre.NU_DISCO to listOf(
            "햇살처럼 반짝이는 펑키 디스코 리듬!",
            "기분 좋게 몸이 들썩이는 80s 모던 디스코!"
        ),
        DanceGenre.TROPICAL_HOUSE to listOf(
            "상쾌한 아침 바람을 닮은 트로피컬 비트!",
            "휴양지처럼 기분이 밝아지는 마림바 그루브!"
        ),
        DanceGenre.ELECTRO_POP to listOf(
            "톡톡 터지는 신디사이저로 잠이 확 깨는 팝 댄스!",
            "경쾌한 멜로디와 함께 시작하는 에너지 부스트!"
        ),
        DanceGenre.SYNTHWAVE to listOf(
            "레트로 네온 감성이 빛나는 드라이빙 댄스!",
            "강렬한 아날로그 신스 베이스로 힘찬 아침!"
        ),
        DanceGenre.FUTURE_FUNK to listOf(
            "어깨가 절로 들썩이는 초강력 펑키 바운스!",
            "신나는 브라스 신스와 찰진 베이스 리듬!"
        ),
        DanceGenre.EURODANCE to listOf(
            "심장을 뛰게 하는 초고속 모닝 파워 비트!",
            "가장 신나는 130+ BPM 슈퍼소 댄스 트랙!"
        ),
        DanceGenre.LATIN_HOUSE to listOf(
            "정열적인 퍼커션이 이끄는 흥겨운 모닝 리듬!",
            "발걸음이 가벼워지는 라틴 하우스 그루브!"
        )
    )

    private val genreDanceCues = mapOf(
        DanceGenre.NU_DISCO to listOf(
            "좌우 투스텝과 함께 양팔을 번갈아 뻗어보세요!",
            "무릎 바운스를 타며 리듬감 있게 어깨를 흔들어보세요!"
        ),
        DanceGenre.TROPICAL_HOUSE to listOf(
            "가볍게 제자리 조깅 스텝에 팔 돌리기를 더해보세요!",
            "웨이브 타듯 상체를 부드럽게 좌우로 움직여 보세요!"
        ),
        DanceGenre.ELECTRO_POP to listOf(
            "박자에 맞춰 손뼉 치기와 하이니 스텝을 이어가세요!",
            "점프 없이 가볍게 발뒤꿈치를 들며 리듬을 타보세요!"
        ),
        DanceGenre.SYNTHWAVE to listOf(
            "큰 보폭으로 사이드 런지 리듬을 타며 팔을 흔드세요!",
            "비트마다 주먹을 앞으로 뻗는 펀치 댄스로 기상!"
        ),
        DanceGenre.FUTURE_FUNK to listOf(
            "골반 바운스와 함께 신나게 리듬을 쪼개보세요!",
            "손가락을 튕기듯 펑키한 바운스 스텝을 밟아보세요!"
        ),
        DanceGenre.EURODANCE to listOf(
            "빠른 비트에 맞춰 파워풀한 셔플 스텝 고고!",
            "양팔을 높이 들고 하늘을 향해 점핑 잭 리듬!"
        ),
        DanceGenre.LATIN_HOUSE to listOf(
            "허리와 힙을 리드미컬하게 흔드는 라틴 트위스트!",
            "발끝으로 톡톡 스텝을 밟으며 경쾌하게 회전해보세요!"
        )
    )

    fun trackForDate(date: DanceDate): DanceTrack {
        val epoch = date.toEpochDay()
        val genres = DanceGenre.entries
        // Rotate genre every single day deterministically without consecutive repeats
        val genreIndex = abs((epoch % genres.size).toInt())
        val genre = genres[genreIndex]

        val seed = epoch * 7919L + 104729L
        val rng = Random(seed)

        val key = keys[rng.nextInt(keys.size)]
        val scale = if (key.isMinor) minorDanceScale else majorDanceScale
        val progression = if (key.isMinor) {
            chordProgressionsMinor[rng.nextInt(chordProgressionsMinor.size)]
        } else {
            chordProgressionsMajor[rng.nextInt(chordProgressionsMajor.size)]
        }

        val bpm = genre.bpmMin + rng.nextInt(genre.bpmMax - genre.bpmMin + 1)
        val bassPattern = bassGrooves[rng.nextInt(bassGrooves.size)]

        // Generate a catchy, musical 16-step arpeggio motif for this exact date
        val arpPattern = MutableList(16) { step ->
            val onBeat = step % 2 == 0
            val playChance = if (onBeat) 0.85 else 0.45
            if (rng.nextDouble() < playChance) {
                rng.nextInt(scale.size)
            } else {
                -1
            }
        }
        // Guarantee strong downbeat hook on step 0 and step 8
        arpPattern[0] = 0
        if (arpPattern[8] == -1) arpPattern[8] = rng.nextInt(3, 6)

        val prefix = titlePrefixes[abs(((epoch * 3 + rng.nextInt(5)) % titlePrefixes.size).toInt())]
        val suffix = titleSuffixes[abs(((epoch * 7 + rng.nextInt(7)) % titleSuffixes.size).toInt())]
        val title = "$prefix $suffix"

        val subs = genreSubtitles[genre].orEmpty()
        val subtitle = subs[rng.nextInt(subs.size)]

        val cues = genreDanceCues[genre].orEmpty()
        val danceCue = cues[rng.nextInt(cues.size)]

        val energyStars = if (bpm >= 126) 5 else if (bpm >= 120) 4 else 3

        return DanceTrack(
            id = "track_${date.year}_${date.month}_${date.dayOfMonth}",
            date = date,
            title = title,
            subtitle = subtitle,
            genre = genre,
            bpm = bpm,
            keyLabel = key.label,
            rootMidi = key.rootMidi,
            scaleIntervals = scale,
            chordProgression = progression,
            bassPattern = bassPattern,
            arpPattern = arpPattern,
            danceCue = danceCue,
            energyStars = energyStars,
            coverResId = genre.defaultCoverRes
        )
    }

    fun getTodayTrack(): DanceTrack = trackForDate(DanceDate.today())

    fun getLineupAround(
        centerDate: DanceDate = DanceDate.today(),
        daysBefore: Int = 1,
        daysAfter: Int = 13
    ): List<DanceTrack> {
        return (-daysBefore..daysAfter).map { offset ->
            trackForDate(centerDate.plusDays(offset.toLong()))
        }
    }
}
