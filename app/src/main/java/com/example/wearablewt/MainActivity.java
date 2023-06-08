package com.example.wearablewt;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.gun0912.tedpermission.PermissionListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        copyDatabase();

        Intent intent = new Intent(this, CalendarActivity.class);
        startActivity(intent);

        finish();
    }

    private void copyDatabase() {

        String DB_PATH = "/data/data/" + getApplicationContext().getPackageName() + "/databases/";
        String DB_NAME = "weightTraining.db";

        try{
            File fDir = new File( DB_PATH );
            if( !fDir.exists() ) { fDir.mkdir(); }

            String strOutFile = DB_PATH + DB_NAME;
            File dbFile = new File(strOutFile);
            if(!dbFile.exists()) {
                InputStream inputStream = getApplicationContext().getAssets().open( DB_NAME );
                OutputStream outputStream = new FileOutputStream( strOutFile );

                byte[] mBuffer = new byte[1024];
                int mLength;
                while( ( mLength = inputStream.read( mBuffer) ) > 0 ) {
                    outputStream.write( mBuffer, 0, mLength );
                }

                outputStream.flush();
                outputStream.close();
                inputStream.close();
            }
        }catch( Exception e ) {
            e.printStackTrace();
        }
    }
}