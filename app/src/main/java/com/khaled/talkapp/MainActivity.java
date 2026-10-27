package com.khaled.talkapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class MainActivity extends Activity {

    // Live Cloudflare quick tunnel to the sandbox's server — ephemeral, dies on
    // sandbox/tunnel restart. Re-check the current URL before rebuilding.
    private static final String SERVER_URL = "https://decreased-hide-soundtrack-arch.trycloudflare.com";
    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);

        webView = new WebView(this);
        setContentView(webView);
        // Only in a debuggable build: WebView contents debugging lets chrome://inspect
        // attach for diagnosing WebView-only bugs. Leaving it enabled in a release APK
        // would let anyone with adb access attach to the WebView and run JS against the
        // signed-in session. The previous code claimed "debug build only" but never
        // actually gated it — this enforces that intent.
        if ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(() -> {
                    // The web app only needs the microphone. Grant *only* audio
                    // capture, and only when it was actually requested and we hold
                    // RECORD_AUDIO — never blanket-grant request.getResources(),
                    // which could include camera or other resources we neither have
                    // an OS permission for nor want to expose to remote web content.
                    boolean wantsAudio = false;
                    for (String r : request.getResources()) {
                        if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(r)) {
                            wantsAudio = true;
                            break;
                        }
                    }
                    if (wantsAudio && checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                            == PackageManager.PERMISSION_GRANTED) {
                        request.grant(new String[]{ PermissionRequest.RESOURCE_AUDIO_CAPTURE });
                    } else {
                        request.deny();
                    }
                });
            }
        });

        webView.loadUrl(SERVER_URL);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
