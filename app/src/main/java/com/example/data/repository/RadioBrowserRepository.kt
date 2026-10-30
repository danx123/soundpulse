package com.example.data.repository

import com.example.data.model.RadioCountry
import com.example.data.model.RadioPage
import com.example.data.model.StreamingStation
import com.example.data.remote.Http
import org.json.JSONArray
import java.io.IOException
import java.net.URLEncoder

/**
 * Radio Browser (https://www.radio-browser.info): direktori stasiun radio internet gratis.
 * Server utama de2; bila gagal, mencoba server cadangan lain.
 */
class RadioBrowserRepository {

    private val servers = listOf(
        "https://de2.api.radio-browser.info",
        "https://de1.api.radio-browser.info",
        "https://fi1.api.radio-browser.info",
        "https://nl1.api.radio-browser.info"
    )
    @Volatile private var activeServer = servers.first()

    suspend fun getCountries(): List<RadioCountry> {
        val body = request("/json/countries?hidebroken=true&order=name") ?: return emptyList()
        val arr = JSONArray(body)
        val out = mutableListOf<RadioCountry>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val name = o.optString("name").trim()
            val code = o.optString("iso_3166_1").trim().uppercase()
            val count = o.optInt("stationcount", 0)
            if (name.isNotEmpty() && code.length == 2 && count > 0) {
                out.add(RadioCountry(name, code, count))
            }
        }
        return out.sortedBy { it.name.lowercase() }
    }

    /**
     * Tanpa filter memakai /json/stations, dengan filter memakai /json/stations/search.
     * @throws IOException bila semua server gagal dihubungi.
     */
    suspend fun searchStations(
        countryCode: String?,
        query: String,
        offset: Int,
        limit: Int = PAGE_SIZE
    ): RadioPage {
        val common = "hidebroken=true&order=clickcount&reverse=true&limit=$limit&offset=$offset"
        val path = if (countryCode == null && query.isBlank()) {
            "/json/stations?$common"
        } else {
            buildString {
                append("/json/stations/search?").append(common)
                if (countryCode != null) append("&countrycode=").append(enc(countryCode))
                if (query.isNotBlank()) append("&name=").append(enc(query.trim()))
            }
        }

        val body = request(path) ?: return RadioPage(emptyList(), false)
        val arr = JSONArray(body)
        val stations = mutableListOf<StreamingStation>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val uuid = o.optString("stationuuid")
            val url = o.optString("url_resolved").ifBlank { o.optString("url") }
            val name = o.optString("name").trim()
            if (uuid.isBlank() || url.isBlank() || name.isEmpty()) continue

            val tags = o.optString("tags").split(',')
                .map { it.trim() }.filter { it.isNotEmpty() }.take(2).joinToString(" / ")
            val codec = o.optString("codec").trim()
            val bitrate = o.optInt("bitrate", 0)
            val country = o.optString("country").trim()
            val state = o.optString("state").trim()
            val favicon = o.optString("favicon").trim()

            stations.add(
                StreamingStation(
                    id = "rb_$uuid",
                    name = name,
                    genre = tags.ifBlank { "Radio" },
                    streamUrl = url,
                    description = listOf(state, country).filter { it.isNotEmpty() }
                        .joinToString(", ").ifBlank { "Radio internet" },
                    bitrateBadge = when {
                        codec.isNotEmpty() && bitrate > 0 -> "${codec.uppercase()} ${bitrate}k"
                        codec.isNotEmpty() -> codec.uppercase()
                        else -> "Stream"
                    },
                    country = country.ifBlank { "Global" },
                    listenerCount = compactCount(o.optInt("clickcount", 0)),
                    artworkUrl = favicon.takeIf { it.startsWith("http") }
                )
            )
        }
        return RadioPage(stations.distinctBy { it.id }, hasMore = arr.length() >= limit)
    }

    /** Mendaftarkan "klik" ke Radio Browser (etika API; membantu peringkat popularitas). */
    suspend fun registerClick(stationUuid: String) {
        try {
            request("/json/url/${enc(stationUuid)}")
        } catch (_: Exception) {
            // abaikan: tidak memengaruhi pemutaran
        }
    }

    private suspend fun request(path: String): String? {
        val ordered = listOf(activeServer) + servers.filter { it != activeServer }
        var lastError: IOException? = null
        for (server in ordered) {
            try {
                val body = Http.getString(server + path)
                activeServer = server
                return body
            } catch (e: IOException) {
                lastError = e
            }
        }
        throw lastError ?: IOException("Radio Browser tidak dapat dihubungi")
    }

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

    private fun compactCount(n: Int): String = when {
        n >= 1_000_000 -> "%.1fM".format(n / 1_000_000f)
        n >= 1_000 -> "%.1fk".format(n / 1_000f)
        else -> n.toString()
    }

    companion object {
        const val PAGE_SIZE = 50
    }
}
