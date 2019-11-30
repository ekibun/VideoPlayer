package soko.ekibun.videoplayer.ui.manga

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_manga.*
import soko.ekibun.util.AppUtil
import soko.ekibun.videoplayer.App
import soko.ekibun.videoplayer.R
import soko.ekibun.videoplayer.bean.VideoSubject
import soko.ekibun.videoplayer.model.MangaProvider
import soko.ekibun.videoplayer.model.SubjectProvider
import soko.ekibun.videoplayer.ui.dialog.ProviderAdapter
import soko.ekibun.videoplayer.ui.setting.SettingsActivity

class MangaActivity : ProviderAdapter.LineProviderActivity<MangaProvider.ProviderInfo>() {
    override val classT = MangaProvider.ProviderInfo::class.java
    override val fileType: String = "image/*"
    override val lineProvider by lazy { App.from(this).mangaProvider }
    
    private val subjectPresenter by lazy { SubjectPresenter(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manga)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        subjectPresenter.init()
    }

    override fun onDestroy() {
        super.onDestroy()
        subjectPresenter.destroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK){
            processBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> processBack()
            R.id.action_share -> AppUtil.shareString(this, "${subjectPresenter.subject.name}\n${subjectPresenter.subject.url}")
            R.id.action_refresh -> subjectPresenter.refreshSubject()
            R.id.action_settings -> SettingsActivity.startActivity(this)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.action_video, menu)
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        subjectPresenter.onActivityResult(requestCode, resultCode, data)
        subjectPresenter.refreshSubject()
    }

    companion object {
        fun startActivity(context: Context, subject: VideoSubject, newTask:Boolean = false) {
            context.startActivity(parseIntent(context, subject, newTask))
        }

        fun parseIntent(context: Context, subject: VideoSubject, newTask:Boolean = true): Intent {
            val intent = Intent(context, MangaActivity::class.java)
            if(newTask) intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
            intent.putExtra(SubjectProvider.EXTRA_SUBJECT, subject)
            return intent
        }
    }
}
