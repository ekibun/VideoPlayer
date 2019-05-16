package soko.ekibun.videoplayer.ui.provider

import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import com.google.android.material.snackbar.Snackbar
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import kotlinx.android.synthetic.main.activity_provider.*
import kotlinx.android.synthetic.main.activity_provider.root_layout
import kotlinx.android.synthetic.main.activity_provider.toolbar
import soko.ekibun.util.JsonUtil
import soko.ekibun.util.KeyboardUtil
import soko.ekibun.videoplayer.R
import soko.ekibun.videoplayer.model.VideoProvider

class ProviderActivity : AppCompatActivity(), ColorPickerDialogListener {
    override fun onDialogDismissed(dialogId: Int) {
    }

    override fun onColorSelected(dialogId: Int, color: Int) {
        item_provider_color_hex.text = colorToString(color)
        item_provider_color_prev.backgroundTintList = ColorStateList.valueOf(color)
    }

    val color get()= Color.parseColor(item_provider_color_hex.text.toString())

    fun colorToString(color: Int): String{
        return "#" + String.format("%08x", color).substring(2)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_provider)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        KeyboardUtil(this, root_layout)

        item_provider_color_hex.text = colorToString(0)
        JsonUtil.toEntity(intent?.getStringExtra(EXTRA_PROVIDER_INFO)?:"", VideoProvider.ProviderInfo::class.java)?.let{ setProvider(it) }

        item_provider_color.setOnClickListener {
            ColorPickerDialog.newBuilder().setColor(color)
                .setDialogType(ColorPickerDialog.TYPE_CUSTOM)
                .setAllowPresets(false)
                .setDialogId(0)
                .show(this)
        }
    }

    fun setProvider(info: VideoProvider.ProviderInfo){
        item_provider_site.setText(info.site)
        item_provider_color_hex.text = colorToString(info.color)
        item_provider_color_prev.backgroundTintList = ColorStateList.valueOf(color)
        item_provider_title.setText(info.title)
        item_provider_search.codeText.setText(info.search)
        item_provider_get_video_info.codeText.setText(info.getVideoInfo)
        item_provider_get_video.codeText.setText(info.getVideo)
        item_provider_get_danmaku_key.codeText.setText(info.getDanmakuKey)
        item_provider_get_danmaku.codeText.setText(info.getDanmaku)
    }

    private fun processBack(){
        AlertDialog.Builder(this).setMessage("保存修改？")
            .setPositiveButton("确定"){ _: DialogInterface, _: Int ->
                setResult(getProvider())
            }.setNegativeButton("取消"){ _: DialogInterface, _: Int ->
                finish()
            }.show()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK){
            processBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun getProvider():VideoProvider.ProviderInfo {
        return VideoProvider.ProviderInfo(item_provider_site.text.toString(), color, item_provider_title.text.toString(),
            item_provider_search.codeText.text.toString(), item_provider_get_video_info.codeText.text.toString(), item_provider_get_video.codeText.text.toString(),
            item_provider_get_danmaku_key.codeText.text.toString(),item_provider_get_danmaku.codeText.text.toString())
    }

    private fun setResult(info: VideoProvider.ProviderInfo?){
        val intent = Intent()
        if(info != null) intent.putExtra(EXTRA_PROVIDER_INFO, JsonUtil.toJson(info))
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    val clipboardManager by lazy { getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> processBack()
            R.id.action_remove -> {
                AlertDialog.Builder(this).setMessage("删除这个接口？")
                    .setPositiveButton("确定"){ _: DialogInterface, _: Int ->
                        setResult(null)
                    }.show()
            }
            R.id.action_submit -> {
                setResult(getProvider())
            }
            R.id.action_inport -> {
                JsonUtil.toEntity(clipboardManager.primaryClip?.getItemAt(0)?.text?.toString()?:"", VideoProvider.ProviderInfo::class.java)?.let{
                    setProvider(it)
                }?:{
                    Snackbar.make(root_layout, "剪贴板没有数据", Snackbar.LENGTH_LONG).show()
                }()
            }
            R.id.action_export -> {
                clipboardManager.primaryClip = ClipData.newPlainText("videoplayer.providerInfo", JsonUtil.toJson(getProvider()))
                Snackbar.make(root_layout, "数据已导出至剪贴板", Snackbar.LENGTH_LONG).show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.action_edit, menu)
        if(JsonUtil.toEntity(intent?.getStringExtra(EXTRA_PROVIDER_INFO)?:"", VideoProvider.ProviderInfo::class.java) == null){
            menu?.findItem(R.id.action_remove)?.isVisible = false
        }
        return true
    }

    companion object {
        const val EXTRA_PROVIDER_INFO = "extraProvider"
    }
}
