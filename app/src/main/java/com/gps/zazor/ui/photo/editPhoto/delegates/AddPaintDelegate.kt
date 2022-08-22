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

class AddPaintDelegate(sheetBinding: BottomSheetAddNoteBinding,
                      viewModel: EditPhotoViewModel) : EditPhotoDelegate(sheetBinding, viewModel),
    OnSelectedColorListener {

    private val binding = IncludePaintContentBinding.bind(sheetBinding.clRoot)

    override fun observeState(state: EditPhotoContract.State?) {
        when (state) {
            is EditPhotoContract.State.PaintScreen -> {
                state.selectedColor?.let(::onSelectedColor)
                binding.clPaintContainer.show()
                sheetBinding.tvTitle.text = getString(R.string.paint)
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
                    .getIntArray(R.array.material_colors)
                    .map { CircleProperty(it, ContextCompat.getColor(root.context, R.color.material_color_yellow_900)) },
                WeakReference(this@AddPaintDelegate)
            )
            rvPaintColorPicker.adapter = colorPickerAdapter
            ivLine.setOnClickListener {
                viewModel.sendEvent(EditPhotoContract.Event.PaintTabPressed(Mode.LINE))
            }
            ivArrow.setOnClickListener {
                viewModel.sendEvent(EditPhotoContract.Event.PaintTabPressed(Mode.ARROW))
            }
            ivCircle.setOnClickListener {
                viewModel.sendEvent(EditPhotoContract.Event.PaintTabPressed(Mode.CIRCLE))
            }
        }
    }

    override fun clear() {
        viewModel.sendEvent(EditPhotoContract.Event.ClearPaint)
    }

    override fun onSelectedColor(color: Int) {
        viewModel.sendEvent(EditPhotoContract.Event.PaintColorPicked(color))
    }
}