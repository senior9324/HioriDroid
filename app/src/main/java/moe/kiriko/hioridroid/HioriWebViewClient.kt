package moe.kiriko.hioridroid

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.File
import java.io.FileInputStream

class HioriWebViewClient(var mContext: Context): WebViewClient() {
    @TargetApi(Build.VERSION_CODES.N)
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest): Boolean {
        if (urlConfirm(request.url.toString())) {
            view?.loadUrl(request.url.toString());
            return true;
        }
        return super.shouldOverrideUrlLoading(view, request)
    }

    @Suppress("OverridingDeprecatedMember", "DEPRECATION")
    override fun shouldOverrideUrlLoading(view: WebView?, url: String): Boolean {
        if (urlConfirm(url)) {
            view?.loadUrl(url);
            return true;
        }
        return super.shouldOverrideUrlLoading(view, url)
    }

    fun urlConfirm(url: String): Boolean {
        return url.startsWith("https://shinycolors.enza.fun");
    }

    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        if (request.url.toString().startsWith("https://shinycolors.enza.fun/assets/")) {
            val filename = request.url.toString().split("/assets/")[1]
            val resourceFile = File(mContext.filesDir.absolutePath + "/assets/image/" + filename + ".png")
            if (resourceFile.exists())
                return WebResourceResponse(
                        "binary/octet-stream",
                        null,
                        FileInputStream(resourceFile))
        }
        return super.shouldInterceptRequest(view, request)
    }

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        val script = File(mContext.filesDir.absolutePath + "/assets/injects.js")
        view.evaluateJavascript(script.readText(), null)
    }
}