package com.example.myexampleapplication;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myexampleapplication.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    // all devices or only those who match the service string
    static final boolean SHOW_ALL_DEVICES = false;

    private static final int ENABLE_BLUETOOTH_REQUEST_CODE = 1;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 2;

    private ActivityMainBinding binding;

    private BluetoothAdapter ble_adapter;
    private BluetoothLeScanner ble_scanner;
    private ScanSettings ble_settings;

    private MyScanCache cache = null;

    static final String serviceIDString = "3f1a1596-ee7f-42bd-84d1-b1a294f82ecf";
    private ScanFilter lumenFilter = null;
    private final List<ScanFilter> noFilter = null; // no filter - show any device

    private boolean isPermissionOK;

    private void updateResultDisplay() {
        String message = "Scan found %d device(s) so far";
        String displayMessage = String.format(Locale.ENGLISH, message, cache.count());
        binding.tvLog.setText(displayMessage);
    }

    private ScanCallback callback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            cache.addDevice(result.getDevice().getAddress(), null, result.getDevice().getName());
            updateResultDisplay();
            Log.i("MyScanner", "Found device " + result.getDevice().getAddress());
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            for (ScanResult result : results) {
                cache.addDevice(result.getDevice().getAddress(), null, result.getDevice().getName());
            }
            updateResultDisplay();
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            setIsScanning(false);
            Log.e("MyScanner", "Scan failed with error code " + errorCode);
        }
    };

    // callback function from request to grant location permission on the smartphone
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Log.e("MyScanner", "permission deined");
                requestLocationPermission();
                isPermissionOK = false;
            } else {
                Log.e("MyScanner", "permission went ok");
                isPermissionOK = true;
            }
        }
    }

    // callback function from request to enable BT on the smartphone
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ENABLE_BLUETOOTH_REQUEST_CODE) {
            if (resultCode != RESULT_OK) {
                checkEnableBT();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setIsScanning(false);
        cache = new MyScanCache();
        lumenFilter = new ScanFilter.Builder()
                .setServiceUuid(ParcelUuid.fromString(serviceIDString))
                .build();

        if (ble_adapter == null || !ble_adapter.isEnabled()) {
            final BluetoothManager ble_mng = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
            ble_adapter = ble_mng.getAdapter();
            ble_scanner = ble_adapter.getBluetoothLeScanner();
        }

        if (ble_settings == null) {
            ble_settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
        }

        binding.bStart.setOnClickListener(v -> {
            if (!checkEnableBT()) {
                return;
            }
            if (SHOW_ALL_DEVICES) { // this is known at compile time, only here for easy debugging
                ble_scanner.startScan(noFilter, ble_settings, callback);
            } else {
                ble_scanner.startScan(Collections.singletonList(lumenFilter), ble_settings, callback);
            }
            setIsScanning(true);
            cache.clear();
            binding.tvLog.setText("Scan is starting ...\n");
        });

        binding.bStop.setOnClickListener(v -> {
            ble_scanner.stopScan(callback);
            setIsScanning(false);
            binding.tvLog.append("\nScan stopped.\n");
            for (MyScanCache.Device entry : cache.values()) {
                binding.tvLog.append(entry.view() + " \n");
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        cache.clear();
        checkEnableBT();
        checkPermissions();
    }

    /// Checks if location permissions are already granted. Requests them otherwise.
    private void checkPermissions() {
        if (isPermissionGranted()) {
            binding.tvLog.setText("Bluetooth permission is OK\n");
        } else {
            binding.tvLog.setText("Bluetooth permission problems. Asking to enable location permission...\n");
            requestLocationPermission();
        }
    }

    private boolean isPermissionGranted() {
        isPermissionOK = checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED;
        isPermissionOK = isPermissionOK && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        return isPermissionOK;
    }

    private void requestLocationPermission() {
        if (isPermissionGranted()) {
            return;
        }
        runOnUiThread(() -> {
            AlertDialog.Builder diag_builder = new AlertDialog.Builder(this);
            diag_builder.setTitle("Location")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage("We need location access to scan with Bluetooth")
                    .setPositiveButton("OK", (dialog, which) -> {
                        requestPermissions(
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                LOCATION_PERMISSION_REQUEST_CODE);
                    })
                    .setNegativeButton("No", (dialog, which) -> {
                        Toast.makeText(getApplicationContext(),"clicked no", Toast.LENGTH_LONG).show();
                    })
                    .setCancelable(false)
                    .create().show();
        });
    }

    /// Check if Android device has Bluetooth enabled. If not, ask to enable it.
    /// @return true if BT was already enabled.
    private boolean checkEnableBT() {
        if (!ble_adapter.isEnabled()) {
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                    ENABLE_BLUETOOTH_REQUEST_CODE);
            return false;
        }
        return true;
    }

    /// Change Enabled status in buttons according to status of the application.
    private void setIsScanning(boolean status) {
        binding.bStart.setEnabled(!status);
        binding.bStop.setEnabled(status);
    }
}