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
    private static final long ITEM_SEND_DELAY = 2000; // 2 seconds between items
    private static final long LIST_TRANSITION_DELAY = 3000; // 3 seconds between lists
    private static final long ACK_TIMEOUT = 5000; // 5 seconds wait for ACK

    private boolean isConnected = false;
    private boolean waitingForAck = false;
    private String lastSentItem = null;

    private List<List<String>> itemList;
    private int listIndex = 0;
    private int itemIndex = 0;
    private int totalLists = 0;
    private int totalItemsToSend = 0;
    private int acknowledgedItems = 0;
    private int currentListAcknowledged = 0;

    private NotificationManager notificationManager;
    private Handler handler;
    private SendReceive sendReceive;
    private Runnable timeoutRunnable;

    public String getLastSentItem() {
        return lastSentItem;
    }

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
            Notification notification = createNotification("Initializing transfer...", 0, 0);
            startForeground(NOTIFICATION_ID, notification);

            itemList = (List<List<String>>) intent.getSerializableExtra("itemList");
            resetCounters();

            if (itemList == null || itemList.isEmpty()) {
                stopSelf();
                return START_NOT_STICKY;
            }

            calculateTotalItems();
            updateNotification("Waiting for connection...", 0, 0);
            Log.d(TAG, "Service started with " + itemList.size() + " lists");
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
        if (timeoutRunnable != null) {
            handler.removeCallbacks(timeoutRunnable);
        }

        if (!isConnected || sendReceive == null || !sendReceive.isRunning()) {
            Log.w(TAG, "Connection not ready - retrying");
            handler.postDelayed(this::sendNextItem, ITEM_SEND_DELAY);
            return;
        }

        if (listIndex >= totalLists) {
            handleTransmissionComplete();
            return;
        }

        List<String> currentList = itemList.get(listIndex);
        if (itemIndex < currentList.size() && !waitingForAck) {
            sendCurrentItem(currentList);
        } else if (itemIndex >= currentList.size()) {
            handleListTransition(currentList);
        }
    }

    private void sendCurrentItem(List<String> currentList) {
        String item = currentList.get(itemIndex);
        Log.d(TAG, "Sending: " + item);

        try {
            sendReceive.write(item.getBytes());
            waitingForAck = true;
            lastSentItem = item;

            updateNotificationStatus("Sending: " + item, null);
            setupAckTimeout(item);
        } catch (Exception e) {
            Log.e(TAG, "Send failed: " + e.getMessage());
            handler.postDelayed(this::sendNextItem, ITEM_SEND_DELAY);
        }
    }

    private void setupAckTimeout(String item) {
        timeoutRunnable = () -> {
            if (waitingForAck && item.equals(lastSentItem)) {
                Log.w(TAG, "Timeout for: " + item);
                waitingForAck = false;
                itemIndex++;
                handler.postDelayed(this::sendNextItem, ITEM_SEND_DELAY);
            }
        };
        handler.postDelayed(timeoutRunnable, ACK_TIMEOUT);
    }

    private void handleListTransition(List<String> currentList) {
        if (listIndex < totalLists - 1) {
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
        updateNotification("Starting list:"+(listIndex+1), overallProgress, 0);

        handler.postDelayed(this::sendNextItem, LIST_TRANSITION_DELAY);
    }

    private void handleTransmissionComplete() {
        updateNotification("Transmission complete", 100, 100);
        handler.postDelayed(this::stopSelf, ITEM_SEND_DELAY);
    }

    public void onAcknowledgmentReceived(String ackItem) {
        if (ackItem.equals(lastSentItem)) {
            if (timeoutRunnable != null) {
                handler.removeCallbacks(timeoutRunnable);
            }
            waitingForAck = false;
            acknowledgedItems++;
            currentListAcknowledged++;

            updateNotificationStatus("Sent: " + ackItem, ackItem);

            itemIndex++;
            handler.postDelayed(this::sendNextItem, ITEM_SEND_DELAY);
        } else {
            Log.w(TAG, "Unexpected ACK received: " + ackItem);
        }
    }

    private void updateNotificationStatus(String status, String ackItem) {
        List<String> currentList = itemList.get(listIndex);
        int overallProgress = (int) (((double) acknowledgedItems / totalItemsToSend) * 100);
        int listProgress = currentList.isEmpty() ? 0 :
                (int) (((double) currentListAcknowledged / currentList.size()) * 100);

        String notificationText = status + "\t" +
                (ackItem != null ? "Ack: " + ackItem + "\n" : "") +
                "List progress: " + listProgress + "%\t" +
                "Overall progress: " + overallProgress + "%";

        updateNotification(notificationText, overallProgress, listProgress);
    }

    private void updateNotification(String message, int overallProgress, int listProgress) {
        if (notificationManager != null) {
            Notification notification = createNotification(message, overallProgress, listProgress);
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }

    private Notification createNotification(String message, int overallProgress, int listProgress) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Bluetooth Transfer")
                .setContentText(message)
                .setSmallIcon(R.drawable.baseline_notifications_24)
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setProgress(100, listProgress, false);

        if (overallProgress == 100) {
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

        if (this.isConnected && !wasConnected) {
            Log.d(TAG, "New connection established, starting transmission");
            sendNextItem();
        } else if (!this.isConnected) {
            updateNotification("Connection lost", 0, 0);
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