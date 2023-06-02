package com.example.wearablewt;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class CalendarActivity extends AppCompatActivity {

    private LocalDate selectedDate;
    private TextView yearMonthText;
    private TextView selectedDateText;
    private TextView modifyDailyTrainingText;
    private LocalDate selectedMonth;
    private LinearLayout trainingSummaryLayout;
    private RecyclerView calendar;
    private LinearLayout trainingSummaryListLayout;
    private int densityDPI;

    private DBHelper dbHelper;
    Button prevBtn;
    Button nextBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        findView();

        selectedDate = LocalDate.now();
        selectedMonth = LocalDate.now();

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        densityDPI = displayMetrics.densityDpi;
        dbHelper = new DBHelper(CalendarActivity.this, 1);
        setCalendarView();
        setSelectedDate();
        setTrainingSummaryView();

        prevBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedMonth = selectedMonth.minusMonths(1);
                setCalendarView();
            }
        });

        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedMonth = selectedMonth.plusMonths(1);
                setCalendarView();
            }
        });

        selectedDateText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                int clickedDate = Integer.parseInt(selectedDateText.getText().toString().substring(3,5));
                if(clickedDate != selectedDate.getDayOfMonth()) {
                    selectedDate = selectedDate.plusDays(clickedDate - selectedDate.getDayOfMonth());
                }
                setTrainingSummaryView();
            }
        });
        /*temp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), TrainingRecordActivity.class);
                intent.putExtra("selectedDate", selectedDateText.getText());
                startActivity(intent);
            }
        });*/

        modifyDailyTrainingText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), TrainingRecordActivity.class);

                String yyyy = yearMonthText.getText().toString().substring(0,4);
                String MMdd = selectedDateText.getText().toString();

                intent.putExtra("selectedDate", yyyy + "-" + MMdd);
                startActivity(intent);
            }
        });

        /*
        trainingSummaryListLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), TrainingRecordActivity.class);
                intent.putExtra("selectedDate", selectedDateText.getText().toString());
                startActivity(intent);
            }
        });*/
    }

    private void findView() {
        yearMonthText = findViewById(R.id.yearMonthTextView);
        selectedDateText = findViewById(R.id.selectedDateTextView);
        //trainingSummaryLayout = findViewById(R.id.trainingSummaryLayout);
        ScrollView temp = findViewById(R.id.trainingSummaryScrollView);
        trainingSummaryListLayout = findViewById(R.id.trainingSummaryListLayout);
        prevBtn = findViewById(R.id.prevMonthButton);
        nextBtn = findViewById(R.id.nextMonthButton);
        calendar = findViewById(R.id.calendar);
        modifyDailyTrainingText = findViewById(R.id.modifyDailyTrainingTextView);
    }

    private String yearMonthFromDate(LocalDate date){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        return date.format(formatter);
    }

    private void setCalendarView() {
        yearMonthText.setText(yearMonthFromDate(selectedMonth));

        ArrayList<String> dayList = daysInMonthArray();

        CalendarAdapter adapter = new CalendarAdapter(dayList, selectedDateText, densityDPI);

        RecyclerView.LayoutManager manager = new GridLayoutManager(getApplicationContext(), 7);
        calendar.setLayoutManager(manager);
        calendar.setAdapter(adapter);
    }

    private void setTrainingSummaryView() {

        ViewGroup parent = trainingSummaryListLayout;
        parent.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        String yyyy = yearMonthText.getText().toString().substring(0,4);
        String MMdd = selectedDateText.getText().toString();

        String date = yyyy + "-" + MMdd;

        ArrayList<Pair<String, Integer>> dailyTrainingSummary = dbHelper.getDailyTrainingSummary(date);
        if(dailyTrainingSummary == null || dailyTrainingSummary.size() == 0) return ;
        for (int i = 0; i < dailyTrainingSummary.size(); i++) {
            View view = inflater.inflate(R.layout.training_summary_cell, parent, true);
        }

        for (int i = 0; i < dailyTrainingSummary.size(); i++) {
            View trainingSummary = trainingSummaryListLayout.getChildAt(i);
            TextView trainingNameTextView = trainingSummary.findViewById(R.id.summaryTrainingNameTextView);
            TextView setsNumTextView = trainingSummary.findViewById(R.id.summarySetsNumTextView);

            trainingNameTextView.setText(dailyTrainingSummary.get(i).first);
            setsNumTextView.setText(dailyTrainingSummary.get(i).second + "Set");
        }

    }

    private ArrayList<String> daysInMonthArray() {
        ArrayList<String> dayList = new ArrayList<>();

        int lastDate = selectedMonth.lengthOfMonth();
        int dayOfWeek = selectedMonth.withDayOfMonth(1).getDayOfWeek().getValue() % 7;
        System.out.println("dayOfWeek = " + dayOfWeek);
        int gridNum = ((dayOfWeek + lastDate) / 7 + ((dayOfWeek + lastDate) % 7 == 0 ? 0 : 1)) * 7;
        System.out.println("gridNum = " + gridNum);
        for(int i = 1; i <= gridNum; i++) {
            if(i <= dayOfWeek || i > dayOfWeek + lastDate) {
                dayList.add(" ");
            } else {
                dayList.add(String.valueOf(i - dayOfWeek));
            }
        }

        System.out.println("test : " + dayList.size());
        return dayList;
    }

    private void setSelectedDate(){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        selectedDateText.setText(selectedDate.format(formatter));
    }
}
