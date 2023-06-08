package com.example.wearablewt;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
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
import androidx.core.os.HandlerCompat;


import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.normal.TedPermission;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TrainingRecordActivity extends AppCompatActivity {

    TextView trainingDateText;
    TextView routineSettingText;
    TextView addTrainingTextView;
    TextView startDailyTrainingText;
    TextView settingBluetoothDeviceTextView;
    DBHelper dbHelper;
    LinearLayout trainingListLayout;
    HashMap<String, String> trainingPartMap;
    HashMap<String, String> trainingNameIdMap;
    HashMap<String, String> trainingIdNameMap;

    String selectedDate;
    ActivityResultLauncher<Intent> startActivityResultRoutine;
    ActivityResultLauncher<Intent> startActivityResultAddSet;
    Pair<ArrayList<String>, ArrayList<ArrayList<Record>>> trainingRecord;

    ArrayList<String> trainingNameList;
    private BluetoothAdapter bluetoothAdapter;
    private Set<BluetoothDevice> devices; // 블루투스 디바이스 데이터 셋
    private BluetoothDevice bluetoothDevice; // 블루투스 디바이스

    Handler bluetoothHandler;
    BluetoothSocket bluetoothSocket;

    final static int BT_REQUEST_ENABLE = 1;
    final static int BT_MESSAGE_READ = 2;
    final static int BT_CONNECTING_STATUS = 3;
    final static UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private boolean scanning = false;

    String selectedDeviceAddress;
    Bluetooth bluetooth;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training_record);

        Intent intent = getIntent();

        selectedDate = intent.getStringExtra("selectedDate");
        findView();
        initVar();

        //trainingRecord = dbHelper.getDailyTrainingRecord(selectedDate);
        updateTrainingRecord();
        settingBluetoothDeviceTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectBluetoothDevice();
            }
        });


        BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.e("TEST", action);
                if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                    //Toast.makeText(TrainingRecordActivity.this, "Bluetooth Connected3", Toast.LENGTH_LONG);
                    Log.e("TEST", "Receiver");
                    //BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    //String deviceAddress = device.getAddress();
                    //abortBroadcast();
                }
            }
        };

        startDailyTrainingText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(trainingListLayout.getChildCount() <= 1) return ;
                trainingNameList = new ArrayList<>();
                for (int i = 0; i < trainingListLayout.getChildCount() - 1; i++) {
                    TextView trainingNameTextView = trainingListLayout.getChildAt(i).findViewById(R.id.trainingNameText1);
                    String trainingName = trainingNameTextView.getText().toString();
                    trainingNameList.add(trainingName);
                }

                SharedPreferences connectedWearable = getSharedPreferences("connectedWearable", Activity.MODE_PRIVATE);
                selectedDeviceAddress = connectedWearable.getString("address", null);

                if(selectedDeviceAddress == null) {
                    Toast.makeText(TrainingRecordActivity.this, "웨어러블 기기를 선택해주세요", Toast.LENGTH_LONG);
                    return ;
                }
                Log.e("TEST", selectedDeviceAddress);
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {

                        ArrayList<String> trainingIdList = new ArrayList<>();
                        for(int i = 0; i < trainingNameList.size(); i++) {
                            trainingIdList.add(trainingNameIdMap.get(trainingNameList.get(i)));
                        }
                        bluetooth = new Bluetooth(TrainingRecordActivity.this, selectedDeviceAddress, trainingIdList, selectedDate);
                        bluetooth.scanDevices();
                    }
                });

                thread.start();
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
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

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        setResult(Activity.RESULT_OK, intent);
        super.onBackPressed();
    }

    private void findView() {
        trainingDateText = findViewById(R.id.trainingDateTextView);
        routineSettingText = findViewById(R.id.routineSettingTextView);
        addTrainingTextView = findViewById(R.id.addTrainingOnRecord);
        trainingListLayout = findViewById(R.id.trainingListLayout);
        startDailyTrainingText = findViewById(R.id.startDailyTrainingTextView);
        settingBluetoothDeviceTextView = findViewById(R.id.settingBluetoothDeviceTextView);
    }

    private void initVar() {
        dbHelper = new DBHelper(TrainingRecordActivity.this, 1);
        trainingNameIdMap = dbHelper.getTrainingNameIdMap();
        trainingIdNameMap = dbHelper.getTrainingIdNameMap();
        trainingDateText.setText(selectedDate);
    }

    public void updateTrainingRecord() {
        ViewGroup parent = trainingListLayout;
        //trainingListLayout.removeAllViews();
        trainingListLayout.removeViews(0, trainingListLayout.getChildCount() - 1);

        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        trainingRecord = dbHelper.getDailyTrainingRecord(trainingDateText.getText().toString());

        ArrayList<String> trainingIdList = trainingRecord.first;
        ArrayList<ArrayList<Record>> recordMapList = trainingRecord.second;

        for (int i = 0; i < trainingIdList.size(); i++) {
            int sequenceNum = i;
            String trainingId = trainingIdList.get(i);
            View view = inflater.inflate(R.layout.training_record_cell, parent, false);
            parent.addView(view, i);

            ImageView trainingImageView = view.findViewById(R.id.trainingImageView);
            Bitmap resized = DataProcessing.getTrainingImage(this, trainingId);
            trainingImageView.setImageBitmap(resized);

            TextView trainingNameTextView = view.findViewById(R.id.trainingNameText1);
            trainingNameTextView.setText(trainingIdNameMap.get(trainingId));

            LinearLayout setsLinearLayout = view.findViewById(R.id.setsLinearLayout);
            ArrayList<Record> recordList = recordMapList.get(i);

            for (int j = 0; j < recordList.size(); j++) {
                View set = inflater.inflate(R.layout.set_cell, setsLinearLayout, false);
                Record record = recordList.get(j);

                //View set = setsLinearLayout.getChildAt(j);
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

            TextView addSetTextView = view.findViewById(R.id.addSetTextView);
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
        tedPermission();

        if (ActivityCompat.checkSelfPermission(TrainingRecordActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

        }
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        devices = bluetoothAdapter.getBondedDevices();

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

            final CharSequence[] charSequences = bluetoothDeviceNameList.toArray(new CharSequence[bluetoothDeviceNameList.size()]);
            bluetoothDeviceNameList.toArray(new CharSequence[bluetoothDeviceNameList.size()]);

            builder.setItems(charSequences, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    CharSequence cs = bluetoothDeviceAddressList.get(which);
                    SharedPreferences connectedWearable = getSharedPreferences("connectedWearable", Activity.MODE_PRIVATE);
                    SharedPreferences.Editor editor = connectedWearable.edit();
                    editor.putString("address", cs.toString());
                    editor.apply();
                }
            });
            Handler mHandler = new Handler(Looper.getMainLooper());
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // 사용하고자 하는 코드
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                    }
                    }, 0);
            }
        //}
    }

    private void tedPermission() {

        PermissionListener permissionlistener = new PermissionListener() {
            @Override
            public void onPermissionGranted() {
            }

            @Override
            public void onPermissionDenied(List<String> deniedPermissions) {
            }
        };

        TedPermission.create()
                .setPermissionListener(permissionlistener)
                .setDeniedMessage("If you reject permission,you can not use this service\n\nPlease turn on permissions at [Setting] > [Permission]")
                .setPermissions(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
                .check();


    }


}
