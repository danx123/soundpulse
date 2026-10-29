package com.example.data.remote

import android.os.SystemClock
import com.example.data.model.AudioSourceType
import com.example.data.model.AudioTrack
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.net.URLEncoder
import kotlin.math.abs

data class ItunesTrack(
    val trackName: String,
    val artistName: String,
    val collectionName: String?,
    val durationMs: Long,
    val artworkUrl: String?
)

/** Judul/artis yang sudah dibersihkan dari nama file mentah. */
data class TrackQuery(val title: String, val artist: String?) {

    companion object {
        private val UNKNOWN_ARTISTS = setOf(
            "", "unknown", "unknown artist", "<unknown>", "local artist",
            "cloud stream", "various artists"
        )
        private val LEADING_NUMBER = Regex("^\\s*\\d{1,3}\\s*[-._)]\\s*")
        private val BRACKETS = Regex("\\(.*?\\)|\\[.*?]")

        fun isUnknownArtist(artist: String?): Boolean =
            artist == null || artist.trim().lowercase() in UNKNOWN_ARTISTS

        fun from(track: AudioTrack): TrackQuery {
            var title = track.title.replace('_', ' ').replace(LEADING_NUMBER, "")
            var artist: String? = track.artist.takeUnless { isUnknownArtist(it) }

            // Pola umum nama file: "Artis - Judul"
            if (artist == null && title.contains(" - ")) {
                val left = title.substringBefore(" - ").trim()
                val right = title.substringAfter(" - ").trim()
                if (left.isNotEmpty() && right.isNotEmpty()) {
                    artist = left
                    title = right
                }
            }
            title = title.replace(BRACKETS, " ").replace(Regex("\\s+"), " ").trim()
            return TrackQuery(title, artist?.trim())
        }

        fun normalize(s: String): String = s.lowercase()
            .replace(BRACKETS, " ")
            .replace(Regex("\\b(feat|ft|featuring)\\b.*"), " ")
            .replace(Regex("[^\\p{L}\\p{Nd}]+"), " ")
            .trim()
    }
}

/** Sumber lagu yang masuk akal dicari di iTunes (demo & radio dilewati). */
fun AudioTrack.isOnlineLookupEligible(): Boolean =
    sourceType == AudioSourceType.LOCAL_FILE ||
        sourceType == AudioSourceType.LOCAL_FOLDER ||
        sourceType == AudioSourceType.CLOUD_STORAGE

/**
 * iTunes Search API (tanpa API key). Dipakai untuk artwork dan untuk menormalkan
 * metadata lagu. CATATAN: API ini TIDAK menyediakan lirik.
 */
object ItunesApi {
    private const val MIN_INTERVAL_MS = 2000L // iTunes membatasi ~20 request/menit
    private val gate = Mutex()
    @Volatile private var lastCall = 0L

    /**
     * @param priority true = lewati antrean/throttle (untuk aksi yang ditunggu user,
     *                 mis. lagu yang sedang diputar atau lirik layar penuh).
     * @throws java.io.IOException bila jaringan gagal.
     */
    suspend fun searchSong(
        title: String,
        artist: String?,
        durationMs: Long,
        priority: Boolean = false
    ): ItunesTrack? {
        if (title.isBlank()) return null
        if (priority) return doSearch(title, artist, durationMs)

        return gate.withLock {
            val wait = MIN_INTERVAL_MS - (SystemClock.elapsedRealtime() - lastCall)
            if (wait > 0) delay(wait)
            try {
                doSearch(title, artist, durationMs)
            } finally {
                lastCall = SystemClock.elapsedRealtime()
            }
        }
    }

    private suspend fun doSearch(title: String, artist: String?, durationMs: Long): ItunesTrack? {
        val term = listOfNotNull(artist, title).joinToString(" ")
        val url = "https://itunes.apple.com/search?term=" +
            URLEncoder.encode(term, "UTF-8") + "&media=music&entity=song&limit=10"
        val body = Http.getString(url) ?: return null

        val results = JSONObject(body).optJSONArray("results") ?: return null
        val qTitle = TrackQuery.normalize(title)
        val qArtist = artist?.let { TrackQuery.normalize(it) }?.takeIf { it.isNotEmpty() }

        var best: ItunesTrack? = null
        var bestScore = Int.MIN_VALUE
        for (i in 0 until results.length()) {
            val r = results.optJSONObject(i) ?: continue
            val candidate = ItunesTrack(
                trackName = r.optString("trackName"),
                artistName = r.optString("artistName"),
                collectionName = r.optString("collectionName").takeIf { it.isNotBlank() },
                durationMs = r.optLong("trackTimeMillis", 0L),
                artworkUrl = r.optString("artworkUrl100").takeIf { it.isNotBlank() }
                    ?.replace("100x100bb", "600x600bb")
            )
            val score = score(candidate, qTitle, qArtist, durationMs)
            if (score > bestScore) {
                bestScore = score
                best = candidate
            }
        }
        val threshold = if (qArtist != null) 3 else 5
        return if (best != null && bestScore >= threshold) best else null
    }

    private fun score(r: ItunesTrack, qTitle: String, qArtist: String?, durationMs: Long): Int {
        val t = TrackQuery.normalize(r.trackName)
        val a = TrackQuery.normalize(r.artistName)
        if (t.isEmpty() || qTitle.isEmpty()) return -1

        val titleScore = when {
            t == qTitle -> 3
            t.startsWith(qTitle) || qTitle.startsWith(t) -> 2
            t.contains(qTitle) || qTitle.contains(t) -> 1
            else -> 0
        }
        if (titleScore == 0) return -1

        var s = titleScore
        if (qArtist != null) {
            val artistScore = when {
                a == qArtist -> 3
                a.contains(qArtist) || qArtist.contains(a) -> 2
                sharesToken(a, qArtist) -> 1
                else -> 0
            }
            if (artistScore == 0) return -1
            s += artistScore
        } else if (titleScore < 3) {
            return -1 // artis tidak diketahui: wajib judul persis
        }

        if (durationMs > 0 && r.durationMs > 0) {
            val diff = abs(durationMs - r.durationMs)
            s += when {
                diff <= 3000 -> 2
                diff <= 8000 -> 1
                diff > 20000 -> -2
                else -> 0
            }
        }
        return s
    }

    private fun sharesToken(a: String, b: String): Boolean {
        val tokens = b.split(' ').filter { it.length >= 3 }.toSet()
        return a.split(' ').any { it in tokens }
    }
}
