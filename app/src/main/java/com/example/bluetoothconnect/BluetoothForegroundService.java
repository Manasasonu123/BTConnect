//
//package com.example.bluetoothconnect;
//
//import android.app.Notification;
//import android.app.NotificationChannel;
//import android.app.NotificationManager;
//import android.app.PendingIntent;
//import android.app.Service;
//import android.content.Intent;
//import android.os.Build;
//import android.os.Handler;
//import android.os.IBinder;
//import android.os.Looper;
//import androidx.annotation.Nullable;
//import androidx.core.app.NotificationCompat;
//import java.util.List;
//
//public class BluetoothForegroundService extends Service {
//    private static final String CHANNEL_ID = "BluetoothForegroundServiceChannel";
//    private static final int NOTIFICATION_ID = 1;
//    private static final long DELAY_BETWEEN_ITEMS = 5000; // 5 seconds delay between items
//    private static final long DELAY_BETWEEN_LISTS = 12000; // 12 seconds delay between lists
//
//    private NotificationManager notificationManager;
//    private SendReceive sendReceive;
//    private List<List<String>> itemList;
//    private int listIndex = 0;
//    private int itemIndex = 0;
//    private int totalLists = 0;
//    private Handler handler;
//
//    @Override
//    public void onCreate() {
//        super.onCreate();
//        notificationManager = getSystemService(NotificationManager.class);
//        createNotificationChannel();
//        handler = new Handler(Looper.getMainLooper());
//    }
//
//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        if (intent != null) {
//            itemList = (List<List<String>>) intent.getSerializableExtra("itemList");
//            sendReceive = MainActivity.sendReceive; // Ensure this is properly initialized
//            listIndex = 0; // Always start from the first list
//            itemIndex = 0; // Reset item index
//            totalLists = itemList != null ? itemList.size() : 0;
//
//            if (itemList == null || totalLists == 0) {
//                stopSelf(); // Stop if no valid data
//                return START_NOT_STICKY;
//            }
//
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                startForeground(NOTIFICATION_ID, createNotification("Starting list " + (listIndex + 1), listIndex, totalLists, 0));
//            }
//
//            // Start sending items
//            sendNextItem();
//        }
//        return START_STICKY;
//    }
//
//    private void sendNextItem() {
//        if (sendReceive == null || itemList == null || listIndex >= totalLists) {
//            stopSelf(); // Stop if conditions aren't met
//            return;
//        }
//
//        List<String> currentList = itemList.get(listIndex);
//
//        if (itemIndex < currentList.size()) {
//            String item = currentList.get(itemIndex);
//            sendReceive.write(item.getBytes());
//            updateNotification("Sending: " + item, listIndex, totalLists, itemIndex);
//
//            // Schedule the next item
//            handler.postDelayed(() -> {
//                itemIndex++;
//                sendNextItem();
//            }, DELAY_BETWEEN_ITEMS);
//        } else if (listIndex < totalLists - 1) {
//            // Move to the next list
//            listIndex++;
//            itemIndex = 0;
//            updateNotification("Starting list " + (listIndex + 1), listIndex, totalLists, 0);
//
//            handler.postDelayed(this::sendNextItem, DELAY_BETWEEN_LISTS);
//        } else {
//            // All lists processed
//            updateNotification("All lists completed", listIndex, totalLists, currentList.size() - 1);
//            handler.postDelayed(this::stopSelf, DELAY_BETWEEN_ITEMS);
//        }
//    }
//
//    private void updateNotification(String message, int listIndex, int totalLists, int itemIndex) {
//        if (notificationManager != null) {
//            int totalItems = itemList.get(listIndex).size();
//            int progress = totalItems > 0 ? (itemIndex * 100 / totalItems) : 0; // Avoid division by zero
//            notificationManager.notify(NOTIFICATION_ID, createNotification(message, listIndex, totalLists, progress));
//        }
//    }
//
//    private Notification createNotification(String message, int listIndex, int totalLists, int progress) {
//        Intent notificationIntent = new Intent(this, MainActivity.class);
//        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
//
//        return new NotificationCompat.Builder(this, CHANNEL_ID)
//                .setContentTitle("Bluetooth Foreground Service - List " + (listIndex + 1) + " of " + totalLists)
//                .setContentText(message)
//                .setSmallIcon(R.drawable.baseline_notifications_24)
//                .setContentIntent(pendingIntent)
//                .setProgress(100, progress, false)
//                .build();
//    }
//
//    private void createNotificationChannel() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationChannel channel = new NotificationChannel(
//                    CHANNEL_ID,
//                    "Bluetooth Foreground Service Channel",
//                    NotificationManager.IMPORTANCE_DEFAULT
//            );
//            notificationManager.createNotificationChannel(channel);
//        }
//    }
//
//    @Override
//    public void onDestroy() {
//        super.onDestroy();
//        if (sendReceive != null) {
//            sendReceive.cancel();
//        }
//        handler.removeCallbacksAndMessages(null);
//    }
//
//    @Nullable
//    @Override
//    public IBinder onBind(Intent intent) {
//        return null;
//    }
//}
//package com.example.bluetoothconnect;
//import android.app.Notification;
//import android.app.NotificationChannel;
//import android.app.NotificationManager;
//import android.app.PendingIntent;
//import android.app.Service;
//import android.content.Intent;
//import android.os.Build;
//import android.os.Handler;
//import android.os.IBinder;
//import android.os.Looper;
//
//import androidx.annotation.Nullable;
//import androidx.core.app.NotificationCompat;
//
//import com.example.bluetoothconnect.MainActivity;
//import com.example.bluetoothconnect.SendReceive;
//
//import java.util.List;
//
//public class BluetoothForegroundService extends Service {
//    private static final String CHANNEL_ID = "BluetoothForegroundServiceChannel";
//    private static final int NOTIFICATION_ID = 1;
//    private static final long DELAY_BETWEEN_ITEMS = 3000; // 2 seconds between items
//    private static final long DELAY_BETWEEN_LISTS = 15000; // 3 seconds between lists
//
//    private NotificationManager notificationManager;
//    private SendReceive sendReceive;
//    private List<List<String>> itemList;
//    private int listIndex = 0;
//    private int itemIndex = 0;
//    private int totalLists = 0;
//    private Handler handler;
//    private int totalItemsToSend = 0;
//
//    @Override
//    public void onCreate() {
//        super.onCreate();
//        notificationManager = getSystemService(NotificationManager.class);
//        createNotificationChannel();
//        handler = new Handler(Looper.getMainLooper());
//    }
//
//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        if (intent != null) {
//            itemList = (List<List<String>>) intent.getSerializableExtra("itemList");
//            sendReceive = MainActivity.sendReceive;
//            listIndex = 0;
//            itemIndex = 0;
//            totalLists = itemList != null ? itemList.size() : 0;
//
//            if (itemList == null || totalLists == 0) {
//                stopSelf();
//                return START_NOT_STICKY;
//            }
//
//            // Calculate total items for overall progress
//            totalItemsToSend = 0;
//            for (List<String> list : itemList) {
//                totalItemsToSend += list.size();
//            }
//
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                startForeground(NOTIFICATION_ID, createNotification("Starting list " + (listIndex + 1), 0, 0));
//            }
//
//            sendNextItem();
//        }
//        return START_STICKY;
//    }
//
//    private void sendNextItem() {
//        if (sendReceive == null || itemList == null || listIndex >= totalLists) {
//            updateNotification("Transmission complete", 100, 100);
//            handler.postDelayed(this::stopSelf, DELAY_BETWEEN_ITEMS);
//            return;
//        }
//
//        List<String> currentList = itemList.get(listIndex);
//
//        if (itemIndex < currentList.size()) {
//            String item = currentList.get(itemIndex);
//            sendReceive.write(item.getBytes());
//
//            // Calculate progress for current list (0-100)
//            int listProgress = (int) (((float)(itemIndex + 1) / currentList.size()) * 100);
//
//            // Calculate overall progress (0-100)
//            int itemsSentSoFar = 0;
//            for (int i = 0; i < listIndex; i++) {
//                itemsSentSoFar += itemList.get(i).size();
//            }
//            itemsSentSoFar += itemIndex + 1;
//            int overallProgress = (int) (((float)itemsSentSoFar / totalItemsToSend) * 100);
//
//            updateNotification("Sending: " + item + " (List " + (listIndex + 1) + ")",
//                    listProgress, overallProgress);
//
//            handler.postDelayed(() -> {
//                itemIndex++;
//                sendNextItem();
//            }, DELAY_BETWEEN_ITEMS);
//        } else if (listIndex < totalLists - 1) {
//            // Move to next list
//            listIndex++;
//            itemIndex = 0;
//            updateNotification("Starting list " + (listIndex + 1), 0,
//                    (int) (((float)listIndex / totalLists) * 100));
//            handler.postDelayed(this::sendNextItem, DELAY_BETWEEN_LISTS);
//        } else {
//            // All lists processed
//            updateNotification("Transmission complete", 100, 100);
//            handler.postDelayed(this::stopSelf, DELAY_BETWEEN_ITEMS);
//        }
//    }
//
//    private void updateNotification(String message, int listProgress, int overallProgress) {
//        if (notificationManager != null) {
//            Notification notification = createNotification(message, listProgress, overallProgress);
//            notificationManager.notify(NOTIFICATION_ID, notification);
//        }
//    }
//
//    private Notification createNotification(String message, int listProgress, int overallProgress) {
//        Intent notificationIntent = new Intent(this, MainActivity.class);
//        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
//                PendingIntent.FLAG_IMMUTABLE);
//
//        return new NotificationCompat.Builder(this, CHANNEL_ID)
//                .setContentTitle("Bluetooth Transfer - List " + (listIndex + 1) + "/" + totalLists)
//                .setContentText(message + " | Overall: " + overallProgress + "%")
//                .setSmallIcon(R.drawable.baseline_notifications_24)
//                .setContentIntent(pendingIntent)
//                .setProgress(100, listProgress, false)
//                .setOnlyAlertOnce(true)
//                .build();
//    }
//
//    private void createNotificationChannel() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationChannel channel = new NotificationChannel(
//                    CHANNEL_ID,
//                    "Bluetooth Transfer",
//                    NotificationManager.IMPORTANCE_LOW
//            );
//            channel.setDescription("Bluetooth file transfer progress");
//            notificationManager.createNotificationChannel(channel);
//        }
//    }
//
//    @Override
//    public void onDestroy() {
//        super.onDestroy();
//        if (sendReceive != null) {
//            sendReceive.cancel();
//        }
//        handler.removeCallbacksAndMessages(null);
//    }
//
//    @Nullable
//    @Override
//    public IBinder onBind(Intent intent) {
//        return null;
//    }
//}

