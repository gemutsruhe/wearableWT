package com.example.wearablewt;

import static androidx.core.app.ActivityCompat.startActivityForResult;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ConnectBluetooth extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 10; // 블루투스 활성화 상태
    private BluetoothAdapter bluetoothAdapter; // 블루투스 어댑터
    private Set<BluetoothDevice> devices; // 블루투스 디바이스 데이터 셋
    private BluetoothDevice bluetoothDevice; // 블루투스 디바이스
    private BluetoothSocket bluetoothSocket = null; // 블루투스 소켓
    private OutputStream outputStream = null; // 블루투스에 데이터를 출력하기 위한 출력 스트림
    private InputStream inputStream = null; // 블루투스에 데이터를 입력하기 위한 입력 스트림
    private Thread workerThread = null; // 문자열 수신에 사용되는 쓰레드
    private byte[] readBuffer; // 수신 된 문자열을 저장하기 위한 버퍼
    private int readBufferPosition; // 버퍼 내 문자 저장 위치
    static final int REQUEST_CODE = 100;
    private View dummy;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dummy);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        dummy = findViewById(R.id.dummyLayout);
        enableBluetooth();

        //finish();
    }

    public void enableBluetooth() {
        if (bluetoothAdapter == null) {

        } else {
            if (bluetoothAdapter.isEnabled()) {
                Log.e("TEST", "bluetooth Enabled");
                selectBluetoothDevice();
            } else {
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                if (ActivityCompat.checkSelfPermission(ConnectBluetooth.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                startActivityForResult(intent, REQUEST_ENABLE_BT);
            }
        }
    }

    public void selectBluetoothDevice() {

        SharedPreferences connectedWearable = getSharedPreferences("connectedWearable", Activity.MODE_PRIVATE);
        String address = connectedWearable.getString("address", null);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        devices = bluetoothAdapter.getBondedDevices();
        Log.e("TEST", String.valueOf(devices.size()));
        if (address != null) {

            //connectDevice(address);
        }

        if (bluetoothDevice != null) {

            return;
        }
        int pairedDeviceCount = devices.size();
        if (pairedDeviceCount == 0) {
            /*AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
            builder.setTitle("페어링 되어있는 블루투스 디바이스 목록");
            List<String> bluetoothDeviceNameList = new ArrayList<>();
            bluetoothDeviceNameList.add("취소");
            final CharSequence[] charSequences = bluetoothDeviceNameList.toArray(new CharSequence[bluetoothDeviceNameList.size()]);
            builder.setItems(charSequences, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //SharedPreferences.Editor editor = connectedWearable.edit();
                    //editor.putString("address", charSequences[which].toString());
                    //editor.apply();
                    //connectDevice(charSequences[which].toString());

                }
            });
            AlertDialog alertDialog = builder.create();
            alertDialog.show();*/
            Log.e("TEST", String.valueOf(bluetoothAdapter.startDiscovery()));
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
            builder.setTitle("페어링 되어있는 블루투스 디바이스 목록");
            List<String> bluetoothDeviceNameList = new ArrayList<>();
            List<String> bluetoothDeviceAddressList = new ArrayList<>();
            for (BluetoothDevice device : devices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress();
            }
            for (BluetoothDevice bluetoothDevice : devices) {
                bluetoothDeviceNameList.add(bluetoothDevice.getName());
                bluetoothDeviceAddressList.add(bluetoothDevice.getAddress());
            }
            bluetoothDeviceNameList.add("취소");
            // List를 CharSequence 배열로 변경
            final CharSequence[] charSequences = bluetoothDeviceNameList.toArray(new CharSequence[bluetoothDeviceNameList.size()]);
            bluetoothDeviceNameList.toArray(new CharSequence[bluetoothDeviceNameList.size()]);
            // 해당 아이템을 눌렀을 때 호출 되는 이벤트 리스너
            builder.setItems(charSequences, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    SharedPreferences.Editor editor = connectedWearable.edit();
                    editor.putString("address", charSequences[which].toString());
                    editor.apply();
                    connectDevice(charSequences[which].toString());

                }
            });
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }
    }

    public void connectDevice(String address) {
        for (BluetoothDevice device : devices) {
            if (address.equals(device.getAddress())) {
                bluetoothDevice = device;
                break;
            }
        }

        UUID uuid = java.util.UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

        // Rfcomm 채널을 통해 블루투스 디바이스와 통신하는 소켓 생성

        try {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
            bluetoothSocket.connect();
            // 데이터 송,수신 스트림을 얻어옵니다.
            outputStream = bluetoothSocket.getOutputStream();
            inputStream = bluetoothSocket.getInputStream();
            // 데이터 수신 함수 호출
            //receiveData();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.e("TEST", "requestPermission2");
        Log.e("TEST", String.valueOf(requestCode));
        switch(requestCode) {
            case REQUEST_CODE:
                Log.e("TEST", "requestPermission3");
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.e("TEST", "requestPermission3");
                }
                break;
        }
    }
}
