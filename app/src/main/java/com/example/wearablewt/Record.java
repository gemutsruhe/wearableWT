package com.example.wearablewt;

public class Record {
    int setsNum;
    double weight;
    String unit;
    int repeat;

    Record(int setsNum, double weight, String unit, int repeat) {
        this.setsNum = setsNum;
        this.weight = weight;
        this.unit = unit;
        this.repeat = repeat;
    }

    public int getSetsNum() {
        return setsNum;
    }

    public double getWeight() {
        return weight;
    }

    public String getUnit() {
        return unit;
    }

    public int getRepeat() {
        return repeat;
    }
}
