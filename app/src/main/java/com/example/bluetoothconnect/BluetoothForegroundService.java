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
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.util.List;

public class BluetoothForegroundService extends Service {
    private static final String TAG = "BluetoothForegroundService";
    private static final String CHANNEL_ID = "BluetoothForegroundServiceChannel";
    private static final int NOTIFICATION_ID = 1;
    private static final long DELAY_BETWEEN_ITEMS = 5000; // 2 seconds
    private static final long DELAY_BETWEEN_LISTS = 3000; // 3 seconds
    private static final String ACK_PREFIX = "ACK:";

    private boolean isConnected = false;
    private boolean waitingForAck = false;
    private String lastSentItem=null;

    private List<List<String>> itemList;
    private int listIndex = 0;
    private int itemIndex = 0;
    private int totalLists = 0;
    private int totalItemsToSend = 0;
    private int acknowledgedItems = 0;
    private int acksender=0;
    private int currentListAcknowledged=0;

    private NotificationManager notificationManager;
    private Handler handler;
    private SendReceive sendReceive;
    private Runnable timeoutRunnable; // Store timeout reference

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
            // Initialize notification first to prevent ANR
            Notification notification = createNotification("Initializing transfer...", 0, 0, false);
            startForeground(NOTIFICATION_ID, notification);

            // Initialize data
            itemList = (List<List<String>>) intent.getSerializableExtra("itemList");
            resetCounters();

            if (itemList == null || itemList.isEmpty()) {
                stopSelf();
                return START_NOT_STICKY;
            }

            calculateTotalItems();
            updateNotification("Waiting for connection...", 0, 0, false);
            Log.d(TAG, "Service started with " + (itemList != null ? itemList.size() : 0) + " lists");
        }
        return START_STICKY;
    }
    private void resetCounters() {
        listIndex = 0;
        itemIndex = 0;
        acknowledgedItems = 0;
        currentListAcknowledged = 0;
        totalLists = itemList != null ? itemList.size() : 0;
    }

    private void calculateTotalItems() {
        totalItemsToSend = 0;
        for (List<String> list : itemList) {
            totalItemsToSend += list.size();
        }
    }

    private void sendNextItem() {
//        handler.removeCallbacksAndMessages(null); // Clear previous timeouts
        if (timeoutRunnable != null) {
            handler.removeCallbacks(timeoutRunnable);
        }


        if (!isConnected || sendReceive == null || !sendReceive.isRunning()) {
            Log.w(TAG, "Connection not ready - retrying");
            handler.postDelayed(this::sendNextItem, 2000);
            return;
        }

        if (listIndex >= totalLists) {
            handleTransmissionComplete();
            return;
        }

        List<String> currentList = itemList.get(listIndex);
        if (itemIndex < currentList.size() && !waitingForAck) {
            sendCurrentItem(currentList);
        } else {
            handleListTransition(currentList);
        }
        Log.d(TAG, "sendNextItem - isConnected: " + isConnected +
                ", sendReceive: " + (sendReceive != null) +
                ", isRunning: " + (sendReceive != null ? sendReceive.isRunning() : false));
    }
//    private void sendCurrentItem(List<String> currentList) {
//        String item = currentList.get(itemIndex);
//        Log.d(TAG, "Sending: " + item);
//
//        try {
//            sendReceive.write(item.getBytes());
//            waitingForAck = true;
//            lastSentItem=item;
//
//            updateNotificationProgress(currentList, item);
//            setupAckTimeout(item);
//        } catch (Exception e) {
//            Log.e(TAG, "Send failed: " + e.getMessage());
//            handler.postDelayed(this::sendNextItem, 2000);
//        }
//    }
private void sendCurrentItem(List<String> currentList) {
    String item = currentList.get(itemIndex);
    Log.d(TAG, "Sending: " + item);

    try {
        sendReceive.write(item.getBytes());
        waitingForAck = true;
        lastSentItem = item;
        acksender++;

        updateNotificationProgress(currentList, "📤 Sending --> " + item);
        setupAckTimeout(item);
    } catch (Exception e) {
        Log.e(TAG, "Send failed: " + e.getMessage());
        handler.postDelayed(this::sendNextItem, 2000);
    }
}


    private void updateNotificationProgress(List<String> currentList, String item) {
        int overallProgress = (int)(((double)acksender / (double)totalItemsToSend) * 100);
        int listProgress = (int)(((double)currentListAcknowledged / (double)currentList.size()) * 100);

        Log.d(TAG, "--> Ack: "+acksender+ " , Total:  "+totalItemsToSend);
        Log.d(TAG, "--> List Prgress: "+currentListAcknowledged+ " , Total:  "+currentList.size());
        Log.d(TAG, "--> Item: "+item);


        updateNotification("⬆️ Sending -> " + item, overallProgress, listProgress, false);
    }

    private void setupAckTimeout(String item) {
        if (timeoutRunnable != null) {
            handler.removeCallbacks(timeoutRunnable);
        }

//        handler.postDelayed(() -> {
//            if (waitingForAck && !item.equals(lastSentItem)) {
//                Log.w(TAG, "Timeout for: " + item);
//                waitingForAck = false;
//                itemIndex++;
//                sendNextItem();
//            }
//        }, DELAY_BETWEEN_ITEMS);
        timeoutRunnable = () -> {
            if (waitingForAck && item.equals(lastSentItem)) {
                //Toast.makeText(this,"Timeout for:"+ item,Toast.LENGTH_SHORT).show();// Ensure correct item is checked
                Log.w(TAG, "Timeout for: " + item);
                waitingForAck = false;
                itemIndex++;
                sendNextItem();
            }
        };

        handler.postDelayed(timeoutRunnable, DELAY_BETWEEN_ITEMS);
    }

    private void handleListTransition(List<String> currentList) {
        if (itemIndex >= currentList.size() && listIndex < totalLists - 1) {
            transitionToNextList();
        } else {
            handleTransmissionComplete();
        }
    }

    private void transitionToNextList() {
        listIndex++;
        itemIndex = 0;
        currentListAcknowledged = 0;
        waitingForAck = false;

        int overallProgress = (int)((float)acknowledgedItems / totalItemsToSend * 100);
        updateNotification("➡️ Starting list " + (listIndex + 1),
                overallProgress, 0, false);

        handler.postDelayed(this::sendNextItem, DELAY_BETWEEN_LISTS);
    }

    private void handleTransmissionComplete() {
        updateNotification("✅ Transmission complete", 100, 100, true);
        handler.postDelayed(this::stopSelf, DELAY_BETWEEN_ITEMS);
    }

