package com.opentogethertube.tv

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webView)

        enterImmersiveMode()
        setupWebView()
        loadRoom()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enterImmersiveMode()
        }
    }

    override fun onBackPressed() {
        // Bloqueia saída acidental da room com o botão back do comando.
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) {
            return super.dispatchKeyEvent(event)
        }

        val handled = when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_MEDIA_PLAY,
            KeyEvent.KEYCODE_MEDIA_PAUSE -> executePlayerCommand("togglePlayPause()")

            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
            KeyEvent.KEYCODE_DPAD_RIGHT -> executePlayerCommand("seekBy(10)")

            KeyEvent.KEYCODE_MEDIA_REWIND,
            KeyEvent.KEYCODE_DPAD_LEFT -> executePlayerCommand("seekBy(-10)")

            KeyEvent.KEYCODE_VOLUME_UP -> executePlayerCommand("changeVolume(0.1)")
            KeyEvent.KEYCODE_VOLUME_DOWN -> executePlayerCommand("changeVolume(-0.1)")

            KeyEvent.KEYCODE_CAPTIONS,
            KeyEvent.KEYCODE_C,
            KeyEvent.KEYCODE_PROG_BLUE -> executePlayerCommand("toggleSubtitles()")

            KeyEvent.KEYCODE_MEDIA_STEP_FORWARD,
            KeyEvent.KEYCODE_MEDIA_NEXT,
            KeyEvent.KEYCODE_PROG_YELLOW -> executePlayerCommand("cyclePlaybackSpeed()")

            KeyEvent.KEYCODE_MENU,
            KeyEvent.KEYCODE_F5,
            KeyEvent.KEYCODE_PROG_RED -> {
                loadRoom(forceReload = true)
                true
            }

            else -> false
        }

        return handled || super.dispatchKeyEvent(event)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            javaScriptCanOpenWindowsAutomatically = false
            cacheMode = WebSettings.LOAD_DEFAULT
            useWideViewPort = true
            loadWithOverviewMode = true
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            userAgentString = "$userAgentString OTTTV/1.0 (${Build.MODEL}; AndroidTV)"
        }

        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        webView.isFocusable = true
        webView.isFocusableInTouchMode = true
        webView.requestFocus()
        webView.webChromeClient = WebChromeClient()

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                enterImmersiveMode()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                if (!isRoomUrl(url)) {
                    loadRoom()
                    return
                }

                injectControlScript()
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val target = request?.url?.toString().orEmpty()
                if (!isRoomUrl(target)) {
                    loadRoom()
                    return true
                }

                return false
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    Toast.makeText(
                        this@MainActivity,
                        "Erro de rede a carregar a room. A tentar novamente...",
                        Toast.LENGTH_LONG
                    ).show()
                    webView.postDelayed({ loadRoom() }, 2000)
                }
            }
        }
    }

    private fun loadRoom(forceReload: Boolean = false) {
        if (forceReload) {
            webView.stopLoading()
            webView.loadUrl(ROOM_URL)
            return
        }

        val current = webView.url.orEmpty()
        if (!isRoomUrl(current)) {
            webView.loadUrl(ROOM_URL)
        }
    }

    private fun isRoomUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val normalized = url.lowercase(Locale.ROOT)
        return normalized.startsWith(ROOM_URL.lowercase(Locale.ROOT))
    }

    private fun executePlayerCommand(functionCall: String): Boolean {
        webView.evaluateJavascript("window.__ottTv && window.__ottTv.$functionCall;", null)
        return true
    }

    private fun injectControlScript() {
        webView.evaluateJavascript(CONTROL_SCRIPT, null)
    }

    private fun enterImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.systemBars())
                controller.systemBarsBehavior =
                    android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                )
        }
    }

    companion object {
        private const val ROOM_URL = "https://opentogethertube.com/room/MahS2Dani"

        private const val CONTROL_SCRIPT = """
(function() {
  const ROOM_PATH = '/room/MahS2Dani';

  const getVideo = () => document.querySelector('video');
  const clamp = (n, min, max) => Math.min(max, Math.max(min, n));

  const forceRoom = () => {
    if (!window.location.pathname.startsWith(ROOM_PATH)) {
      window.location.href = 'https://opentogethertube.com' + ROOM_PATH;
    }
  };

  const hideTopBar = () => {
    const selectors = [
      'header',
      'nav',
      '[class*=header]',
      '[class*=navbar]',
      '[class*=topbar]',
      '[id*=header]',
      '[id*=navbar]',
      'a[href="/browse"]',
      'a[href="/faq"]',
      'a[href="/create"]'
    ];

    selectors.forEach((selector) => {
      document.querySelectorAll(selector).forEach((el) => {
        if (el && el.style) {
          el.style.display = 'none';
          el.style.maxHeight = '0';
          el.style.overflow = 'hidden';
        }
      });
    });

    document.documentElement.style.background = '#000';
    document.body.style.background = '#000';
    document.body.style.margin = '0';
    document.body.style.padding = '0';
  };

  const forceVideoFullscreenLayout = () => {
    const v = getVideo();
    if (!v) return;

    const root = v.parentElement || v;
    if (root && root.style) {
      root.style.position = 'fixed';
      root.style.inset = '0';
      root.style.width = '100vw';
      root.style.height = '100vh';
      root.style.zIndex = '2147483647';
      root.style.background = '#000';
    }

    v.style.width = '100vw';
    v.style.height = '100vh';
    v.style.objectFit = 'contain';
    v.style.background = '#000';

    if (v.paused && v.src) {
      v.play().catch(() => {});
    }
  };

  const lockChatReadOnly = () => {
    document.querySelectorAll('textarea, input[type="text"]').forEach((el) => {
      el.setAttribute('readonly', 'readonly');
      el.setAttribute('disabled', 'disabled');
      el.style.pointerEvents = 'none';
      el.style.opacity = '0.6';
      el.setAttribute('placeholder', 'Chat somente leitura na TV');
    });

    document.querySelectorAll('button').forEach((btn) => {
      const txt = (btn.innerText || '').trim().toLowerCase();
      if (txt === 'send' || txt === 'enviar') {
        btn.style.display = 'none';
      }
    });
  };

  const installApi = () => {
    if (window.__ottTv) return;

    window.__ottTv = {
      togglePlayPause: () => {
        const v = getVideo();
        if (!v) return;
        if (v.paused) v.play().catch(() => {}); else v.pause();
      },
      seekBy: (seconds) => {
        const v = getVideo();
        if (!v) return;
        v.currentTime = Math.max(0, v.currentTime + seconds);
      },
      changeVolume: (delta) => {
        const v = getVideo();
        if (!v) return;
        v.volume = clamp(v.volume + delta, 0, 1);
      },
      toggleSubtitles: () => {
        const v = getVideo();
        if (!v || !v.textTracks || v.textTracks.length === 0) return;
        let hiddenAny = false;
        for (let i = 0; i < v.textTracks.length; i += 1) {
          const track = v.textTracks[i];
          if (track.mode === 'showing') {
            track.mode = 'hidden';
            hiddenAny = true;
          }
        }
        if (!hiddenAny) {
          v.textTracks[0].mode = 'showing';
        }
      },
      cyclePlaybackSpeed: () => {
        const v = getVideo();
        if (!v) return;
        const rates = [0.75, 1, 1.25, 1.5, 2];
        const current = rates.indexOf(v.playbackRate);
        v.playbackRate = rates[(current + 1 + rates.length) % rates.length];
      },
      refreshRoom: () => window.location.reload(),
    };
  };

  const tick = () => {
    forceRoom();
    hideTopBar();
    forceVideoFullscreenLayout();
    lockChatReadOnly();
    installApi();
  };

  tick();
  setInterval(tick, 1200);
  new MutationObserver(tick).observe(document.documentElement, { childList: true, subtree: true, attributes: true });
})();
        """
    }
}
