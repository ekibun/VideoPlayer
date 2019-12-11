@file:Suppress("DEPRECATION")

package soko.ekibun.util

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.Headers
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.hippo.image.Image

/**
 * 防止Glide崩溃
 */
object GlideUtil {

    const val TYPE_RESOURCE = 0
    const val TYPE_PLACEHOLDER = 1
    const val TYPE_ERROR = 2

    /**
     * Glide进度
     */
    fun loadWithProgress(url: String, context: Context, onProgress: (Float)->Unit, callback: (Int, Bitmap?) -> Unit): Target<Bitmap>? {
        val request = with(context) ?: return null
        ProgressAppGlideModule.expect(url, object : ProgressAppGlideModule.UIonProgressListener {
            override fun onProgress(bytesRead: Long, expectedLength: Long) {
                onProgress(bytesRead * 1f / expectedLength)
            }

            override fun getGranualityPercentage(): Float {
                return 1.0f
            }
        })
        return request.asBitmap().load(GlideUrl(url, Headers { mapOf("referer" to url) })).into(object : SimpleTarget<Bitmap>() {
            override fun onLoadStarted(placeholder: Drawable?) {
                callback(TYPE_PLACEHOLDER, null)
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {
                callback(TYPE_ERROR, null)
            }

            override fun onLoadCleared(placeholder: Drawable?) {
                callback(TYPE_PLACEHOLDER, null)
            }

            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                callback(TYPE_RESOURCE, resource)
            }

            override fun onDestroy() {
                ProgressAppGlideModule.forget(url)
            }
        })

    }

    fun with(context: Context): RequestManager? {
        return try {
            Glide.with(context)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    fun with(activity: Activity): RequestManager? {
        return try {
            Glide.with(activity)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    fun with(activity: FragmentActivity): RequestManager? {
        return try {
            Glide.with(activity)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    fun with(fragment: Fragment): RequestManager? {
        return try {
            Glide.with(fragment)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    fun with(view: View): RequestManager? {
        return try {
            Glide.with(view)
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}