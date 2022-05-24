package org.ahlab.troi.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.google.common.collect.EvictingQueue;

import org.tensorflow.lite.InterpreterApi;
import org.tensorflow.lite.InterpreterFactory;
import org.tensorflow.lite.support.common.FileUtil;

import java.io.IOException;
import java.nio.MappedByteBuffer;

@SuppressWarnings("UnstableApiUsage")
public class MLService extends Service {
  private static final String TAG = " ### ML Service ###";
  private final IBinder binder = new LocalBinder();
  private EmpaticaListener empaticaListener;
  private EvictingQueue<Integer> categoricalPrediction;
  private EvictingQueue<Integer> arousalPrediction;
  private EvictingQueue<Integer> valencePrediction;
  private InterpreterApi model;

  public MLService() {}

  @Override
  public void onCreate() {
    super.onCreate();
    Log.i(TAG, "ML Service created");
    empaticaListener = EmpaticaListener.getInstance();
    initModel();
    categoricalPrediction = EvictingQueue.create(10);
    arousalPrediction = EvictingQueue.create(10);
    valencePrediction = EvictingQueue.create(10);
  }

  public String makePrediction() {
    return "success";
  }

  @Override
  public IBinder onBind(Intent intent) {
    return binder;
  }

  private void initModel() {
    MappedByteBuffer multimodalModel;
    try {
      multimodalModel = FileUtil.loadMappedFile(getApplicationContext(), "model.tflite");
      model = new InterpreterFactory().create(multimodalModel, new InterpreterApi.Options());
      Log.i(TAG, "ML Model loaded successfully");
    } catch (IOException e) {
      Log.e(TAG, "Error while loading ml model", e);
    }
  }

  public class LocalBinder extends Binder {
    public MLService getService() {
      return MLService.this;
    }
  }
}
