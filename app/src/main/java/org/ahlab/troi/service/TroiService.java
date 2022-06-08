package org.ahlab.troi.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import org.ahlab.troi.MainActivity;
import org.ahlab.troi.R;
import org.ahlab.troi.ui.selfreport.SelfReportActivity;
import org.tensorflow.lite.InterpreterApi;
import org.tensorflow.lite.InterpreterFactory;
import org.tensorflow.lite.support.common.FileUtil;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.Random;

public class TroiService extends Service {
  private static final String NOTIFICATION_CHANNEL_ID = "troi_notification_channel";
  private static final String HP_NOTIFICATION_CHANNEL_ID = "troi_high_priority";
  private static final long NOTIFY_INTERVAL = 20 * 1000 * 60L; // 20 minute timer
  private static final String TAG = "### TROI_SERVICE ###";

  private NotificationManager notificationManager;
  private Handler periodicHandler;
  private Random random;
  private final Runnable runnable = () -> selfReportNotification(1);

  private void selfReportNotification(int mode) {
    Intent intent = new Intent(getApplicationContext(), SelfReportActivity.class);
    intent.putExtra(getString(R.string.key_trigger_mode), mode);
    PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 1, intent, 0);
    Notification notification =
        new Notification.Builder(getApplicationContext(), HP_NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Troi")
            .setContentText("Make a self report")
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.drawable.ic_troi_update)
            .setAutoCancel(true)
            .build();

    notificationManager.notify(random.nextInt(1000), notification);
  }

  @Override
  public void onCreate() {
    super.onCreate();
    EmpaticaListener.getInstance();
    notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    createNotificationChannel();
    periodicHandler = new Handler();
    random = new Random();
    initModel();
  }

  private void initModel() {
    MappedByteBuffer multimodalModel;
    try {
      multimodalModel = FileUtil.loadMappedFile(getApplicationContext(), "model.tflite");
      new InterpreterFactory().create(multimodalModel, new InterpreterApi.Options());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.i(TAG, " Setting up the notification ");
    Intent notificationIntent = new Intent(this, MainActivity.class);
    PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

    Notification notification =
        new Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Troi Service")
            .setContentText("Troi is running in background")
            .setContentIntent(pendingIntent)
            .build();
    int onGoingId = 99;
    startForeground(onGoingId, notification);

    resetTimer();
    return super.onStartCommand(intent, flags, startId);
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  private void resetTimer() {
    periodicHandler.removeCallbacks(runnable);
    periodicHandler.postDelayed(runnable, NOTIFY_INTERVAL);
  }

  private void createNotificationChannel() {
    NotificationChannel channel =
        new NotificationChannel(
            NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_ID, NotificationManager.IMPORTANCE_LOW);
    notificationManager.createNotificationChannel(channel);

    NotificationChannel channel1 =
        new NotificationChannel(
            HP_NOTIFICATION_CHANNEL_ID,
            "high_priority_channel",
            NotificationManager.IMPORTANCE_HIGH);
    notificationManager.createNotificationChannel(channel1);
  }
}
