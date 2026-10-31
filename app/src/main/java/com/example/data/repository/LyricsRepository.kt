package com.example.data.repository

import android.content.Context
import android.util.Log
import android.util.LruCache
import com.example.data.model.AudioSourceType
import com.example.data.model.AudioTrack
import com.example.data.model.LrcParser
import com.example.data.model.LyricsData
import com.example.data.model.LyricsUiState
import com.example.data.remote.Http
import com.example.data.remote.ItunesApi
import com.example.data.remote.MusixmatchApi
import com.example.data.remote.TrackQuery
import com.example.data.remote.sha1Hex
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.URLEncoder
import java.util.Collections
import kotlin.math.abs

/**
 * Mengambil lirik (berinterval waktu / synced bila tersedia).
 *
 * Alur pengambilan:
 *  1. Cek memori cache & disk persisten (internal filesDir/saved_lyrics).
 *  2. Jika user klik refresh, hapus cache lama dan lakukan re-fetch.
 *  3. Cari di LRCLIB dengan query yang telah dioptimalkan (pembersihan judul, multi-endpoint).
 *  4. Jika LRCLIB gagal terhubung / tidak menemukan lirik, lakukan fallback otomatis ke Musixmatch.
 *  5. Jika masih belum ditemukan, coba normalkan judul/artis via iTunes lalu coba lagi ke LRCLIB & Musixmatch.
 *  6. Simpan lirik yang berhasil dimuat ke penyimpanan persisten agar tersedia offline.
 */
class LyricsRepository(context: Context) {

    private val dir = File(context.applicationContext.filesDir, "saved_lyrics").apply { mkdirs() }
    private val memory = LruCache<String, LyricsData>(48)
    private val notFound: MutableSet<String> = Collections.synchronizedSet(HashSet())

