package soko.ekibun.util

import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import androidx.annotation.Keep
import okhttp3.*
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import soko.ekibun.videoplayer.model.VideoProvider
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URI
import java.util.zip.Inflater

object HttpUtil {
    @Keep
    fun makeRequest(request: WebResourceRequest): VideoProvider.VideoRequest{
        val headers = HashMap(request.requestHeaders)
        headers["cookie"] = CookieManager.getInstance().getCookie(request.url.host)?:""
        return VideoProvider.VideoRequest(request.url.toString(), headers)
    }

    @Keep
    fun createBody(data: Map<String, String>): RequestBody{
        val builder = FormBody.Builder()
        data.forEach { builder.add(it.key, it.value) }
        return builder.build()
    }
    @Keep
    fun createBody(mediaType: String, data: String): RequestBody{
        return data.toRequestBody(mediaType.toMediaTypeOrNull())
    }
    @Keep
    fun getCall(url: String, header: Map<String, String> = HashMap(), body: RequestBody? = null): Call {
        val request = Request.Builder()
                .url(url)
                .headers(header.toHeaders())
        if (body != null)
            request.post(body)
        return OkHttpClient.Builder().cookieJar(WebViewCookieHandler()).build().newCall(request.build())
    }

    @Keep
    fun getUrl(url: String, baseUri: URI?): String{
        return try{
            baseUri?.resolve(url)?.toASCIIString() ?: URI.create(url).toASCIIString()
        }catch (e: Exception){ url }
    }

    @Keep
    fun inflate(data: ByteArray, nowrap: Boolean = false): ByteArray {
        var output: ByteArray

        val inflater = Inflater(nowrap)
        inflater.reset()
        inflater.setInput(data)

        val o = ByteArrayOutputStream(data.size)
        try {
            val buf = ByteArray(1024)
            while (!inflater.finished()) {
                val i = inflater.inflate(buf)
                o.write(buf, 0, i)
            }
            output = o.toByteArray()
        } catch (e: java.lang.Exception) {
            output = data
            e.printStackTrace()
        } finally {
            try {
                o.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        inflater.end()
        return output
    }
}