package com.example.bluetoothconnect;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.List;

public class BluetoothForegroundService extends Service {
    private static final String TAG = "BluetoothForegroundService";
    private static final String CHANNEL_ID = "BluetoothForegroundServiceChannel";
    private static final int NOTIFICATION_ID = 1;
    private static final long DELAY_BETWEEN_ITEMS = 2000; // 2 seconds
    private static final long DELAY_BETWEEN_LISTS = 3000; // 3 seconds
    private static final String ACK_PREFIX = "ACK:";

    private NotificationManager notificationManager;
    private List<List<String>> itemList;
    private int listIndex = 0;
    private int itemIndex = 0;
    private int totalLists = 0;
    private int totalItemsToSend = 0;
    private int acknowledgedItems = 0;
    private boolean waitingForAck = false;
    private Handler handler;
    private SendReceive sendReceive;

    public class LocalBinder extends Binder {
        BluetoothForegroundService getService() {
            return BluetoothForegroundService.this;
        }
    }
    private final IBinder binder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = getSystemService(NotificationManager.class);
        createNotificationChannel();
        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            itemList = (List<List<String>>) intent.getSerializableExtra("itemList");
            listIndex = 0;
            itemIndex = 0;
            acknowledgedItems = 0;
            totalLists = itemList != null ? itemList.size() : 0;

