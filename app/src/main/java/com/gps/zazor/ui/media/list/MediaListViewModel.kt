package com.gps.zazor.ui.media.list

import com.gps.zazor.data.models.Photo
import com.gps.zazor.data.repositories.PhotoRepository
import com.gps.zazor.ui.base.BaseViewModel
import com.gps.zazor.ui.base.BaseViewModelImpl
import com.gps.zazor.utils.export.TrackFileWriter
import com.gps.zazor.utils.export.TrackFormat
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
    private val photoRepository: PhotoRepository,
    private val trackFileWriter: TrackFileWriter
) : BaseViewModelImpl<MediaListContract.State, MediaListContract.Event>(), MediaListViewModel {

    private val effectFlow = MutableSharedFlow<MediaListContract.Effect>(extraBufferCapacity = 8)

    override val effects: SharedFlow<MediaListContract.Effect> = effectFlow.asSharedFlow()

    private var selectedPhotos: MutableList<Photo>? = null

    private var backfillJob: Job? = null

    /** Last list rendered, so an export uses exactly what the user is looking at. */
    private var photos: List<Photo> = emptyList()

    override suspend fun initialState(): MediaListContract.State {
        photos = photoRepository.getPhotos()
        return MediaListContract.State.Initial(photos)
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
            is MediaListContract.Event.ExportTrack -> exportTrack(event.format)
            else -> Unit
        }
    }

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
                photos = photoRepository.getPhotos()
                uiState.value = MediaListContract.State.Initial(photos)
                effectFlow.emit(MediaListContract.Effect.AddressesFilled(filled))
            }
        }
    }

    private fun exportTrack(format: TrackFormat) {
        launchIo {
            // Read fresh rather than trusting the cached list: init() fills it asynchronously, so
            // an export tapped immediately after opening the gallery would otherwise export nothing.
            val exported = selectedPhotos?.takeIf { it.isNotEmpty() }
                ?: photoRepository.getPhotos().also { photos = it }
            val name = "Zazor " + PhotoClock.formatDate(PhotoClock.now())
            val file = trackFileWriter.write(exported, format, name)
            effectFlow.emit(
                when {
                    exported.none { it.lat != null && it.lng != null } ->
                        MediaListContract.Effect.ExportEmpty
                    file == null -> MediaListContract.Effect.ExportFailed
                    else -> MediaListContract.Effect.TrackExported(file, format)
                }
            )
        }
    }

    private fun deletePhoto(photo: Photo) {
        launchIo {
            selectedPhotos?.remove(photo)
            photos = photoRepository.deletePhoto(photo)
            uiState.value = MediaListContract.State.Initial(photos)
        }
    }
}
