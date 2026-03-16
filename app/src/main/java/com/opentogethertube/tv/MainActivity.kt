package com.opentogethertube.tv

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webView)

        enterImmersiveMode()
        setupWebView()
        webView.loadUrl(ROOM_URL)
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
            KeyEvent.KEYCODE_REFRESH,
            KeyEvent.KEYCODE_PROG_RED -> executePlayerCommand("refreshRoom()")

            else -> false
        }

        return handled || super.dispatchKeyEvent(event)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            useWideViewPort = true
            loadWithOverviewMode = true
        }

        webView.isFocusable = true
        webView.isFocusableInTouchMode = true
        webView.requestFocus()
        webView.webChromeClient = WebChromeClient()

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                injectControlScript()
                super.onPageFinished(view, url)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val target = request?.url?.toString().orEmpty()
                return !target.startsWith(ROOM_URL)
            }
        }
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
  if (window.__ottTv) return;

  const getVideo = () => document.querySelector('video');

  const clamp = (n, min, max) => Math.min(max, Math.max(min, n));

  const enforceFullscreen = () => {
    const v = getVideo();
    if (!v) return;
    const root = v.parentElement || v;
    if (!document.fullscreenElement && root.requestFullscreen) {
      root.requestFullscreen().catch(() => {});
    }
    v.style.width = '100vw';
    v.style.height = '100vh';
    v.style.objectFit = 'contain';
  };

  const lockChatReadOnly = () => {
    document.querySelectorAll('textarea, input[type="text"]').forEach(el => {
      el.setAttribute('readonly', 'readonly');
      el.setAttribute('disabled', 'disabled');
      el.style.pointerEvents = 'none';
      el.style.opacity = '0.6';
      el.setAttribute('placeholder', 'Chat somente leitura na TV');
    });

    document.querySelectorAll('button').forEach(btn => {
      const txt = (btn.innerText || '').trim().toLowerCase();
      if (txt === 'send' || txt === 'enviar') {
        btn.style.display = 'none';
      }
    });
  };

  const api = {
    togglePlayPause: () => {
      const v = getVideo();
      if (!v) return;
      if (v.paused) v.play(); else v.pause();
      enforceFullscreen();
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
      let enabled = false;
      for (let i = 0; i < v.textTracks.length; i += 1) {
        const track = v.textTracks[i];
        if (track.mode === 'showing') {
          track.mode = 'hidden';
          enabled = true;
        }
      }
      if (!enabled) {
        v.textTracks[0].mode = 'showing';
      }
    },
    cyclePlaybackSpeed: () => {
      const v = getVideo();
      if (!v) return;
      const rates = [0.75, 1, 1.25, 1.5, 2];
      const idx = rates.indexOf(v.playbackRate);
      v.playbackRate = rates[(idx + 1) % rates.length];
    },
    refreshRoom: () => window.location.reload(),
  };

  window.__ottTv = api;

  const observer = new MutationObserver(() => {
    enforceFullscreen();
    lockChatReadOnly();
  });

  observer.observe(document.documentElement, { childList: true, subtree: true, attributes: true });
  document.addEventListener('fullscreenchange', enforceFullscreen);
  setInterval(() => {
    enforceFullscreen();
    lockChatReadOnly();
  }, 1500);

  enforceFullscreen();
  lockChatReadOnly();
})();
        """
    }
}
