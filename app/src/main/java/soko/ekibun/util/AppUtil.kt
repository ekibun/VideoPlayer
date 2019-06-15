package soko.ekibun.util

import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.util.Size
import android.view.WindowManager


object AppUtil {
    fun getScreenSize(context: Context): Size{
        val p = Point()
        (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealSize(p)
        return Size(Math.min(p.x, p.y), Math.max(p.x, p.y))
    }

    fun shareString(context: Context, str: String){
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_TEXT, str)
        intent.type = "text/plain"
        context.startActivity(Intent.createChooser(intent, "share"))
    }
}