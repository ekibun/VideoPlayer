package soko.ekibun.videoplayer.ui.provider

import android.text.Editable
import android.text.TextWatcher
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_code_panel.view.*
import soko.ekibun.videoplayer.ui.dialog.ProviderAdapter
import java.lang.reflect.Field
import soko.ekibun.videoplayer.R

class CodeAdapter(
    var provider: ProviderAdapter.ProviderInfo,
    data: List<Field>?) : BaseQuickAdapter<Field, BaseViewHolder>(R.layout.item_code_panel, data) {
    override fun convert(helper: BaseViewHolder, item: Field) {
        helper.itemView.item_label.text = item.getAnnotation(ProviderAdapter.Code::class.java)?.label
        item.isAccessible = true
        helper.itemView.item_code.codeText.setText(item.get(provider) as? String)
        helper.itemView.item_code.codeText.addTextChangedListener(object: TextWatcher{
            override fun afterTextChanged(s: Editable?) {
                item.set(provider, s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { /* no-op */ }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { /* no-op */ }
        })
    }
}