    suspend fun getLyrics(track: AudioTrack, forceRefresh: Boolean = false): LyricsUiState {
        if (track.sourceType == AudioSourceType.STREAMING_RADIO) {
            return LyricsUiState.Unavailable("Lirik tidak tersedia untuk siaran radio langsung.")
        }
        if (track.sourceType == AudioSourceType.BUILTIN_DEMO) {
            return LyricsUiState.Unavailable("Lirik tidak tersedia untuk lagu demo.")
        }

        val query = TrackQuery.from(track)
        if (query.title.isBlank()) return LyricsUiState.NotFound

        val key = sha1Hex(
            TrackQuery.normalize(query.artist ?: "") + "|" +
                TrackQuery.normalize(query.title) + "|" + (track.durationMs / 5000)
        )

        if (forceRefresh) {
            notFound.remove(key)
            memory.remove(key)
            deleteDisk(key)
        } else {
            memory.get(key)?.let { return LyricsUiState.Success(it) }
            readDisk(key)?.let {
                memory.put(key, it)
                return LyricsUiState.Success(it)
            }
            if (key in notFound) return LyricsUiState.NotFound
        }

        return try {
            val data = fetchRemote(track, query)
            if (data == null) {
                notFound.add(key)
                LyricsUiState.NotFound
            } else {
                memory.put(key, data)
                writeDisk(key, data)
                LyricsUiState.Success(data)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            LyricsUiState.Error("Tidak dapat terhubung ke server lirik (LRCLIB & Musixmatch). Periksa koneksi internet lalu coba lagi.")
        } catch (e: Exception) {
            LyricsUiState.Error("Gagal memuat lirik: ${e.localizedMessage ?: "kesalahan tidak dikenal"}")
        }
    }

    private suspend fun fetchRemote(track: AudioTrack, q: TrackQuery): LyricsData? {
        val durationSec = (track.durationMs / 1000).toInt()
        val album = albumOf(track)

        // 1) Coba LRCLIB dengan judul & artis yang sudah dibersihkan
        val lrclibLyrics = tryLookupLrclib(q.title, q.artist, album, durationSec)
        if (lrclibLyrics != null) {
            return lrclibLyrics
        }

        // 2) Fallback otomatis ke Musixmatch jika LRCLIB gagal / tidak menemukan
        val mxmLyrics = tryLookupMusixmatch(q.title, q.artist, track.durationMs)
        if (mxmLyrics != null) {
            return mxmLyrics
        }

        // 3) Normalisasi metadata lewat iTunes Search API, lalu coba kedua provider lagi
        try {
            val itunes = ItunesApi.searchSong(q.title, q.artist, track.durationMs, priority = true)
            if (itunes != null) {
                val sec = if (itunes.durationMs > 0) (itunes.durationMs / 1000).toInt() else durationSec

                // Coba LRCLIB dengan nama dari iTunes
                val itunesLrclib = tryLookupLrclib(itunes.trackName, itunes.artistName, itunes.collectionName, sec)
                if (itunesLrclib != null) {
                    return itunesLrclib
                }

                // Coba Musixmatch dengan nama dari iTunes
                val itunesMxm = tryLookupMusixmatch(itunes.trackName, itunes.artistName, itunes.durationMs)
                if (itunesMxm != null) {
                    return itunesMxm
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "iTunes normalization lookup skipped or failed: ${e.message}")
        }

        return null
    }

    private suspend fun tryLookupLrclib(
        title: String,
        artist: String?,
        album: String?,
        durationSec: Int
    ): LyricsData? {
        return try {
            lookupLrclib(title, artist, album, durationSec)
        } catch (e: Exception) {
            Log.w(TAG, "LRCLIB query failed or timed out (${e.message}), will fallback", e)
            null
        }
    }

    private suspend fun tryLookupMusixmatch(
        title: String,
        artist: String?,
        durationMs: Long
    ): LyricsData? {
        return try {
            MusixmatchApi.fetchLyrics(title, artist, durationMs)
        } catch (e: Exception) {
            Log.w(TAG, "Musixmatch query failed: ${e.message}", e)
            null
        }
    }

    private suspend fun lookupLrclib(
        title: String,
        artist: String?,
        album: String?,
        durationSec: Int
    ): LyricsData? {
        val cleanTitle = TrackQuery.cleanTitle(title)
        val artistName = artist?.takeUnless { TrackQuery.isUnknownArtist(it) }?.trim()

        // 1. Pencarian tepat (jika artis dan durasi tersedia)
        if (artistName != null && durationSec > 0) {
            val url = if (!album.isNullOrBlank()) {
                "$BASE_URL/get?track_name=${enc(cleanTitle)}&artist_name=${enc(artistName)}" +
                    "&album_name=${enc(album)}&duration=$durationSec"
            } else {
                "$BASE_URL/get?track_name=${enc(cleanTitle)}&artist_name=${enc(artistName)}"
            }
            val body = Http.getString(url)
            if (body != null) {
                parseLrclibRecord(JSONObject(body))?.let { return it }
            }
        }

        // 2. Pencarian terarah di LRCLIB (/api/search)
        val searchParams = if (artistName != null) {
            "track_name=${enc(cleanTitle)}&artist_name=${enc(artistName)}"
        } else {
            "track_name=${enc(cleanTitle)}"
        }
        val searchBody = Http.getString("$BASE_URL/search?$searchParams")
        if (searchBody != null) {
            pickBestLrclib(JSONArray(searchBody), cleanTitle, artistName, durationSec)?.let { return it }
        }

        // 3. Pencarian bebas luas (q=...) jika pencarian terarah belum membuahkan hasil
        val broadQuery = if (artistName != null) "$cleanTitle $artistName" else cleanTitle
        val broadBody = Http.getString("$BASE_URL/search?q=${enc(broadQuery)}")
        if (broadBody != null) {
            pickBestLrclib(JSONArray(broadBody), cleanTitle, artistName, durationSec)?.let { return it }
        }

        return null
    }

    private fun pickBestLrclib(arr: JSONArray, title: String, artist: String?, durationSec: Int): LyricsData? {
        val qTitle = TrackQuery.normalize(title)
        val qArtist = artist?.let { TrackQuery.normalize(it) }?.takeIf { it.isNotEmpty() }

        var best: JSONObject? = null
        var bestScore = Int.MIN_VALUE
        for (i in 0 until arr.length()) {
            val rec = arr.optJSONObject(i) ?: continue
            val hasContent = rec.optBoolean("instrumental", false) ||
                !str(rec, "syncedLyrics").isNullOrBlank() ||
                !str(rec, "plainLyrics").isNullOrBlank()
            if (!hasContent) continue

            val t = TrackQuery.normalize(str(rec, "trackName") ?: "")
            val a = TrackQuery.normalize(str(rec, "artistName") ?: "")
            var score = when {
                t == qTitle -> 4
                t.isNotEmpty() && (t.contains(qTitle) || qTitle.contains(t)) -> 2
                else -> continue
            }
            if (qArtist != null) {
                score += when {
                    a == qArtist -> 4
                    a.contains(qArtist) || qArtist.contains(a) -> 2
                    else -> continue
                }
            }

            val recDuration = rec.optDouble("duration", 0.0)
            if (durationSec > 0 && recDuration > 0) {
                val diff = abs(recDuration - durationSec)
                score += when {
                    diff <= 2 -> 3
                    diff <= 5 -> 2
                    diff <= 10 -> 1
                    diff > 25 -> -3
                    else -> 0
                }
                // Artis tidak diketahui: durasi wajib cocok agar tidak salah lagu
                if (qArtist == null && diff > 8) continue
            } else if (qArtist == null) {
                continue
            }
            if (!str(rec, "syncedLyrics").isNullOrBlank()) score += 2

            if (score > bestScore) {
                bestScore = score
                best = rec
            }
        }
        val threshold = if (qArtist != null) 4 else 5
        return if (best != null && bestScore >= threshold) parseLrclibRecord(best) else null
    }

    private fun parseLrclibRecord(obj: JSONObject): LyricsData? {
        val instrumental = obj.optBoolean("instrumental", false)
        val synced = str(obj, "syncedLyrics")
        val plain = str(obj, "plainLyrics")
        return buildData(synced, plain, instrumental, source = "LRCLIB")
    }

    private fun buildData(
        synced: String?,
        plain: String?,
        instrumental: Boolean,
        source: String
    ): LyricsData? {
        val lines = if (!synced.isNullOrBlank()) LrcParser.parse(synced) else emptyList()
        val plainText = plain?.takeIf { it.isNotBlank() }
        if (lines.isEmpty() && plainText == null && !instrumental) return null
        return LyricsData(
            syncedLines = lines,
            plainText = plainText,
            instrumental = instrumental && lines.isEmpty() && plainText == null,
            source = source
        )
    }

    // ---- Penyimpanan Disk Persisten ----

    private fun readDisk(key: String): LyricsData? = try {
        val f = File(dir, "$key.json")
        if (!f.exists()) {
            null
        } else {
            val o = JSONObject(f.readText())
            val src = o.optString("source").takeIf { !it.isNullOrBlank() } ?: "Tersimpan"
            buildData(str(o, "synced"), str(o, "plain"), o.optBoolean("instrumental", false), source = src)
        }
    } catch (e: Exception) {
        null
    }

    private suspend fun writeDisk(key: String, data: LyricsData) = withContext(Dispatchers.IO) {
        try {
            val synced = data.syncedLines.joinToString("\n") { l ->
                val m = l.timeMs / 60_000
                val s = (l.timeMs % 60_000) / 1000
                val ms = l.timeMs % 1000
                "[%02d:%02d.%03d]%s".format(m, s, ms, l.text)
            }
            val o = JSONObject()
                .put("synced", synced)
                .put("plain", data.plainText ?: "")
                .put("instrumental", data.instrumental)
                .put("source", data.source)
                .put("saved_at", System.currentTimeMillis())
            File(dir, "$key.json").writeText(o.toString())
        } catch (e: Exception) {
            Log.w(TAG, "Gagal menulis lirik ke disk: ${e.message}")
        }
    }

    private fun deleteDisk(key: String) {
        try {
            val f = File(dir, "$key.json")
            if (f.exists()) {
                f.delete()
            }
        } catch (_: Exception) {}
    }

    // ---- util ----

    private fun albumOf(track: AudioTrack): String? {
        val a = track.album.trim()
        val generic = setOf("", "unknown album", "unknown", "local files", "imported", "device audio")
        return if (a.lowercase() in generic || a == track.cloudProvider) null else a
    }

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

    /** optString yang benar untuk JSON null (org.json mengembalikan "null"). */
    private fun str(o: JSONObject, name: String): String? =
        if (o.isNull(name)) null else o.optString(name).takeIf { it.isNotEmpty() }

    private companion object {
        const val TAG = "LyricsRepository"
        const val BASE_URL = "https://lrclib.net/api"
    }
}
