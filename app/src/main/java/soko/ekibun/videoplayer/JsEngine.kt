package soko.ekibun.videoplayer

import android.os.AsyncTask
import android.util.Log
import android.webkit.WebResourceRequest
import androidx.annotation.Keep
import org.mozilla.javascript.Context
import org.mozilla.javascript.ScriptableObject
import soko.ekibun.util.HttpUtil
import soko.ekibun.util.JsonUtil
import soko.ekibun.videoplayer.model.VideoProvider
import soko.ekibun.videoplayer.ui.view.BackgroundWebView
import java.util.*

class JsEngine(val context: App) {
    private val webviewList = WeakHashMap<String, BackgroundWebView>()
    @Keep
    fun webviewload(key: String, url: String, header: Map<String, String>, script: String, onInterceptRequest: (WebResourceRequest)->VideoProvider.VideoRequest?): VideoProvider.VideoRequest?{
        var ret: VideoProvider.VideoRequest? = null
        var finished = false
        var pageFinish = false
        var lastTime = System.currentTimeMillis()
        context.handler.post {
            val webview = webviewList.getOrPut(key){ BackgroundWebView(context) }
            webview.settings.userAgentString = header["User-Agent"]?:webview.settings.userAgentString
            val map = HashMap<String, String>()
            map["referer"]=url
            map.putAll(header)
            webview.onInterceptRequest = {
                onInterceptRequest(it)?.let{
                    ret = it
                    finished = true
                    webview.onInterceptRequest = {}
                    webview.onPageFinished = {}
                }
                lastTime = System.currentTimeMillis()
            }
            webview.onPageFinished = {
                webview.evaluateJavascript(script) {
                    Log.v("javascript", it.toString())
                    JsonUtil.toEntity(it, VideoProvider.VideoRequest::class.java)?.let{
                        ret = it
                        finished = true
                        webview.onInterceptRequest = {}
                        webview.onPageFinished = {}
                    }
                }
                lastTime = System.currentTimeMillis()
                pageFinish = true
            }
            webview.loadUrl(url, map)
        }
        while(!finished){
            Thread.sleep(1000)
            if(pageFinish && System.currentTimeMillis() - lastTime > 30 * 1000){//30sec Timeout
                return ret
            }
        }
        return ret
    }
    @Keep
    fun print(msg: String){
        Log.v("JsEngine", msg)
    }

    fun runScript(script: String, key: String): String{
        val methods = """
            |var _http = Packages.${HttpUtil.javaClass.name}.INSTANCE;
            |var _json = Packages.${JsonUtil.javaClass.name}.INSTANCE;
            |var Jsoup = Packages.org.jsoup.Jsoup;
            |var http = {
            |   get(url, header) {
            |       return _http.getCall(url, header||{}, null).execute();
            |   },
            |   post(url, header, data, mediaType){
            |       if(mediaType) return _http.getCall(url, header||{}, _http.createBody(mediaType, data)).execute();
            |       else return _http.getCall(url, header||{}, _http.createBody(data)).execute();
            |   },
            |   inflate(bytes, encoding){
            |       if(encoding == "deflate"){
            |           return "" + new java.lang.String(_http.inflate(bytes, true));
            |       }else if(encoding == "gzip"){
            |           return "" + new java.lang.String(_http.inflate(bytes, false));
            |       }else return "" + new java.lang.String(bytes, encoding);
            |   }
            |}
            |var webview = {
            |   toRequest(request){
            |       return _http.makeRequest(request);
            |   },
            |   load(url, header, script, onInterceptRequest){
            |       return _jsEngine.webviewload(${ JsonUtil.toJson(key) }, url||"", header||{}, script||"", onInterceptRequest||function(request){
            |           if(request.getRequestHeaders().get("Range") != null) return webview.toRequest(request);
            |           else return null;
            |       });
            |   }
            |}
            |function print(obj){
            |   _jsEngine.print("" + obj);
            |}
        """.trimMargin()

        return try {
            val rhino = Context.enter()
            rhino.wrapFactory.isJavaPrimitiveWrap = false
            rhino.optimizationLevel = -1
            val scope = rhino.initStandardObjects()
            ScriptableObject.putProperty(scope, "_jsEngine", Context.javaToJS(this, scope))
            rhino.evaluateString(scope, methods, "header", 1, null)
            rhino.evaluateString(scope, """(function(){var _ret = (function(){$script
                |   }());
                |   if(_ret instanceof Packages.java.lang.Object) return _json.toJson(_ret);
                |   else return JSON.stringify(_ret);
                |}())""".trimMargin(), "script", 0, null).toString()
        }finally {
            webviewList.remove(key)?.finish()
            Context.exit()
        }
    }

    class ScriptTask<T>(private val jsEngine: JsEngine, private val script: String, private val key: String, val converter: (String)->T): AsyncTask<String, Unit, String>(){
        var onFinish: (T)->Unit = {}
        var onReject: (Exception)->Unit = {}
        var exception: Exception? = null
        override fun doInBackground(vararg params: String?): String? {
            return try{ jsEngine.runScript(script, key) }
            catch (e: InterruptedException) { null }
            catch(e: Exception){ exception = e; null }
        }
        override fun onPostExecute(result: String?) {
            result?.let { try{ onFinish(converter(it)) }
                catch(e: Exception){ exception = e } }
            exception?.let { it.printStackTrace(); onReject(it) }
            super.onPostExecute(result)
        }

        fun enqueue(onFinish: (T)->Unit, onReject: (Exception)->Unit){
            this.onFinish = onFinish
            this.onReject = onReject
            this.executeOnExecutor(App.cachedThreadPool)
        }
    }
}