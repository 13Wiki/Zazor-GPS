package com.gps.zazor.ui.photo.editPhoto.delegates

import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.gps.zazor.R
import com.gps.zazor.databinding.BottomSheetAddNoteBinding
import com.gps.zazor.databinding.IncludeTextContentBinding
import com.gps.zazor.ui.photo.editPhoto.EditPhotoContract
import com.gps.zazor.ui.photo.editPhoto.EditPhotoViewModel
import com.gps.zazor.utils.FontsFactory
import com.gps.zazor.utils.colorPicker.CircleProperty
import com.gps.zazor.utils.colorPicker.ColorPickerAdapter
import com.gps.zazor.utils.colorPicker.OnSelectedColorListener
import com.gps.zazor.utils.extensions.hide
import com.gps.zazor.utils.extensions.show
import java.lang.ref.WeakReference

class AddOverlayDelegate(sheetBinding: BottomSheetAddNoteBinding,
                         viewModel: EditPhotoViewModel): EditPhotoDelegate(sheetBinding, viewModel),
    OnSelectedColorListener {

    private val binding = IncludeTextContentBinding.bind(sheetBinding.clRoot)

    private val fontsAdapter by lazy {
        ArrayAdapter(
            sheetBinding.root.context, android.R.layout.simple_list_item_1,
            sheetBinding.root.context.resources.getStringArray(
                R.array.fonts
            ))
    }

    private val fonts = FontsFactory.getFonts()

    private lateinit var colorPickerAdapter: ColorPickerAdapter

    override fun observeState(state: EditPhotoContract.State?) {
        when (state) {
            is EditPhotoContract.State.TextScreen -> {
                updateArtState(binding.etText.text.toString(), state.selectedColor, state.selectedFont)
                initFontsSelector(fonts.indexOf(state.selectedFont ?: 0))
                sheetBinding.tvTitle.text = getString(R.string.add_text)
                binding.llWordSettings.show()
            }
            else -> binding.llWordSettings.hide()
        }
    }

    override fun onShown() {
        super.onShown()
        with(binding) {
            rvColorPicker.layoutManager =
                LinearLayoutManager(
                    root.context,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )
            colorPickerAdapter = ColorPickerAdapter(
                root.context,
                root.context.resources
                    .getIntArray(R.array.material_colors)
                    .map { CircleProperty(it, ContextCompat.getColor(root.context, R.color.material_color_yellow_900)) },
                WeakReference(this@AddOverlayDelegate)
            )
            rvColorPicker.adapter = colorPickerAdapter
            etText.doOnTextChanged { text, _, _, _ ->
                updateArtState(text = text.toString())
            }
        }
    }

    override fun clear() {
        binding.etText.setText("")
    }

    override fun onSelectedColor(color: Int) {
        updateArtState(binding.etText.text.toString(), color)
    }

    private fun initFontsSelector(position: Int) {
        with(binding) {
            spFonts.adapter = fontsAdapter
            spFonts.setSelection(position)
            spFonts.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    updateArtState(binding.etText.text.toString(),
                        fontId = fonts[position])
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // do nothing
                }
            }
        }
    }

    private fun updateArtState(text: String, color: Int? = null, fontId: Int? = null) {
        viewModel.sendEvent(EditPhotoContract.Event.OverlayEntered(text, color, fontId))
    }
}