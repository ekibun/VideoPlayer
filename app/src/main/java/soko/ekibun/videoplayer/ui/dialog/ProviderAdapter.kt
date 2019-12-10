package soko.ekibun.videoplayer.ui.dialog

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import com.zhy.adapter.abslistview.CommonAdapter
import com.zhy.adapter.abslistview.ViewHolder
import soko.ekibun.util.AppUtil
import soko.ekibun.util.JsonUtil
import soko.ekibun.util.StorageUtil
import soko.ekibun.videoplayer.JsEngine
import soko.ekibun.videoplayer.model.LineInfoModel
import soko.ekibun.videoplayer.ui.provider.ProviderActivity

class ProviderAdapter(context: Context?, data: List<ProviderInfo>?) : CommonAdapter<ProviderAdapter.ProviderInfo>(context, android.R.layout.simple_spinner_dropdown_item, data) {

    @Target(AnnotationTarget.FIELD)
    annotation class Code(val label: String, val index: Int)

    open class ProviderInfo(
        var site: String,
        var color: Int,
        var title: String,
        @Code("搜索", 0) val search: String = ""
    ){
        fun search(scriptKey: String, jsEngine: JsEngine, key: String): JsEngine.ScriptTask<List<LineInfoModel.LineInfo>>{
            return JsEngine.ScriptTask(jsEngine,"var key = ${JsonUtil.toJson(key)};\n$search", scriptKey){
                JsonUtil.toEntity<List<LineInfoModel.LineInfo>>(it)?:ArrayList()
            }
        }

        override fun hashCode(): Int {
            return site.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            if (site != (other as ProviderInfo).site) return false
            return true
        }
    }

    interface LineProvider<T: ProviderInfo> {
        val providerList: HashMap<String, T>
        fun getProvider(site: String): T?
        fun addProvider(provider: T)
        fun removeProvider(site: String)
    }

    abstract class LineProviderActivity<T: ProviderInfo>: AppCompatActivity() {
        abstract val lineProvider: LineProvider<T>
        abstract val classT: Class<T>
        abstract val fileType: String

        private var loadFileCallback:((String?)-> Unit)? = null
        fun loadFile(callback: (String?) -> Unit) {
            loadFileCallback = callback
            if (!AppUtil.checkStorage(this)) return
            val intent = Intent()
            intent.type = fileType
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(intent, AppUtil.REQUEST_FILE_CODE)
        }

        private var loadProviderCallback:((T?)-> Unit)? = null
        fun loadProvider(info: T?, callback: (T?) -> Unit) {
            loadProviderCallback = callback
            val intent = Intent(this, ProviderActivity::class.java)
            intent.putExtra(ProviderActivity.EXTRA_PROVIDER_CLASS, classT)
            info?.let { intent.putExtra(ProviderActivity.EXTRA_PROVIDER_INFO, JsonUtil.toJson(it)) }
            startActivityForResult(intent, AppUtil.REQUEST_PROVIDER)
        }

        override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            if (requestCode == AppUtil.REQUEST_STORAGE_CODE) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED && loadFileCallback != null) {
                    loadFile(loadFileCallback!!)
                } else {
                    loadFileCallback?.invoke(null)
                    loadFileCallback = null
                }
            }
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)

            if (requestCode == AppUtil.REQUEST_FILE_CODE && resultCode == RESULT_OK) {//文件
                val uri = data?.data?: return
                val path = StorageUtil.getRealPathFromUri(this, uri)
                loadFileCallback?.invoke(path)
            }

            if (requestCode == AppUtil.REQUEST_PROVIDER && resultCode == RESULT_OK) {//Provider
                loadProviderCallback?.invoke(JsonUtil.toEntity(data?.getStringExtra(
                    ProviderActivity.EXTRA_PROVIDER_INFO)?:"", classT))
            }
        }
    }

    override fun convert(viewHolder: ViewHolder, item: ProviderInfo, position: Int) {
        viewHolder.setText(android.R.id.text1, item.title)
    }
}