package com.example.wearablewt;

import android.icu.util.Calendar;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>{

    ArrayList<String> dayList;

    Calendar calendar;
    TextView selectedDateText;
    int densityDPI;

    public CalendarAdapter(ArrayList<String> dayList, TextView selectedDateText, int densityDPI) {

        this.dayList = dayList;
        this.selectedDateText = selectedDateText;
        this.densityDPI = densityDPI;
    }

    class CalendarViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        LinearLayout dateLayout;
        TextView dateText;

        public CalendarViewHolder(@NonNull View itemView) {
            super(itemView);
            dateLayout = itemView.findViewById(R.id.dateLayout);
            dateText = itemView.findViewById(R.id.dateTextView);
        }

        public void onClick(View view) {
            //if(dateClickListener)
        }
    }

    @NonNull
    @Override
    public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        View view = inflater.inflate(R.layout.date_cell, parent, false);

        return new CalendarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position) {
        holder.dateText.setText(dayList.get(position));
        GridLayoutManager.LayoutParams layoutParams = (GridLayoutManager.LayoutParams)holder.itemView.getLayoutParams();
        layoutParams.height = 300 / ((dayList.size()) / 7) * densityDPI / 160;
        holder.itemView.requestLayout();
        holder.dateText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String prefix = selectedDateText.getText().toString().substring(0, 3);
                selectedDateText.setText(prefix + String.format("%02d", Integer.parseInt(holder.dateText.getText().toString())));
            }
        });
    }

    @Override
    public int getItemCount() {
        return dayList.size();
    }

    public interface ItemClickListener {
        void onItemClick(View view, String day, boolean isInMonth);
    }

    private void setCalendarView() {
        int lastMonthStartDay;

        dayList.clear();

        int i = calendar.DAY_OF_WEEK_IN_MONTH;
    }
}