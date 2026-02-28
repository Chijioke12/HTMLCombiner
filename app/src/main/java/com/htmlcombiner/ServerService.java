package com.htmlcombiner;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

/**
 * Optional foreground service so the server keeps running
 * even when the app is in the background.
 */
public class ServerService extends Service {

    private static final String CHANNEL_ID = "html_combiner_server";
    private static final int    NOTIF_ID   = 1001;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        PendingIntent pi = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE);

        Notification n = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("HTML Combiner Server")
                .setContentText("Running on http://localhost:8080")
                .setContentIntent(pi)
                .setOngoing(true)
                .build();

        startForeground(NOTIF_ID, n);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    private void createNotificationChannel() {
        NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID, "Local Server", NotificationManager.IMPORTANCE_LOW);
        ch.setDescription("HTML Combiner local HTTP server status");
        getSystemService(NotificationManager.class).createNotificationChannel(ch);
    }
}
