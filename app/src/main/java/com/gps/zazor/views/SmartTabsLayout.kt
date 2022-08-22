package com.gps.zazor.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import com.ogaclejapan.smarttablayout.SmartTabLayout

class SmartTabsLayout
@JvmOverloads
constructor(context: Context, attrs: AttributeSet?, defStyle: Int = 0)
    : SmartTabLayout(context, attrs, defStyle) {

    override fun onTouchEvent(ev: MotionEvent?) = false
}