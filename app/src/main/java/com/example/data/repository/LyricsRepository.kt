package com.example.data.repository

import android.content.Context
import android.util.LruCache
import com.example.data.model.AudioSourceType
import com.example.data.model.AudioTrack
import com.example.data.model.LrcParser
import com.example.data.model.LyricsData
import com.example.data.model.LyricsUiState
import com.example.data.remote.Http
import com.example.data.remote.ItunesApi
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
 * iTunes Search API TIDAK menyediakan lirik, jadi alurnya:
 *  1. Cari di LRCLIB memakai metadata lagu itu sendiri.
 *  2. Bila tidak ketemu, normalkan judul/artis/album/durasi lewat iTunes Search lalu cari lagi.
 * Hasil di-cache di memori dan disk agar tidak mengunduh ulang.
 */
class LyricsRepository(context: Context) {

    private val dir = File(context.applicationContext.cacheDir, "lyrics").apply { mkdirs() }
    private val memory = LruCache<String, LyricsData>(32)
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
            LyricsUiState.Error("Tidak dapat terhubung ke server lirik. Periksa koneksi internet lalu coba lagi.")
        } catch (e: Exception) {
            LyricsUiState.Error("Gagal memuat lirik: ${e.localizedMessage ?: "kesalahan tidak dikenal"}")
        }
    }

    private suspend fun fetchRemote(track: AudioTrack, q: TrackQuery): LyricsData? {
        val durationSec = (track.durationMs / 1000).toInt()

        // 1) Metadata lagu sendiri
        lookup(q.title, q.artist, albumOf(track), durationSec)?.let { return it }

        // 2) Normalisasi metadata lewat iTunes, lalu coba lagi
        val itunes = ItunesApi.searchSong(q.title, q.artist, track.durationMs, priority = true)
        if (itunes != null) {
            val sec = if (itunes.durationMs > 0) (itunes.durationMs / 1000).toInt() else durationSec
            lookup(itunes.trackName, itunes.artistName, itunes.collectionName, sec)?.let { return it }
        }
        return null
    }

    private suspend fun lookup(
        title: String,
        artist: String?,
        album: String?,
        durationSec: Int
    ): LyricsData? {
        val artistName = artist?.takeUnless { TrackQuery.isUnknownArtist(it) }

        // Pencarian tepat (butuh artis, album, dan durasi)
        if (artistName != null && !album.isNullOrBlank() && durationSec > 0) {
            val url = "$BASE_URL/get?track_name=${enc(title)}&artist_name=${enc(artistName)}" +
                "&album_name=${enc(album)}&duration=$durationSec"
            val body = Http.getString(url)
            if (body != null) {
                parseRecord(JSONObject(body))?.let { return it }
            }
        }

        // Pencarian fuzzy
        val params = if (artistName != null) {
            "track_name=${enc(title)}&artist_name=${enc(artistName)}"
        } else {
            "track_name=${enc(title)}"
        }
        val body = Http.getString("$BASE_URL/search?$params") ?: return null
        return pickBest(JSONArray(body), title, artistName, durationSec)
    }

    private fun pickBest(arr: JSONArray, title: String, artist: String?, durationSec: Int): LyricsData? {
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
                t == qTitle -> 3
                t.isNotEmpty() && (t.contains(qTitle) || qTitle.contains(t)) -> 1
                else -> continue
            }
            if (qArtist != null) {
                score += when {
                    a == qArtist -> 3
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
                    diff > 20 -> -3
                    else -> 0
                }
                // Artis tidak diketahui: durasi wajib cocok agar tidak salah lagu
                if (qArtist == null && diff > 5) continue
            } else if (qArtist == null) {
                continue
            }
            if (!str(rec, "syncedLyrics").isNullOrBlank()) score += 1

            if (score > bestScore) {
                bestScore = score
                best = rec
            }
        }
        val threshold = if (qArtist != null) 4 else 6
        return if (best != null && bestScore >= threshold) parseRecord(best) else null
    }

    private fun parseRecord(obj: JSONObject): LyricsData? {
        val instrumental = obj.optBoolean("instrumental", false)
        val synced = str(obj, "syncedLyrics")
        val plain = str(obj, "plainLyrics")
        return buildData(synced, plain, instrumental)
    }

    private fun buildData(synced: String?, plain: String?, instrumental: Boolean): LyricsData? {
        val lines = if (!synced.isNullOrBlank()) LrcParser.parse(synced) else emptyList()
        val plainText = plain?.takeIf { it.isNotBlank() }
        if (lines.isEmpty() && plainText == null && !instrumental) return null
        return LyricsData(
            syncedLines = lines,
            plainText = plainText,
            instrumental = instrumental && lines.isEmpty() && plainText == null,
            source = "LRCLIB"
        )
    }

    // ---- cache disk ----

    private fun readDisk(key: String): LyricsData? = try {
        val f = File(dir, "$key.json")
        if (!f.exists()) {
            null
        } else {
            val o = JSONObject(f.readText())
            buildData(str(o, "synced"), str(o, "plain"), o.optBoolean("instrumental", false))
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
            File(dir, "$key.json").writeText(o.toString())
        } catch (_: Exception) {
        }
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
        const val BASE_URL = "https://lrclib.net/api"
    }
}
