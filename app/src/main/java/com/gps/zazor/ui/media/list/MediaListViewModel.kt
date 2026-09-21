package com.gps.zazor.ui.media.list

import com.gps.zazor.data.models.Photo
import com.gps.zazor.data.repositories.PhotoRepository
import com.gps.zazor.ui.base.BaseViewModel
import com.gps.zazor.ui.base.BaseViewModelImpl
import com.gps.zazor.utils.time.PhotoClock
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

interface MediaListViewModel : BaseViewModel<MediaListContract.State, MediaListContract.Event> {

    /** One-shot results; see [MediaListContract.Effect]. */
    val effects: SharedFlow<MediaListContract.Effect>

    fun backPressed(): Boolean
}

class MediaListViewModelImpl(
    private val photoRepository: PhotoRepository
) : BaseViewModelImpl<MediaListContract.State, MediaListContract.Event>(), MediaListViewModel {

    private val effectFlow = MutableSharedFlow<MediaListContract.Effect>(extraBufferCapacity = 8)

    override val effects: SharedFlow<MediaListContract.Effect> = effectFlow.asSharedFlow()

    private var selectedPhotos: MutableList<Photo>? = null

    private var backfillJob: Job? = null

    /** Everything stored, before the filter. */
    private var allPhotos: List<Photo> = emptyList()

    private var filter: MediaListContract.Filter = MediaListContract.Filter.ALL

    /** What is on screen right now, so an export sends exactly what the user is looking at. */
    private var photos: List<Photo> = emptyList()

    override suspend fun initialState(): MediaListContract.State {
        allPhotos = photoRepository.getPhotos()
        return contentState()
    }

    override fun init() {
        super.init()
        backfillAddresses()
    }

    override fun backPressed(): Boolean {
        selectedPhotos?.let {
            selectedPhotos = null
            uiState.value = MediaListContract.State.ClearSelectedMode
            return false
        }
        return true
    }

    override fun onEventArrived(event: MediaListContract.Event?) {
        when (event) {
            is MediaListContract.Event.DeletePhoto -> deletePhoto(event.photo)
            is MediaListContract.Event.SwitchPhotoSelected -> {
                if (event.isSelected) selectedPhotos?.add(event.photo)
                else selectedPhotos?.remove(event.photo)
            }
            is MediaListContract.Event.SharePhotos -> {
                selectedPhotos?.takeIf { it.isNotEmpty() }?.let { chosen ->
                    launchIo {
                        effectFlow.emit(MediaListContract.Effect.OpenShare(chosen.map { it.path }))
                    }
                }
            }
            is MediaListContract.Event.TurnOnSelectionMode -> {
                if (selectedPhotos == null) selectedPhotos = mutableListOf()
            }
            is MediaListContract.Event.DeleteSelected -> deleteSelected()
            is MediaListContract.Event.FilterSelected -> {
                filter = event.filter
                uiState.value = contentState()
            }
            else -> Unit
        }
    }

    /**
     * The feed under the current filter.
     *
     * "Marked up" means the shot carries a note - that is what the person wrote it down as, and it
     * is the only mark that survives as data rather than as pixels. "Panoramas" are the frames the
     * wide lens took.
     */
    private fun contentState(): MediaListContract.State.Initial {
        photos = when (filter) {
            MediaListContract.Filter.ALL -> allPhotos
            MediaListContract.Filter.MARKED -> allPhotos.filter { it.name.isNotBlank() }
            MediaListContract.Filter.WIDE -> allPhotos.filter { it.isWide }
        }
        return MediaListContract.State.Initial(photos, filter, seriesSizes())
    }

    /** Counted over everything, not the filtered feed: a series is a series either way. */
    private fun seriesSizes(): Map<String, Int> =
        allPhotos.mapNotNull { it.seriesId?.takeIf(String::isNotBlank) }
            .groupingBy { it }
            .eachCount()

    /**
     * A photo taken out of network coverage is stored without an address; the geocoder is retried
     * whenever the gallery opens, so the row fills itself in once the phone is back online.
     */
    private fun backfillAddresses() {
        // The gallery is opened often; without this guard every visit starts another serial pass
        // over the same address-less rows while the previous one is still geocoding.
        if (backfillJob?.isActive == true) return
        backfillJob = launchIo {
            val filled = photoRepository.backfillAddresses()
            if (filled > 0) {
                allPhotos = photoRepository.getPhotos()
                uiState.value = contentState()
                effectFlow.emit(MediaListContract.Effect.AddressesFilled(filled))
            }
        }
    }

    private fun deleteSelected() {
        val chosen = selectedPhotos?.toList().orEmpty()
        if (chosen.isEmpty()) return
        launchIo {
            selectedPhotos = null
            allPhotos = photoRepository.deletePhotos(chosen)
            uiState.value = contentState()
            // The list and the "selection is over" signal must not both go through the conflated
            // uiState - the first write would be swallowed. The effect carries the second.
            effectFlow.emit(MediaListContract.Effect.SelectionDeleted(chosen.size))
        }
    }

    private fun deletePhoto(photo: Photo) {
        launchIo {
            selectedPhotos?.remove(photo)
            allPhotos = photoRepository.deletePhoto(photo)
            uiState.value = contentState()
        }
    }
}
