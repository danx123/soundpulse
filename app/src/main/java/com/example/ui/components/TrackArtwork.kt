package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.example.data.model.AudioSourceType
import com.example.data.model.AudioTrack
import com.example.data.remote.Http
import com.example.data.remote.ItunesApi
import com.example.data.remote.TrackQuery
import com.example.data.remote.isOnlineLookupEligible
import com.example.data.remote.sha1Hex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * Memuat artwork lagu dengan urutan:
 *  1. Artwork embedded di tag file (ID3 / FLAC / M4A)
 *  2. Thumbnail sistem (MediaStore / DocumentsProvider, API 29+)
 *  3. iTunes Search API bila lagu tidak punya artwork (di-cache di disk)
 */
object ArtworkLoader {
    private const val SMALL = 160
    private const val LARGE = 512
    private const val NO_MATCH_TTL_MS = 3L * 24 * 60 * 60 * 1000

    private fun bucket(px: Int) = if (px <= 200) SMALL else LARGE
    private fun cacheKey(track: AudioTrack, px: Int) = track.uri + "#" + bucket(px)

    // ~24 MB cache (satuan KB)
    private val cache = object : LruCache<String, Bitmap>(24 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }
    private val localFailed: MutableSet<String> = Collections.synchronizedSet(HashSet())
    private val onlineFailed: MutableSet<String> = Collections.synchronizedSet(HashSet())
    private val localGate = Semaphore(3) // batasi ekstraksi paralel saat scroll list
    private val keyLocks = ConcurrentHashMap<String, Mutex>()

    fun peek(track: AudioTrack, targetPx: Int = LARGE): Bitmap? = cache.get(cacheKey(track, targetPx))

    /** True bila semua sumber sudah dicoba dan tidak ada artwork. */
    fun hasFailed(track: AudioTrack): Boolean =
        localFailed.contains(track.uri) &&
            (!track.isOnlineLookupEligible() || onlineFailed.contains(track.uri))

    /**
     * @param online   izinkan fallback ke iTunes
     * @param priority lewati antrean throttle iTunes (lagu yang sedang diputar)
     */
    suspend fun load(
        context: Context,
        track: AudioTrack,
        targetPx: Int = LARGE,
        online: Boolean = true,
        priority: Boolean = false
    ): Bitmap? {
        val key = cacheKey(track, targetPx)
        cache.get(key)?.let { return it }
        if (track.sourceType == AudioSourceType.STREAMING_RADIO) return null

        val lock = keyLocks.getOrPut(key) { Mutex() }
        return lock.withLock {
            val cached = cache.get(key)
            if (cached != null) return@withLock cached

            val px = bucket(targetPx)
            var bmp: Bitmap? = null

            if (!localFailed.contains(track.uri)) {
                bmp = localGate.withPermit {
                    withContext(Dispatchers.IO) { extractLocal(context, track.uri, px) }
                }
                if (bmp == null) localFailed.add(track.uri)
            }

            if (bmp == null && online && track.isOnlineLookupEligible() &&
                !onlineFailed.contains(track.uri)
            ) {
                bmp = fetchOnline(context, track, px, priority)
            }

            if (bmp != null) cache.put(key, bmp)
            bmp
        }
    }

    // ---------------------------------------------------------------- lokal

    private fun extractLocal(context: Context, uriStr: String, targetPx: Int): Bitmap? {
        val uri = Uri.parse(uriStr)
        // Hanya file lokal (content/file). Sumber http tidak diekstrak dari tag.
        if (uri.scheme != "content" && uri.scheme != "file") return null

        val mmr = MediaMetadataRetriever()
        try {
            mmr.setDataSource(context, uri)
            val bytes = mmr.embeddedPicture
            if (bytes != null) {
                decodeSampled(bytes, targetPx)?.let { return it }
            }
        } catch (_: Exception) {
        } finally {
            try { mmr.release() } catch (_: Exception) {}
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                return context.contentResolver.loadThumbnail(uri, Size(targetPx, targetPx), null)
            } catch (_: Exception) {}
        }
        return null
    }

    // ---------------------------------------------------------------- iTunes

    private suspend fun fetchOnline(
        context: Context,
        track: AudioTrack,
        targetPx: Int,
        priority: Boolean
    ): Bitmap? {
        val q = TrackQuery.from(track)
        if (q.title.isBlank()) {
            onlineFailed.add(track.uri)
            return null
        }

        val dir = File(context.cacheDir, "itunes_artwork").apply { mkdirs() }
        val name = sha1Hex(TrackQuery.normalize(q.artist ?: "") + "|" + TrackQuery.normalize(q.title))
        val imageFile = File(dir, "$name.jpg")
        val noneFile = File(dir, "$name.none")

        // 1) Cache disk
        val fromDisk = withContext(Dispatchers.IO) {
            if (imageFile.exists()) decodeSampled(imageFile.readBytes(), targetPx) else null
        }
        if (fromDisk != null) return fromDisk

        val recentlyMissed = withContext(Dispatchers.IO) {
            noneFile.exists() && System.currentTimeMillis() - noneFile.lastModified() < NO_MATCH_TTL_MS
        }
        if (recentlyMissed) {
            onlineFailed.add(track.uri)
            return null
        }

        // 2) Cari di iTunes
        return try {
            val hit = ItunesApi.searchSong(q.title, q.artist, track.durationMs, priority)
            val artUrl = hit?.artworkUrl
            if (artUrl == null) {
                markNoMatch(noneFile)
                onlineFailed.add(track.uri)
                null
            } else {
                val bytes = Http.getBytes(artUrl)
                if (bytes == null) {
                    markNoMatch(noneFile)
                    onlineFailed.add(track.uri)
                    null
                } else {
                    withContext(Dispatchers.IO) { imageFile.writeBytes(bytes) }
                    decodeSampled(bytes, targetPx)
                }
            }
        } catch (e: IOException) {
            null // jaringan bermasalah: jangan di-cache sebagai "tidak ada"
        }
    }

    private suspend fun markNoMatch(noneFile: File) = withContext(Dispatchers.IO) {
        try { noneFile.writeText("") } catch (_: Exception) {}
    }

    private fun decodeSampled(bytes: ByteArray, target: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= target && bounds.outHeight / (sample * 2) >= target) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }
}

@Composable
fun TrackArtwork(
    track: AudioTrack?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    sizePx: Int = 512,
    online: Boolean = true,
    priority: Boolean = false,
    fallback: @Composable () -> Unit = {
        Image(
            painter = painterResource(id = R.drawable.img_default_album_cover),
            contentDescription = "Album Art",
            contentScale = contentScale,
            modifier = modifier
        )
    }
) {
    val context = LocalContext.current.applicationContext
    val bitmap by produceState<Bitmap?>(
        initialValue = track?.let { ArtworkLoader.peek(it, sizePx) },
        track?.uri,
        sizePx
    ) {
        value = track?.let { ArtworkLoader.peek(it, sizePx) }
        if (value == null && track != null) {
            value = ArtworkLoader.load(context, track, sizePx, online, priority)
        }
    }
    val bmp = bitmap
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = "Album Art",
            contentScale = contentScale,
            modifier = modifier
        )
    } else {
        fallback()
    }
}
