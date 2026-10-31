package com.example.data.remote

import android.os.SystemClock
import android.util.Log
import com.example.data.model.LrcParser
import com.example.data.model.LyricsData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder

/**
 * Fallback provider lirik berbasis Musixmatch API (mobile client endpoint).
 * Mendukung pengambilan lirik berinterval waktu (.lrc) dan lirik teks polos.
 */
object MusixmatchApi {
    private const val TAG = "MusixmatchApi"
    private const val BASE_URL = "https://apic-appmobile.musixmatch.com/ws/1.1"
    private const val APP_ID = "mac-ios-v2.0"
    private const val USER_AGENT = "Musixmatch/2023120101 (iPhone; iOS 17.1.1; Scale/3.00)"

    private val HEADERS = mapOf("User-Agent" to USER_AGENT)

    private val mutex = Mutex()
    @Volatile private var userToken: String? = null
    @Volatile private var tokenExpiryUptime: Long = 0L

    private val MXM_DISCLAIMER_REGEX = Regex(
        "(?s)\\s*\\*+\\s*This Lyrics is NOT for Commercial use\\s*\\*+.*$",
        RegexOption.IGNORE_CASE
    )

    /**
     * Mengambil token autentikasi Musixmatch. Token di-cache selama 6 jam.
     */
    private suspend fun getOrRefreshToken(forceNew: Boolean = false): String? = mutex.withLock {
        val now = SystemClock.elapsedRealtime()
        if (!forceNew && userToken != null && now < tokenExpiryUptime) {
            return userToken
        }

        try {
            val url = "$BASE_URL/token.get?app_id=$APP_ID"
            val resp = Http.getString(url, HEADERS) ?: return null
            val obj = JSONObject(resp)
            val header = obj.optJSONObject("message")?.optJSONObject("header")
            val statusCode = header?.optInt("status_code", -1) ?: -1
            if (statusCode == 200) {
                val token = obj.optJSONObject("message")
                    ?.optJSONObject("body")
                    ?.optString("user_token")
                if (!token.isNullOrBlank() && token != "00000000000000000000000000000000000000000000000000000000") {
                    userToken = token
                    tokenExpiryUptime = now + (6 * 3600 * 1000L) // 6 jam
                    return token
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gagal mendapatkan token Musixmatch: ${e.message}")
        }
        return null
    }

    /**
     * Mencari lirik lagu lewat endpoint macro Musixmatch.
     * Mengembalikan [LyricsData] jika ditemukan, atau null jika tidak ada.
     */
    suspend fun fetchLyrics(title: String, artist: String?, durationMs: Long): LyricsData? =
        withContext(Dispatchers.IO) {
            val cleanTitle = TrackQuery.cleanTitle(title)
            if (cleanTitle.isBlank()) return@withContext null
            val cleanArtist = artist?.takeUnless { TrackQuery.isUnknownArtist(it) }?.trim()

            var token = getOrRefreshToken(forceNew = false) ?: return@withContext null

            var result = doMacroSubtitlesGet(cleanTitle, cleanArtist, token)
            // Jika token kadaluarsa atau ditolak, coba refresh token 1 kali
            if (result == null && userToken != null) {
                val freshToken = getOrRefreshToken(forceNew = true)
                if (freshToken != null) {
                    result = doMacroSubtitlesGet(cleanTitle, cleanArtist, freshToken)
                }
            }

            // Jika masih null dan terdapat artis tapi pencarian spesifik gagal, coba pencarian judul saja
            if (result == null && cleanArtist != null) {
                result = doMacroSubtitlesGet(cleanTitle, null, userToken ?: token)
            }

            result
        }

    private suspend fun doMacroSubtitlesGet(
        title: String,
        artist: String?,
        token: String
    ): LyricsData? {
        val qTrack = enc(title)
        val qArtist = if (artist != null) enc(artist) else ""
        val url = "$BASE_URL/macro.subtitles.get?format=json&q_track=$qTrack" +
            (if (artist != null) "&q_artist=$qArtist" else "") +
            "&user_language=en&namespace=lyrics_richsync&subtitle_format=lrc&app_id=$APP_ID&usertoken=$token"

        val bodyStr = try {
            Http.getString(url, HEADERS)
        } catch (e: IOException) {
            Log.w(TAG, "Koneksi ke Musixmatch bermasalah: ${e.message}")
            return null
        } catch (e: Exception) {
            Log.w(TAG, "Error tak terduga Musixmatch: ${e.message}")
            return null
        } ?: return null

        return parseMacroResponse(bodyStr)
    }

    private fun parseMacroResponse(jsonStr: String): LyricsData? {
        try {
            val root = JSONObject(jsonStr)
            val msg = root.optJSONObject("message") ?: return null
            val header = msg.optJSONObject("header")
            val statusCode = header?.optInt("status_code", -1) ?: -1
            if (statusCode != 200) return null

            val body = msg.optJSONObject("body") ?: return null
            val macro = body.optJSONObject("macro_calls") ?: return null

            // 1. Validasi matcher track
            val trackCall = macro.optJSONObject("matcher.track.get")
            val trackObj = trackCall?.optJSONObject("message")
                ?.optJSONObject("body")
                ?.optJSONObject("track")

            val trackName = trackObj?.optString("track_name", "") ?: ""
            // Jika dikembalikan decoy "NOKIA", token tidak sah
            if (trackName.equals("NOKIA", ignoreCase = true)) {
                return null
            }
            val isInstrumental = trackObj?.optInt("instrumental", 0) == 1

            // 2. Ambil lirik berinterval waktu (synced subtitle / LRC)
            val subCall = macro.optJSONObject("track.subtitles.get")
            val subBody = subCall?.optJSONObject("message")?.opt("body")
            var subtitleLrc: String? = null

            if (subBody is JSONObject) {
                val list = subBody.optJSONArray("subtitle_list")
                if (list != null && list.length() > 0) {
                    subtitleLrc = list.optJSONObject(0)?.optJSONObject("subtitle")?.optString("subtitle_body")
                } else {
                    subtitleLrc = subBody.optJSONObject("subtitle")?.optString("subtitle_body")
                }
            }

            // Validasi subtitle bukan teks decoy ("Wob gopini den...")
            if (!subtitleLrc.isNullOrBlank() && !subtitleLrc.contains("Wob gopini den", ignoreCase = true)) {
                val lines = LrcParser.parse(subtitleLrc)
                if (lines.isNotEmpty()) {
                    return LyricsData(
                        syncedLines = lines,
                        plainText = null,
                        instrumental = false,
                        source = "Musixmatch"
                    )
                }
            }

            // 3. Jika tidak ada lirik sinkron, coba lirik teks polos
            val lyrCall = macro.optJSONObject("track.lyrics.get")
            val lyrBody = lyrCall?.optJSONObject("message")?.optJSONObject("body")
            val lyrObj = lyrBody?.optJSONObject("lyrics")
            val rawPlain = lyrObj?.optString("lyrics_body")

            if (!rawPlain.isNullOrBlank() && !rawPlain.contains("Wob gopini den", ignoreCase = true)) {
                val cleanedPlain = rawPlain.replace(MXM_DISCLAIMER_REGEX, "").trim()
                if (cleanedPlain.isNotEmpty()) {
                    return LyricsData(
                        syncedLines = emptyList(),
                        plainText = cleanedPlain,
                        instrumental = false,
                        source = "Musixmatch"
                    )
                }
            }

            // 4. Jika instrumental
            if (isInstrumental) {
                return LyricsData(
                    syncedLines = emptyList(),
                    plainText = null,
                    instrumental = true,
                    source = "Musixmatch"
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gagal mengurai respons Musixmatch: ${e.message}")
        }
        return null
    }

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")
}
