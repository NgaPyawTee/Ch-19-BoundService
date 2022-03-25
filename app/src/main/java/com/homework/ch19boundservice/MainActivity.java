package com.homework.ch19boundservice;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.widget.TextView;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private OdometerService odometerService;
    private boolean bound;
    private static final int PERMISSION_CODE = 698;
    private static final String NOTI_CHANNEL = "noti channel";
    private NotificationManager manager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        displayDistance();
    }

    private void displayDistance() {
        final TextView textView1 = findViewById(R.id.tv1);
        final Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                double distance = 0.0;
                if (bound && odometerService != null){
                    distance = odometerService.getDistance();
                }

                String s = String.format(Locale.getDefault(),"%.2f miles",distance);
                textView1.setText(s);
                handler.postDelayed(this,1000);
            }
        });
    }

    ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            OdometerService.OdometerBinder binder = (OdometerService.OdometerBinder) iBinder;
            odometerService = binder.getOdometer();
            bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bound = false;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();

        if (ContextCompat.checkSelfPermission(this,OdometerService.PERMISSION_STRING) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{OdometerService.PERMISSION_STRING},PERMISSION_CODE);
        }else{
            Intent intent = new Intent(this,OdometerService.class);
            bindService(intent,connection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSION_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent(this, OdometerService.class);
                    bindService(intent, connection, BIND_AUTO_CREATE);
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        NotificationChannel channel = new NotificationChannel(NOTI_CHANNEL, "Noti channel", NotificationManager.IMPORTANCE_HIGH);
                        manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                        manager.createNotificationChannel(channel);
                    }
                    Notification notification = new NotificationCompat.Builder(this, NOTI_CHANNEL)
                            .setSmallIcon(R.drawable.ic_noti)
                            .setPriority(NotificationManager.IMPORTANCE_HIGH)
                            .setContentTitle("Warning!!!")
                            .setContentText("Need to grant location permission")
                            .build();
                    manager.notify(1, notification);
                }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(bound){
            unbindService(connection);
            bound = false;
        }
    }
}