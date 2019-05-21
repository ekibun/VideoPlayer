package soko.ekibun.videoplayer.model

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import soko.ekibun.videoplayer.IVideoSubjectProvider
import soko.ekibun.videoplayer.bean.VideoEpisode
import soko.ekibun.videoplayer.bean.VideoSubject
import soko.ekibun.videoplayer.callback.IListEpisodeCallback
import soko.ekibun.videoplayer.callback.ISubjectCallback

class SubjectProvider(val context: AppCompatActivity, val listener: OnChangeListener): ServiceConnection {
    interface OnChangeListener{
        fun onSubjectChange(subject: VideoSubject)
        fun onEpisodeListChange(eps: List<VideoEpisode>, merge: Boolean)
    }

    var subject = context.intent.getParcelableExtra<VideoSubject>(EXTRA_SUBJECT)!!
    var aidl: IVideoSubjectProvider? = null

    fun bindService(){
        val aidlIntent = Intent("soko.ekibun.videoplayer.subjectprovider.${subject.site}")
        val resloveInfos = context.packageManager.queryIntentServices(aidlIntent, 0)
        aidlIntent.component = ComponentName(resloveInfos[0].serviceInfo.packageName, resloveInfos[0].serviceInfo.name)
        context.bindService(aidlIntent, this, Context.BIND_AUTO_CREATE)
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(resultCode != AppCompatActivity.RESULT_OK) return
        when(requestCode){
            REQUEST_UPADTE_COLLECTION -> {
                subject = data?.getParcelableExtra(EXTRA_SUBJECT)?:subject
                listener.onSubjectChange(subject)
            }
            REQUEST_UPADTE_PROGRESS -> {
                val eps = data?.getParcelableArrayListExtra<VideoEpisode>(EXTRA_EPISODE_LIST)?: return
                listener.onEpisodeListChange(eps, true)
            }
        }
    }

    fun refreshSubject() {
        aidl?.refreshSubject(subject, object: ISubjectCallback.Stub() {
            override fun onFinish(result: VideoSubject) {
                subject = result
                listener.onSubjectChange(subject)
            }
            override fun onReject(reason: String) {}
        })
    }

    fun refreshEpisode() {
        aidl?.refreshEpisode(subject, object: IListEpisodeCallback.Stub() {
            override fun onFinish(result: MutableList<VideoEpisode>) {
                listener.onEpisodeListChange(result, false)
            }
            override fun onReject(reason: String) {}
        })
    }

    fun updateCollection(){
        val dialogIntent = Intent("soko.ekibun.videoplayer.updateCollection.${subject.site}")
        dialogIntent.putExtra(EXTRA_SUBJECT, subject)
        context.startActivityForResult(dialogIntent, REQUEST_UPADTE_COLLECTION)
    }

    fun updateProgress(eps: List<VideoEpisode>){
        val dialogIntent = Intent("soko.ekibun.videoplayer.updateProgress.${subject.site}")
        dialogIntent.putExtra(EXTRA_SUBJECT, subject)
        dialogIntent.putParcelableArrayListExtra(EXTRA_EPISODE_LIST, ArrayList(eps))
        context.startActivityForResult(dialogIntent, REQUEST_UPADTE_PROGRESS)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        aidl = null
        if(!context.isDestroyed) bindService()
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder) {
        aidl = IVideoSubjectProvider.Stub.asInterface(service)
        refreshSubject()
        refreshEpisode()
    }

    companion object {
        const val REQUEST_UPADTE_COLLECTION = 101
        const val REQUEST_UPADTE_PROGRESS = 102
        const val EXTRA_SUBJECT = "extraSubject"
        const val EXTRA_EPISODE_LIST = "extraEpisodeList"
    }
}