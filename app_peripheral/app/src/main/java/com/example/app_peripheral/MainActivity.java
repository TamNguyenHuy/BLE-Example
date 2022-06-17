package com.example.app_peripheral;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Switch;
import android.widget.Toast;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";
    private static final UUID HEART_RATE_SERVICE_UUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb");
    private static final UUID BODY_SENSOR_LOCATION_CHARACTERISTIC_UUID = UUID.fromString("00002A38-0000-1000-8000-00805f9b34fb");
    private static final int PERMISSION_REQUEST_CODE = 2;

    enum Light {
        RED,
        GREEN,
        GREY
    }

    private BluetoothGattCharacteristic mSampleCharacteristic;

    private HashSet<BluetoothDevice> mBluetoothDevices;
    private BluetoothGattServer mGattServer;
    View mCircleView;

    private final BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    mBluetoothDevices.add(device);
                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    mBluetoothDevices.remove(device);
                    setLightCircle(Light.GREY);
                }
            } else {
                mBluetoothDevices.remove(device);
            }
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            super.onServiceAdded(status, service);

        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                                BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);

            if (null == mGattServer) {
                return;
            }
            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                                                 BluetoothGattCharacteristic characteristic,
                                                 boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            mSampleCharacteristic.setValue(value);
            if (responseNeeded) {
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value);
            }

            String valueString = new String(value, StandardCharsets.UTF_8);
            runOnUiThread(() -> {
                if (valueString.equals("RED")) {
                    setLightCircle(Light.RED);
                } else if (valueString.equals("GREEN")) {
                    setLightCircle(Light.GREEN);
                } else {
                    setLightCircle(Light.GREY);
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCircleView = findViewById(R.id.light_circle);

        Switch advertiseSwitch = findViewById(R.id.advertise_switch);
        advertiseSwitch.setOnClickListener(view -> {
            if (advertiseSwitch.isChecked()) {
                startAdvertising();
            } else {
                stopAdvertising();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (PERMISSION_REQUEST_CODE == requestCode) {
            if (grantResults.length == 1) {
                if (PackageManager.PERMISSION_GRANTED == grantResults[0]) {
                    setGattServer();
                    setBluetoothService();
                    startAdvertising();
                } else {
                    requestPermission();
                }
            } else if (grantResults.length == 4) {
                if (PackageManager.PERMISSION_GRANTED == grantResults[0]
                        && PackageManager.PERMISSION_GRANTED == grantResults[1]
                        && PackageManager.PERMISSION_GRANTED == grantResults[2]
                        && PackageManager.PERMISSION_GRANTED == grantResults[3]) {
                    setGattServer();
                    setBluetoothService();
                    startAdvertising();
                } else {
                    requestPermission();
                }
            }
        }
    }

    private void requestPermission() {
        if (isGrantedAllPermission()) {
            return;
        }

        if (Build.VERSION.SDK_INT >= 31) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_ADVERTISE,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSION_REQUEST_CODE);
        } else {
            requestPermissions(new String[]{
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSION_REQUEST_CODE);
        }
    }

    private boolean isGrantedAllPermission() {
        if (Build.VERSION.SDK_INT >= 31) {
            return isHasPermission(Manifest.permission.BLUETOOTH_ADVERTISE) &&
                    isHasPermission(Manifest.permission.ACCESS_FINE_LOCATION) &&
                    isHasPermission(Manifest.permission.BLUETOOTH_CONNECT) &&
                    isHasPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
        } else {
            return isHasPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
    }

    private boolean isHasPermission(String permissionType) {
        return ActivityCompat.checkSelfPermission(this, permissionType)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void setGattServer() {
        mBluetoothDevices = new HashSet<>();
        BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (null != mBluetoothManager) {
            mGattServer = mBluetoothManager.openGattServer(this, mGattServerCallback);
        } else {
            Toast.makeText(this, "mBluetoothManager Failure", Toast.LENGTH_LONG).show();
        }
    }

    private void setBluetoothService() {

        BluetoothGattService mSampleService = new BluetoothGattService(HEART_RATE_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        mSampleCharacteristic = new BluetoothGattCharacteristic(BODY_SENSOR_LOCATION_CHARACTERISTIC_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY
                        | BluetoothGattCharacteristic.PERMISSION_READ
                        | BluetoothGattCharacteristic.PERMISSION_WRITE);

        // add the Characteristic to the Service
        mSampleService.addCharacteristic(mSampleCharacteristic);

        // add the Service to the Server/Peripheral
        if (null != mGattServer) {
            mGattServer.addService(mSampleService);
        }
    }

    private void setLightCircle(Light value) {
        switch (value) {
            case RED:
                mCircleView.setBackground(getDrawable(R.drawable.circle_red));
                return;
            case GREEN:
                mCircleView.setBackground(getDrawable(R.drawable.circle_green));
                return;
            case GREY:
            default:
                mCircleView.setBackground(getDrawable(R.drawable.circle_grey));
        }
    }

    private void startAdvertising() {
        if (isGrantedAllPermission()) {
            setGattServer();
            setBluetoothService();
            startService(getServiceIntent(this));
        } else {
            requestPermission();
        }
    }

    private void stopAdvertising() {
        stopService(getServiceIntent(this));
    }

    private Intent getServiceIntent(Context context) {
        return new Intent(context, PeripheralAdvertiseService.class);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAdvertising();
    }
}