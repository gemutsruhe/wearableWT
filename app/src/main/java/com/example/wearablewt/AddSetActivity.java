package com.example.wearablewt;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AddSetActivity extends Activity {

    TextView trainingNameTextView;
    EditText weightText;
    TextView increaseWeightTextView;
    TextView decreaseWeightTextView;
    EditText repeatText;
    TextView increaseRepeatTextView;
    TextView decreaseRepeatTextView;
    TextView weightUnitTextView;
    TextView cancelTextView;
    TextView addTextView;

    String dateId;
    String trainingId;
    String trainingName;
    int sequenceNum;
    int setsNum;
    double weight;
    int repeat;
    double unitWeight;
    String unit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_add_set);

        unitWeight = 5;
        unit = "kg";

        findView();
        getIntentData();
        initView();
        setClickListener();
    }

    private void findView() {
        trainingNameTextView = findViewById(R.id.trainingNameOnAddSet);
        weightText = findViewById(R.id.weightEditText);
        repeatText = findViewById(R.id.repeatEditText);
        increaseWeightTextView = findViewById(R.id.increaseWeightTextView);
        decreaseWeightTextView = findViewById(R.id.decreaseWeightTextView);
        weightUnitTextView = findViewById(R.id.weightUnitTextView);
        increaseRepeatTextView = findViewById(R.id.increaseRepeatTextView);
        decreaseRepeatTextView = findViewById(R.id.decreaseRepeatTextView);
        cancelTextView = findViewById(R.id.cancelAddSetTextView);
        addTextView = findViewById(R.id.confirmAddSetTextView);
    }

    private void getIntentData() {
        Intent intent = getIntent();
        dateId = intent.getStringExtra("dateId");
        trainingId = intent.getStringExtra("trainingId");
        trainingName = intent.getStringExtra("trainingName");
        sequenceNum = intent.getIntExtra("sequenceNum", 0);
        setsNum = intent.getIntExtra("setsNum", 0);
        weight = intent.getDoubleExtra("weight", 0);
        repeat = intent.getIntExtra("repeat", 0);
        unit = intent.getStringExtra("unit");
        unitWeight = intent.getIntExtra("unitWeight", 0);
    }

    private void initView() {
        trainingNameTextView.setText(trainingName);
        increaseWeightTextView.setText("+" + unitWeight);
        decreaseWeightTextView.setText("-" + unitWeight);
        weightText.setText(String.valueOf(weight));
        repeatText.setText(String.valueOf(repeat));
    }

    private void setClickListener(){
        increaseWeightTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double weightNum = Double.parseDouble(weightText.getText().toString());
                weightNum += unitWeight;
                weightText.setText(String.valueOf(weightNum));
            }
        });

        decreaseWeightTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double weightNum = Double.parseDouble(weightText.getText().toString());
                weightNum -= unitWeight;
                weightText.setText(String.valueOf(weightNum > 0 ? weightNum : 0));
            }
        });

        weightUnitTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(weightUnitTextView.getText().toString().compareTo("kg") == 0) {
                    weightUnitTextView.setText("lb");
                } else {
                    weightUnitTextView.setText("kg");
                }
            }
        });

        increaseRepeatTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int repeatNum = Integer.parseInt(repeatText.getText().toString());
                repeatText.setText(String.valueOf(repeatNum + 1));
            }
        });

        decreaseRepeatTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int repeatNum = Integer.parseInt(repeatText.getText().toString());
                if(repeatNum > 0)  repeatText.setText(String.valueOf(repeatNum - 1));
            }
        });

        cancelTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        addTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                weight = Double.parseDouble(weightText.getText().toString());
                repeat = Integer.parseInt(repeatText.getText().toString());
                unit = weightUnitTextView.getText().toString();

                DBHelper dbHelper = new DBHelper(AddSetActivity.this, 1);
                dbHelper.addRecord(dateId, trainingId, sequenceNum, setsNum, weight, unit, repeat);

                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        });
    }
}
