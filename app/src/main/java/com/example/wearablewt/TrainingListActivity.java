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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;

public class TrainingListActivity extends AppCompatActivity {

    LinearLayout trainingPartListLayout;
    LinearLayout trainingListLayout;
    TextView addTrainingTextView;
    String selectedPart;
    DBHelper dbHelper;
    ArrayList<String> selectedTraining;
    HashMap<String, String> trainingNameIdMap;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training_list);

        trainingPartListLayout = findViewById(R.id.trainingPartListLayout);
        trainingListLayout = findViewById(R.id.trainingListLayout);
        addTrainingTextView = findViewById(R.id.addTrainingTextView);
        selectedTraining = new ArrayList<>();


        dbHelper = new DBHelper(TrainingListActivity.this, 1);
        trainingNameIdMap = dbHelper.getTrainingNameIdMap();

        addTrainingPart();

        selectedPart = "전체";
        addTrainingList();

        addTrainingTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(selectedTraining.size() > 0) {
                    Intent intent = new Intent();
                    String trainingList = "";
                    for(int i = 0; i < selectedTraining.size(); i++) {
                        trainingList += selectedTraining.get(i);
                        if(i != selectedTraining.size() - 1) trainingList += ",";
                    }
                    intent.putExtra("selectedTraining", trainingList);
                    setResult(Activity.RESULT_OK, intent);
                    finish();
                }
            }
        });
    }

    public void addTrainingPart() {
        ViewGroup parent = trainingPartListLayout;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ArrayList<String> partList = dbHelper.getTrainingPartList();

        for(int i = 0; i < partList.size(); i++) {
            View view = inflater.inflate(R.layout.part_cell, parent, true);
        }

        for(int i = 0; i < trainingPartListLayout.getChildCount(); i++) {
            TextView partNameTextView = trainingPartListLayout.getChildAt(i).findViewById(R.id.partNameTextView);
            partNameTextView.setText(partList.get(i));
            partNameTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(selectedPart.compareTo(partNameTextView.getText().toString()) != 0) {
                        selectedPart = partNameTextView.getText().toString();

                        addTrainingList();
                    }
                }
            });
        }
    }

    public void addTrainingList(){
        ViewGroup parent = trainingListLayout;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        trainingListLayout.removeAllViews();
        ArrayList<ArrayList<String>> trainingList = dbHelper.getTrainingList(selectedPart);

        for(int i = 0; i < trainingList.size(); i++) {
            for(int j = 0; j < trainingList.get(i).size(); j++) {
                View view = inflater.inflate(R.layout.training_cell, parent, true);
            }
        }
        int count = 0;
        for(int i = trainingList.size() - 1; i >= 0; i--) {

            for(int j = 0; j < trainingList.get(i).size(); j++) {
                View trainingLayout = trainingListLayout.getChildAt(count);
                ImageView trainingImageView = trainingLayout.findViewById(R.id.addRoutineTrainingImageView);
                String trainingId = trainingNameIdMap.get(trainingList.get(i).get(j));

                String trainingImageName = DataProcessing.trainingIdToImageName(trainingId);
                int imageId = getResources().getIdentifier(trainingImageName, "drawable", getPackageName());

                Bitmap src  = BitmapFactory.decodeResource(getResources(), imageId);
                Bitmap resized = Bitmap.createScaledBitmap(src, 120, 80, true);
                trainingImageView.setImageBitmap(resized);
                TextView trainingNameTextView = trainingLayout.findViewById(R.id.routineTrainingNameTextView);
                trainingLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String thisTraining = trainingNameTextView.getText().toString();
                        int i;
                        for(i = 0; i < selectedTraining.size(); i++) {
                            if(selectedTraining.get(i).compareTo(thisTraining) == 0) {
                                break;
                            }
                        }
                        if(i == selectedTraining.size()) {
                            trainingLayout.setBackgroundResource(R.drawable.selected_layout_border);
                            selectedTraining.add(thisTraining);
                        } else {
                            trainingLayout.setBackgroundColor(Color.WHITE);
                            selectedTraining.remove(i);
                        }
                    }
                });

                trainingNameTextView.setText(trainingList.get(i).get(j));
                TextView favoriteTextView = trainingLayout.findViewById(R.id.favoriteTextView);
                if(i == 0) favoriteTextView.setTextColor(Color.GRAY);
                favoriteTextView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int favorite;
                        if(favoriteTextView.getCurrentTextColor() == 0XFFFFEB3B) {
                            favorite = 0;
                            favoriteTextView.setTextColor(Color.GRAY);
                        } else {
                            favorite = 1;
                            favoriteTextView.setTextColor(0XFFFFEB3B);
                        }
                        dbHelper.updateFavorite(trainingNameTextView.getText().toString(), favorite);
                    }
                });


                for(int k = 0; k < selectedTraining.size(); k++) {
                    if (selectedTraining.get(k).compareTo(trainingNameTextView.getText().toString()) == 0) {
                        trainingLayout.setBackgroundResource(R.drawable.selected_layout_border);
                    }
                }

                count++;
            }

        }
    }
}
