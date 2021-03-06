package org.ahlab.troi;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.le.ScanCallback;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.empatica.empalink.ConnectionNotAllowedException;
import com.empatica.empalink.EmpaDeviceManager;
import com.empatica.empalink.EmpaticaDevice;
import com.empatica.empalink.config.EmpaSensorType;
import com.empatica.empalink.config.EmpaStatus;
import com.empatica.empalink.delegate.EmpaStatusDelegate;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import org.ahlab.troi.databinding.ActivityMainBinding;
import org.ahlab.troi.service.EmpaticaListener;
import org.ahlab.troi.service.TroiService;
import org.ahlab.troi.ui.selfreport.SelfReportActivity;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.InterpreterApi;
import org.tensorflow.lite.InterpreterFactory;
import org.tensorflow.lite.Tensor;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements EmpaStatusDelegate {

  private static final int REQUEST_PERMISSION_ACCESS_COARSE_LOCATION = 1;
  private static final String TAG = "### Main Activity ###";
  InterpreterApi tflite;
  Map<String, Integer> emotionDistro = new HashMap<>();
  private EmpaDeviceManager deviceManager;
  private ActivityMainBinding binding;

  private ActionBarDrawerToggle actionBarDrawerToggle;

  private EmpaticaListener empaticaListener;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    binding = ActivityMainBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    actionBarDrawerToggle =
        new ActionBarDrawerToggle(
            this, binding.drawerLayout, R.string.nav_open, R.string.nav_close);
    binding.drawerLayout.addDrawerListener(actionBarDrawerToggle);
    actionBarDrawerToggle.syncState();
    if (getSupportActionBar() != null) {
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
    empaticaListener = EmpaticaListener.getInstance();
    initView();
    checkE4Connection();
    initModel();
    initButtons();
    initMenu();
    initDb();
    initService();
  }

  private void checkE4Connection() {
    long diff = System.currentTimeMillis() - empaticaListener.getLastUpdateTs();
    double difInMin = diff / (1000 * 60.0);
    if (difInMin < 1) {

      runOnUiThread(
          () -> {
            binding.tvStatus.setText(R.string.e4_connected);
            binding.btnConnect.setVisibility(View.INVISIBLE);
            binding.btnReport.setVisibility(View.VISIBLE);
          });
    }
  }

  private void initDb() {
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    FirebaseFirestoreSettings settings =
        new FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build();
    db.setFirestoreSettings(settings);
  }

  private void initMenu() {
    SharedPreferences sharedPreferences =
        getSharedPreferences(getString(R.string.shared_preference), Context.MODE_PRIVATE);
    String pid = sharedPreferences.getString(getString(R.string.key_pid), "P -1");

    Menu menu = binding.navMenu.getMenu();
    MenuItem pidMenuItem = menu.findItem(R.id.nav_pid);
    if (pid.equals("P -1")) {
      pidMenuItem.setTitle("Set Participant ID");
    } else {
      pidMenuItem.setTitle("Participant ID: " + pid);
    }

    pidMenuItem.setOnMenuItemClickListener(
        menuItem -> {
          AlertDialog.Builder builder = new AlertDialog.Builder(this);
          final EditText txtPid = new EditText(getApplicationContext());

          builder.setTitle("Enter Participant ID");
          LinearLayout linearLayout = new LinearLayout(this);
          linearLayout.setOrientation(LinearLayout.VERTICAL);
          linearLayout.addView(txtPid);
          builder.setView(linearLayout);

          builder
              .setPositiveButton(
                  R.string.confirm,
                  (dialogInterface, i) -> {
                    String pid1 = txtPid.getText().toString().trim();
                    if (!pid1.isEmpty()) {
                      sharedPreferences.edit().putString(getString(R.string.key_pid), pid1).apply();
                      pidMenuItem.setTitle("Participant ID: " + pid1);
                    }
                  })
              .setNegativeButton("cancel", (dialogInterface, i) -> dialogInterface.cancel());
          builder.show();

          return false;
        });

    MenuItem itemEndService = menu.findItem(R.id.end_service);
    itemEndService.setOnMenuItemClickListener(
        item -> {
          Intent intent = new Intent(this, TroiService.class);
          stopService(intent);
          return true;
        });

    MenuItem batteryLevel = menu.findItem(R.id.battery_level);
    batteryLevel.setTitle("Empatica Battery Level: " + empaticaListener.getBatteryLevel());
  }

  private void initButtons() {
    // connecting empatica device
    binding.btnConnect.setOnClickListener(
        view -> {
          initEmpaticaDeviceManager();
          binding.tvStatus.setText(R.string.status_e4_search_device);
        });

    // prediction button
    binding.btnPrediction.setOnClickListener(
        view -> {
          if (empaticaListener.isDataReady()) {
            makePrediction();
          } else {
            Log.i(TAG, "waiting for data");
          }
        });

    // self report button
    binding.btnReport.setOnClickListener(
        view -> {
          Intent intent = new Intent(this, SelfReportActivity.class);
          startActivity(intent);
        });
  }

  private void initEmpaticaDeviceManager() {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(
          this,
          new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
          REQUEST_PERMISSION_ACCESS_COARSE_LOCATION);
      Log.d(TAG, "initEmpaticaDeviceManager: request permission");
    } else {
      Log.d(TAG, "initEmpaticaDeviceManager: connection request");

      deviceManager = new EmpaDeviceManager(getApplicationContext(), empaticaListener, this);
      deviceManager.authenticateWithAPIKey(getResources().getString(R.string.empatica_api_key));
    }
  }

  private void makePrediction() {
    if (tflite == null) {
      Log.i(TAG, "makePrediction: Error loading model");
      return;
    }
    int numInputs = tflite.getInputTensorCount();
    Object[] inputs = new Object[numInputs];
    Log.i(TAG, "makePrediction: num inputs: " + numInputs);
    for (int i = 0; i < numInputs; i++) {
      Tensor input = tflite.getInputTensor(i);
      Log.i(TAG, "makePrediction: input name: " + input.name());
      Log.i(TAG, "makePrediction: input shape: " + Arrays.toString(input.shape()));
      Log.i(TAG, "makePrediction: input type: " + input.dataType());

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

      Log.i(TAG, "makePrediction: data: " + Arrays.toString(tmp));

      TensorBuffer bufferFloat = TensorBuffer.createFixedSize(input.shape(), DataType.FLOAT32);
      bufferFloat.loadArray(tmp);
      inputs[i] = bufferFloat.getBuffer();
    }

    Tensor output = tflite.getOutputTensor(0);
    Log.i(TAG, "makePrediction: output name: " + output.name());
    Log.i(TAG, "makePrediction: output shape: " + Arrays.toString(output.shape()));
    Log.i(TAG, "makePrediction: output type: " + output.dataType());
    Map<Integer, Object> outputs = new HashMap<>();
    float[][] out = new float[][] {{0, 0, 0, 0, 0, 0}};
    outputs.put(0, out);
    tflite.runForMultipleInputsOutputs(inputs, outputs);

    float[][] prediction = (float[][]) outputs.get(0);
    if (prediction == null) {
      return;
    }
    String[] emotionLabels = {"cheerful", "happy", "angry", "nervous", "sad", "neutral"};

    float max = Float.MIN_VALUE;
    int argmax = -1;

    for (int i = 0; i < 6; i++) {
      if (max < prediction[0][i]) {
        max = prediction[0][i];
        argmax = i;
      }
      Log.i(TAG, "makePrediction: " + emotionLabels[i] + " -> " + prediction[0][i]);
    }
    if (argmax == -1) {
      return;
    }
    String emotionLabel = emotionLabels[argmax];
    Log.i(TAG, "makePrediction: final prediction " + emotionLabel + " prob: " + max);
    emotionDistro.put(emotionLabels[argmax], emotionDistro.getOrDefault(emotionLabel, 0) + 1);

    Log.i(TAG, "makePrediction: " + emotionDistro);
  }

  private void initView() {
    binding.tvStatus.setText(R.string.status_e4_not_connected);
    binding.btnPrediction.setVisibility(View.INVISIBLE);
    binding.btnReport.setVisibility(View.INVISIBLE);
  }

  private void initModel() {
    MappedByteBuffer tfliteModel;
    try {
      tfliteModel = FileUtil.loadMappedFile(getApplicationContext(), "model.tflite");
      tflite = new InterpreterFactory().create(tfliteModel, new InterpreterApi.Options());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  protected void onStop() {
    super.onStop();
    if (deviceManager != null) {
      deviceManager.stopScanning();
    }
  }

  private void initService() {
    Log.i(TAG, "initService: starting service");
    Intent intent = new Intent(this, TroiService.class);
    startForegroundService(intent);
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == REQUEST_PERMISSION_ACCESS_COARSE_LOCATION) {
      if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        Log.i(TAG, "onRequestPermissionsResult: location permission granted");
        initEmpaticaDeviceManager();
      } else {
        Log.i(TAG, "onRequestPermissionsResult: location permission denied");
        final boolean needRationale =
            ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.ACCESS_COARSE_LOCATION);
        new AlertDialog.Builder(this)
            .setTitle("Permission required")
            .setMessage(
                "Without this permission bluetooth low energy devices cannot be found, allow it in order to connect to the device.")
            .setPositiveButton(
                "Retry",
                (dialog, which) -> {
                  // try again
                  if (needRationale) {
                    // the "never ask again" flash is not set, try again with permission request
                    initEmpaticaDeviceManager();
                  } else {
                    // the "never ask again" flag is set so the permission requests is disabled, try
                    // open app settings to enable the permission
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                  }
                })
            .setNegativeButton("Exit application", (dialog, which) -> finish())
            .show();
      }
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void didUpdateStatus(EmpaStatus status) {
    Log.i(TAG, "didUpdateStatus: " + status.toString());
    switch (status) {
      case INITIAL:
        binding.tvStatus.setText(R.string.e4_initial);
        break;
      case READY:
        deviceManager.startScanning();
        binding.tvStatus.setText(R.string.e4_scan);
        break;
      case DISCONNECTED:
        binding.tvStatus.setText(R.string.e4_disconnected);
        binding.btnConnect.setVisibility(View.VISIBLE);
        break;
      case CONNECTING:
        binding.tvStatus.setText(R.string.e4_connecting);
        break;
      case CONNECTED:
        binding.tvStatus.setText(R.string.e4_connected);
        binding.btnConnect.setVisibility(View.INVISIBLE);
        break;
      case DISCONNECTING:
        binding.tvStatus.setText(R.string.e4_disconnecting);
        break;
      case DISCOVERING:
        binding.tvStatus.setText(R.string.e4_discovering);
        break;
    }
  }

  @Override
  public void didEstablishConnection() {
    binding.tvStatus.setText(R.string.e4_established);
  }

  @Override
  public void didUpdateSensorStatus(int status, EmpaSensorType type) {
    Log.i(TAG, "didUpdateSensorStatus: " + status);
  }

  @Override
  public void didDiscoverDevice(
      EmpaticaDevice device, String deviceLabel, int rssi, boolean allowed) {
    Log.i(TAG, "didDiscoverDevice" + deviceLabel + "allowed: " + allowed);

    if (allowed) {
      // Stop scanning. The first allowed device will do.
      deviceManager.stopScanning();
      try {
        // Connect to the device
        deviceManager.connectDevice(device);
        updateLabel(binding.tvStatus, "Connecting to: " + deviceLabel);

      } catch (ConnectionNotAllowedException e) {
        // This should happen only if you try to connect when allowed == false.
        Toast.makeText(
                MainActivity.this, "Sorry, you can't connect to this device", Toast.LENGTH_SHORT)
            .show();
        Log.e(TAG, "didDiscoverDevice" + deviceLabel + " - ConnectionNotAllowedException", e);
      }
    }
  }

  private void updateLabel(final TextView label, final String text) {
    runOnUiThread(() -> label.setText(text));
  }

  @Override
  public void didFailedScanning(int errorCode) {

    switch (errorCode) {
      case ScanCallback.SCAN_FAILED_ALREADY_STARTED:
        Log.e(TAG, "Scan failed: a BLE scan with the same settings is already started by the app");
        binding.tvStatus.setText(R.string.status_e4_already_sesarching);
        break;
      case ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
        Log.e(TAG, "Scan failed: app cannot be registered");
        binding.tvStatus.setText(R.string.status_e4_registration);
        break;
      case ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED:
        Log.e(TAG, "Scan failed: power optimized scan feature is not supported");
        binding.tvStatus.setText(R.string.status_e4_power_error);
        break;
      case ScanCallback.SCAN_FAILED_INTERNAL_ERROR:
        Log.e(TAG, "Scan failed: internal error");
        binding.tvStatus.setText(R.string.status_e4_internal_error);
        break;
      default:
        Log.e(TAG, "Scan failed with unknown error (errorCode=" + errorCode + ")");
        binding.tvStatus.setText(R.string.status_e4_other_error);
        break;
    }
  }

  @Override
  public void didRequestEnableBluetooth() {
    Log.i(TAG, "didRequestEnableBluetooth: ");
  }

  @Override
  public void bluetoothStateChanged() {
    Log.i(TAG, "bluetoothStateChanged: ");
  }

  @Override
  public void didUpdateOnWristStatus(int status) {
    Log.i(TAG, "didUpdateOnWristStatus: ");
  }
}
