package soko.ekibun.videoplayer.callback;

import soko.ekibun.videoplayer.bean.VideoSubject;

interface ISubjectCallback {
    oneway void onFinish(in VideoSubject reason);
    oneway void onReject(String reason);
}