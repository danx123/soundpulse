package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SoundPulseDatabase
import com.example.data.local.entity.PlaylistEntity
import com.example.data.local.entity.PlaylistTrackEntity
import com.example.data.local.entity.PlaylistType
import com.example.data.local.entity.TrackStatEntity
import com.example.data.model.AudioFormat
import com.example.data.model.AudioSourceType
import com.example.data.model.AudioTrack
import com.example.data.model.EqualizerState
import com.example.data.repository.AiMusicRecommender
import com.example.data.repository.AudioRepository
import com.example.data.repository.PlaylistRepository
import com.example.ui.theme.DynamicAuraExtractor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  private lateinit var db: SoundPulseDatabase

  @Before
  fun createDb() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, SoundPulseDatabase::class.java)
      .allowMainThreadQueries()
      .build()
  }

  @After
  fun closeDb() {
    db.close()
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("SoundPulse", appName)

    val footerBrand = context.getString(R.string.about_footer_brand)
    assertEquals("Macan Angkasa", footerBrand)

    val aboutTitle = context.getString(R.string.about_title)
    assertTrue(aboutTitle.isNotEmpty())
  }

  @Test
  fun `verify audio formats and equalizer presets`() {
    val presets = EqualizerState.PRESETS
    assertTrue("Harus memiliki preset equalizer", presets.isNotEmpty())
    assertNotNull("Harus memiliki preset Flat", presets.find { it.name == "Flat" })
    assertNotNull("Harus memiliki preset Bass Boost", presets.find { it.name == "Bass Boost" })

    val flacFormat = AudioFormat.fromExtension("flac")
    assertEquals(AudioFormat.FLAC, flacFormat)
    assertTrue("FLAC harus lossless", flacFormat.isLossless)
  }

  @Test
  fun `verify audio repository initial state`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = AudioRepository(context)

    assertTrue("Repository harus memuat default studio tracks", repository.tracks.value.isNotEmpty())
    assertTrue("Repository harus memuat stasiun streaming pihak ketiga", repository.streamingStations.value.isNotEmpty())
    assertTrue("Repository harus memuat preset cloud storage", repository.cloudPresets.value.isNotEmpty())
  }

  @Test
  fun `verify Room database playlist creation and track organization`() = runBlocking {
    val repository = PlaylistRepository(db.playlistDao())

    val playlistId = repository.createPlaylist(
      title = "My Hi-Res Cloud Vault",
      description = "Lossless FLAC and WAV streaming",
      type = PlaylistType.CLOUD
    )
    assertTrue("Playlist ID harus valid", playlistId > 0)

    val track1 = AudioTrack(
      id = "cloud_track_1",
      title = "Symphony No. 5 in C Minor",
      artist = "Orchestra Hall",
      album = "Master Archive",
      durationMs = 420000L,
      uri = "https://storage.googleapis.com/audio/beethoven5.flac",
      sourceType = AudioSourceType.CLOUD_STORAGE,
      format = AudioFormat.FLAC,
      bitrateKbps = 1411,
      sampleRateHz = 96000,
      cloudProvider = "Google Cloud Storage"
    )

    val track2 = AudioTrack(
      id = "local_track_1",
      title = "Local Acoustic Session",
      artist = "Indie Trio",
      album = "Live Studio",
      durationMs = 210000L,
      uri = "content://media/external/audio/media/101",
      sourceType = AudioSourceType.LOCAL_FILE,
      format = AudioFormat.WAV,
      bitrateKbps = 1411,
      sampleRateHz = 48000
    )

    val added1 = repository.addTrackToPlaylist(playlistId, track1)
    val added2 = repository.addTrackToPlaylist(playlistId, track2)
    assertTrue("Track 1 harus berhasil ditambahkan", added1)
    assertTrue("Track 2 harus berhasil ditambahkan", added2)

    val duplicateAdd = repository.addTrackToPlaylist(playlistId, track1)
    assertTrue("Track duplikat tidak boleh ditambahkan lagi", !duplicateAdd)

    val playlistWithTracks = repository.getPlaylist(playlistId).first()
    assertNotNull("PlaylistWithTracks harus ditemukan", playlistWithTracks)
    assertEquals(2, playlistWithTracks?.trackCount)
    assertEquals("My Hi-Res Cloud Vault", playlistWithTracks?.playlist?.title)
    assertTrue(playlistWithTracks?.hasCloudTracks == true)
    assertTrue(playlistWithTracks?.hasLocalTracks == true)

    repository.removeTrackByTrackId(playlistId, "cloud_track_1")
    val updated = repository.getPlaylist(playlistId).first()
    assertEquals(1, updated?.trackCount)
    assertEquals("local_track_1", updated?.tracks?.first()?.trackId)

    repository.deletePlaylist(playlistId)
    val afterDelete = repository.getPlaylist(playlistId).first()
    assertTrue("Playlist harus terhapus", afterDelete == null)
  }

  @Test
  fun `verify track stats and AI smart playlist recommender`() = runBlocking {
    val statDao = db.trackStatDao()
    statDao.insertOrUpdate(
      TrackStatEntity(
        trackId = "track_synth",
        title = "Neon Nightdrive (Synthwave)",
        artist = "Pulse",
        playCount = 12
      )
    )
    statDao.insertOrUpdate(
      TrackStatEntity(
        trackId = "track_lofi",
        title = "Midnight Coffee Lo-Fi",
        artist = "Aura",
        playCount = 20
      )
    )

    val mostPlayed = statDao.getMostPlayed().first()
    assertEquals(2, mostPlayed.size)
    assertEquals("track_lofi", mostPlayed[0].trackId)
    assertEquals(20, mostPlayed[0].playCount)

    // Test AI Recommender
    val recommender = AiMusicRecommender()
    val allTracks = listOf(
      AudioTrack(
        id = "track_lofi",
        title = "Midnight Coffee Lo-Fi",
        artist = "Aura",
        durationMs = 180000L,
        uri = "https://example.com/lofi.mp3",
        format = AudioFormat.MP3
      ),
      AudioTrack(
        id = "track_synth",
        title = "Neon Nightdrive (Synthwave)",
        artist = "Pulse",
        durationMs = 210000L,
        uri = "https://example.com/synth.mp3",
        format = AudioFormat.FLAC
      )
    )

    val smartPlaylists = recommender.generateSmartPlaylists(allTracks, mostPlayed)
    assertTrue("Harus menghasilkan smart playlist", smartPlaylists.isNotEmpty())

    val mostPlayedSmartList = smartPlaylists.find { it.id == "smart_most_played" }
    assertNotNull("Harus memiliki smart playlist Most Played", mostPlayedSmartList)
    assertEquals("track_lofi", mostPlayedSmartList?.tracks?.first()?.id)

    val customMix = recommender.createCustomAiPlaylist("santai", allTracks)
    assertNotNull(customMix)
    assertTrue(customMix.tracks.isNotEmpty())
  }

  @Test
  fun `verify dynamic aura color extraction`() {
    val synthTrack = AudioTrack(
      id = "test_synth",
      title = "Cybernetic Horizon (Lossless Synth)",
      artist = "Pulse Studio",
      uri = "https://example.com/synth.mp3",
      format = AudioFormat.FLAC
    )

    val auraColors = DynamicAuraExtractor.extract(synthTrack)
    assertNotNull(auraColors.dominant)
    assertNotNull(auraColors.secondary)
    assertNotNull(auraColors.auraLinearBrush)
  }
}
