package com.example.siedem.pomiartemperatury;


import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.opencsv.CSVWriter;

import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.os.Environment;

public class MainActivity extends AppCompatActivity {
    TextView textview,textdata;
    BroadcastReceiver batteryBroadcast;
    IntentFilter intentFilter;
    Button exportButton,graphButton;
    int temperatura;
    //zczytywanie danych
    MyDatabaseHelper myDB;
    ArrayList<String> __id, _temp,_data;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    private Handler mHandler = new Handler();

    private Runnable mToastRunnable = new Runnable() {
        @Override
        public void run() {
          //  Toast.makeText(MainActivity.this, "15sec", Toast.LENGTH_SHORT).show();
            MyDatabaseHelper myDB = new MyDatabaseHelper(MainActivity.this);
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss");
            String currentTime = df.format(Calendar.getInstance().getTime());
            myDB.addTemp(currentTime,temperatura-6);



            mHandler.postDelayed(this, 15000);
        }
    };

//funkcje start pomiaru i stop pomiaru
    public void startRepeating(View view){
        mToastRunnable.run();

    }

    public void stopRepeating(View view){
        mHandler.removeCallbacks(mToastRunnable);
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textview = findViewById(R.id.TextTemp);
        textdata = findViewById(R.id.TextData);
       exportButton = findViewById(R.id.button_export);
        graphButton = findViewById(R.id.button_graph);
        final GraphView graph = (GraphView) findViewById(R.id.graph);
        intentFilterAndBroadcast();
     //   DateFormat df = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss");
     //   final String currentTime = df.format(Calendar.getInstance().getTime());
       // textdata.setText("Time: " + currentTime);


        //tablica na dane z SQL
        myDB = new MyDatabaseHelper(MainActivity.this);
        __id = new ArrayList<>();
        _data = new ArrayList<>();
        _temp = new ArrayList<>();




        exportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    verifyStoragePermissions(MainActivity.this);
                    String csv = (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+ "/TestCSV.csv");

                    CSVWriter csvWrite = new CSVWriter(new FileWriter(csv));
                    SQLiteDatabase db = myDB.getWritableDatabase();
                    Cursor curCSV = db.rawQuery("select * from " + "pomiar", null);

                    csvWrite.writeNext(curCSV.getColumnNames());

                    while (curCSV.moveToNext()) {
                        String[] arrayStr = {curCSV.getString(0), curCSV.getString(1),
                                curCSV.getString(2)};
                        csvWrite.writeNext(arrayStr);
                    }
                    csvWrite.close();
                    curCSV.close();

                } catch (IOException e) {
                    e.printStackTrace();

                }

            }
        });


        graphButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                                __id.clear();
                                _data.clear();
                                _temp.clear();
                                storeDataInArrays();
                                textdata.setText("Ilosc danych: " + __id.size());
                                if (__id.size() > 0) {
                                    LineGraphSeries<DataPoint> series = new LineGraphSeries<>();
                                    for (int i = 0; i < __id.size(); i++) {
                                        DataPoint point = new DataPoint(Integer.parseInt(__id.get(i))*15, Integer.parseInt(_temp.get(i)));
                                        series.appendData(point, true, __id.size());
                                    }
                                    graph.getViewport().setScalable(true);
                                    graph.getViewport().setScalableY(false);
                                    graph.getViewport().setMinY(0);
                                    graph.getViewport().setMaxY(40);
                                    graph.getViewport().setMinX(0);
                                    graph.getViewport().setMaxX(__id.size()*15);
                                    graph.getGridLabelRenderer().setVerticalAxisTitle("Temperatura [°C]");
                                    graph.getGridLabelRenderer().setHorizontalAxisTitle("Czas[s]");
                                    graph.addSeries(series);
                                }

            }
        });
    }


    void storeDataInArrays()
    {
        Cursor cursor = myDB.readAllData();
        if(cursor.getCount()==0){
            Toast.makeText(getApplicationContext(), "no data", Toast.LENGTH_SHORT).show();
        } else{
            while (cursor.moveToNext()){
               __id.add(cursor.getString(0));
                _data.add(cursor.getString(1));
               _temp.add(cursor.getString(2));
            }
        }
    }




    private void intentFilterAndBroadcast() {
        intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        batteryBroadcast = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction()))
                {
                    float tempTemp = (float) intent.getIntExtra("temperature", -1)/10;
                    textview.setText("Obecna temperatura"+ (tempTemp-6) +" °C");
                    temperatura = Math.round(tempTemp);
                }
            }
        };
    }


    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(batteryBroadcast,intentFilter);
    }


    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(batteryBroadcast);
    }
}



