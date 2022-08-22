package com.gps.zazor.ui.photo.editPhoto

import android.graphics.Color
import com.gps.zazor.ui.base.UiEvent
import com.gps.zazor.ui.base.UiState
import com.gps.zazor.views.Mode

const val MAX_SIZE_IMAGE = 1024.0 * 1024.0 * 5
const val DEFAULT_BLUR_RADIUS = 1F
const val DEFAULT_PROGRESS = 0
const val START_PROGRESS = 0
const val PROGRESS_BLUR_FORMAT = "00"
const val PROGRESS_SHADOW_FORMAT = "%d%s"
const val PROGRESS_SHADOW_UNIT = "%"
const val DEFAULT_TEXT = ""
const val IMAGE_MIME_TYPE = "image/*"
const val DEFAULT_DATE_PATTERN = "yyyyMMdd_HHmmss"
const val IMAGE_FORMAT = ".png"
const val SELECTOR_COLOR_DEFAULT = Color.WHITE
const val SELECTOR_BUTTON_COLOR_DEFAULT = Color.WHITE
const val OFFSET_ITEM_POSITION = 1
const val DASH_PATH_ON_DISTANCE = 30F
const val DASH_PATH_OFF_DISTANCE = 10F
const val DASH_PATH_PHASE = 0F
const val DEFAULT_LEFT_TOP_ANGLE = 0F
const val STROKE_WIDTH_FOR_DASH_LINE = 0F

class EditPhotoContract {

    sealed class Event : UiEvent {

        object CancelPressed : Event()
        object NotesTabPressed : Event()
        data class PaintTabPressed(val paintMode: Mode) : Event()
        object TextTabPressed : Event()
        object DonePressed : Event()

        data class NotesEntered(val notes: String) : Event()

        data class OverlayEntered(val text: String?,
                                  val color: Int?,
                                  val fontId: Int?) : Event()

        data class PaintColorPicked(val color: Int) : Event()
        object ClearPaint : Event()
    }

    sealed class State : UiState {

        object NotesScreen : State()
        data class PaintScreen(val selectedColor: Int?) : State()
        data class TextScreen(val selectedColor: Int?, val selectedFont: Int?) : State()
        object ShowOverlay : State()
    }

    sealed class Flow {

        object Idle : Flow()
        object Cancel : Flow()
        object Done : Flow()

        data class AddNote(val note: String) : Flow()
        data class AddOverlay(val text: String?, val color: Int?, val fontId: Int?) : Flow()
        data class AllowPaint(val color: Int?, val mode: Mode) : Flow()
        object DisallowPaint : Flow()
        object ClearPaint : Flow()
    }
}