package org.ahlab.troi.ui.speech;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.ahlab.troi.databinding.ActivitySpeechInputBinding;
import org.ahlab.troi.service.MLService;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class SpeechInputActivity extends AppCompatActivity {

  private static final String TAG = "### Speech Input Activity ###";
  private static final int AUDIO_RECORD_PERMISSION_REQUEST_ID = 9;

  private final List<String> neutralSentences =
      new ArrayList<>(
          Arrays.asList(
              "I’m on my way to the meeting",
              "I wonder what that is about",
              "Have you seen him?",
              "The airplane is almost full",
              "Can you hear me?",
              "Maybe tomorrow it will be cold",
              "I would like a new alarm clock",
              "Can you call me tomorrow?",
              "I think I have a doctor’s appointment",
              "We’ll stop in a couple of minutes",
              "How did he know that?",
              "Don’t forget a jacket",
              "I think I’ve seen this before",
              "The surface is slick"));
  boolean recording;
  private ActivitySpeechInputBinding binding;
  private Random random;
  private MLService mlService;
  private boolean isServiceBound = false;
  private final ServiceConnection connection =
      new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
          MLService.LocalBinder binder = (MLService.LocalBinder) service;
          mlService = binder.getService();
          isServiceBound = true;
          Log.i(TAG, "onServiceConnected: ");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
          Log.i(TAG, "ML Service disconnected.");
          isServiceBound = false;
        }
      };
  private AudioRecordThread recordThread;

  private void startRecording() {
    if (!checkPermission()) {
      return;
    }
    if (recordThread == null) {
      recordThread = new AudioRecordThread();
    }
    if (!recordThread.isRunning()) {
      recordThread.start();
      recording = true;
    }
  }

  @Override
  protected void onStart() {
    super.onStart();
    Log.i(TAG, "onStart: ");
    Intent intent = new Intent(this, MLService.class);
    bindService(intent, connection, Context.BIND_AUTO_CREATE);
  }

  @Override
  protected void onStop() {
    super.onStop();
    unbindService(connection);
    isServiceBound = false;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = ActivitySpeechInputBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());
    random = new Random();
    checkPermission();
    initButton();
  }

  private boolean checkPermission() {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(
          this,
          new String[] {Manifest.permission.RECORD_AUDIO},
          AUDIO_RECORD_PERMISSION_REQUEST_ID);
      return false;
    }
    return true;
  }

  private void initButton() {
    binding.fabRecord.setOnClickListener(
        view -> {
          if (recording) {
            endRecording();
            if (isServiceBound) {
              Log.i(
                  TAG,
                  String.format("Service Output: %s", Arrays.toString(mlService.makePrediction())));
            }
            recording = !recording;
          } else {
            startRecording();
          }
        });
  }

  private void endRecording() {
    if (recordThread != null) {
      recordThread.stopRecording();
      recordThread = null;
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    provideSentence();
  }

  private void provideSentence() {
    String sentence = neutralSentences.get(random.nextInt(neutralSentences.size()));
    binding.prompt.setText(sentence);
  }

  private class AudioRecordThread extends Thread {
    public static final int SAMPLE_RATE_IN_HZ = 16000;
    public static final int CHANNEL_MASK = AudioFormat.CHANNEL_IN_MONO;
    public static final int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private final int minBufferSize =
        2 * AudioRecord.getMinBufferSize(SAMPLE_RATE_IN_HZ, CHANNEL_MASK, AUDIO_ENCODING);
    private AudioRecord audioRecord;
    private boolean isRunning;
    private File file;

    public boolean isRunning() {
      return isRunning;
    }

    private void writeWavHeader(OutputStream out) {
      // Convert the multi-byte integers to raw bytes in little endian format as required by
      // the
      // spec
      byte[] littleBytes =
          ByteBuffer.allocate(14)
              .order(ByteOrder.LITTLE_ENDIAN)
              .putShort((short) 1)
              .putInt(AudioRecordThread.SAMPLE_RATE_IN_HZ)
              .putInt(AudioRecordThread.SAMPLE_RATE_IN_HZ * ((short) 16 / 8))
              .putShort((short) ((short) 16 / 8))
              .putShort((short) 16)
              .array();

      // Not necessarily the best, but it's very easy to visualize this way
      try {

        out.write(
            new byte[] {
              'R',
              'I',
              'F',
              'F',
              0,
              0,
              0,
              0,
              'W',
              'A',
              'V',
              'E',
              'f',
              'm',
              't',
              ' ',
              16,
              0,
              0,
              0,
              1,
              0,
              littleBytes[0],
              littleBytes[1],
              littleBytes[2],
              littleBytes[3],
              littleBytes[4],
              littleBytes[5],
              littleBytes[6],
              littleBytes[7],
              littleBytes[8],
              littleBytes[9],
              littleBytes[10],
              littleBytes[11],
              littleBytes[12],
              littleBytes[13],
              'd',
              'a',
              't',
              'a',
              0,
              0,
              0,
              0
            });

      } catch (IOException e) {
        Log.e(TAG, "error while writing headers", e);
      }
    }

    private void updateWavHeader(File wav) {

      byte[] sizes =
          ByteBuffer.allocate(8)
              .order(ByteOrder.LITTLE_ENDIAN)
              .putInt((int) (wav.length() - 8))
              .putInt((int) (wav.length() - 44))
              .array();

      try (RandomAccessFile accessWave = new RandomAccessFile(wav, "rw")) {
        // ChunkSize
        accessWave.seek(4);
        accessWave.write(sizes, 0, 4);

        // SubChunk2Size
        accessWave.seek(40);
        accessWave.write(sizes, 4, 4);
      } catch (IOException ex) {
        // Rethrow but we still close accessWave in our finally
        Log.e(TAG, "updateWavHeader: ", ex);
      }
      //
    }

    @Override
    public void run() {
      super.run();

      initAudioRecord();
      audioRecord.startRecording();
      file = new File(String.format("%s/speech.wav", getExternalCacheDir().getAbsolutePath()));
      if (file.exists()) {
        Log.i(TAG, String.format("existing file length: %d", file.length()));
      }
      try (FileOutputStream outputStream = new FileOutputStream(file, false)) {
        writeWavHeader(outputStream);

        writeAudioData(outputStream);

      } catch (FileNotFoundException e) {
        Log.e(TAG, "error creating the output file", e);
      } catch (IOException e) {
        Log.e(TAG, "error while writing the wav file", e);
      } finally {
        if (audioRecord != null) {
          if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
            audioRecord.stop();
          }
          if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            audioRecord.release();
          }
        }
      }
      updateWavHeader(file);
    }

    public void stopRecording() {
      if (audioRecord != null && audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
        audioRecord.stop();
        audioRecord.release();
        isRunning = false;
      }
    }

    private void writeAudioData(FileOutputStream outputStream) throws IOException {
      isRunning = true;
      Log.i(TAG, "writeAudioData: started");
      long total = 0;
      byte[] buffer = new byte[minBufferSize];
      while (isRunning && audioRecord != null) {
        int read = audioRecord.read(buffer, 0, buffer.length);
        if (total + read > 4294967295L) {

          for (int i = 0; i < read && total <= 4294967295L; i++, total++) {
            outputStream.write(buffer[i]);
          }
          isRunning = false;
        } else {
          try {
            outputStream.write(buffer, 0, read);
            total += read;
          } catch (Exception e) {
            Log.e(TAG, "writeAudioData: ", e);
          }
        }
        updateWavHeader(file);
      }
    }

    @SuppressLint("MissingPermission") // permission already checked before the thread.
    private void initAudioRecord() {
      audioRecord =
          new AudioRecord.Builder()
              .setAudioSource(MediaRecorder.AudioSource.MIC)
              .setAudioFormat(
                  new AudioFormat.Builder()
                      .setEncoding(AUDIO_ENCODING)
                      .setSampleRate(SAMPLE_RATE_IN_HZ)
                      .setChannelMask(CHANNEL_MASK)
                      .build())
              .setBufferSizeInBytes(minBufferSize)
              .build();
    }
  }
}
