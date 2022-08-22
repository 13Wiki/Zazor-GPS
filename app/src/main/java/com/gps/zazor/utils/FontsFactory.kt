package com.gps.zazor.utils

import android.graphics.Typeface
import com.gps.zazor.R

internal object FontsFactory {

    internal fun getFonts() = arrayOf(
            Typeface.DEFAULT.style, R.font.indie_flower,
            R.font.lora_bold, R.font.lora_bold_italic,
            R.font.lora_italic, R.font.lora_regular,
            R.font.noto_nans_italic, R.font.noto_sans_bold,
            R.font.noto_sans_bold_italic, R.font.noto_sans_regular,
            R.font.playfair_display_black, R.font.playfair_display_black_italic,
            R.font.playfair_display_bold, R.font.playfair_display_bold_italic,
            R.font.playfair_display_italic, R.font.playfair_display_regular,
            R.font.raleway_black)
}
