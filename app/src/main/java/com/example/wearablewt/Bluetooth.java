package com.example.wearablewt;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.os.HandlerCompat;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Bluetooth {
    public static final int INTENT_REQUEST_BLUETOOTH_ENABLE = 0x0701;

    private final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
    //private final List<BluetoothGatt> gattList = new ArrayList<>();
    BluetoothGatt gatt = null;
    private final HashMap<String, BluetoothDevice> hashDeviceMap = new HashMap<>();
    private final Handler mainThreadHandler = HandlerCompat.createAsync(Looper.getMainLooper());

    private boolean scanning = false;
    Context context;
    String selectedDeviceAddress;

    Boolean connected = false;
    DBHelper dbHelper;
    BluetoothLeScanner scanner;

    String date;

    ArrayList<String> trainingIdList;
    Bluetooth(Context context, String selectedDeviceAddress, ArrayList<String> trainingIdList, String date) {
        this.context = context;
        this.selectedDeviceAddress = selectedDeviceAddress;
        this.date = date;
        this.trainingIdList = trainingIdList;
        dbHelper = new DBHelper(context, 1);
    }

    /**
     * System Bluetooth On Check
     */
    public boolean isOn() {
        return adapter.isEnabled();
    }

    /**
     * System Bluetooth On
     */
    public void on(AppCompatActivity activity) {
        if (!adapter.isEnabled()) {
        }
    }

    /**
     * System Bluetooth On Result
     */
    public boolean onActivityResult(int requestCode, int resultCode) {
        return requestCode == Bluetooth.INTENT_REQUEST_BLUETOOTH_ENABLE
                && Activity.RESULT_OK == resultCode;
    }


    /**
     * System Bluetooth Off
     */
    public void off() {
        if (adapter.isEnabled())
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

                return;
            }
        adapter.disable();
    }

    /**
     * Check model for ScanRecodeData
     */
    private final ScanCallback callback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (connected == false && result.getDevice().getAddress().compareTo(selectedDeviceAddress) == 0) {

                if(connGATT(context, result.getDevice()) == true) {
                    connected = true;
                } else {

                }
            } else return;

        }
    };

    public BluetoothGatt getGatt() {
        return gatt;
    }

    public void scanDevices() {
        scanning = true;
        if (!adapter.isEnabled()) return;
        if (!scanning) return;
        scanner = adapter.getBluetoothLeScanner();
        mainThreadHandler.postDelayed(() -> {
            scanning = false;
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            scanner.stopScan(callback);
        }, 2 * 60 * 1000);

        scanning = true;
        scanner.startScan(callback);
    }

    public boolean connGATT(Context context, BluetoothDevice device) {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        gatt = device.connectGatt(context, false, gattCallback);
        if(gatt != null) return true;
        else return false;
    }

    public void disconnectGATT() {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        gatt.disconnect();
        gatt.close();
    }


    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (status == BluetoothGatt.GATT_FAILURE) {
                if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

                    return;
                }
                gatt.disconnect();
                gatt.close();
                hashDeviceMap.remove(gatt.getDevice().getAddress());
                return;
            }
            if (status == 133) // Unknown Error
            {
                gatt.disconnect();
                gatt.close();
                hashDeviceMap.remove(gatt.getDevice().getAddress());
                return;
            }
            if (newState == BluetoothGatt.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                gatt.discoverServices();
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            if (status == BluetoothGatt.GATT_SUCCESS) {

                List<BluetoothGattService> services = gatt.getServices();
                for (BluetoothGattService service : services) {

                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {

                        if (hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_READ)) {

                            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                return;
                            }

                            gatt.readCharacteristic(characteristic);
                        }

                        if (hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_NOTIFY)) {
                            gatt.setCharacteristicNotification(characteristic, false);
                        }

                        if (hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_WRITE)) {

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                gatt.writeCharacteristic(characteristic, characteristic.getUuid().toString().getBytes(), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                            } else {
                                for (int i = 0; i < trainingIdList.size(); i++) {
                                    characteristic.setValue(trainingIdList.get(i).getBytes());
                                    gatt.writeCharacteristic(characteristic);
                                }
                                characteristic.setValue("startTraining".getBytes());
                                gatt.writeCharacteristic(characteristic);

                            }
                            //disconnectGATT();
                            //break;
                        }
                    }
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

            if( status == BluetoothGatt.GATT_SUCCESS) {
                byte[] dataArray = characteristic.getValue();
                String data = dataArray.toString();
                if(data.compareTo("finishTraining") == 0) {
                    disconnectGATT();
                } else {
                    String[] splitData = data.split(",");
                    String trainingId = splitData[0];
                    int sequenceNum = Integer.parseInt(splitData[1]);
                    int setsNum = Integer.parseInt(splitData[2]);
                    double weightNum = Double.parseDouble(splitData[3]);
                    String weightUnit = splitData[4];
                    int repeatNum = Integer.parseInt(splitData[5]);
                    dbHelper.addRecord(date, trainingId, sequenceNum, setsNum, weightNum, weightUnit, repeatNum);
                }
            }
        }


        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

            super.onCharacteristicWrite(gatt, characteristic, status);
            if(status == BluetoothGatt.GATT_SUCCESS) {

            } else {

            }
        }

    };

    public boolean hasProperty(BluetoothGattCharacteristic characteristic, int property)
    {
        int prop = characteristic.getProperties() & property;
        return prop == property;
    }

    private byte[] stringToBytes(String data) {
        return data.getBytes(StandardCharsets.UTF_8);
    }
}
