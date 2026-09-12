package com.dino.message.threadfeature.presentation.viewmodel

import com.dino.message.corefeature.presentation.util.asString
import com.dino.message.R
import com.dino.message.corefeature.domain.settings.AppSettingsStore
import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.corefeature.domain.resource.StringResolver
import com.dino.message.corefeature.presentation.viewmodel.ScopedStateHolder
import com.dino.message.threadfeature.domain.model.PlaylistTrack
import com.dino.message.threadfeature.domain.usecase.AddTrackToPlaylistUseCase
import com.dino.message.threadfeature.domain.usecase.GetPlaylistTracksUseCase
import com.dino.message.threadfeature.domain.usecase.RemoveTrackFromPlaylistUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class PlaylistBottomSheetViewModel constructor(
    private val stringResolver: StringResolver,
    private val dataStoreUtil: AppSettingsStore,
    private val getPlaylistTracksUseCase: GetPlaylistTracksUseCase,
    private val removeTrackFromPlaylistUseCase: RemoveTrackFromPlaylistUseCase,
    private val addTrackToPlaylistUseCase: AddTrackToPlaylistUseCase,
) : ScopedStateHolder() {

    private val playlistPageLimit = 200
    private var isPlaylistRequestRunning = false

    private val _playlistTracks = MutableStateFlow<List<PlaylistTrack>>(emptyList())
    val playlistTracks: StateFlow<List<PlaylistTrack>> = _playlistTracks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<PlaylistSnackbarEvent>()
    val snackbarEvents = _snackbarEvents.asSharedFlow()
    private val _viewerHasBadge = MutableStateFlow(false)
    val viewerHasBadge: StateFlow<Boolean> = _viewerHasBadge.asStateFlow()

    private val pendingRemovalsByTrackId = linkedMapOf<Long, PendingRemoval>()

    init {
        loadPlaylistTracks()
        observeViewerBadgeAccess()
    }

    private fun observeViewerBadgeAccess() = viewModelScope.launch {
        dataStoreUtil.getBadgeAccessEnabled().collect { hasBadge ->
            _viewerHasBadge.value = hasBadge
        }
    }

    fun loadPlaylistTracks() = viewModelScope.launch {
        if (isPlaylistRequestRunning) return@launch
        isPlaylistRequestRunning = true

        getPlaylistTracksUseCase.execute(page = 1, limit = playlistPageLimit).collect { res ->
            when (res) {
                is Resource.Success -> {
                    _playlistTracks.value = res.data.tracks
                    _error.value = null
                    _isLoading.value = false
                    isPlaylistRequestRunning = false
                }

                is Resource.Loading -> {
                    _isLoading.value = true
                }

                is Resource.Error -> {
                    _error.value = res.message.resolveWith(stringResolver)
                    _isLoading.value = false
                    isPlaylistRequestRunning = false
                }
            }
        }
    }

    fun stageTrackRemoval(track: PlaylistTrack) {
        if (pendingRemovalsByTrackId.containsKey(track.id)) return

        val currentTracks = _playlistTracks.value
        val originalIndex = currentTracks.indexOfFirst { it.id == track.id }
        if (originalIndex < 0) return

        _playlistTracks.value = currentTracks.toMutableList().also { it.removeAt(originalIndex) }

        pendingRemovalsByTrackId[track.id] = PendingRemoval(
            track = track,
            originalIndex = originalIndex,
        )

        removeTrackNow(trackId = track.id)
    }

    fun undoTrackRemoval(trackId: Long) {
        val pendingRemoval = pendingRemovalsByTrackId.remove(trackId) ?: return
        viewModelScope.launch {
            addTrackToPlaylistUseCase.execute(mediaId = pendingRemoval.track.mediaId).collect { res ->
                when (res) {
                    is Resource.Success -> {
                        val restoredTrack = res.data.track
                        restoreTrack(
                            track = restoredTrack,
                            originalIndex = pendingRemoval.originalIndex
                        )
                        _error.value = null
                    }

                    is Resource.Loading -> Unit

                    is Resource.Error -> {
                        _error.value = res.message.resolveWith(stringResolver)
                        _snackbarEvents.emit(
                            PlaylistSnackbarEvent(
                                message = stringResolver.resolveStringId(R.string.playlist_undo_remove_failed)
                            )
                        )
                    }
                }
            }
        }
    }

    fun clearPendingRemoval(trackId: Long) {
        pendingRemovalsByTrackId.remove(trackId)
    }

    private fun removeTrackNow(trackId: Long) = viewModelScope.launch {
        val pendingRemoval = pendingRemovalsByTrackId[trackId] ?: return@launch
        removeTrackFromPlaylistUseCase.execute(trackId).collect { res ->
            when (res) {
                is Resource.Success -> {
                    _error.value = null
                    _snackbarEvents.emit(
                        PlaylistSnackbarEvent(
                            message = stringResolver.resolveStringId(R.string.playlist_removed_message, pendingRemoval.track.title),
                            actionLabel = stringResolver.resolveStringId(R.string.undo),
                            trackId = trackId,
                        )
                    )
                }

                is Resource.Loading -> Unit

                is Resource.Error -> {
                    pendingRemovalsByTrackId.remove(trackId)
                    restoreTrackAfterFailedRemoval(pendingRemoval)
                    _error.value = res.message.resolveWith(stringResolver)
                    _snackbarEvents.emit(
                        PlaylistSnackbarEvent(
                            message = stringResolver.resolveStringId(R.string.playlist_remove_failed_restored)
                        )
                    )
                }
            }
        }
    }

    private fun restoreTrackAfterFailedRemoval(pendingRemoval: PendingRemoval) {
        restoreTrack(
            track = pendingRemoval.track,
            originalIndex = pendingRemoval.originalIndex
        )
    }

    private fun restoreTrack(track: PlaylistTrack, originalIndex: Int) {
        val currentTracks = _playlistTracks.value.toMutableList()
        val alreadyInList = currentTracks.any { it.id == track.id }
        if (alreadyInList) return

        val insertIndex = originalIndex.coerceIn(0, currentTracks.size)
        currentTracks.add(insertIndex, track)
        _playlistTracks.value = currentTracks
    }

    private data class PendingRemoval(
        val track: PlaylistTrack,
        val originalIndex: Int,
    )

    data class PlaylistSnackbarEvent(
        val message: String,
        val actionLabel: String? = null,
        val trackId: Long? = null,
    )
}
