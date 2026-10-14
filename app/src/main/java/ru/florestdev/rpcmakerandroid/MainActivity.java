package ru.florestdev.rpcmakerandroid;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String DISCORD_LOGIN_URL = "https://discord.com/login";

    private static final String[] TYPE_LABELS = {
            "Playing", "Streaming", "Listening", "Watching", "Competing"
    };
    private static final String[] TYPE_VALUES = {
            "playing", "streaming", "listening", "watching", "competing"
    };

    private static final String INJECT_SCRIPT =
            "(function() {" +
                    "    if (window.__rpcTokenHookInstalled) return;" +
                    "    window.__rpcTokenHookInstalled = true;" +
                    "    function sendToken(token) {" +
                    "        try {" +
                    "            if (token && token !== 'null' && window.AndroidBridge) {" +
                    "                window.AndroidBridge.onTokenReceived(String(token));" +
                    "            }" +
                    "        } catch (e) {}" +
                    "    }" +
                    "    function extractAuth(headers) {" +
                    "        if (!headers) return null;" +
                    "        try {" +
                    "            if (typeof headers.get === 'function') {" +
                    "                return headers.get('Authorization') || headers.get('authorization');" +
                    "            }" +
                    "            if (Array.isArray(headers)) {" +
                    "                for (var i = 0; i < headers.length; i++) {" +
                    "                    var pair = headers[i];" +
                    "                    if (pair && pair.length === 2 && (pair[0].toLowerCase() === 'authorization')) return pair[1];" +
                    "                }" +
                    "                return null;" +
                    "            }" +
                    "            if (typeof headers === 'object') {" +
                    "                for (var k in headers) { if (k.toLowerCase() === 'authorization') return headers[k]; }" +
                    "            }" +
                    "        } catch (e) {}" +
                    "        return null;" +
                    "    }" +
                    "    var originalFetch = window.fetch;" +
                    "    if (originalFetch) {" +
                    "        window.fetch = function(input, init) {" +
                    "            try {" +
                    "                var url = (typeof input === 'string') ? input : (input && input.url);" +
                    "                var headers = (init && init.headers) || (input && input.headers);" +
                    "                if (url && url.indexOf('/api/') !== -1) {" +
                    "                    var auth = extractAuth(headers);" +
                    "                    if (auth) sendToken(auth);" +
                    "                }" +
                    "            } catch (e) {}" +
                    "            return originalFetch.apply(this, arguments);" +
                    "        };" +
                    "    }" +
                    "    var XO = XMLHttpRequest.prototype.open;" +
                    "    var XS = XMLHttpRequest.prototype.setRequestHeader;" +
                    "    var XSend = XMLHttpRequest.prototype.send;" +
                    "    XMLHttpRequest.prototype.open = function(method, url) {" +
                    "        try { this.__rpcUrl = url; this.__rpcAuth = null; } catch (e) {}" +
                    "        return XO.apply(this, arguments);" +
                    "    };" +
                    "    XMLHttpRequest.prototype.setRequestHeader = function(name, value) {" +
                    "        try { if (name && name.toLowerCase() === 'authorization') this.__rpcAuth = value; } catch (e) {}" +
                    "        return XS.apply(this, arguments);" +
                    "    };" +
                    "    XMLHttpRequest.prototype.send = function() {" +
                    "        try {" +
                    "            if (this.__rpcUrl && this.__rpcUrl.indexOf('/api/') !== -1 && this.__rpcAuth) sendToken(this.__rpcAuth);" +
                    "        } catch (e) {}" +
                    "        return XSend.apply(this, arguments);" +
                    "    };" +
                    "    try {" +
                    "        var t = window.localStorage.getItem('token');" +
                    "        if (t && t !== 'null') sendToken(t.replace(/^\"|\"$/g, ''));" +
                    "    } catch (e) {}" +
                    "})();";

    private WebView webView;
    private ScrollView formContainer;

    private EditText etName, etDetails, etState;
    private Spinner spinnerType;
    private EditText etLargeImage, etLargeText, etSmallImage, etSmallText;
    private EditText etBtn1Label, etBtn1Url, etBtn2Label, etBtn2Url;
    private Button btnStartStop;

    private Worker worker;
    private volatile boolean tokenExtracted = false;
    private volatile boolean rpcRunning = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        formContainer = findViewById(R.id.formContainer);

        etName = findViewById(R.id.etName);
        etDetails = findViewById(R.id.etDetails);
        etState = findViewById(R.id.etState);
        spinnerType = findViewById(R.id.spinnerType);
        etLargeImage = findViewById(R.id.etLargeImage);
        etLargeText = findViewById(R.id.etLargeText);
        etSmallImage = findViewById(R.id.etSmallImage);
        etSmallText = findViewById(R.id.etSmallText);
        etBtn1Label = findViewById(R.id.etBtn1Label);
        etBtn1Url = findViewById(R.id.etBtn1Url);
        etBtn2Label = findViewById(R.id.etBtn2Label);
        etBtn2Url = findViewById(R.id.etBtn2Url);
        btnStartStop = findViewById(R.id.btnStartStop);

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, TYPE_LABELS);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(typeAdapter);

        // Дефолтные значения
        etName.setText("RPCMaker");
        etState.setText("в сети");
        etDetails.setText("Использует RPCMaker Android");

        btnStartStop.setOnClickListener(v -> onStartStopClicked());

        setupWebView();
        webView.loadUrl(DISCORD_LOGIN_URL);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setSaveFormData(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        }
        CookieManager.getInstance().setAcceptCookie(true);

        settings.setUserAgentString(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/120.0.0.0 Safari/537.36"
        );

        webView.addJavascriptInterface(new TokenBridge(), "AndroidBridge");
        webView.setWebViewClient(new DiscordWebViewClient());
        webView.setWebChromeClient(new WebChromeClient());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
    }

    private class TokenBridge {
        @JavascriptInterface
        public void onTokenReceived(String token) {
            if (token == null) return;
            final String cleaned = token.trim();
            if (cleaned.isEmpty() || cleaned.equals("null")) return;
            if (tokenExtracted) return;
            tokenExtracted = true;
            runOnUiThread(() -> onTokenExtracted(cleaned));
        }
    }

    private class DiscordWebViewClient extends WebViewClient {

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            view.evaluateJavascript(INJECT_SCRIPT, null);
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            try {
                String url = request.getUrl().toString();
                if (!tokenExtracted && url.contains("/api/")) {
                    String auth = request.getRequestHeaders().get("Authorization");
                    if (auth != null && !auth.isEmpty() && !auth.equals("null")) {
                        tokenExtracted = true;
                        final String token = auth;
                        view.post(() -> onTokenExtracted(token));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private void onTokenExtracted(String token) {
        Toast.makeText(this, "Токен получен", Toast.LENGTH_SHORT).show();

        if (webView != null) {
            webView.setVisibility(WebView.GONE);
        }
        formContainer.setVisibility(ScrollView.VISIBLE);

        if (worker != null) {
            worker.close();
        }
        worker = new Worker(token);
        rpcRunning = false;
        updateButtonState();
    }

    private void onStartStopClicked() {
        if (worker == null) {
            Toast.makeText(this, "Токен ещё не получен", Toast.LENGTH_SHORT).show();
            return;
        }

        if (rpcRunning) {
            worker.clearActivity();
            rpcRunning = false;
            updateButtonState();
            Toast.makeText(this, "RPC остановлен", Toast.LENGTH_SHORT).show();
        } else {
            String name = etName.getText().toString().trim();
            String details = etDetails.getText().toString().trim();
            String state = etState.getText().toString().trim();
            String type = TYPE_VALUES[spinnerType.getSelectedItemPosition()];

            String largeImage = etLargeImage.getText().toString().trim();
            String largeText = etLargeText.getText().toString().trim();
            String smallImage = etSmallImage.getText().toString().trim();
            String smallText = etSmallText.getText().toString().trim();

            String btn1Label = etBtn1Label.getText().toString().trim();
            String btn1Url = etBtn1Url.getText().toString().trim();
            String btn2Label = etBtn2Label.getText().toString().trim();
            String btn2Url = etBtn2Url.getText().toString().trim();

            if (TextUtils.isEmpty(name)) name = "RPCMaker";

            // Формируем список кнопок.
            // ВАЖНО: формат элемента зависит от твоей версии KizzyRPC.
            // Если библиотека ожидает "Label|URL" — оставь как ниже.
            // Если ожидает просто URL — оставь только btn1Url / btn2Url.
            List<String> buttons = new ArrayList<>();
            if (!TextUtils.isEmpty(btn1Label) && !TextUtils.isEmpty(btn1Url)) {
                buttons.add(btn1Label + "|" + btn1Url);
                if (!TextUtils.isEmpty(btn2Label) && !TextUtils.isEmpty(btn2Url)) {
                    buttons.add(btn2Label + "|" + btn2Url);
                }
            }
            if (buttons.isEmpty()) buttons = null;

            worker.runActivity(
                    name,
                    state,
                    details,
                    type,
                    largeImage, largeText,
                    smallImage, smallText,
                    buttons
            );

            rpcRunning = true;
            updateButtonState();
            Toast.makeText(this, "RPC запущен", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateButtonState() {
        if (rpcRunning) {
            btnStartStop.setText("STOP");
            btnStartStop.setBackgroundTintList(
                    ColorStateList.valueOf(0xFFED4245)); // красный
        } else {
            btnStartStop.setText("START");
            btnStartStop.setBackgroundTintList(
                    ColorStateList.valueOf(0xFF5865F2)); // синий
        }
    }

    @Override
    protected void onDestroy() {
        if (worker != null) {
            worker.close();
            worker = null;
        }
        if (webView != null) {
            webView.loadUrl("about:blank");
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}