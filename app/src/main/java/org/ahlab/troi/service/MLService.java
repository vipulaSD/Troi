package org.ahlab.troi.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;

import com.jlibrosa.audio.JLibrosa;
import com.jlibrosa.audio.exception.FileFormatNotSupportedException;
import com.jlibrosa.audio.wavFile.WavFileException;

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

public class MLService extends Service {
  private static final String TAG = " ### ML Service ###";
  private static final int AROUSAL_IDX = 0;
  private static final int VALENCE_IDX = 1;
  private static final int CATEGORICAL_IDX = 2;
  private final IBinder binder = new LocalBinder();
  private EmpaticaListener empaticaListener;
  private InterpreterApi model;

  public MLService() {
    // nothing to init in the constructor.
  }

  @Override
  public void onCreate() {
    super.onCreate();
    Log.i(TAG, "ML Service created");
    empaticaListener = EmpaticaListener.getInstance();
    initModel();
  }

  public int[] makePrediction() {
    Log.i(TAG, "makePrediction: ");
    float[] mfcc = getMFCC();

    int numInputs = model.getInputTensorCount();
    Object[] inputs = prepareInputs(mfcc, numInputs);

    Map<Integer, Object> modelOutput = prepareOutputStub();
    model.runForMultipleInputsOutputs(inputs, modelOutput);

    int arousalArgMax = resolvePrediction(modelOutput, AROUSAL_IDX, 3);
    int valenceArgMax = resolvePrediction(modelOutput, VALENCE_IDX, 3);
    int categoricalArgMax = resolvePrediction(modelOutput, CATEGORICAL_IDX, 6);

    return new int[] {arousalArgMax, valenceArgMax, categoricalArgMax};
  }

  private int resolvePrediction(
      Map<Integer, Object> modelOutput, int emotionTarget, int numClasses) {
    int argmax = -1;
    float[][] modelPred = (float[][]) modelOutput.get(emotionTarget);
    if (modelPred != null) {
      float max = Float.MIN_VALUE;
      for (int i = 0; i < numClasses; i++) {
        if (max < modelPred[0][i]) {
          max = modelPred[0][i];
          argmax = i;
        }
      }
    }
    return argmax;
  }

  @NonNull
  private Map<Integer, Object> prepareOutputStub() {
    Map<Integer, Object> modelOutput = new HashMap<>();

    float[][] arousalOut = new float[][] {{0, 0, 0}};
    modelOutput.put(AROUSAL_IDX, arousalOut);

    float[][] valenceOut = new float[][] {{0, 0, 0}};
    modelOutput.put(VALENCE_IDX, valenceOut);

    float[][] categoricalOut = new float[][] {{0, 0, 0, 0, 0, 0}};
    modelOutput.put(CATEGORICAL_IDX, categoricalOut);

    return modelOutput;
  }

  @NonNull
  private Object[] prepareInputs(float[] mfcc, int numInputs) {
    Object[] inputs = new Object[numInputs];
    for (int i = 0; i < numInputs; i++) {
      Tensor input = model.getInputTensor(i);

      float[] tmp;
      switch (i) {
        case 0:
          tmp = empaticaListener.getEDASnapshot();
          break;
        case 1:
          tmp = empaticaListener.getBVPSnapshot();
          break;
        case 2:
          tmp = empaticaListener.getAccSnapshot();
          break;
        case 3:
          tmp = empaticaListener.getTempSnapshot();
          break;
        default:
          tmp = mfcc;
          break;
      }

      TensorBuffer bufferFloat = TensorBuffer.createFixedSize(input.shape(), DataType.FLOAT32);
      bufferFloat.loadArray(tmp);
      inputs[i] = bufferFloat.getBuffer();
    }
    return inputs;
  }

  private float[] getMFCC() {
    int sampleRate = 16_000;
    int audioDuration = -1;
    float[] mfcc = new float[40 * 400];
    JLibrosa jLibrosa = new JLibrosa();
    try {
      String audioPath = String.format("%s/speech.wav", getExternalCacheDir().getAbsolutePath());

      float[] audio = jLibrosa.loadAndRead(audioPath, sampleRate, audioDuration);
      float[][] mfccTemp =
          jLibrosa.generateMFCCFeatures(
              audio, sampleRate, 40, (int) (0.025 * sampleRate), 40, (int) (0.10 * sampleRate));
      for (int i = 0; i < mfccTemp.length; i++) {
        for (int j = 0; j < mfccTemp[0].length; j++) {
          if (j >= 400) {
            break;
          }
          mfcc[40 * i + j] = mfccTemp[i][j];
        }
      }
    } catch (IOException e) {
      Log.e(TAG, "Error while loading audio", e);
    } catch (WavFileException e) {
      Log.e(TAG, "Issue in wav file", e);
    } catch (FileFormatNotSupportedException e) {
      Log.e(TAG, "Wrong file format", e);
    }
    return mfcc;
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
