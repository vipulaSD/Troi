package org.ahlab.troi.ui.speech;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.ahlab.troi.databinding.ActivitySpeechInputBinding;
import org.ahlab.troi.service.MLService;

import java.io.IOException;
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
  private String filename;
  private ActivitySpeechInputBinding binding;
  private Random random;
  private MediaRecorder recorder;
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
    filename = String.format("%s/speech.mp3", getExternalCacheDir().getAbsolutePath());
    checkPermission();
    initButton();
  }

  private void startRecording() {
    if (!checkPermission()) {
      return;
    }
    recorder = new MediaRecorder();
    recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
    recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
    recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
    recorder.setOutputFile(filename);
    try {
      recorder.prepare();
    } catch (IOException e) {
      Log.e(TAG, "Error while starting audio recording: ", e);
    }
    recorder.start();
    recording = true;

  }

  private boolean endRecording() {
    if (recorder == null) {
      return false;
    }
    recorder.stop();
    recorder.reset();
    recorder.release();
    recorder = null;
    recording = false;

    Log.i(TAG, "Finish recording audio");
    return true;
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
            if (endRecording() && isServiceBound) {
              Log.i(TAG, String.format("Service Output: %s", mlService.makePrediction()));
            }
            recording = !recording;
          } else {
            startRecording();
          }
        });
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
}
