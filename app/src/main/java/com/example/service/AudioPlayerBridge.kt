package com.example.service

import com.example.data.model.AudioTrack

/**
 * Bridge singleton connecting the background AudioPlaybackService with the
 * AudioPlayerViewModel and UI layer for bi-directional media controls across
 * Lockscreen, Notification Shade, and Bluetooth/Headset peripherals.
 */
object AudioPlayerBridge {
    var onPlayPause: (() -> Unit)? = null
    var onPlay: (() -> Unit)? = null
    var onPause: (() -> Unit)? = null
    var onNext: (() -> Unit)? = null
    var onPrev: (() -> Unit)? = null
    var onSeekTo: ((Long) -> Unit)? = null
    var onStop: (() -> Unit)? = null

    @Volatile
    var activeTrack: AudioTrack? = null

    @Volatile
    var isPlaying: Boolean = false

    @Volatile
    var currentPositionMs: Long = 0L

    @Volatile
    var durationMs: Long = 0L

    var onUpdatePlaybackState: ((positionMs: Long, durationMs: Long, isPlaying: Boolean) -> Unit)? = null
}
