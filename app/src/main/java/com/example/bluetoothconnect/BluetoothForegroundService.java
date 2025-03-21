package com.example.bluetoothconnect;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import java.util.List;

public class BluetoothForegroundService extends Service {
    private static final String CHANNEL_ID = "BluetoothForegroundServiceChannel";
    private static final int NOTIFICATION_ID = 1;
    private NotificationManager notificationManager;
    private SendReceive sendReceive;
    private List<List<String>> itemList;
    private int listIndex = 0;
    private int itemIndex = 0;
    private Handler handler;// To track whether sending is active

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = getSystemService(NotificationManager.class);

        // Create notification channel once
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Bluetooth Service", NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
       if(intent!=null){
           List<String> currentList=(List<String>) intent.getSerializableExtra("currentList");
           int listIndex=intent.getIntExtra("listIndex",0);
           int totalLists=intent.getIntExtra("totalLists",1);
           createNotificationChannel();
           if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
               startForeground(NOTIFICATION_ID,createNotification("Starting list "+(listIndex+1),listIndex,totalLists), 0);
           }
           sendNextItem(currentList,listIndex,totalLists);
       }
       return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void sendNextItem(List<String> currentList,int listIndex,int totalLists) {
        if(sendReceive!=null && currentList!=null && itemIndex<itemList.size()){
            String item=itemList.get(listIndex).get(itemIndex);
            sendReceive.write(item.getBytes());
            updateNotification("Sending:"+item,listIndex,itemIndex);
        }
    }

    private void updateNotification(String message,int listIndex,int itemIndex) {
       NotificationManager notificationManager=(NotificationManager) getSystemService(NOTIFICATION_SERVICE);
       if(notificationManager!=null){
           int totalItems=itemList.get(listIndex).size();
           int progress=(itemIndex+1)*100/totalItems;
           notificationManager.notify(NOTIFICATION_ID,createNotification(message,listIndex,progress));
       }
    }
    private Notification createNotification(String message,int listIndex,int progress){
        Intent notificationIntent=new Intent(this,MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder=new NotificationCompat.Builder(this,CHANNEL_ID)
                .setContentTitle("Bluetooth Foreground Service")
                .setContentText(message)
                .setSmallIcon(R.drawable.baseline_notifications_24)
                .setContentIntent(pendingIntent)
                .setProgress(100,progress,false);
        return builder.build();
    }
    private void createNotificationChannel(){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            NotificationChannel serviceChannel=new NotificationChannel(
                    CHANNEL_ID,
                    "Bluetooth Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager=getSystemService(NotificationManager.class);
            if(manager!=null){
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
    public void onDestroy(){
        super.onDestroy();
        if(sendReceive!=null){
            sendReceive.cancel();
        }
    }
}
