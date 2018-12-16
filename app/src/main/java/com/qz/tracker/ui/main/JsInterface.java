package com.qz.tracker.ui.main;

import android.webkit.WebView;

public class JsInterface {

    private WebView webView;

    public JsInterface(WebView webView) {
        this.webView = webView;
    }

    @android.webkit.JavascriptInterface
    public void on(String event) {
        System.out.println(event);
    }
}
