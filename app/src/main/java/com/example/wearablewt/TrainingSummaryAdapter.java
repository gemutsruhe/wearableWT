package com.example.wearablewt;

import android.icu.util.Calendar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class TrainingSummaryAdapter extends RecyclerView.Adapter<TrainingSummaryAdapter.TrainingSummaryViewHolder>{

    ArrayList<String> trainingList;

    Calendar calendar;
    TextView selectedDateText;

    public TrainingSummaryAdapter(ArrayList<String> trainingList) {

        this.trainingList = trainingList;
    }

    class TrainingSummaryViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView trainingNameTextView;
        TextView setsNumTextView;

        public TrainingSummaryViewHolder(@NonNull View itemView) {
            super(itemView);
            trainingNameTextView = itemView.findViewById(R.id.summaryTrainingNameTextView);
            setsNumTextView = itemView.findViewById(R.id.summarySetsNumTextView);
        }

        public void onClick(View view) {
            //if(dateClickListener)
        }
    }

    @NonNull
    @Override
    public TrainingSummaryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        View view = inflater.inflate(R.layout.training_summary_cell, parent, false);

        return new TrainingSummaryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrainingSummaryViewHolder holder, int position) {
        holder.trainingNameTextView.setText("레그레이즈");
        holder.setsNumTextView.setText(trainingList.get(position));
    }

    @Override
    public int getItemCount() {
        return trainingList.size();
    }
}