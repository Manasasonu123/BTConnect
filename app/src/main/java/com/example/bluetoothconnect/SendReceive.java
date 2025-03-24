//package com.example.bluetoothconnect;
//
//import android.bluetooth.BluetoothSocket;
//import android.os.Handler;
//import android.os.Parcel;
//import android.os.Parcelable;
//import android.util.Log;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStream;
//
//public class SendReceive extends Thread {
//    private static final String TAG = "SendReceive";
//    private BluetoothSocket bluetoothSocket;
//    private InputStream inputStream;
//    private OutputStream outputStream;
//    private Handler handler;
//    private volatile boolean isRunning = true;
//
//    public SendReceive(BluetoothSocket socket, Handler handler) {
//        this.bluetoothSocket = socket;
//        this.handler = handler;
//        try {
//            this.inputStream = socket.getInputStream();
//            this.outputStream = socket.getOutputStream();
//        } catch (IOException e) {
//            Log.e(TAG, "Error getting input or output stream", e);
//        }
//    }
//
//    @Override
//    public void run() {
//        byte[] buffer = new byte[1024];
//        int bytes;
//
//        while (isRunning) {
//            try {
//                bytes = inputStream.read(buffer);
//                if (bytes == -1) {
//                    Log.d(TAG, "InputStream returned -1, socket closed");
//                    break;
//                }
//                if (bytes > 0) {
//                    handler.obtainMessage(MainActivity.STATE_MESSAGE_RECIEVED, bytes, -1, buffer).sendToTarget();
//                }
//            } catch (IOException e) {
//                Log.e(TAG, "Error reading from input stream", e);
//                break;
//            }
//        }
//
//        handler.obtainMessage(MainActivity.STATE_CONNECTION_FAILED).sendToTarget();
//        cancel();
//    }
//
//    public void write(byte[] bytes) {
//        try {
//            outputStream.write(bytes);
//        } catch (IOException e) {
//            Log.e(TAG, "Error writing to output stream: " + e.getMessage());
//            handler.obtainMessage(MainActivity.STATE_CONNECTION_FAILED).sendToTarget();
//            cancel();
//        }
//    }
//
//    public void cancel() {
//        isRunning = false;
//        try {
//            if (inputStream != null) inputStream.close();
//            if (outputStream != null) outputStream.close();
//            if (bluetoothSocket != null) bluetoothSocket.close();
//        } catch (IOException e) {
//            Log.e(TAG, "Error closing streams or socket", e);
//        }
//    }
//}


package com.example.bluetoothconnect;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SendReceive extends Thread {
    private static final String TAG = "SendReceive";
    private static final String ACK_PREFIX = "ACK:";
    private BluetoothSocket bluetoothSocket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private Handler handler;
    private BluetoothForegroundService service;
    private volatile boolean isRunning = true;

    public SendReceive(BluetoothSocket socket, Handler handler, BluetoothForegroundService service) {
        this.bluetoothSocket = socket;
        this.handler = handler;
        this.service = service;
        try {
            this.inputStream = socket.getInputStream();
            this.outputStream = socket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "Error getting input or output stream", e);
        }
    }

    @Override
    public void run() {
        byte[] buffer = new byte[1024];
        int bytes;

        while (isRunning) {
            try {
                bytes = inputStream.read(buffer);
                if (bytes == -1) {
                    Log.d(TAG, "InputStream returned -1, socket closed");
                    break;
                }
                if (bytes > 0) {
                    String receivedMessage = new String(buffer, 0, bytes).trim();
                    Log.d(TAG, "Received: " + receivedMessage);

                    if (receivedMessage.startsWith(ACK_PREFIX)) {
                        // Handle acknowledgment
                        String ackItem = receivedMessage.substring(ACK_PREFIX.length());
                        Log.d(TAG, "Processing acknowledgment for: " + ackItem);
                        if (service != null) {
                            new Handler(Looper.getMainLooper()).post(() -> {
                                service.onAcknowledgmentReceived(ackItem);
                            });
                        }
                    } else {
                        // Send acknowledgment back to sender
                        Log.d(TAG, "Sending acknowledgment for: " + receivedMessage);
                        sendAcknowledgment(receivedMessage);
                        handler.obtainMessage(MainActivity.STATE_MESSAGE_RECIEVED, bytes, -1, buffer).sendToTarget();
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Error reading from input stream", e);
                break;
            }
        }
        cancel();
    }

    public void write(byte[] bytes) {
        try {
            outputStream.write(bytes);
            Log.d(TAG, "Sent: " + new String(bytes));
        } catch (IOException e) {
            Log.e(TAG, "Error writing to output stream: " + e.getMessage());
            handler.obtainMessage(MainActivity.STATE_CONNECTION_FAILED).sendToTarget();
            cancel();
        }
    }

    private void sendAcknowledgment(String receivedItem) {
        try {
            String ackMessage = ACK_PREFIX + receivedItem;
            outputStream.write(ackMessage.getBytes());
            outputStream.flush(); // Ensure data is sent immediately
            Log.d(TAG, "Successfully sent acknowledgment: " + ackMessage);
        } catch (IOException e) {
            Log.e(TAG, "Error sending acknowledgment", e);
            handler.obtainMessage(MainActivity.STATE_CONNECTION_FAILED).sendToTarget();
            cancel();
        }
    }

    public void cancel() {
        isRunning = false;
        try {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (bluetoothSocket != null) bluetoothSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Error closing streams or socket", e);
        }
    }
}