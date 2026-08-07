package com.dino.message.threadfeature.presentation.music

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata as ExoMediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.dino.message.R
import com.dino.message.corefeature.presentation.activity.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class MusicPlaybackService : Service() {
    companion object {
        const val ACTION_PLAY = "music.action.PLAY"
        const val ACTION_PLAY_PLAYLIST = "music.action.PLAY_PLAYLIST"
        const val ACTION_TOGGLE = "music.action.TOGGLE"
        const val ACTION_PAUSE = "music.action.PAUSE"
        const val ACTION_RESUME = "music.action.RESUME"
        const val ACTION_SEEK_TO = "music.action.SEEK_TO"
        const val ACTION_SEEK_BY = "music.action.SEEK_BY"
        const val ACTION_STOP = "music.action.STOP"
        const val ACTION_REWIND_10 = "music.action.REWIND_10"
        const val ACTION_FORWARD_10 = "music.action.FORWARD_10"
        const val ACTION_SET_SHUFFLE = "music.action.SET_SHUFFLE"
        const val ACTION_SET_REPEAT = "music.action.SET_REPEAT"

        const val EXTRA_TRACK_URL = "music.extra.TRACK_URL"
        const val EXTRA_TITLE = "music.extra.TITLE"
        const val EXTRA_ARTIST = "music.extra.ARTIST"
        const val EXTRA_POSITION_MS = "music.extra.POSITION_MS"
        const val EXTRA_DELTA_MS = "music.extra.DELTA_MS"
        const val EXTRA_QUEUE_TRACKS = "music.extra.QUEUE_TRACKS"
        const val EXTRA_QUEUE_TRACK_BUNDLES = "music.extra.QUEUE_TRACK_BUNDLES"
        const val EXTRA_QUEUE_ITEM_TRACK_URL = "music.extra.QUEUE_ITEM_TRACK_URL"
        const val EXTRA_QUEUE_ITEM_TITLE = "music.extra.QUEUE_ITEM_TITLE"
        const val EXTRA_QUEUE_ITEM_ARTIST = "music.extra.QUEUE_ITEM_ARTIST"
        const val EXTRA_QUEUE_START_INDEX = "music.extra.QUEUE_START_INDEX"
        const val EXTRA_ENABLED = "music.extra.ENABLED"

        private const val NOTIFICATION_CHANNEL_ID = "music_playback_channel"
        private const val NOTIFICATION_ID = 1066
        private const val MUSIC_CACHE_MAX_BYTES = 256L * 1024L * 1024L
        private const val IDLE_SERVICE_STOP_DELAY_MS = 90_000L
        private const val TAG = "MusicPlaybackService"

        private val _playbackState = MutableStateFlow(MediaPlaybackState())
        val playbackState: StateFlow<MediaPlaybackState> = _playbackState.asStateFlow()
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var progressJob: Job? = null
    private var idleStopJob: Job? = null

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var simpleCache: SimpleCache

    private var playbackQueue: List<MediaQueueItem> = emptyList()
    private var currentQueueIndex: Int = -1
    private var playbackOrder: List<Int> = emptyList()
    private var currentPlaybackOrderPosition: Int = -1

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED && playNextFromQueueIfAvailable()) {
                return
            }
            _playbackState.value = _playbackState.value.copy(
                isBuffering = isPlayerBuffering(),
                durationMs = player.duration.safeDuration(),
                positionMs = player.currentPosition.safePosition()
            )
            syncSessionAndNotification()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            syncSessionAndNotification()
        }

        override fun onPlayerError(error: PlaybackException) {
            _playbackState.value = _playbackState.value.copy(
                isPlaying = false,
                isBuffering = false,
                errorMessage = error.localizedMessage ?: "Music playback failed"
            )
            syncSessionAndNotification()
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = NotificationManagerCompat.from(this)
        createNotificationChannel()
        setupPlayer()
        setupMediaSession()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == null) {
            if (_playbackState.value.currentTrackUrl.isNullOrBlank()) {
                stopPlaybackAndService()
            }
            return START_NOT_STICKY
        }

        if (action.requiresImmediateForeground()) {
            val startedInForeground = startForegroundSafely(buildNotification(_playbackState.value))
            if (!startedInForeground) {
                stopPlaybackAndService()
                return START_NOT_STICKY
            }
        }

        when (action) {
            ACTION_PLAY -> {
                handlePlayIntent(intent)
            }
            ACTION_PLAY_PLAYLIST -> {
                handlePlayPlaylistIntent(intent)
            }
            ACTION_TOGGLE -> {
                handleToggleIntent(intent)
            }
            ACTION_PAUSE -> pausePlayback()
            ACTION_RESUME -> resumePlayback()
            ACTION_SEEK_TO -> {
                val position = intent.getLongExtra(EXTRA_POSITION_MS, -1L)
                if (position >= 0L) {
                    seekTo(position)
                }
            }
            ACTION_SEEK_BY -> {
                val delta = intent.getLongExtra(EXTRA_DELTA_MS, 0L)
                seekBy(delta)
            }
            ACTION_REWIND_10 -> seekBy(-10_000L)
            ACTION_FORWARD_10 -> seekBy(10_000L)
            ACTION_SET_SHUFFLE -> {
                setShuffleEnabled(intent.getBooleanExtra(EXTRA_ENABLED, false))
            }
            ACTION_SET_REPEAT -> {
                setRepeatEnabled(intent.getBooleanExtra(EXTRA_ENABLED, false))
            }
            ACTION_STOP -> stopPlaybackAndService()
        }

        if (_playbackState.value.currentTrackUrl.isNullOrBlank() && !player.isPlaying) {
            stopPlaybackAndService()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Treat task dismissal as explicit app close.
        stopPlaybackAndService()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        stopProgressUpdates()
        cancelIdleStop()
        player.removeListener(playerListener)
        player.release()
        runCatching { simpleCache.release() }
        mediaSession.release()
        val current = _playbackState.value
        _playbackState.value = MediaPlaybackState(
            queue = current.queue,
            queueIndex = current.queueIndex,
            isShuffleEnabled = current.isShuffleEnabled,
            isRepeatEnabled = current.isRepeatEnabled
        )
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun setupPlayer() {
        val upstreamFactory = DefaultDataSource.Factory(
            applicationContext,
            DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true)
        )
        val cacheDirectory = File(applicationContext.cacheDir, "music_track_cache")
        simpleCache = SimpleCache(
            cacheDirectory,
            LeastRecentlyUsedCacheEvictor(MUSIC_CACHE_MAX_BYTES),
            StandaloneDatabaseProvider(applicationContext)
        )
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(simpleCache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        player = ExoPlayer.Builder(applicationContext)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory))
            .build()
            .apply {
            val attrs = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()
            setAudioAttributes(attrs, true)
            setHandleAudioBecomingNoisy(true)
            setWakeMode(C.WAKE_MODE_NETWORK)
            addListener(playerListener)
            }
    }

    private fun setupMediaSession() {
        mediaSession = MediaSession(this, "SecMessageMusicSession").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() = resumePlayback()
                override fun onPause() = pausePlayback()
                override fun onStop() = stopPlaybackAndService()
                override fun onSeekTo(pos: Long) = seekTo(pos)
                override fun onFastForward() = seekBy(10_000L)
                override fun onRewind() = seekBy(-10_000L)
            })
            isActive = true
        }
    }

    private fun handlePlayIntent(intent: Intent) {
        val trackUrl = intent.getStringExtra(EXTRA_TRACK_URL)?.trim().orEmpty()
        if (trackUrl.isBlank()) return
        val title = intent.getStringExtra(EXTRA_TITLE)
        val artist = intent.getStringExtra(EXTRA_ARTIST)
        playTrack(trackUrl = trackUrl, title = title, artist = artist)
    }

    private fun handlePlayPlaylistIntent(intent: Intent) {
        val incomingQueue = readQueueFromIntent(intent)

        val sanitizedQueue = incomingQueue.mapNotNull { queueItem ->
            val normalizedUrl = queueItem.trackUrl.trim()
            if (normalizedUrl.isBlank()) {
                null
            } else {
                queueItem.copy(trackUrl = normalizedUrl)
            }
        }
        if (sanitizedQueue.isEmpty()) return

        playbackQueue = sanitizedQueue
        val startIndex = intent.getIntExtra(EXTRA_QUEUE_START_INDEX, 0)
            .coerceIn(0, playbackQueue.lastIndex)
        playbackOrder = buildInitialPlaybackOrder(startIndex)
        currentPlaybackOrderPosition = 0
        playQueueIndex(playbackOrder.firstOrNull() ?: startIndex)
    }

    private fun readQueueFromIntent(intent: Intent): List<MediaQueueItem> {
        val queueFromBundles = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableArrayListExtra(EXTRA_QUEUE_TRACK_BUNDLES, Bundle::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra<Bundle>(EXTRA_QUEUE_TRACK_BUNDLES)
            }.orEmpty()
        }.getOrElse { throwable ->
            Log.w(TAG, "Ignoring malformed bundled playlist payload", throwable)
            emptyList()
        }

        if (queueFromBundles.isNotEmpty()) {
            return queueFromBundles.mapNotNull { itemBundle ->
                val trackUrl = itemBundle.getString(EXTRA_QUEUE_ITEM_TRACK_URL)?.trim().orEmpty()
                if (trackUrl.isBlank()) {
                    null
                } else {
                    MediaQueueItem(
                        trackUrl = trackUrl,
                        title = itemBundle.getString(EXTRA_QUEUE_ITEM_TITLE),
                        artist = itemBundle.getString(EXTRA_QUEUE_ITEM_ARTIST)
                    )
                }
            }
        }
        return emptyList()
    }

    private fun handleToggleIntent(intent: Intent) {
        val trackUrl = intent.getStringExtra(EXTRA_TRACK_URL)?.trim().orEmpty()
        val title = intent.getStringExtra(EXTRA_TITLE)
        val artist = intent.getStringExtra(EXTRA_ARTIST)
        val current = _playbackState.value

        if (trackUrl.isNotBlank() && current.currentTrackUrl == trackUrl) {
            if (current.isPlaying) {
                pausePlayback()
            } else {
                resumePlayback()
            }
            return
        }
        if (trackUrl.isNotBlank()) {
            playTrack(trackUrl = trackUrl, title = title, artist = artist)
        }
    }

    private fun playTrack(
        trackUrl: String,
        title: String?,
        artist: String?,
        fromQueue: Boolean = false
    ) {
        if (!fromQueue) {
            playbackQueue = emptyList()
            currentQueueIndex = -1
            playbackOrder = emptyList()
            currentPlaybackOrderPosition = -1
        }
        val mediaItem = MediaItem.Builder()
            .setUri(trackUrl.toUri())
            .setMediaMetadata(
                ExoMediaMetadata.Builder()
                    .setTitle(title ?: "Music track")
                    .setArtist(artist)
                    .build()
            )
            .build()

        _playbackState.value = _playbackState.value.copy(
            currentTrackUrl = trackUrl,
            title = title ?: "Music track",
            artist = artist,
            queue = playbackQueue,
            queueIndex = currentQueueIndex,
            errorMessage = null,
            isBuffering = true
        )
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
        player.play()
        syncSessionAndNotification()
    }

    private fun playQueueIndex(index: Int) {
        if (index !in playbackQueue.indices) return
        currentQueueIndex = index
        val queueItem = playbackQueue[index]
        playTrack(
            trackUrl = queueItem.trackUrl,
            title = queueItem.title,
            artist = queueItem.artist,
            fromQueue = true
        )
    }

    private fun buildInitialPlaybackOrder(startIndex: Int): List<Int> {
        if (playbackQueue.isEmpty()) return emptyList()
        val validStart = startIndex.coerceIn(0, playbackQueue.lastIndex)
        val allIndices = playbackQueue.indices.toList()
        return if (_playbackState.value.isShuffleEnabled) {
            val remaining = allIndices
                .filterNot { it == validStart }
                .shuffled(Random(System.currentTimeMillis()))
            listOf(validStart) + remaining
        } else {
            val afterStart = ((validStart + 1)..playbackQueue.lastIndex).toList()
            val beforeStart = (0 until validStart).toList()
            listOf(validStart) + afterStart + beforeStart
        }
    }

    private fun buildRepeatCycleOrder(): List<Int> {
        if (playbackQueue.isEmpty()) return emptyList()
        val allIndices = playbackQueue.indices.toList()
        return if (_playbackState.value.isShuffleEnabled) {
            allIndices.shuffled(Random(System.currentTimeMillis()))
        } else {
            allIndices
        }
    }

    private fun rebuildRemainingOrderFromCurrent() {
        if (playbackQueue.isEmpty()) return
        if (currentQueueIndex !in playbackQueue.indices) return
        val currentIndex = currentQueueIndex
        val remainingIndices = playbackOrder.drop((currentPlaybackOrderPosition + 1).coerceAtLeast(0))
        val rebuiltRemaining = if (_playbackState.value.isShuffleEnabled) {
            remainingIndices.shuffled(Random(System.currentTimeMillis()))
        } else {
            remainingIndices.sorted()
        }
        playbackOrder = listOf(currentIndex) + rebuiltRemaining
        currentPlaybackOrderPosition = 0
    }

    private fun playNextFromQueueIfAvailable(): Boolean {
        if (playbackQueue.isEmpty() || playbackOrder.isEmpty()) return false

        val nextOrderPosition = currentPlaybackOrderPosition + 1
        if (nextOrderPosition in playbackOrder.indices) {
            currentPlaybackOrderPosition = nextOrderPosition
            playQueueIndex(playbackOrder[nextOrderPosition])
            return true
        }

        if (!_playbackState.value.isRepeatEnabled) {
            stopPlaybackAndService()
            return true
        }

        val nextCycleOrder = buildRepeatCycleOrder()
        if (nextCycleOrder.isEmpty()) return false
        playbackOrder = nextCycleOrder
        currentPlaybackOrderPosition = 0
        playQueueIndex(playbackOrder[0])
        return true
    }

    private fun setShuffleEnabled(enabled: Boolean) {
        if (_playbackState.value.isShuffleEnabled == enabled) return
        _playbackState.value = _playbackState.value.copy(isShuffleEnabled = enabled)
        if (playbackOrder.isNotEmpty() && currentPlaybackOrderPosition in playbackOrder.indices) {
            rebuildRemainingOrderFromCurrent()
        }
        syncSessionAndNotification()
        if (_playbackState.value.currentTrackUrl.isNullOrBlank()) {
            stopSelf()
        }
    }

    private fun setRepeatEnabled(enabled: Boolean) {
        if (_playbackState.value.isRepeatEnabled == enabled) return
        _playbackState.value = _playbackState.value.copy(isRepeatEnabled = enabled)
        syncSessionAndNotification()
        if (_playbackState.value.currentTrackUrl.isNullOrBlank()) {
            stopSelf()
        }
    }

    private fun pausePlayback() {
        player.pause()
        syncSessionAndNotification()
    }

    private fun resumePlayback() {
        if (_playbackState.value.currentTrackUrl.isNullOrBlank()) return
        player.playWhenReady = true
        player.play()
        syncSessionAndNotification()
    }

    private fun seekTo(positionMs: Long) {
        val duration = player.duration.safeDuration()
        val clamped = if (duration > 0L) {
            min(max(positionMs, 0L), duration)
        } else {
            max(positionMs, 0L)
        }
        player.seekTo(clamped)
        _playbackState.value = _playbackState.value.copy(positionMs = clamped)
        syncSessionAndNotification()
    }

    private fun seekBy(deltaMs: Long) {
        seekTo(player.currentPosition.safePosition() + deltaMs)
    }

    private fun stopPlaybackAndService() {
        val current = _playbackState.value
        cancelIdleStop()
        player.playWhenReady = false
        player.stop()
        player.clearMediaItems()
        playbackQueue = emptyList()
        currentQueueIndex = -1
        playbackOrder = emptyList()
        currentPlaybackOrderPosition = -1
        _playbackState.value = MediaPlaybackState(
            isShuffleEnabled = current.isShuffleEnabled,
            isRepeatEnabled = current.isRepeatEnabled
        )
        stopProgressUpdates()
        mediaSession.isActive = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun syncSessionAndNotification() {
        val updated = _playbackState.value.copy(
            isPlaying = player.isPlaying,
            durationMs = player.duration.safeDuration(),
            positionMs = player.currentPosition.safePosition(),
            isBuffering = isPlayerBuffering()
        )
        _playbackState.value = updated
        updateMediaSession(updated)
        updateNotification(updated)
        if (updated.isPlaying) {
            startProgressUpdates()
            cancelIdleStop()
        } else {
            stopProgressUpdates()
            scheduleIdleStopIfNeeded(updated)
        }
    }

    private fun updateMediaSession(state: MusicPlaybackUiState) {
        val playbackActions = (
            PlaybackState.ACTION_PLAY or
                PlaybackState.ACTION_PAUSE or
                PlaybackState.ACTION_PLAY_PAUSE or
                PlaybackState.ACTION_SEEK_TO or
                PlaybackState.ACTION_STOP or
                PlaybackState.ACTION_FAST_FORWARD or
                PlaybackState.ACTION_REWIND
            )

        val playbackStateBuilder = PlaybackState.Builder()
            .setActions(playbackActions)
            .setState(
                if (state.isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                state.positionMs,
                if (state.isPlaying) 1f else 0f
            )
        mediaSession.setPlaybackState(playbackStateBuilder.build())

        val metadata = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, state.title ?: "Music track")
            .putString(MediaMetadata.METADATA_KEY_ARTIST, state.artist ?: "")
            .putLong(MediaMetadata.METADATA_KEY_DURATION, state.durationMs)
            .build()
        mediaSession.setMetadata(metadata)
        mediaSession.isActive = state.currentTrackUrl != null
    }

    private fun updateNotification(state: MusicPlaybackUiState) {
        if (state.currentTrackUrl == null) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            notificationManager.cancel(NOTIFICATION_ID)
            return
        }

        val notification = buildNotification(state)
        val keepForeground = state.isPlaying || state.isBuffering
        if (keepForeground) {
            if (!startForegroundSafely(notification)) {
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
        } else {
            stopForeground(STOP_FOREGROUND_DETACH)
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(state: MusicPlaybackUiState): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            5010,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val rewindIntent = buildServicePendingIntent(5011, ACTION_REWIND_10)
        val toggleIntent = buildServicePendingIntent(
            requestCode = 5012,
            action = if (state.isPlaying) ACTION_PAUSE else ACTION_RESUME
        )
        val forwardIntent = buildServicePendingIntent(5013, ACTION_FORWARD_10)
        val stopIntent = buildServicePendingIntent(5014, ACTION_STOP)

        val builder = Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(state.title ?: "Music track")
            .setContentText(state.artist ?: "SecMessage")
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentIntent(contentIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(state.isPlaying)
            .setShowWhen(false)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .addAction(
                Notification.Action.Builder(
                    android.graphics.drawable.Icon.createWithResource(this, android.R.drawable.ic_media_rew),
                    "-10s",
                    rewindIntent
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    android.graphics.drawable.Icon.createWithResource(
                        this,
                        if (state.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
                    ),
                    if (state.isPlaying) "Pause" else "Play",
                    toggleIntent
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    android.graphics.drawable.Icon.createWithResource(this, android.R.drawable.ic_media_ff),
                    "+10s",
                    forwardIntent
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    android.graphics.drawable.Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel),
                    "Stop",
                    stopIntent
                ).build()
            )
            .setStyle(
                Notification.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )

        return builder.build()
    }

    private fun serviceIntent(action: String): Intent {
        return Intent(this, MusicPlaybackService::class.java).apply {
            this.action = action
        }
    }

    private fun buildServicePendingIntent(requestCode: Int, action: String): PendingIntent {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val intent = serviceIntent(action)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && action.requiresImmediateForeground()) {
            PendingIntent.getForegroundService(this, requestCode, intent, flags)
        } else {
            PendingIntent.getService(this, requestCode, intent, flags)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val existing = manager.getNotificationChannel(NOTIFICATION_CHANNEL_ID)
        if (existing != null) return
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Music Playback",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Foreground controls for music posts"
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
    }

    private fun String.requiresImmediateForeground(): Boolean {
        return this == ACTION_PLAY ||
            this == ACTION_PLAY_PLAYLIST ||
            this == ACTION_TOGGLE ||
            this == ACTION_RESUME
    }

    private fun startProgressUpdates() {
        if (progressJob?.isActive == true) return
        progressJob = serviceScope.launch {
            while (true) {
                if (!player.isPlaying) break
                val updated = _playbackState.value.copy(
                    positionMs = player.currentPosition.safePosition(),
                    durationMs = player.duration.safeDuration(),
                    isPlaying = true
                )
                _playbackState.value = updated
                updateMediaSession(updated)
                delay(300)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun isPlayerBuffering(): Boolean {
        val hasTrack = !_playbackState.value.currentTrackUrl.isNullOrBlank()
        return hasTrack && player.playbackState == Player.STATE_BUFFERING
    }

    private fun scheduleIdleStopIfNeeded(state: MusicPlaybackUiState) {
        if (state.currentTrackUrl.isNullOrBlank()) {
            stopPlaybackAndService()
            return
        }
        if (state.isBuffering) {
            cancelIdleStop()
            return
        }
        if (idleStopJob?.isActive == true) return
        idleStopJob = serviceScope.launch {
            delay(IDLE_SERVICE_STOP_DELAY_MS)
            val latestState = _playbackState.value
            if (!latestState.isPlaying && !latestState.isBuffering) {
                stopPlaybackAndService()
            }
        }
    }

    private fun cancelIdleStop() {
        idleStopJob?.cancel()
        idleStopJob = null
    }

    private fun startForegroundSafely(notification: Notification): Boolean {
        return runCatching {
            startForeground(NOTIFICATION_ID, notification)
            true
        }.getOrElse { throwable ->
            val runtimeException = throwable as? RuntimeException
            if (runtimeException != null && runtimeException.isForegroundStartBlocked()) {
                Log.w(TAG, "Foreground start not allowed right now", runtimeException)
                false
            } else if (runtimeException is IllegalStateException) {
                Log.w(TAG, "Foreground start failed due to illegal service state", runtimeException)
                false
            } else {
                throw throwable
            }
        }
    }

    private fun RuntimeException.isForegroundStartBlocked(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            javaClass.name == "android.app.ForegroundServiceStartNotAllowedException"
    }

    private fun Long?.safeDuration(): Long {
        val value = this ?: 0L
        return if (value <= 0L || value == C.TIME_UNSET) 0L else value
    }

    private fun Long?.safePosition(): Long {
        val value = this ?: 0L
        return if (value < 0L || value == C.TIME_UNSET) 0L else value
    }
}
