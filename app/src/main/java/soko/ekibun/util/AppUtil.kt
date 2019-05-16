package soko.ekibun.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import com.bumptech.glide.load.resource.gif.GifDrawable
import java.io.File
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import java.io.FileOutputStream
import androidx.core.content.FileProvider
import android.util.Size
import android.view.WindowManager
import java.nio.ByteBuffer


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

    fun shareDrawable(context: Context, drawable: Drawable){
        try {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs() // don't forget to make the directory
            val stream = FileOutputStream(cachePath.toString() + "/image", false) // overwrites this image every time
            if(drawable is GifDrawable){
                val newGifDrawable = (drawable.constantState!!.newDrawable().mutate()) as GifDrawable
                val byteBuffer = newGifDrawable.buffer
                val bytes = ByteArray(byteBuffer.capacity())
                (byteBuffer.duplicate().clear() as ByteBuffer).get(bytes)
                stream.write(bytes, 0 ,bytes.size)
            }else if(drawable is BitmapDrawable){
                drawable.bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            stream.close()

            val imageFile = File(cachePath, "image")
            val contentUri = FileProvider.getUriForFile(context, "soko.ekibun.bangumi.fileprovider", imageFile)

            if (contentUri != null) {
                val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // temp permission for receiving app to read this file
                shareIntent.setDataAndType(contentUri, "image/*")
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
                context.startActivity(Intent.createChooser(shareIntent, "分享图片"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun openBrowser(context: Context, url: String){
        try{
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        }catch(e: Exception){ e.printStackTrace() }
    }

    fun getVersionName(context: Context): String {
        return getPackageInfo(context)?.versionName?:""
    }

    fun getVersionCode(context: Context): Int {
        return getPackageInfo(context)?.versionCode?:0
    }

    private fun getPackageInfo(context: Context): PackageInfo? {
        return context.packageManager.getPackageInfo(context.packageName,
                PackageManager.GET_CONFIGURATIONS)
    }
}