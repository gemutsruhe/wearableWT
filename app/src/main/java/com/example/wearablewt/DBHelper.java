package com.example.wearablewt;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.util.Pair;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;

public class DBHelper extends SQLiteOpenHelper {
    static final String DATABASE_NAME = "weightTraining.db";

    // DBHelper 생성자
    public DBHelper(Context context, int version) {
        super(context, DATABASE_NAME, null, version);
    }

    // Person Table 생성
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE Person(name TEXT, Age INT, ADDR TEXT)");
    }
    // Person Table Upgrade
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS Person");
        onCreate(db);
    }

    // Person Table 데이터 입력
    public void insert(LocalDate date, int age, String Addr) {
        SQLiteDatabase db = getWritableDatabase();
        //db.execSQL("INSERT INTO Person VALUES('" + name + "', " + age + ", '" + Addr + "')");
        db.close();
    }

    // Person Table 데이터 수정
    public void Update(String name, int age, String Addr) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("UPDATE Person SET age = " + age + ", ADDR = '" + Addr + "'" + " WHERE NAME = '" + name + "'");
        db.close();
    }

    public void updateFavorite(String trainingName, int favorite){
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("UPDATE training SET favorite = " + favorite + " WHERE training_name = '" + trainingName + "'");
    }

    // Person Table 데이터 삭제
    public void Delete(String name) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE Person WHERE NAME = '" + name + "'");
        db.close();
    }

    public void getTrainingData(String trainingId){
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT training_name, unit, unit_weight FROM training WHERE training_id = '" + trainingId + "'",null);
        //return
    }

    public HashMap<String, String> getTrainingIdNameMap(){
        SQLiteDatabase db = getReadableDatabase();
        HashMap<String, String> trainingIdNameMap = new HashMap<>();

        Cursor cursor = db.rawQuery("SELECT training_id, training_name FROM training",null);
        while(cursor.moveToNext()) {
            String trainingId = cursor.getString(0);
            String trainingName = cursor.getString(1);
            trainingIdNameMap.put(trainingId, trainingName);
        }

        return trainingIdNameMap;
    }

    public HashMap<String, String> getTrainingNameIdMap(){
        SQLiteDatabase db = getReadableDatabase();
        HashMap<String, String> trainingNameIdMap = new HashMap<>();

        Cursor cursor = db.rawQuery("SELECT training_id, training_name FROM training",null);
        while(cursor.moveToNext()) {
            String trainingId = cursor.getString(0);
            String trainingName = cursor.getString(1);
            trainingNameIdMap.put(trainingName, trainingId);
        }

        return trainingNameIdMap;
    }

    public void saveNewRoutine(String routine_name, ArrayList<String> trainingIdList){
        SQLiteDatabase dbWrite = getWritableDatabase();
        dbWrite.execSQL("INSERT INTO routine (routine_name) VALUES('" + routine_name + "')");
        dbWrite.close();

        SQLiteDatabase dbRead = getReadableDatabase();
        Cursor cursor = dbRead.rawQuery("SELECT routine_id FROM routine WHERE routine_name = '" + routine_name + "'", null);
        cursor.moveToFirst();
        int routine_id = cursor.getInt(0);
        dbRead.close();

        dbWrite = getWritableDatabase();
        for(int i = 0; i < trainingIdList.size(); i++) {
            dbWrite.execSQL("INSERT INTO routineSequence (routine_id, training_id, sequence_num) VALUES(" + routine_id + ", '" + trainingIdList.get(i) + "', " + i + ")");
        }
        dbWrite.close();
    }

    public void addRecord(String dateId, String trainingId, int sequenceNum, int setsNum, double weight, String unit, int repeat) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("INSERT INTO record (date_id, training_id, sequence_num, sets_num, weight, unit, repeat) VALUES('" + dateId + "', '" + trainingId + "', "
                + sequenceNum + ", " + setsNum + ", " + weight + ", '" + unit + "', " + repeat + ")");
        db.close();
    }

    public void deleteRecord(String dateId, String trainingId, int sequenceNum, int setsNum) {

        SQLiteDatabase dbRead = getReadableDatabase();
        ArrayList<Integer> updateList = new ArrayList<>();

        Cursor cursor = dbRead.rawQuery("SELECT record_id FROM record WHERE date_id = '" + dateId + "' and training_id = '" + trainingId + "' and sequence_num = " + sequenceNum + " and sets_num >= " + setsNum, null);
        while(cursor.moveToNext()) {
            updateList.add(cursor.getInt(0));

        }
        dbRead.close();

        SQLiteDatabase dbWrite = getWritableDatabase();
        dbWrite.execSQL("DELETE FROM record WHERE record_id = " + updateList.get(0));
        for(int i = 1; i < updateList.size(); i++) {
            dbWrite.execSQL("UPDATE record SET sets_num = " + (setsNum + i - 1) + " WHERE record_id = " + updateList.get(i));
        }


    }

    public ArrayList<Pair<String, Integer>> getDailyTrainingSummary(String date) {
        ArrayList<Pair<String, Integer>> trainingSummaryList = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT training_id, COUNT(*) FROM record WHERE date_id = '" + date + "'" + " GROUP BY sequence_num ORDER BY sequence_num", null);
        while(cursor.moveToNext()) {
            String trainingId = cursor.getString(0);
            int setsNum = cursor.getInt(1) - 1;
            if(setsNum > 0) trainingSummaryList.add(new Pair<>(trainingId, setsNum));
        }
        return trainingSummaryList;
    }

    public void addDailyTrainingList(String date, ArrayList<String> trainingIdList) {

        SQLiteDatabase dbRead = getReadableDatabase();
        Cursor cursor = dbRead.rawQuery("SELECT COUNT(*) FROM record WHERE date_id = '" + date + "'", null);
        cursor.moveToNext();
        int maxSequenceNum = cursor.getInt(0);
        dbRead.close();

        SQLiteDatabase dbWrite = getWritableDatabase();
        for(int i = 0; i < trainingIdList.size(); i++) {
            dbWrite.execSQL("INSERT INTO record (date_id, training_id, sequence_num) VALUES (?, ?, ?)", new Object[]{date, trainingIdList.get(i), maxSequenceNum + i});
        }
        dbWrite.close();
    }

    public Pair<ArrayList<String>, ArrayList<ArrayList<Record>>> getDailyTrainingRecord(String date){
        SQLiteDatabase db = getReadableDatabase();

        ArrayList<String> trainingIdList = new ArrayList<>();
        ArrayList<ArrayList<Record>> recordList = new ArrayList<>();

        Cursor cursor = db.rawQuery("SELECT DISTINCT training_id FROM record WHERE date_id = '" + date + "' and sets_num = 0", null);
        while(cursor.moveToNext()) {
            String trainingId = cursor.getString(0);
            trainingIdList.add(trainingId);
            Log.e("TEST", trainingId);
            recordList.add(new ArrayList<>());
        }

        cursor = db.rawQuery("SELECT sequence_num, sets_num, weight, unit, repeat FROM record WHERE date_id = '" + date + "'and sets_num > 0 ORDER BY sequence_num, sets_num", null);

        while(cursor.moveToNext()) {

            int sequenceNum = cursor.getInt(0);
            int setsNum = cursor.getInt(1);
            double weight = cursor.getDouble(2);
            String unit = cursor.getString(3);
            int repeat = cursor.getInt(4);
            Log.e("TEST", sequenceNum + " " + setsNum + " " + weight + " " + unit + " " + repeat);
            Record record = new Record(setsNum, weight, unit, repeat);
            recordList.get(sequenceNum).add(record);
        }
        db.close();

        return new Pair(trainingIdList, recordList);
    }

    public HashMap<String, String> getTrainingPartMap(){
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT training_name, target_part FROM training", null);
        HashMap<String, String> trainingPartMap = new HashMap<>();
        while(cursor.moveToNext()) {
            String trainingName = cursor.getString(0);
            String partName = cursor.getString(1);
            trainingPartMap.put(trainingName, partName);
        }
        return trainingPartMap;
    }

    public ArrayList<String> getTrainingPartList() {
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT DISTINCT target_part FROM training", null);
        ArrayList<String> partList = new ArrayList<>();
        partList.add("전체");
        while(cursor.moveToNext()) {
            String partName = cursor.getString(0);
            partList.add(partName);
        }

        return partList;
    }

    public ArrayList<ArrayList<String>> getTrainingList(String part){
        SQLiteDatabase db = getReadableDatabase();

        String partQuery;
        if(part.compareTo("전체") == 0) partQuery = "";
        else partQuery = " WHERE target_part = '" + part + "'";
        Cursor cursor = db.rawQuery("SELECT training_name, favorite FROM training" + partQuery + " ORDER BY favorite DESC, training_name", null);
        ArrayList<ArrayList<String>> trainingList = new ArrayList<>();
        trainingList.add(new ArrayList<>());
        trainingList.add(new ArrayList<>());
        while(cursor.moveToNext()) {
            String trainingName = cursor.getString(0);
            int favorite = Integer.parseInt(cursor.getString(1));
            trainingList.get(favorite).add(trainingName);
        }

        return trainingList;
    }

    public HashMap<String, ArrayList<String>> getRoutineList(){
        SQLiteDatabase db = getReadableDatabase();
        HashMap<Integer, String> routineIdNameMap = new HashMap<>();
        Cursor cursor = db.rawQuery("SELECT routine_id, routine_name FROM routine", null);
        while(cursor.moveToNext()) {
            int routine_id = cursor.getInt(0);
            String routine_name = cursor.getString(1);
            routineIdNameMap.put(routine_id, routine_name);
        }
        HashMap<String, ArrayList<String>> routineNameTrainingIdMap = new HashMap<>();
        cursor = db.rawQuery("SELECT routine_id, training_id, sequence_num FROM routineSequence ORDER BY routine_id, sequence_num",null);
        while(cursor.moveToNext()) {
            int routine_id = cursor.getInt(0);
            String routine_name = routineIdNameMap.get(routine_id);
            String training_id = cursor.getString(1);
            if(routineNameTrainingIdMap.containsKey(routine_name) == false) {
                ArrayList<String> temp = new ArrayList<>();
                routineNameTrainingIdMap.put(routine_name, temp);
            }
            routineNameTrainingIdMap.get(routine_name).add(training_id);
        }

        return routineNameTrainingIdMap;
    }

    public ArrayList<String> getRoutine(String routineName){
        SQLiteDatabase db = getReadableDatabase();
        ArrayList<String> trainingList = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT routine_id FROM routine WHERE routine_name = '" + routineName + "'", null);

        cursor.moveToNext();
        int routine_id = cursor.getInt(0);

        HashMap<String, ArrayList<String>> routineNameTrainingIdMap = new HashMap<>();
        cursor = db.rawQuery("SELECT training_id, sequence_num FROM routineSequence WHERE routine_id = " + routine_id + " ORDER BY sequence_num",null);
        while(cursor.moveToNext()) {
            String training_id = cursor.getString(0);
            trainingList.add(training_id);
        }

        return trainingList;
    }

    public void removeRoutine(String routineName){
        SQLiteDatabase dbRead = getReadableDatabase();
        Cursor cursor = dbRead.rawQuery("SELECT routine_id FROM routine WHERE routine_name = '" + routineName + "'", null);
        cursor.moveToNext();
        int routine_id = cursor.getInt(0);
        dbRead.close();

        SQLiteDatabase dbWrite = getReadableDatabase();
        dbWrite.execSQL("DELETE FROM routine WHERE routine_id = " + routine_id);
        dbWrite.execSQL("DELETE FROM routineSequence WHERE routine_id = " + routine_id);
    }

    // Person Table 조회
    public String getResult() {
        // 읽기가 가능하게 DB 열기
        SQLiteDatabase db = getReadableDatabase();
        String result = "";

        // DB에 있는 데이터를 쉽게 처리하기 위해 Cursor를 사용하여 테이블에 있는 모든 데이터 출력
        Cursor cursor = db.rawQuery("SELECT * FROM Person", null);
        while (cursor.moveToNext()) {
            result += " 이름 : " + cursor.getString(0)
                    + ", 나이 : "
                    + cursor.getInt(1)
                    + ", 주소 : "
                    + cursor.getString(2)
                    + "\n";
        }

        return result;
    }
}