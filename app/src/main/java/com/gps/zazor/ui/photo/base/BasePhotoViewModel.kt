package com.gps.zazor.ui.photo.base

import android.graphics.Bitmap
import android.location.Location
import com.gps.zazor.data.models.Photo
import com.gps.zazor.data.prefs.AppPreferences
import com.gps.zazor.data.repositories.PhotoRepository
import com.gps.zazor.ui.base.BaseViewModel
import com.gps.zazor.ui.base.BaseViewModelImpl
import com.gps.zazor.ui.photo.editPhoto.EditPhotoContract
import com.gps.zazor.utils.PhotoStorage
import com.gps.zazor.utils.camera.Camera
import com.gps.zazor.utils.location.AddressResolver
import com.gps.zazor.utils.location.LocationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
import org.joda.time.DateTime

const val DATE_PATTERN = "dd.MM.yyyy"
const val TIME_PATTERN = "HH:mm"

interface BasePhotoViewModel : BaseViewModel<BasePhotoContract.State, BasePhotoContract.Event>

open class BasePhotoViewModelImpl(
    private val editPhotoFlow: MutableSharedFlow<EditPhotoContract.Flow>,
    private val prefs: AppPreferences,
    private val photoRepository: PhotoRepository,
    private val photoStorage: PhotoStorage,
    private val locationProvider: LocationProvider,
    private val addressResolver: AddressResolver
) : BaseViewModelImpl<BasePhotoContract.State, BasePhotoContract.Event>(), BasePhotoViewModel {

    private var activeCamera: Camera = Camera.BACK

    private var isPreviewShown: Boolean = false

    private var isFlashOn: Boolean = false

    private var addNoteJob: Job? = null

    private var locationJob: Job? = null

    protected var lastLocation: Location? = null
        private set

    private var photoTime: DateTime? = null

    /** Note text typed for the picture currently being edited. */
    private var pendingNote: String? = null

    open fun onSaveEdits(edits: BasePhotoContract.Event.SaveEdits) {
        saveEdits(edits.bitmap)
    }

    override suspend fun initialState(): BasePhotoContract.State =
        BasePhotoContract.State.Initial(isTrialWatermarkVisible())

    override fun init() {
        super.init()
        observeLocation()
    }

    override fun onEventArrived(event: BasePhotoContract.Event?) {
        when (event) {
            is BasePhotoContract.Event.Resume -> {
                observeLocation()
                subscribeToAddNoteFlow()
                refreshTrialState()
            }
            is BasePhotoContract.Event.FlipCamera -> handleCameraFlip()
            is BasePhotoContract.Event.ToggleFlash -> handleFlashToggle()
            is BasePhotoContract.Event.PhotoCaptured -> {
                isPreviewShown = true
                photoTime = DateTime.now()
                pendingNote = null
                uiState.value = BasePhotoContract.State.ShowPreview(event.photo, buildNotes())
            }
            is BasePhotoContract.Event.SaveEdits -> onSaveEdits(event)
            is BasePhotoContract.Event.BackPressed -> handleBackPressed()
            is BasePhotoContract.Event.Pause -> unSubscribeFromAddNoteFlow()
            is BasePhotoContract.Event.Stop -> stopObservingLocation()
            else -> Unit
        }
    }

    override fun onCleared() {
        stopObservingLocation()
        super.onCleared()
    }

    protected open fun handleBackPressed() {
        launchIo {
            val wasPreviewShown = isPreviewShown
            isPreviewShown = false
            pendingNote = null
            uiState.value =
                if (wasPreviewShown) BasePhotoContract.State.HidePreview(isTrialWatermarkVisible())
                else BasePhotoContract.State.Exit
        }
    }

    protected suspend fun resolveAddress(): String? =
        withContext(Dispatchers.IO) { addressResolver.resolve(lastLocation) }

    private fun observeLocation() {
        if (locationJob?.isActive == true) return
        locationJob = launch {
            locationProvider.locations().collect { lastLocation = it }
        }
    }

    private fun stopObservingLocation() {
        locationJob?.cancel()
        locationJob = null
    }

    private suspend fun isTrialWatermarkVisible(): Boolean =
        prefs.isTrial() && photoRepository.getPhotos().size > TRIAL_COUNT

    private fun refreshTrialState() {
        launchIo {
            uiState.value = initialState()
        }
    }

    private fun subscribeToAddNoteFlow() {
        if (addNoteJob?.isActive == true) return
        addNoteJob = launch {
            editPhotoFlow.collect { flowState ->
                when (flowState) {
                    is EditPhotoContract.Flow.Cancel -> handleBackPressed()
                    is EditPhotoContract.Flow.AddNote -> {
                        pendingNote = flowState.note
                        uiState.value = buildNotes()
                    }
                    is EditPhotoContract.Flow.AddOverlay -> flowState.run {
                        uiState.value = BasePhotoContract.State.AddOverlay(text, color, fontId)
                    }
                    is EditPhotoContract.Flow.Done -> {
                        uiState.value = BasePhotoContract.State.SaveNotes
                    }
                    is EditPhotoContract.Flow.AllowPaint -> {
                        uiState.value =
                            BasePhotoContract.State.AllowDraw(flowState.color, flowState.mode)
                    }
                    is EditPhotoContract.Flow.DisallowPaint -> {
                        uiState.value = BasePhotoContract.State.DisallowDraw
                    }
                    is EditPhotoContract.Flow.ClearPaint -> {
                        uiState.value = BasePhotoContract.State.ClearDraw
                    }
                    is EditPhotoContract.Flow.Idle -> Unit
                }
            }
        }
    }

    /**
     * Builds the note overlay for the picture being edited.
     *
     * Coordinates and accuracy are simply omitted when there is no fix yet; previously this
     * returned `null` without a location, which swallowed the whole capture.
     */
    private fun buildNotes(): BasePhotoContract.State.AddNotes {
        val location = lastLocation
        val date = photoTime ?: DateTime.now()
        val showCoordinates = prefs.isDisplayCoordinates() && location != null
        return BasePhotoContract.State.AddNotes(
            pendingNote,
            location?.latitude?.formatCoordinate().takeIf { showCoordinates },
            location?.longitude?.formatCoordinate().takeIf { showCoordinates },
            date.toString(DATE_PATTERN).takeIf { prefs.isDisplayDate() },
            date.toString(TIME_PATTERN).takeIf { prefs.isDisplayTime() },
            location?.accuracy?.toInt()?.toString().takeIf { prefs.isDisplayAccuracy() && location != null }
        )
    }

    private fun unSubscribeFromAddNoteFlow() {
        addNoteJob?.cancel()
        addNoteJob = null
    }

    private fun handleCameraFlip() {
        activeCamera = activeCamera.flipped()
        uiState.value = BasePhotoContract.State.FlipCamera(activeCamera)
    }

    private fun handleFlashToggle() {
        isFlashOn = !isFlashOn
        uiState.value = BasePhotoContract.State.ToggleFlash(isFlashOn)
    }

    private fun saveEdits(bitmap: Bitmap) {
        launchIo {
            photoStorage.save(bitmap)?.let { path ->
                photoRepository.savePhoto(
                    Photo(
                        path = path,
                        name = "",
                        date = photoTime ?: DateTime.now(),
                        address = resolveAddress().orEmpty(),
                        lat = lastLocation?.latitude,
                        lng = lastLocation?.longitude
                    )
                )
            }
            handleBackPressed()
        }
    }

    private fun Double.formatCoordinate() = String.format(java.util.Locale.US, "%.6f", this)
}
