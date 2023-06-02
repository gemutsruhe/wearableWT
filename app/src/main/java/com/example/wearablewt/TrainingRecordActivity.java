package com.example.wearablewt;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;


import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.normal.TedPermission;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class TrainingRecordActivity extends AppCompatActivity {

    TextView trainingDateText;
    TextView routineSettingText;
    TextView addTrainingTextView;
    TextView startDailyTrainingText;
    DBHelper dbHelper;
    LinearLayout trainingListLayout;
    HashMap<String, String> trainingPartMap;
    HashMap<String, String> trainingNameIdMap;
    HashMap<String, String> trainingIdNameMap;

    String selectedDate;
    ActivityResultLauncher<Intent> startActivityResultRoutine;
    ActivityResultLauncher<Intent> startActivityResultAddSet;
    Pair<ArrayList<String>, ArrayList<ArrayList<Record>>> trainingRecord;

    private BluetoothAdapter bluetoothAdapter;
    private Set<BluetoothDevice> devices; // 블루투스 디바이스 데이터 셋
    private BluetoothDevice bluetoothDevice; // 블루투스 디바이스

    Handler bluetoothHandler;
    ConnectedBluetoothThread connectedBluetoothThread;
    BluetoothSocket bluetoothSocket;

    final static int BT_REQUEST_ENABLE = 1;
    final static int BT_MESSAGE_READ = 2;
    final static int BT_CONNECTING_STATUS = 3;
    final static UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training_record);

        Intent intent = getIntent();

        selectedDate = intent.getStringExtra("selectedDate");
        findView();
        initVar();

        trainingRecord = dbHelper.getDailyTrainingRecord(selectedDate);

        trainingDateText.setText(selectedDate);
        startDailyTrainingText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (ActivityCompat.checkSelfPermission(TrainingRecordActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    tedPermission();
                } else {
                    Toast.makeText(TrainingRecordActivity.this, "Permission Granted", Toast.LENGTH_SHORT).show();
                }
                selectBluetoothDevice();

                if(connectedBluetoothThread != null && bluetoothDevice != null) {
                    for(int i = 0; i < trainingListLayout.getChildCount(); i++) {
                        TextView trainingNameTextView = trainingListLayout.getChildAt(i).findViewById(R.id.trainingNameText1);
                        String trainingName = trainingNameTextView.getText().toString();
                        connectedBluetoothThread.write(trainingNameIdMap.get(trainingName));
                    }
                    connectedBluetoothThread.write("startTraining");
                }
                //bluetoothAdaptor
            }
        });

        startActivityResultRoutine = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            Intent data = result.getData();
                            updateTrainingRecord();
                        }
                    }
                }
        );

        routineSettingText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), RoutineActivity.class);
                startActivityResultRoutine.launch(intent);
            }
        });

        startActivityResultAddSet = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            updateTrainingRecord();
                        }
                    }
                }
        );

        ActivityResultLauncher<Intent> startActivityResultTraining = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            Intent data = result.getData();
                            String selectedTrainingName = data.getStringExtra("selectedTraining");
                            String[] selectedTrainingArray = selectedTrainingName.split(",");
                            ArrayList<String> selectedTrainingList = new ArrayList<>();

                            for (int i = 0; i < selectedTrainingArray.length; i++) {
                                selectedTrainingList.add(trainingNameIdMap.get(selectedTrainingArray[i]));
                            }

                            dbHelper.addDailyTrainingList(selectedDate, selectedTrainingList);

                            updateTrainingRecord();
                        }
                    }
                }
        );

        addTrainingTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), TrainingListActivity.class);
                startActivityResultTraining.launch(intent);
            }
        });
    }

    private void findView() {
        trainingDateText = findViewById(R.id.trainingDateTextView);
        routineSettingText = findViewById(R.id.routineSettingTextView);
        addTrainingTextView = findViewById(R.id.addTrainingOnRecord);
        trainingListLayout = findViewById(R.id.trainingListLayout);
        startDailyTrainingText = findViewById(R.id.startDailyTrainingTextView);
    }

    private void initVar() {
        dbHelper = new DBHelper(TrainingRecordActivity.this, 1);
        trainingNameIdMap = dbHelper.getTrainingNameIdMap();
        trainingIdNameMap = dbHelper.getTrainingIdNameMap();
    }

    public void updateTrainingRecord() {
        ViewGroup parent = trainingListLayout;
        trainingListLayout.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        trainingRecord = dbHelper.getDailyTrainingRecord(trainingDateText.getText().toString());

        ArrayList<String> trainingIdList = trainingRecord.first;
        ArrayList<ArrayList<Record>> recordMapList = trainingRecord.second;

        for (int i = 0; i < trainingIdList.size(); i++) {
            View view = inflater.inflate(R.layout.training_record_cell, parent, true);
        }

        for (int i = 0; i < trainingIdList.size(); i++) {
            String trainingId = trainingIdList.get(i);
            View cell = parent.getChildAt(i);
            int sequenceNum = i;
            ImageView trainingImageView = cell.findViewById(R.id.trainingImageView);
            Bitmap resized = DataProcessing.getTrainingImage(this, trainingId);
            trainingImageView.setImageBitmap(resized);

            TextView trainingNameTextView = cell.findViewById(R.id.trainingNameText1);
            trainingNameTextView.setText(trainingIdNameMap.get(trainingId));

            LinearLayout setsLinearLayout = cell.findViewById(R.id.setsLinearLayout);
            ArrayList<Record> recordList = recordMapList.get(i);

            for (int j = 0; j < recordList.size(); j++) {
                View view = inflater.inflate(R.layout.set_cell, setsLinearLayout, true);
            }

            for (int j = 0; j < recordList.size(); j++) {
                Record record = recordList.get(j);

                View set = setsLinearLayout.getChildAt(j);
                TextView weightNumTextView1 = set.findViewById(R.id.weightNumTextView1);
                TextView repeatTextView1 = set.findViewById(R.id.repeatTextView1);
                TextView setNumTextView = set.findViewById(R.id.setNumTextView);
                ImageButton deleteSetButton = set.findViewById(R.id.deleteSetButton);

                setNumTextView.setText(String.valueOf(record.getSetsNum()) + "세트");
                weightNumTextView1.setText(String.valueOf(record.getWeight()) + record.getUnit());
                repeatTextView1.setText(String.valueOf(record.getRepeat()) + "회");

                deleteSetButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dbHelper.deleteRecord(selectedDate, trainingId, sequenceNum, record.getSetsNum());
                        updateTrainingRecord();
                    }
                });
            }

            TextView addSetTextView = cell.findViewById(R.id.addSetTextView);
            addSetTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getApplicationContext(), AddSetActivity.class);

                    intent.putExtra("dateId", selectedDate);
                    intent.putExtra("trainingId", trainingId);
                    intent.putExtra("trainingName", trainingIdNameMap.get(trainingId));

                    intent.putExtra("sequenceNum", sequenceNum);
                    intent.putExtra("setsNum", setsLinearLayout.getChildCount() + 1);
                    intent.putExtra("weight", 0);
                    intent.putExtra("repeat", 0);
                    intent.putExtra("unit", "kg");
                    intent.putExtra("unitWeight", 5);

                    startActivityResultAddSet.launch(intent);
                }
            });
        }
    }

    public void selectBluetoothDevice() {

        SharedPreferences connectedWearable = getSharedPreferences("connectedWearable", Activity.MODE_PRIVATE);
        String address = connectedWearable.getString("address", null);

        if (ActivityCompat.checkSelfPermission(TrainingRecordActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            //ActivityCompat.requestPermissions( TrainingRecordActivity.this, new String[]{android.Manifest.permission.BLUETOOTH_CONNECT},
        }
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        devices = bluetoothAdapter.getBondedDevices();

        if (address != null && connectDevice(address)) {
            Toast.makeText(TrainingRecordActivity.this, "자동 연결 완료", Toast.LENGTH_SHORT).show();
        } else {
            int pairedDeviceCount = devices.size();
            if (pairedDeviceCount == 0) {
                Toast.makeText(TrainingRecordActivity.this, "페어링 되어있는 디바이스가 없습니다", Toast.LENGTH_SHORT).show();
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("페어링 블루투스 디바이스 목록");

                List<String> bluetoothDeviceNameList = new ArrayList<>();
                List<String> bluetoothDeviceAddressList = new ArrayList<>();

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
                        Toast.makeText(TrainingRecordActivity.this, charSequences[which], Toast.LENGTH_LONG).show();
                        CharSequence cs = bluetoothDeviceAddressList.get(which);
                        connectDevice(cs.toString());
                    }
                });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            }
        }
    }

    private boolean connectDevice(String address) {
        for (BluetoothDevice device : devices) {
            if (address.equals(device.getAddress())) {
                bluetoothDevice = device;
                SharedPreferences connectedWearable = getSharedPreferences("connectedWearable", Activity.MODE_PRIVATE);
                SharedPreferences.Editor editor = connectedWearable.edit();
                editor.putString("address", address);
                editor.apply();
                return true;
            }
        }

        return false;
    }

    private void tedPermission() {

        PermissionListener permissionlistener = new PermissionListener() {
            @Override
            public void onPermissionGranted() {
                Toast.makeText(TrainingRecordActivity.this, "Permission Granted", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPermissionDenied(List<String> deniedPermissions) {
                Toast.makeText(TrainingRecordActivity.this, "Permission Denied\n" + deniedPermissions.toString(), Toast.LENGTH_SHORT).show();
            }
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissions(
                    new String[]{
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_ADVERTISE,
                            Manifest.permission.BLUETOOTH_CONNECT
                    },
                    1);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            requestPermissions(
                    new String[]{
                            Manifest.permission.BLUETOOTH
                    },
                    1);
        }

        /*TedPermission.create()
                .setPermissionListener(permissionlistener)
                .setDeniedMessage("If you reject permission,you can not use this service\n\nPlease turn on permissions at [Setting] > [Permission]")
                .setPermissions(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
                .check();*/
    }


    void connectSelectedDevice(String selectedDeviceName) {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(BT_UUID);
            bluetoothSocket.connect();
            connectedBluetoothThread = new ConnectedBluetoothThread(bluetoothSocket);
            connectedBluetoothThread.start();
            bluetoothHandler.obtainMessage(BT_CONNECTING_STATUS, 1, -1).sendToTarget();
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "블루투스 연결 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
        }
    }

    private class ConnectedBluetoothThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedBluetoothThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "소켓 연결 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = mmInStream.available();
                    if (bytes != 0) {
                        SystemClock.sleep(100);
                        bytes = mmInStream.available();
                        bytes = mmInStream.read(buffer, 0, bytes);
                        bluetoothHandler.obtainMessage(BT_MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                        if(buffer.toString().compareTo("finishTraining") == 0) {
                            return ;
                        } else {

                        }
                }
                } catch (IOException e) {
                    break;
                }
            }
        }
        public void write(String str) {
            byte[] bytes = str.getBytes();
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "데이터 전송 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
            }
        }
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "소켓 해제 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
            }
        }
    }

}
