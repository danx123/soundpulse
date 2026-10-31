package com.example.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * HTTP helper bersama untuk iTunes Search API dan LRCLIB.
 * - 200 -> isi respons
 * - 404 -> null (data memang tidak ada)
 * - lainnya / jaringan putus -> IOException (kegagalan sementara, jangan di-cache sebagai "tidak ada")
 */
object Http {
    private const val USER_AGENT = "SoundPulse/1.4.2 (https://github.com/danx123/soundpulse)"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    suspend fun getString(url: String): String? =
        withContext(Dispatchers.IO) { fetch(url)?.toString(Charsets.UTF_8) }

    suspend fun getBytes(url: String): ByteArray? =
        withContext(Dispatchers.IO) { fetch(url) }

    private fun fetch(url: String): ByteArray? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .build()
        return client.newCall(request).execute().use { response ->
            when {
                response.code == 404 -> null
                !response.isSuccessful -> throw IOException("HTTP ${response.code}")
                else -> response.body?.bytes()
            }
        }
    }
}

/** Hash SHA-1 heksadesimal, dipakai sebagai nama file cache. */
fun sha1Hex(input: String): String =
    MessageDigest.getInstance("SHA-1")
        .digest(input.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
