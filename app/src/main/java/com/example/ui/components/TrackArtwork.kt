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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.example.data.model.AudioSourceType
import com.example.data.model.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.Collections

object ArtworkLoader {
    // ~24 MB cache (satuan KB)
    private val cache = object : LruCache<String, Bitmap>(24 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }
    private val failed: MutableSet<String> = Collections.synchronizedSet(HashSet())
    private val gate = Semaphore(3) // batasi ekstraksi paralel saat scroll list

    fun peek(track: AudioTrack): Bitmap? = cache.get(track.uri)
    fun hasFailed(track: AudioTrack): Boolean = failed.contains(track.uri)

    suspend fun load(context: Context, track: AudioTrack, targetPx: Int = 512): Bitmap? {
        cache.get(track.uri)?.let { return it }
        if (failed.contains(track.uri)) return null
        if (track.sourceType == AudioSourceType.STREAMING_RADIO) return null

        val bmp = gate.withPermit {
            withContext(Dispatchers.IO) { extract(context, track.uri, targetPx) }
        }
        if (bmp != null) cache.put(track.uri, bmp) else failed.add(track.uri)
        return bmp
    }

    private fun extract(context: Context, uriStr: String, targetPx: Int): Bitmap? {
        val uri = Uri.parse(uriStr)
        // Hanya file lokal (content/file). Stream http tidak diekstrak agar tidak boros data.
        if (uri.scheme != "content" && uri.scheme != "file") return null

        // 1) Artwork embedded di tag file
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

        // 2) Fallback: thumbnail dari MediaStore / DocumentsProvider (API 29+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                return context.contentResolver.loadThumbnail(uri, Size(targetPx, targetPx), null)
            } catch (_: Exception) {}
        }
        return null
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
    fallback: @Composable () -> Unit
) {
    val context = LocalContext.current.applicationContext
    val bitmap by produceState<Bitmap?>(initialValue = track?.let { ArtworkLoader.peek(it) }, track?.uri) {
        value = track?.let { ArtworkLoader.peek(it) }
        if (value == null && track != null) {
            value = ArtworkLoader.load(context, track, sizePx)
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