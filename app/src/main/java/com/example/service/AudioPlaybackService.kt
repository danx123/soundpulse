package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.AudioTrack
import com.example.ui.components.ArtworkLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private const val TAG = "AudioPlaybackService"
private const val NOTIFICATION_ID = 1001
private const val CHANNEL_ID = "soundpulse_audio_playback"

class AudioPlaybackService : Service() {

    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var artJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setupMediaSession()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.playback_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.playback_channel_desc)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private var lastNavActionTime: Long = 0L
    private val navDebounceMs: Long = 400L
    private var lastRenderedTrackId: String? = null

    private fun safeSkipNext() {
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastNavActionTime < navDebounceMs) return
        lastNavActionTime = now
        AudioPlayerBridge.onNext?.invoke()
    }

    private fun safeSkipPrev() {
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastNavActionTime < navDebounceMs) return
        lastNavActionTime = now
        AudioPlayerBridge.onPrev?.invoke()
    }

    private fun setupMediaSession() {
        mediaSession = MediaSession(this, "SoundPulseMediaSession").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    AudioPlayerBridge.onPlay?.invoke() ?: AudioPlayerBridge.onPlayPause?.invoke()
                }

                override fun onPause() {
                    AudioPlayerBridge.onPause?.invoke() ?: AudioPlayerBridge.onPlayPause?.invoke()
                }

                override fun onSkipToNext() {
                    safeSkipNext()
                }

                override fun onSkipToPrevious() {
                    safeSkipPrev()
                }

                override fun onSeekTo(pos: Long) {
                    AudioPlayerBridge.currentPositionMs = pos
                    updateMediaSessionState(pos, AudioPlayerBridge.durationMs, AudioPlayerBridge.isPlaying)
                    AudioPlayerBridge.onSeekTo?.invoke(pos)
                }

                override fun onStop() {
                    AudioPlayerBridge.onStop?.invoke()
                    stopServiceInternal()
                }
            })
            isActive = true
        }

        AudioPlayerBridge.onUpdatePlaybackState = { pos, dur, playing ->
            serviceScope.launch(Dispatchers.Main) {
                updateMediaSessionState(pos, dur, playing)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_STICKY

        when (action) {
            ACTION_PLAY_PAUSE -> {
                AudioPlayerBridge.onPlayPause?.invoke()
            }
            ACTION_PLAY -> {
                AudioPlayerBridge.onPlay?.invoke() ?: AudioPlayerBridge.onPlayPause?.invoke()
            }
            ACTION_PAUSE -> {
                AudioPlayerBridge.onPause?.invoke() ?: AudioPlayerBridge.onPlayPause?.invoke()
            }
            ACTION_NEXT -> {
                safeSkipNext()
            }
            ACTION_PREV -> {
                safeSkipPrev()
            }
            ACTION_STOP -> {
                AudioPlayerBridge.onStop?.invoke()
                stopServiceInternal()
                return START_NOT_STICKY
            }
            ACTION_UPDATE -> {
                val track = AudioPlayerBridge.activeTrack
                if (track != null) {
                    render(track, ArtworkLoader.peek(track))

                    // Artwork belum ada di cache: muat di background lalu render ulang
                    if (ArtworkLoader.peek(track) == null && !ArtworkLoader.hasFailed(track)) {
                        artJob?.cancel()
                        artJob = serviceScope.launch {
                            val art = ArtworkLoader.load(applicationContext, track, 512)
                            if (art != null && AudioPlayerBridge.activeTrack?.id == track.id) {
                                render(track, art)
                            }
                        }
                    }
                } else {
                    stopServiceInternal()
                }
            }
        }

        return START_STICKY
    }

    private fun render(track: AudioTrack, art: Bitmap?) {
        val isPlaying = AudioPlayerBridge.isPlaying
        val isNewTrack = track.id != lastRenderedTrackId
        if (isNewTrack) {
            lastRenderedTrackId = track.id
            AudioPlayerBridge.currentPositionMs = 0L
        }
        val pos = if (isNewTrack) 0L else AudioPlayerBridge.currentPositionMs
        updateMediaSession(
            track,
            isPlaying,
            pos,
            AudioPlayerBridge.durationMs,
            art
        )
        val notification = buildNotification(track, isPlaying, art)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateMediaSession(
        track: AudioTrack,
        isPlaying: Boolean,
        positionMs: Long,
        durationMs: Long,
        art: Bitmap?
    ) {
        val session = mediaSession ?: return

        val stateBuilder = PlaybackState.Builder()
            .setActions(
                PlaybackState.ACTION_PLAY or
                PlaybackState.ACTION_PAUSE or
                PlaybackState.ACTION_PLAY_PAUSE or
                PlaybackState.ACTION_SKIP_TO_NEXT or
                PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                PlaybackState.ACTION_SEEK_TO or
                PlaybackState.ACTION_STOP
            )
            .setState(
                if (isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                positionMs.coerceAtLeast(0L),
                if (isPlaying) 1.0f else 0.0f
            )

        session.setPlaybackState(stateBuilder.build())

        val finalDuration = if (durationMs > 0) durationMs else track.durationMs
        val metadataBuilder = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, track.title)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, track.artist)
            .putString(MediaMetadata.METADATA_KEY_ALBUM, track.album)
            .putLong(MediaMetadata.METADATA_KEY_DURATION, if (finalDuration > 0) finalDuration else -1L)

        if (art != null) {
            metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, art)
        }

        session.setMetadata(metadataBuilder.build())
    }

    private fun updateMediaSessionState(positionMs: Long, durationMs: Long, isPlaying: Boolean) {
        val session = mediaSession ?: return
        val stateBuilder = PlaybackState.Builder()
            .setActions(
                PlaybackState.ACTION_PLAY or
                PlaybackState.ACTION_PAUSE or
                PlaybackState.ACTION_PLAY_PAUSE or
                PlaybackState.ACTION_SKIP_TO_NEXT or
                PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                PlaybackState.ACTION_SEEK_TO or
                PlaybackState.ACTION_STOP
            )
            .setState(
                if (isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                positionMs.coerceAtLeast(0L),
                if (isPlaying) 1.0f else 0.0f
            )

        session.setPlaybackState(stateBuilder.build())

        val track = AudioPlayerBridge.activeTrack
        if (track != null) {
            val finalDuration = if (durationMs > 0) durationMs else track.durationMs
            val metadataBuilder = MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, track.title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, track.artist)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, track.album)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, if (finalDuration > 0) finalDuration else -1L)

            ArtworkLoader.peek(track)?.let { art ->
                metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, art)
            }
            session.setMetadata(metadataBuilder.build())
        }
    }

    private fun buildNotification(track: AudioTrack, isPlaying: Boolean, art: Bitmap?): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val prevIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, AudioPlaybackService::class.java).apply { action = ACTION_PREV },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val playPauseIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, AudioPlaybackService::class.java).apply { action = ACTION_PLAY_PAUSE },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val nextIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, AudioPlaybackService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            this,
            4,
            Intent(this, AudioPlaybackService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val playPauseIcon = if (isPlaying) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }
        val playPauseTitle = if (isPlaying) "Pause" else "Play"

        val prevAction = Notification.Action.Builder(
            android.R.drawable.ic_media_previous,
            "Previous",
            prevIntent
        ).build()

        val playPauseAction = Notification.Action.Builder(
            playPauseIcon,
            playPauseTitle,
            playPauseIntent
        ).build()

        val nextAction = Notification.Action.Builder(
            android.R.drawable.ic_media_next,
            "Next",
            nextIntent
        ).build()

        val mediaStyle = Notification.MediaStyle()
            .setShowActionsInCompactView(0, 1, 2)
            .apply {
                mediaSession?.sessionToken?.let { token ->
                    setMediaSession(token)
                }
            }

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        return builder
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(track.title)
            .setContentText("${track.artist} • ${track.album}")
            .setSubText("${track.format.displayName} Lossless SoundPulse")
            .setContentIntent(contentIntent)
            .apply { if (art != null) setLargeIcon(art) }
            .setStyle(mediaStyle)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .addAction(prevAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .build()
    }

    private fun stopServiceInternal() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping foreground", e)
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        AudioPlayerBridge.onUpdatePlaybackState = null
        artJob?.cancel()
        serviceScope.cancel()
        mediaSession?.apply {
            isActive = false
            release()
        }
        mediaSession = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_PLAY_PAUSE = "com.example.soundpulse.action.PLAY_PAUSE"
        const val ACTION_PLAY = "com.example.soundpulse.action.PLAY"
        const val ACTION_PAUSE = "com.example.soundpulse.action.PAUSE"
        const val ACTION_NEXT = "com.example.soundpulse.action.NEXT"
        const val ACTION_PREV = "com.example.soundpulse.action.PREV"
        const val ACTION_STOP = "com.example.soundpulse.action.STOP"
        const val ACTION_UPDATE = "com.example.soundpulse.action.UPDATE"

        fun startOrUpdate(
            context: Context,
            track: AudioTrack?,
            isPlaying: Boolean,
            positionMs: Long = 0L,
            durationMs: Long = 0L
        ) {
            AudioPlayerBridge.activeTrack = track
            AudioPlayerBridge.isPlaying = isPlaying
            AudioPlayerBridge.currentPositionMs = positionMs
            AudioPlayerBridge.durationMs = durationMs

            if (track == null) {
                stop(context)
                return
            }

            val intent = Intent(context, AudioPlaybackService::class.java).apply {
                action = ACTION_UPDATE
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start/update AudioPlaybackService", e)
            }
        }

        fun stop(context: Context) {
            AudioPlayerBridge.activeTrack = null
            AudioPlayerBridge.isPlaying = false

            val intent = Intent(context, AudioPlaybackService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to stop AudioPlaybackService", e)
            }
        }
    }
}