            if (itemList == null || totalLists == 0) {
                stopSelf();
                return START_NOT_STICKY;
            }

            totalItemsToSend = 0;
            for (List<String> list : itemList) {
                totalItemsToSend += list.size();
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForeground(NOTIFICATION_ID,
                        createNotification("Starting transmission", 0));
            }

            sendNextItem();
        }
        return START_STICKY;
    }

    private void sendNextItem() {
        if (listIndex >= totalLists) {
            updateNotification("Transmission complete", 100);
            handler.postDelayed(this::stopSelf, DELAY_BETWEEN_ITEMS);
            return;
        }

        List<String> currentList = itemList.get(listIndex);

        if (itemIndex < currentList.size() && !waitingForAck) {
            String item = currentList.get(itemIndex);
            Log.d(TAG, "Sending item: " + item + " from list " + (listIndex + 1));

            if (sendReceive != null) {
                sendReceive.write(item.getBytes());
            }

            waitingForAck = true;
            updateNotification("Sent: " + item + " (Waiting for ack)",
                    (int)((float)acknowledgedItems / totalItemsToSend * 100));

            handler.postDelayed(() -> {
                if (waitingForAck) {
                    Log.w(TAG, "Timeout waiting for ack for item: " + item);
                    waitingForAck = false;
                    itemIndex++;
                    sendNextItem();
                }
            }, DELAY_BETWEEN_ITEMS);
        } else if (itemIndex >= currentList.size() && listIndex < totalLists - 1) {
            listIndex++;
            itemIndex = 0;
            waitingForAck = false;
            updateNotification("Starting list " + (listIndex + 1),
                    (int)((float)acknowledgedItems / totalItemsToSend * 100));
            handler.postDelayed(this::sendNextItem, DELAY_BETWEEN_LISTS);
        } else if (listIndex >= totalLists - 1 && itemIndex >= currentList.size()) {
            updateNotification("Transmission complete", 100);
            handler.postDelayed(this::stopSelf, DELAY_BETWEEN_ITEMS);
        }
    }

    // In BluetoothForegroundService.java
    public void onAcknowledgmentReceived(String ackItem) {
        Log.d(TAG, "Acknowledgment received for item: " + ackItem);
        waitingForAck = false;
        acknowledgedItems++;

        int progress = (int)((float)acknowledgedItems / totalItemsToSend * 100);
        Log.d(TAG, "Progress: " + progress + "%");
        updateNotification("Ack received: " + ackItem, progress);

        itemIndex++;
        sendNextItem();
    }

    private void updateNotification(String message, int progress) {
        if (notificationManager != null) {
            Notification notification = createNotification(message, progress);
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }

    private Notification createNotification(String message, int progress) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Bluetooth Transfer - Progress: " + progress + "%")
                .setContentText(message)
                .setSmallIcon(R.drawable.baseline_notifications_24)
                .setContentIntent(pendingIntent)
                .setProgress(100, progress, false)
                .setOnlyAlertOnce(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Bluetooth Transfer",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Bluetooth file transfer progress");
            notificationManager.createNotificationChannel(channel);
        }
    }
    public void setSendReceive(SendReceive sendReceive) {
        this.sendReceive = sendReceive;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}