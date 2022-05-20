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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@SuppressWarnings("UnstableApiUsage")
public class TroiService extends Service {
    private final String notificationChannelId = "troi_notification_channel";
    private final String hpNotificationChannelId = "troi_high_priority";
    private final long NOTIFY_INTERVAL = 20 * 60 * 1000; // 20 minute timer
    private final long PREDICTION_INTERVAL = 60 * 1000;
    private EmpaticaListener empaticaListener;
    private InterpreterApi categoricalModel;
    private InterpreterApi arousalModel;
    private InterpreterApi valenceModel;
    private final String TAG = "###TROI_SERVICE###";
    private EvictingQueue<Integer> categoricalPrediction;
    private EvictingQueue<Integer> arousalPrediction;
    private EvictingQueue<Integer> valencePrediction;
    private IBinder binder;
    private NotificationManager notificationManager;
    private final Runnable runnable = () -> selfReportNotification(1);
    private Handler periodicHandler;
    private final Runnable makePredictionRunnable = () -> {
        int[] predictions = predict();
        int cat = predictions[0];
        int ar = predictions[1];
        int va = predictions[2];
        if (cat >= 0 && categoricalPrediction.remainingCapacity() == 0) {
            int freq = Collections.frequency(categoricalPrediction, cat);
            if (freq < 3) {
                selfReportNotification(0);
                resetTimer();
                return;
            }
        }

        if (ar >= 0 && arousalPrediction.remainingCapacity() == 0) {
            int freq = Collections.frequency(arousalPrediction, ar);
            if (freq < 3) {
                selfReportNotification(0);
                resetTimer();
                return;
            }
        }

        if (va >= 0 && valencePrediction.remainingCapacity() == 0) {
            int freq = Collections.frequency(valencePrediction, va);
            if (freq < 3) {
                selfReportNotification(0);
                resetTimer();

            }
        }

    };
    private Handler predictionHandler;

    public TroiService() {
    }

    private void selfReportNotification(int mode) {
        Intent intent = new Intent(getApplicationContext(), SelfReportActivity.class);
        intent.putExtra(getString(R.string.key_trigger_mode), mode);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 1, intent, 0);
        Notification notification = new Notification.Builder(getApplicationContext(), hpNotificationChannelId)
                .setContentTitle("Troi").setContentText("Make a self report")
                .setContentIntent(pendingIntent).setSmallIcon(R.drawable.ic_troi_update)
                .setAutoCancel(true).build();
        Random notId = new Random();
        notificationManager.notify(notId.nextInt(1000), notification);
    }

    private int[] predict() {
        if (categoricalModel == null || arousalModel == null || valenceModel == null || !empaticaListener.isDataReady()) {
            Log.i(TAG, "makePrediction: Error loading model");
            return new int[]{-1, -1, -1};
        }
        int numInputs = categoricalModel.getInputTensorCount();
        Object[] inputs = new Object[numInputs];
        for (int i = 0; i < numInputs; i++) {
            Tensor input = categoricalModel.getInputTensor(i);

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
        float[][] catOut = new float[][]{{0, 0, 0, 0, 0, 0}};
        catOutput.put(0, catOut);
        categoricalModel.runForMultipleInputsOutputs(inputs, catOutput);

        Map<Integer, Object> arOutput = new HashMap<>();
        float[][] arOut = new float[][]{{0, 0, 0}};
        arOutput.put(0, arOut);
        arousalModel.runForMultipleInputsOutputs(inputs, arOutput);

        Map<Integer, Object> vaOutput = new HashMap<>();
        float[][] vaOut = new float[][]{{0, 0, 0}};
        vaOutput.put(0, vaOut);
        valenceModel.runForMultipleInputsOutputs(inputs, vaOutput);

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
        return new int[]{catArgMax, arArgMax, vaArgMax};
    }

    @Override
    public void onCreate() {
        super.onCreate();
        empaticaListener = EmpaticaListener.getInstance();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
        periodicHandler = new Handler();
        predictionHandler = new Handler();
        initModel();
        categoricalPrediction = EvictingQueue.create(10);
        arousalPrediction = EvictingQueue.create(10);
        valencePrediction = EvictingQueue.create(10);

    }

    private void initModel() {
        MappedByteBuffer catModel;
        MappedByteBuffer arModel;
        MappedByteBuffer vaModel;
        try {
            catModel = FileUtil.loadMappedFile(getApplicationContext(), "model.tflite");
            categoricalModel = new InterpreterFactory().create(catModel, new InterpreterApi.Options());
            arModel = FileUtil.loadMappedFile(getApplicationContext(), "arousal_model.tflite");
            arousalModel = new InterpreterFactory().create(arModel, new InterpreterApi.Options());

            vaModel = FileUtil.loadMappedFile(getApplicationContext(), "valence_model.tflite");
            valenceModel = new InterpreterFactory().create(vaModel, new InterpreterApi.Options());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "starting service: ");
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new Notification.Builder(this, notificationChannelId).setContentTitle("Troi Service").setContentText("Troi is running in background").setContentIntent(pendingIntent).build();
        int ONGOING_NOT_ID = 99;
        startForeground(ONGOING_NOT_ID, notification);

        resetTimer();
        predictionHandler.postDelayed(makePredictionRunnable, PREDICTION_INTERVAL);
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
        String notificationChannelName = "troi_notification_channel";
        NotificationChannel channel = new NotificationChannel(notificationChannelId, notificationChannelName, NotificationManager.IMPORTANCE_LOW);
        notificationManager.createNotificationChannel(channel);

        NotificationChannel channel1 = new NotificationChannel(hpNotificationChannelId, "high_priority_channel", NotificationManager.IMPORTANCE_HIGH);
        notificationManager.createNotificationChannel(channel1);

    }

}
