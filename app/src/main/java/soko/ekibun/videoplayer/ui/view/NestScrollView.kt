package soko.ekibun.videoplayer.ui.view

import android.content.Context
import androidx.core.widget.NestedScrollView
import android.util.AttributeSet
import android.view.MotionEvent

class NestScrollView constructor(context: Context, attrs: AttributeSet): NestedScrollView(context, attrs){
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        parent.requestDisallowInterceptTouchEvent(true)
        return super.dispatchTouchEvent(ev)
    }
}