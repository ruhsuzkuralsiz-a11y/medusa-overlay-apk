package com.medusa.overlay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

public class OverlayService extends Service {

    private static final String CHANNEL_ID = "medusa_overlay_channel";
    private WindowManager windowManager;
    private TextView badgeView;
    private WebView hiddenAudioWebView;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1, buildNotification());

        // 1. Dokunmayı hiç engellemeyen ufak durum rozeti
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        badgeView = new TextView(this);
        badgeView.setText("MEDUSA AKTİF");
        badgeView.setTextColor(Color.parseColor("#39FF14"));
        badgeView.setBackgroundColor(Color.parseColor("#CC0D0D0D"));
        badgeView.setPadding(24, 10, 24, 10);
        badgeView.setTextSize(11f);

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                flags,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.END;
        params.y = 80;
        windowManager.addView(badgeView, params);

        // 2. Arka planda ses/bildirim akışını çalıştıran gizli WebView (0x0 piksel, dokunmayı bloklamaz)
        hiddenAudioWebView = new WebView(this);
        hiddenAudioWebView.setWebViewClient(new WebViewClient());
        hiddenAudioWebView.setWebChromeClient(new WebChromeClient());

        WebSettings settings = hiddenAudioWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);

        WindowManager.LayoutParams audioParams = new WindowManager.LayoutParams(
                1,
                1,
                type,
                flags,
                PixelFormat.TRANSLUCENT
        );
        audioParams.gravity = Gravity.TOP | Gravity.START;
        windowManager.addView(hiddenAudioWebView, audioParams);
        hiddenAudioWebView.loadUrl("https://medusa-movies-1.onrender.com/overlay");
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Medusa Overlay Servisi",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification() {
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);

        return builder
                .setContentTitle("Medusa Yayında")
                .setContentText("Sesli bildirim ve yayın servisi çalışıyor")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (windowManager != null && badgeView != null) {
            windowManager.removeView(badgeView);
            badgeView = null;
        }
        if (windowManager != null && hiddenAudioWebView != null) {
            windowManager.removeView(hiddenAudioWebView);
            hiddenAudioWebView.destroy();
            hiddenAudioWebView = null;
        }
    }
}
