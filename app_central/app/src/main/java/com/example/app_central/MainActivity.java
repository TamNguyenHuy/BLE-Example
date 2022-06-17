package com.example.app_central;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "BluetoothLE";

    private static final UUID HEART_RATE_SERVICE_UUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb");
    private static final int PERMISSION_REQUEST_CODE = 2;
    private static final String IS_NEED_START_SCAN_KEY = "IS_NEED_START_SCAN_KEY";

    private ScanCallback mScanCallback;
    private BluetoothLeScanner mBluetoothLeScanner;
    private Boolean mIsScanning = false;
    private Button mButtonScanAction;
    private DeviceAdapter mDevicesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView mDeviceRecyclerView = findViewById(R.id.devices_recycler_view);
        mDeviceRecyclerView.setHasFixedSize(true);
        mDeviceRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mDeviceRecyclerView.addItemDecoration(new DividerItemDecoration(getBaseContext(),
                DividerItemDecoration.VERTICAL));

        mDevicesAdapter = new DeviceAdapter(getBaseContext());

        mDeviceRecyclerView.setAdapter(mDevicesAdapter);

        mButtonScanAction = findViewById(R.id.start_scan_btn);
        mButtonScanAction.setOnClickListener(view -> {
            if (mIsScanning) {
                stopBLEScan();
            } else {
                startBLEScan();
            }
        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (PERMISSION_REQUEST_CODE == requestCode) {
            if (grantResults.length == 1) {
                if (PackageManager.PERMISSION_GRANTED == grantResults[0]) {
                    startBLEScan();
                } else {
                    requestPermission();
                }
            } else if (grantResults.length == 3) {
                if (PackageManager.PERMISSION_GRANTED == grantResults[0]
                        && PackageManager.PERMISSION_GRANTED == grantResults[1]
                        && PackageManager.PERMISSION_GRANTED == grantResults[2]) {
                    startBLEScan();

                } else {
                    requestPermission();
                }
            }
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        ManagePreference managePreference = ManagePreference.getInstance(getBaseContext());
        Boolean isNeedStartScan = managePreference.getDataBoolean(IS_NEED_START_SCAN_KEY);

        if (isNeedStartScan) {
            managePreference.saveDataBoolean(IS_NEED_START_SCAN_KEY, false);
        }
    }

    private void stopBLEScan() {
        if (null != mBluetoothLeScanner) {
            mBluetoothLeScanner.stopScan(mScanCallback);
            mIsScanning = false;
            mButtonScanAction.setText(R.string.StartScan);
        }
    }

    private void startBLEScan() {
        if (!isGrantedAllPermission()) {
            requestPermission();
        } else {

            if (null != mDevicesAdapter) {
                mDevicesAdapter.clearArrayList();
            }

            BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();
            if (null != bluetoothAdapter) {
                mBluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

                if (null != mBluetoothLeScanner) {
                    if (null != mScanCallback) {
                        mScanCallback = null;
                    }
                    mScanCallback = new CustomScanCallBack();
                    mBluetoothLeScanner.startScan(buildScanFilters(), buildScanSettings(), mScanCallback);
                    mIsScanning = true;
                    mButtonScanAction.setText(R.string.StopScan);
                }
            }
        }
    }

    private BluetoothAdapter getBluetoothAdapter() {
        BluetoothAdapter bluetoothAdapter;
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        if (null != bluetoothManager) {
            bluetoothAdapter = bluetoothManager.getAdapter();

            // Check bluetooth support on device
            if (null != bluetoothAdapter) {
                // check bluetooth turn on
                if (bluetoothAdapter.isEnabled()) {
                    return bluetoothAdapter;
                }
            }
        }
        return null;
    }

    /**
     * Return a List of {@link ScanFilter} objects to filter by Service UUID.
     */
    private List<ScanFilter> buildScanFilters() {

        List<ScanFilter> scanFilters = new ArrayList<>();

        ScanFilter.Builder builder = new ScanFilter.Builder();
        scanFilters.add(builder.build());

        return scanFilters;
    }

    /**
     * Return a {@link ScanSettings} object set to use low power (to preserve battery life).
     */
    private ScanSettings buildScanSettings() {
        ScanSettings.Builder builder = new ScanSettings.Builder();
        builder.setScanMode(ScanSettings.SCAN_MODE_BALANCED);
        return builder.build();
    }

    /**
     * Custom ScanCallback object - adds to adapter on success, displays error on failure.
     */
    private class CustomScanCallBack extends ScanCallback {

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            mDevicesAdapter.add(results);
            logResults(results);
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            mDevicesAdapter.add(result);

            if (null != result) {
                List<ParcelUuid> serviceUuid = result.getScanRecord().getServiceUuids();
                if (null != serviceUuid) {
                    for (ParcelUuid uuid : serviceUuid) {
                        if (uuid.toString().equals(HEART_RATE_SERVICE_UUID.toString())) {
                            ManagePreference managePreference = ManagePreference.getInstance(getBaseContext());
                            if (!managePreference.getDataBoolean(IS_NEED_START_SCAN_KEY)) {
                                managePreference.saveDataBoolean(IS_NEED_START_SCAN_KEY, true);
                                startConnectionActivity(result.getDevice().getName(), result.getDevice().getAddress());
                            }
                            break;
                        }
                    }
                }
            }
            logResults(result);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e(TAG, "Scan failed with error:" + errorCode);
        }

        private void logResults(List<ScanResult> results) {
            if (results != null) {
                for (ScanResult result : results) {
                    logResults(result);
                }
            }
        }

        private void logResults(ScanResult result) {
            if (result != null) {
                BluetoothDevice device = result.getDevice();
                if (device != null) {
                    Log.v(MainActivity.TAG, device.getName() + " " + device.getAddress());
                    return;
                }
            }
            Log.e(MainActivity.TAG, "error SampleScanCallback");
        }
    }

    private void startConnectionActivity(String deviceName, String deviceAddress) {
        Intent intent = new Intent(this, DeviceConnectionActivity.class);
        intent.putExtra(DeviceConnectionActivity.EXTRAS_DEVICE_NAME, deviceName);
        intent.putExtra(DeviceConnectionActivity.EXTRAS_DEVICE_ADDRESS, deviceAddress);
        startActivity(intent);
    }

    private void requestPermission() {
        if (isGrantedAllPermission()) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 31) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT},
                    PERMISSION_REQUEST_CODE);
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_CODE);
        }
    }

    private boolean isGrantedAllPermission() {
        if (Build.VERSION.SDK_INT >= 31) {
            return isHasPermission(Manifest.permission.BLUETOOTH_SCAN) &&
                    isHasPermission(Manifest.permission.ACCESS_FINE_LOCATION) &&
                    isHasPermission(Manifest.permission.BLUETOOTH_CONNECT);
        } else {
            return isHasPermission(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private boolean isHasPermission(String permissionType) {
        return ContextCompat.checkSelfPermission(this, permissionType)
                == PackageManager.PERMISSION_GRANTED;
    }
}
