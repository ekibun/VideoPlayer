package soko.ekibun.util;

import android.webkit.CookieManager;
import kotlin.Function;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import soko.ekibun.videoplayer.JsEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WebViewCookieHandler implements CookieJar {
    private CookieManager mCookieManager = CookieManager.getInstance();

    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) { }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {
        String urlString = url.toString();
        String cookiesString = mCookieManager.getCookie(urlString);

        if (cookiesString != null && !cookiesString.isEmpty()) {
            String[] cookieHeaders = cookiesString.split(";");
            List<Cookie> cookies = new ArrayList<>(cookieHeaders.length);

            for (String header : cookieHeaders) {
                cookies.add(Cookie.parse(url, header));
            }

            return cookies;
        }

        return Collections.emptyList();
    }
}