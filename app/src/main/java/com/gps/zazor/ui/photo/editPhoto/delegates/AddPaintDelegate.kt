package com.gps.zazor.ui.photo.editPhoto.delegates

import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.gps.zazor.R
import com.gps.zazor.databinding.BottomSheetAddNoteBinding
import com.gps.zazor.databinding.IncludePaintContentBinding
import com.gps.zazor.ui.photo.editPhoto.EditPhotoContract
import com.gps.zazor.ui.photo.editPhoto.EditPhotoViewModel
import com.gps.zazor.utils.colorPicker.CircleProperty
import com.gps.zazor.utils.colorPicker.ColorPickerAdapter
import com.gps.zazor.utils.colorPicker.OnSelectedColorListener
import com.gps.zazor.utils.extensions.hide
import com.gps.zazor.utils.extensions.show
import com.gps.zazor.views.Mode
import java.lang.ref.WeakReference

/** The three line widths the design offers, in pixels on the photo. */
private const val THIN = 12
private const val MEDIUM = 24
private const val THICK = 40

class AddPaintDelegate(sheetBinding: BottomSheetAddNoteBinding,
                      viewModel: EditPhotoViewModel) : EditPhotoDelegate(sheetBinding, viewModel),
    OnSelectedColorListener {

    private val binding = IncludePaintContentBinding.bind(sheetBinding.clRoot)

    override fun observeState(state: EditPhotoContract.State?) {
        when (state) {
            is EditPhotoContract.State.PaintScreen -> {
                binding.clPaintContainer.show()
                // The tool in hand and the width in use both say so, as in the design.
                tools().forEach { (view, mode) -> view.isSelected = mode == state.mode }
                widths().forEach { (view, width) -> view.isSelected = width == state.width }
            }
            else -> binding.clPaintContainer.hide()
        }
    }

    override fun onShown() {
        super.onShown()
        binding.run {
            rvPaintColorPicker.layoutManager =
                LinearLayoutManager(
                    root.context,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )
            val colorPickerAdapter = ColorPickerAdapter(
                root.context,
                root.context.resources
                    .getIntArray(R.array.ds_mark_colors)
                    .map { CircleProperty(it, ContextCompat.getColor(root.context, R.color.ds_text_primary)) },
                WeakReference(this@AddPaintDelegate),
                // The chosen colour wears the ring the design puts around it.
                selectionMode = true
            )
            rvPaintColorPicker.adapter = colorPickerAdapter
            colorPickerAdapter.select(viewModelColor())
            ivLine.setOnClickListener {
                viewModel.sendEvent(EditPhotoContract.Event.PaintTabPressed(Mode.LINE))
            }
            ivArrow.setOnClickListener {
                viewModel.sendEvent(EditPhotoContract.Event.PaintTabPressed(Mode.ARROW))
            }
            ivCircle.setOnClickListener {
                viewModel.sendEvent(EditPhotoContract.Event.PaintTabPressed(Mode.CIRCLE))
            }
            ivMarker.setOnClickListener {
                viewModel.sendEvent(EditPhotoContract.Event.PaintTabPressed(Mode.MARKER))
            }
            widths().forEach { (view, width) ->
                view.setOnClickListener {
                    viewModel.sendEvent(EditPhotoContract.Event.PaintWidthPicked(width))
                }
            }
        }
    }

    private fun tools() = with(binding) {
        listOf(ivLine to Mode.LINE, ivCircle to Mode.CIRCLE, ivArrow to Mode.ARROW, ivMarker to Mode.MARKER)
    }

    private fun widths() = with(binding) {
        listOf(vWidthThin to THIN, vWidthMedium to MEDIUM, vWidthThick to THICK)
    }

    private fun viewModelColor(): Int? =
        (viewModel.uiState.value as? EditPhotoContract.State.PaintScreen)?.selectedColor

    override fun clear() {
        viewModel.sendEvent(EditPhotoContract.Event.ClearPaint)
    }

    override fun onSelectedColor(color: Int) {
        viewModel.sendEvent(EditPhotoContract.Event.PaintColorPicked(color))
    }
}