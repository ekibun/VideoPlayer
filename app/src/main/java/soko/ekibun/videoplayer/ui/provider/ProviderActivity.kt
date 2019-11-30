package soko.ekibun.videoplayer.ui.provider

import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import kotlinx.android.synthetic.main.activity_provider.*
import soko.ekibun.util.JsonUtil
import soko.ekibun.util.KeyboardUtil
import soko.ekibun.util.ReflectUtil
import soko.ekibun.videoplayer.R
import soko.ekibun.videoplayer.ui.dialog.ProviderAdapter

class ProviderActivity : AppCompatActivity(), ColorPickerDialogListener {
    override fun onDialogDismissed(dialogId: Int) {
    }

    override fun onColorSelected(dialogId: Int, color: Int) {
        item_provider_color_hex.text = colorToString(color)
        item_provider_color_prev.backgroundTintList = ColorStateList.valueOf(color)
    }

    val color get() = Color.parseColor(item_provider_color_hex.text.toString())

    private fun colorToString(color: Int): String {
        return "#" + String.format("%08x", color).substring(2)
    }

    val clazz: Class<*> by lazy { intent?.getSerializableExtra(EXTRA_PROVIDER_CLASS) as Class<*> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_provider)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        KeyboardUtil(this, root_layout)

        item_codes.layoutManager = LinearLayoutManager(this)

        item_provider_color_hex.text = colorToString(0)
        setProvider(intent?.getStringExtra(EXTRA_PROVIDER_INFO))

        item_provider_color.setOnClickListener {
            ColorPickerDialog.newBuilder().setColor(color)
                .setDialogType(ColorPickerDialog.TYPE_CUSTOM)
                .setAllowPresets(false)
                .setDialogId(0)
                .show(this)
        }
    }

    var adapter: CodeAdapter? = null
    private fun setProvider(info: String?) {
        val provider = JsonUtil.toEntity(info?:"{}", clazz) as ProviderAdapter.ProviderInfo
        adapter = adapter ?: CodeAdapter(provider, ReflectUtil.getAllFields(clazz).filter {
            it.isAnnotationPresent(ProviderAdapter.Code::class.java)
        }.sortedBy { it.getAnnotation(ProviderAdapter.Code::class.java)!!.index })
        adapter?.provider = provider
        item_codes.adapter = adapter

        item_provider_site.setText(provider.site)
        item_provider_color_hex.text = colorToString(provider.color)
        item_provider_color_prev.backgroundTintList = ColorStateList.valueOf(color)
        item_provider_title.setText(provider.title)
    }

    private fun processBack() {
        AlertDialog.Builder(this).setMessage("保存修改？")
            .setPositiveButton("确定") { _: DialogInterface, _: Int ->
                setResult(getProvider())
            }.setNegativeButton("取消") { _: DialogInterface, _: Int ->
                finish()
            }.show()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            processBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun getProvider(): ProviderAdapter.ProviderInfo {
        val provider = adapter!!.provider
        provider.site = item_provider_site.text.toString()
        provider.title = item_provider_title.text.toString()
        provider.color = color
        return provider
    }

    private fun setResult(info: ProviderAdapter.ProviderInfo?) {
        val intent = Intent()
        if (info != null) intent.putExtra(EXTRA_PROVIDER_INFO, JsonUtil.toJson(info))
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    val clipboardManager by lazy { getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> processBack()
            R.id.action_remove -> {
                AlertDialog.Builder(this).setMessage("删除这个接口？")
                    .setPositiveButton("确定") { _: DialogInterface, _: Int ->
                        setResult(null)
                    }.show()
            }
            R.id.action_submit -> {
                setResult(getProvider())
            }
            R.id.action_inport -> {
                clipboardManager.primaryClip?.getItemAt(0)?.text?.toString()?.let {
                    setProvider(it)
                } ?: {
                    Snackbar.make(root_layout, "剪贴板没有数据", Snackbar.LENGTH_LONG).show()
                }()
            }
            R.id.action_export -> {
                clipboardManager.setPrimaryClip(
                    ClipData.newPlainText(
                        "videoplayer.providerInfo",
                        JsonUtil.toJson(getProvider())
                    )
                )
                Snackbar.make(root_layout, "数据已导出至剪贴板", Snackbar.LENGTH_LONG).show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.action_edit, menu)
        if (JsonUtil.toEntity(intent?.getStringExtra(EXTRA_PROVIDER_INFO) ?: "", clazz) == null) {
            menu?.findItem(R.id.action_remove)?.isVisible = false
        }
        return true
    }

    companion object {
        const val EXTRA_PROVIDER_INFO = "extraProvider"
        const val EXTRA_PROVIDER_CLASS = "extraProviderClass"
    }
}
