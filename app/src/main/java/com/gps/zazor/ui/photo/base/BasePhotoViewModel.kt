package com.gps.zazor.ui.photo.base

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.location.Geocoder
import android.location.Location
import androidx.lifecycle.viewModelScope
import com.gps.zazor.data.models.Photo
import com.gps.zazor.data.prefs.AppPreferences
import com.gps.zazor.data.repositories.PhotoRepository
import com.gps.zazor.ui.base.BaseViewModel
import com.gps.zazor.ui.base.BaseViewModelImpl
import com.gps.zazor.ui.photo.editPhoto.EditPhotoContract
import com.gps.zazor.utils.camera.Camera
import com.gps.zazor.utils.extensions.toBytes
import com.google.android.gms.location.LocationServices
import io.fotoapparat.configuration.UpdateConfiguration
import io.fotoapparat.selector.firstAvailable
import io.fotoapparat.selector.off
import io.fotoapparat.selector.torch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
import java.util.*

interface BasePhotoViewModel : BaseViewModel<BasePhotoContract.State, BasePhotoContract.Event>

@SuppressLint("StaticFieldLeak")
open class BasePhotoViewModelImpl(
    private val context: Context,
    private val editPhotoFlow: MutableSharedFlow<EditPhotoContract.Flow>,
    private val prefs: AppPreferences,
    private val photoRepository: PhotoRepository
) :
    BaseViewModelImpl<BasePhotoContract.State, BasePhotoContract.Event>(), BasePhotoViewModel {

    companion object {

        const val DIR_PHOTOS = "photos"
    }

    private var activeCamera: Camera = Camera.Back

    private var isPreviewShown: Boolean = false

    private var isFlashOn: Boolean = false

    private var addNoteJob: Job? = null

    protected var lastLocation: Location? = null

    private var photoTime: DateTime? = null

    open fun onSaveEdits(edits: BasePhotoContract.Event.SaveEdits) {
        saveEdits(edits.bitmap)
    }

    override suspend fun initialState(): BasePhotoContract.State = BasePhotoContract.State.Initial(prefs.isTrial() && photoRepository.getPhotos().size > TRIAL_COUNT)

    override fun init() {
        super.init()
        getLastLocation(context)
    }

    override fun onEventArrived(event: BasePhotoContract.Event?) {
        when (event) {
            is BasePhotoContract.Event.Resume -> {
                checkTrial()
                subscribeToAddNoteFlow()
            }
            is BasePhotoContract.Event.FlipCamera -> handleCameraFlip()
            is BasePhotoContract.Event.ToggleFlash -> handleFlashToggle()
            is BasePhotoContract.Event.PhotoCaptured -> {
                isPreviewShown = true
                photoTime = DateTime.now()
                handleNoteAdding()?.let {
                    uiState.value = BasePhotoContract.State.ShowPreview(event.photo, it)
                }
            }
            is BasePhotoContract.Event.SaveEdits -> onSaveEdits(event)
            is BasePhotoContract.Event.BackPressed -> handleBackPressed()
            is BasePhotoContract.Event.Pause -> unSubscribeFromAddNoteFlow()
            is BasePhotoContract.Event.Stop -> Unit
        }
    }

    private fun checkTrial() {
        launch {
            uiState.value = initialState()
        }
    }

    private fun subscribeToAddNoteFlow() {
        addNoteJob = launch {
            editPhotoFlow.collect { flowState ->
                when (flowState) {
                    is EditPhotoContract.Flow.Cancel -> {
                        handleBackPressed()
                        //addNoteFlow.value = AddNoteContract.Flow.Idle
                    }
                    is EditPhotoContract.Flow.AddNote -> {
                        uiState.value = handleNoteAdding(flowState.note)
                        //handleBackPressed()
                        //addNoteFlow.value = AddNoteContract.Flow.Idle
                    }
                    is EditPhotoContract.Flow.AddOverlay -> {
                        handleOverlayAdding(flowState)
                    }
                    is EditPhotoContract.Flow.Done -> {
                        uiState.value = BasePhotoContract.State.SaveNotes
                        //savePhoto()
                    }
                    is EditPhotoContract.Flow.AllowPaint -> {
                        uiState.value = BasePhotoContract.State.AllowDraw(flowState.color, flowState.mode)
                    }
                    is EditPhotoContract.Flow.DisallowPaint -> {
                        uiState.value = BasePhotoContract.State.DisallowDraw
                    }
                    is EditPhotoContract.Flow.ClearPaint -> {
                        uiState.value = BasePhotoContract.State.ClearDraw
                    }
                }
            }
        }
    }

    private fun handleNoteAdding(note: String? = null): BasePhotoContract.State.AddNotes? {
        return lastLocation?.let { location ->
            photoTime?.let { date ->
                BasePhotoContract.State.AddNotes(
                    note,
                    location.latitude.toString().takeIf { prefs.isDisplayCoordinates() },
                    location.longitude.toString().takeIf { prefs.isDisplayCoordinates() },
                    date.toString("dd.MM.YYYY").takeIf { prefs.isDisplayDate() },
                    date.toString("hh:mm").takeIf { prefs.isDisplayTime() },
                    location.accuracy.toInt().toString().takeIf { prefs.isDisplayAccuracy() }
                )
            }
        }
    }

    private fun handleOverlayAdding(flowState: EditPhotoContract.Flow.AddOverlay) {
        flowState.run {
            uiState.value = BasePhotoContract.State.AddOverlay(text, color, fontId)
        }
    }

    private fun saveEdits(bitmap: Bitmap) {
        launch {
            val localPath = loadToLocalFile(bitmap.toBytes(), null)
            savePhoto(localPath)
            handleBackPressed()
        }
    }

    private fun unSubscribeFromAddNoteFlow() {
        addNoteJob?.cancel()
        addNoteJob = null
    }

    private fun handleCameraFlip() {
        activeCamera = when (activeCamera) {
            Camera.Front -> Camera.Back
            Camera.Back -> Camera.Front
        }
        uiState.value = BasePhotoContract.State.FlipCamera(activeCamera)
    }

    private fun handleFlashToggle() {
        isFlashOn = !isFlashOn
        uiState.value = BasePhotoContract.State.ToggleFlash(
            UpdateConfiguration(
                flashMode = if (isFlashOn) {
                    firstAvailable(
                        torch(),
                        off()
                    )
                } else {
                    off()
                }
            )
        )
    }

    protected open fun handleBackPressed() {
        viewModelScope.launch(Dispatchers.IO) {
            uiState.value =
                if (isPreviewShown) BasePhotoContract.State.HidePreview(photoRepository.getPhotos().size > TRIAL_COUNT)
                else BasePhotoContract.State.Exit
            isPreviewShown = false
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation(context: Context) {
        LocationServices.getFusedLocationProviderClient(context).lastLocation.addOnSuccessListener {
            lastLocation = it
        }
    }

    private suspend fun savePhoto(path: String) {
        photoRepository.savePhoto(
            Photo(
                path,
                "",
                DateTime.now(),
                getAddress().orEmpty(),
                lastLocation?.latitude,
                lastLocation?.longitude
            )
        )
    }

    protected fun getAddress() =
        lastLocation?.let {
            Geocoder(context, Locale.forLanguageTag("ru")).getFromLocation(it.latitude, it.longitude, 1).firstOrNull()?.run {
                "${getAddressLine(0)}, ${locality}, $countryCode"
            }
        }

    /**
     * @return absolute path to stored local file.
     */
    private fun loadToLocalFile(packet: ByteArray, name: String?): String =
        File(
            context.getExternalFilesDir(DIR_PHOTOS),
            (name ?: DateTime.now().millis.toString()) + ".jpg"
        ).also { file ->
            saveImage(packet, file.outputStream().buffered())
        }.absolutePath

    @Throws(IOException::class)
    private fun saveImage(bytes: ByteArray, outputStream: BufferedOutputStream) {
        outputStream.use {
            it.write(bytes)
            it.flush()
        }
    }
}