//    public void onAcknowledgmentReceived(String ackItem) {
//        if(ackItem.equals(lastSentItem)) {
//            //handler.removeCallbacksAndMessages(null); // Cancel timeout
//            if (timeoutRunnable != null) {
//                handler.removeCallbacks(timeoutRunnable); // Only remove the timeout
//            }
//            waitingForAck = false;
//            acknowledgedItems++;
//            currentListAcknowledged++;
//
//            updateAckNotification(ackItem);
//            itemIndex++;
//            sendNextItem();
//        }else{
//            Log.w(TAG, "Unexpected ACK received: " + ackItem);
//        }
//    }

    public String getLastSentItem() {
        return lastSentItem;
    }


    //This is for receiver....
public void onAcknowledgmentReceived(String ackItem) {
    if (ackItem.equals(lastSentItem)) {
        if (timeoutRunnable != null) {
            handler.removeCallbacks(timeoutRunnable);
        }
        waitingForAck = false;
        acknowledgedItems++;
        currentListAcknowledged++;
        //Toast.makeText(this,"✅ Received: ACK: " + ackItem,Toast.LENGTH_SHORT).show();
        Log.d(TAG, "✅ Received---of Service: ACK: " + ackItem);
        Log.d(TAG,"No of Ack from receiver:"+acknowledgedItems);
        updateAckNotification(ackItem);

        itemIndex++;
        sendNextItem();
    } else {
        Log.w(TAG, "Unexpected ACK received: " + ackItem);
    }
}


//    private void updateAckNotification(String ackItem) {
//        List<String> currentList = itemList.get(listIndex);
//        int overallProgress = (int)((float)acknowledgedItems / totalItemsToSend * 100);
//        int listProgress = (int)((float)currentListAcknowledged / currentList.size() * 100);
//        updateNotification("✅ Ack: " + ackItem, overallProgress, listProgress, true);
//    }
private void updateAckNotification(String ackItem) {
    List<String> currentList = itemList.get(listIndex);
    int overallProgress = (int)((float)acknowledgedItems / totalItemsToSend * 100);
    int listProgress = (int)((float)currentListAcknowledged / currentList.size() * 100);

    Log.d(TAG, "--> Ack: "+acknowledgedItems+ " , Total:  "+totalItemsToSend);
    Log.d(TAG, "--> List Prgress: "+currentListAcknowledged+ " , Total:  "+currentList.size());
    Log.d(TAG, "--> Item: "+ackItem);

    String notificationMessage = "📤 Sent: " + ackItem + "\n✅ Received: ACK: " + ackItem;

    updateNotification(notificationMessage, overallProgress, listProgress, true);
}




    private void updateNotification(String message, int overallProgress, int listProgress, boolean isAck) {
        if (notificationManager != null) {
            Notification notification = createNotification(message, overallProgress, listProgress, isAck);
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }

    private Notification createNotification(String message, int overallProgress, int listProgress, boolean isAck) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Bluetooth Transfer")
                .setContentText(message)
                .setSmallIcon(R.drawable.baseline_notifications_24)
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true);

        // Add progress bars
        builder.setProgress(100, overallProgress, false)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(message + "\n\nOverall: " + overallProgress + "%" +
                                "\nCurrent List: " + listProgress + "%"));

        // Visual distinction for acknowledgments
        if (isAck) {
            builder.setColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
                    .setColorized(true);
        }

        return builder.build();
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
        boolean wasConnected = this.isConnected;
        this.isConnected = (sendReceive != null && sendReceive.isRunning());

        Log.d(TAG, "Connection state - Old: " + wasConnected + " New: " + isConnected);

        if (this.isConnected) {
            if (!wasConnected) { // Only start if this is a NEW connection
                Log.d(TAG, "New connection established, starting transmission");
                sendNextItem();
            }
        } else {
            Log.w(TAG, "Invalid connection state in setSendReceive");
            updateNotification("Connection lost", 0, 0, false);
        }
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (sendReceive != null) {
            sendReceive.cancel();
        }
        stopForeground(true);
    }
}