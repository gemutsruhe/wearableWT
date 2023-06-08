package com.example.wearablewt;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class RoutineActivity extends AppCompatActivity {
    LinearLayout routineTrainingListLayout;
    HashMap<String, String> trainingPartMap;
    HashMap<String, String> trainingNameIdMap;
    HashMap<String, String> trainingIdNameMap;
    DBHelper dbHelper;
    EditText newRoutineNameEditText;
    LinearLayout routineListLayout;
    TextView addRoutine;
    TextView addTraining;
    LinearLayout addRoutineLayout;
    int isModify;
    String prevRoutineName;
    View selectedRoutine;
    TextView useSelectedRoutine;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routine_setting);

        addRoutineLayout = findViewById(R.id.addRoutineLayout);
        addRoutine = findViewById(R.id.addRoutineTextView);
        addTraining = findViewById(R.id.addTrainingOnRoutineTextView);
        routineTrainingListLayout = findViewById(R.id.routineTrainingListLayout);
        newRoutineNameEditText = findViewById(R.id.newRoutineNameEditText);
        dbHelper = new DBHelper(RoutineActivity.this, 1);
        trainingPartMap = dbHelper.getTrainingPartMap();
        trainingNameIdMap = dbHelper.getTrainingNameIdMap();
        trainingIdNameMap = new HashMap<>();
        useSelectedRoutine = findViewById(R.id.useSelectRoutineTextView);
        isModify = 0;
        Iterator<String> keys = trainingNameIdMap.keySet().iterator();
        while(keys.hasNext()) {
            String key = keys.next();
            trainingIdNameMap.put(trainingNameIdMap.get(key), key);
        }


        routineListLayout = findViewById(R.id.routineListLayout);

        addRoutineOnLayout();

        addRoutine.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addRoutineLayout.setVisibility(View.VISIBLE);
                useSelectedRoutine.setVisibility(View.GONE);
            }
        });

        ActivityResultLauncher<Intent> startActivityResult = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if(result.getResultCode() == Activity.RESULT_OK) {
                            Intent data = result.getData();
                            String selectedTraining = data.getStringExtra("selectedTraining");
                            String[] selectedTrainingList = selectedTraining.split(",");
                            addTrainingOnNewRoutine(null, selectedTrainingList);
                        }
                    }
                }
        );

        addTraining.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), TrainingListActivity.class);
                startActivityResult.launch(intent);
            }
        });

        TextView saveRoutine = findViewById(R.id.saveRoutine);
        saveRoutine.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isModify == 1) {
                    dbHelper.removeRoutine(prevRoutineName);
                    prevRoutineName = null;
                }
                String newRoutineName = newRoutineNameEditText.getText().toString();
                if(newRoutineName.length() != 0) {
                    ArrayList<String> trainingIdList = new ArrayList<>();

                    for(int i = 0; i < routineTrainingListLayout.getChildCount(); i++) {
                        TextView trainingNameTextView = routineTrainingListLayout.getChildAt(i).findViewById(R.id.addRoutineTrainingNameTextView);
                        String trainingName = trainingNameTextView.getText().toString();
                        trainingIdList.add(trainingNameIdMap.get(trainingName));
                    }
                    dbHelper.saveNewRoutine(newRoutineName, trainingIdList);
                    addRoutineOnLayout();
                    addRoutineLayout.setVisibility(View.GONE);
                    routineTrainingListLayout.removeAllViews();
                    newRoutineNameEditText.setText("");
                    useSelectedRoutine.setVisibility(View.VISIBLE);
                }
            }
        });

        TextView cancelAddRoutine = findViewById(R.id.cancelAddRoutine);
        cancelAddRoutine.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                routineTrainingListLayout.removeAllViews();
                addRoutineLayout.setVisibility(View.GONE);
                useSelectedRoutine.setVisibility(View.VISIBLE);
            }
        });

        useSelectedRoutine.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(selectedRoutine != null) {
                    TextView selectedRoutineNameTextView = selectedRoutine.findViewById(R.id.routineNameTextView);
                    String selectedRoutineName = selectedRoutineNameTextView.getText().toString();
                    Intent intent = new Intent();
                    intent.putExtra("selectedRoutineName", selectedRoutineName);
                    setResult(Activity.RESULT_OK, intent);
                    finish();
                }
            }
        });
    }

    void addTrainingOnNewRoutine(String prevRoutineName, String[] selectedTrainingNameList) {
        if(prevRoutineName != null) {
            EditText nameEditText = findViewById(R.id.newRoutineNameEditText);
            nameEditText.setText(prevRoutineName);
        }
        ViewGroup parent = routineTrainingListLayout;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        for(int i = 0; i < selectedTrainingNameList.length; i++) {
            View view = inflater.inflate(R.layout.add_routine_added_training_cell, parent, true);
        }

        for(int i = 0; i < selectedTrainingNameList.length; i++) {
            int index = i + routineTrainingListLayout.getChildCount() - selectedTrainingNameList.length;

            ImageView trainingImageView = routineTrainingListLayout.getChildAt(index).findViewById(R.id.addRoutineTrainingImageView);
            String trainingImageName = DataProcessing.trainingIdToImageName(trainingNameIdMap.get(selectedTrainingNameList[i]));
            int imageId = getResources().getIdentifier(trainingImageName, "drawable", getPackageName());
            Bitmap src  = BitmapFactory.decodeResource(getResources(), imageId);
            Bitmap resized = Bitmap.createScaledBitmap(src, 120, 80, true);
            trainingImageView.setImageBitmap(resized);

            TextView trainingNameTextView = routineTrainingListLayout.getChildAt(index).findViewById(R.id.addRoutineTrainingNameTextView);
            trainingNameTextView.setText(selectedTrainingNameList[i]);

            TextView trainingPartTextView = routineTrainingListLayout.getChildAt(index).findViewById(R.id.addRoutineTrainingPartTextView);
            trainingPartTextView.setText(trainingPartMap.get(selectedTrainingNameList[i]));
        }
    }

    void addRoutineOnLayout(){
        ViewGroup parent = routineListLayout;
        routineListLayout.removeAllViews();
        HashMap<String, ArrayList<String>> routineNameTrainingIdMap = dbHelper.getRoutineList();
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        for(int i = 0; i < routineNameTrainingIdMap.size(); i++) {
            View view = inflater.inflate(R.layout.routine_cell, parent, true);
        }
        Iterator<String> keys = routineNameTrainingIdMap.keySet().iterator();
        int count = 0;
        while(keys.hasNext()){
            String key = keys.next();
            View routineLayout = routineListLayout.getChildAt(count);
            TextView routineNameTextView = routineLayout.findViewById(R.id.routineNameTextView);
            routineNameTextView.setText(key);
            count++;
            ArrayList<String> trainingIdList = routineNameTrainingIdMap.get(key);

            ViewGroup routineTrainingListLayout = routineLayout.findViewById(R.id.routineTrainingListLayout);
            Log.e("TEST", routineNameTextView.getText().toString());
            routineNameTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(routineLayout.equals(selectedRoutine)) {
                        selectedRoutine.setBackgroundColor(Color.WHITE);
                        selectedRoutine = null;
                    } else {
                        if(selectedRoutine != null) selectedRoutine.setBackgroundColor(Color.WHITE);
                        selectedRoutine = routineLayout;
                        selectedRoutine.setBackgroundResource(R.drawable.selected_layout_border);
                    }
                }
            });

            for(int i = 0; i < trainingIdList.size(); i++) {
                View view = inflater.inflate(R.layout.routine_list_cell, routineTrainingListLayout, true);
            }

            for(int i = 0; i < trainingIdList.size(); i++) {
                ImageView trainingImageView = routineTrainingListLayout.getChildAt(i).findViewById(R.id.routineTrainingListImageView);
                String trainingImageName = DataProcessing.trainingIdToImageName(trainingIdList.get(i));
                int imageId = getResources().getIdentifier(trainingImageName, "drawable", getPackageName());

                Bitmap src = BitmapFactory.decodeResource(getResources(), imageId);
                Bitmap resized = Bitmap.createScaledBitmap(src, 120, 80, true);
                trainingImageView.setImageBitmap(resized);

                View routineTrainingLayout = routineTrainingListLayout.getChildAt(i);
                TextView trainingNameTextView = routineTrainingLayout.findViewById(R.id.routineTrainingNameTextView);
                trainingNameTextView.setText(trainingIdNameMap.get(trainingIdList.get(i)));
            }

            TextView showRoutineSequenceTextView = routineLayout.findViewById(R.id.showRoutineSequenceTextView);
            showRoutineSequenceTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(routineTrainingListLayout.getVisibility() == View.VISIBLE) {
                        routineTrainingListLayout.setVisibility(View.GONE);
                    } else {
                        routineTrainingListLayout.setVisibility(View.VISIBLE);
                    }
                }
            });
            TextView modifyRoutineTextView = routineLayout.findViewById(R.id.modifyRoutineTextView);
            modifyRoutineTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    isModify = 1;
                    prevRoutineName = key;
                    addRoutineLayout.setVisibility(View.VISIBLE);
                    routineTrainingListLayout.setVisibility(View.GONE);
                    String[] trainingNameList = new String[trainingIdList.size()];
                    for(int i = 0; i < trainingIdList.size(); i++) {
                        String trainingName = trainingIdNameMap.get(trainingIdList.get(i));
                        trainingNameList[i] = trainingName;
                    }
                    addTrainingOnNewRoutine(key, trainingNameList);
                }
            });

            TextView removeRoutineTextView = routineLayout.findViewById(R.id.removeRoutineTextView);
            removeRoutineTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dbHelper.removeRoutine(routineNameTextView.getText().toString());
                    routineListLayout.removeView(routineLayout);
                }
            });
        }
    }
}
