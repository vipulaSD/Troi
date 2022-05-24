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

import com.google.common.collect.EvictingQueue;

import org.ahlab.troi.MainActivity;
import org.ahlab.troi.R;
import org.ahlab.troi.ui.selfreport.SelfReportActivity;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.InterpreterApi;
import org.tensorflow.lite.InterpreterFactory;
import org.tensorflow.lite.Tensor;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@SuppressWarnings("UnstableApiUsage")
public class TroiService extends Service {
  private static final String NOTIFICATION_CHANNEL_ID = "troi_notification_channel";
  private static final String HP_NOTIFICATION_CHANNEL_ID = "troi_high_priority";
  private static final long NOTIFY_INTERVAL = 20 * 1000 * 60L; // 20 minute timer
  private static final String TAG = "### TROI_SERVICE ###";
  private EmpaticaListener empaticaListener;
  private InterpreterApi model;
  private EvictingQueue<Integer> categoricalPrediction;
  private EvictingQueue<Integer> arousalPrediction;
  private EvictingQueue<Integer> valencePrediction;
  private IBinder binder;
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

  private int[] predict() {
    if (model == null || !empaticaListener.isDataReady()) {

      Log.i(TAG, "makePrediction: Error loading model");
      return new int[] {-1, -1, -1};
    }
    int numInputs = model.getInputTensorCount();
    Object[] inputs = new Object[numInputs];
    for (int i = 0; i < numInputs; i++) {
      Tensor input = model.getInputTensor(i);

      float[] tmp;

      if (i == 0) {
        tmp = empaticaListener.getEDASnapshot();
      } else if (i == 1) {
        tmp = empaticaListener.getBVPSnapshot();
      } else if (i == 2) {
        tmp = empaticaListener.getAccSnapshot();
      } else {
        tmp = empaticaListener.getTempSnapshot();
      }

      TensorBuffer bufferFloat = TensorBuffer.createFixedSize(input.shape(), DataType.FLOAT32);
      bufferFloat.loadArray(tmp);
      inputs[i] = bufferFloat.getBuffer();
    }

    Map<Integer, Object> catOutput = new HashMap<>();
    float[][] catOut = new float[][] {{0, 0, 0, 0, 0, 0}};
    catOutput.put(0, catOut);
    model.runForMultipleInputsOutputs(inputs, catOutput);

    Map<Integer, Object> arOutput = new HashMap<>();
    float[][] arOut = new float[][] {{0, 0, 0}};
    arOutput.put(0, arOut);

    Map<Integer, Object> vaOutput = new HashMap<>();
    float[][] vaOut = new float[][] {{0, 0, 0}};
    vaOutput.put(0, vaOut);

    int catArgMax = -1;
    int arArgMax = -1;
    int vaArgMax = -1;

    float[][] catPrediction = (float[][]) catOutput.get(0);
    if (catPrediction != null) {
      float max = Float.MIN_VALUE;
      for (int i = 0; i < 6; i++) {
        if (max < catPrediction[0][i]) {
          max = catPrediction[0][i];
          catArgMax = i;
        }
      }
    }

    float[][] arPrediction = (float[][]) arOutput.get(0);
    if (arPrediction != null) {
      float max = Float.MIN_VALUE;
      for (int i = 0; i < 3; i++) {
        if (max < arPrediction[0][i]) {
          max = arPrediction[0][i];
          arArgMax = i;
        }
      }
    }

    float[][] vaPrediction = (float[][]) vaOutput.get(0);
    if (vaPrediction != null) {
      float max = Float.MIN_VALUE;
      for (int i = 0; i < 3; i++) {
        if (max < vaPrediction[0][i]) {
          max = vaPrediction[0][i];
          vaArgMax = i;
        }
      }
    }

    if (catArgMax > -1) {
      categoricalPrediction.add(catArgMax);
    }
    if (arArgMax > -1) {
      arousalPrediction.add(arArgMax);
    }
    if (vaArgMax > -1) {
      valencePrediction.add(vaArgMax);
    }
    return new int[] {catArgMax, arArgMax, vaArgMax};
  }

  @Override
  public void onCreate() {
    super.onCreate();
    empaticaListener = EmpaticaListener.getInstance();
    notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    createNotificationChannel();
    periodicHandler = new Handler();
    random = new Random();
    initModel();
    categoricalPrediction = EvictingQueue.create(10);
    arousalPrediction = EvictingQueue.create(10);
    valencePrediction = EvictingQueue.create(10);
  }

  private void initModel() {
    MappedByteBuffer multimodalModel;
    try {
      multimodalModel = FileUtil.loadMappedFile(getApplicationContext(), "model.tflite");
      model = new InterpreterFactory().create(multimodalModel, new InterpreterApi.Options());
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
    return binder;
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
