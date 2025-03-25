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
import java.util.Arrays;

public class SendReceive extends Thread {
    private static final String TAG = "SendReceive";
    private static final String ACK_PREFIX = "ACK:";

    // Thread-safe socket and stream references
    private final BluetoothSocket bluetoothSocket;
    private final InputStream inputStream;
    private final OutputStream outputStream;
    private final Handler handler;

    // Service reference with volatile for visibility
    private volatile BluetoothForegroundService service;

    // Thread control
    private volatile boolean isRunning = true;
    private final Object writeLock = new Object(); // Separate lock for write operations

    public SendReceive(BluetoothSocket socket, Handler handler) {
        this(socket, handler, null);
    }

    public SendReceive(BluetoothSocket socket, Handler handler, BluetoothForegroundService service) {
        this.bluetoothSocket = socket;
        this.handler = handler;
        this.service = service;

        // Initialize streams in constructor
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "Error getting streams", e);
        }

        this.inputStream = tmpIn;
        this.outputStream = tmpOut;
    }

    public boolean isReady() {
        return isRunning &&
                bluetoothSocket != null &&
                bluetoothSocket.isConnected() &&
                inputStream != null &&
                outputStream != null;
    }

    public void setService(BluetoothForegroundService service) {
        this.service = service;
    }

    public boolean isRunning() {
        return isRunning &&
                bluetoothSocket != null &&
                bluetoothSocket.isConnected();
    }

    @Override
    public void run() {
        byte[] buffer = new byte[1024];
        int bytes;

        while (isRunning) {
            try {
                bytes = inputStream.read(buffer);
                if (bytes == -1) {
                    Log.d(TAG, "Connection closed by remote");
                    break;
                }

                if (bytes > 0) {
                    processReceivedData(buffer, bytes);
                }
            } catch (IOException e) {
                if (isRunning) { // Only log unexpected errors
                    Log.e(TAG, "Read error: " + e.getMessage());
                }
                break;
            }
        }

        if (isRunning) { // Unexpected termination
            handler.obtainMessage(MainActivity.STATE_CONNECTION_FAILED).sendToTarget();
        }
        cleanup();
    }

    private void processReceivedData(byte[] buffer, int bytes) throws IOException {
        String receivedMessage = new String(buffer, 0, bytes).trim();
        Log.d(TAG, "Received: " + receivedMessage);

        if (receivedMessage.startsWith(ACK_PREFIX)) {
            handleAcknowledgment(receivedMessage);
        } else {
            sendAcknowledgment(receivedMessage);
            forwardMessageToUi(buffer, bytes);
        }
    }

    private void handleAcknowledgment(String ackMessage) {
        String ackItem = ackMessage.substring(ACK_PREFIX.length());
        Log.d(TAG, "Processing ACK: " + ackItem);

        if (service != null) {
            new Handler(Looper.getMainLooper()).post(() -> {
                service.onAcknowledgmentReceived(ackItem);
            });
        }
    }

    private void forwardMessageToUi(byte[] buffer, int bytes) {
        handler.obtainMessage(
                MainActivity.STATE_MESSAGE_RECIEVED,
                bytes,
                -1,
                Arrays.copyOf(buffer, bytes)
        ).sendToTarget();
    }

    public void write(byte[] bytes) {
        if (!isReady()) {
            Log.e(TAG, "Write aborted - connection not ready");
            handler.obtainMessage(MainActivity.STATE_CONNECTION_FAILED).sendToTarget();
            return;
        }

        synchronized (writeLock) {
            try {
                outputStream.write(bytes);
                outputStream.flush();
                Log.d(TAG, "Sent: " + new String(bytes));
            } catch (IOException e) {
                Log.e(TAG, "Write failed: " + e.getMessage());
                handler.obtainMessage(MainActivity.STATE_CONNECTION_FAILED).sendToTarget();
                stopRunning();
            }
        }
    }

    private void sendAcknowledgment(String receivedItem) throws IOException {
        String ackMessage = ACK_PREFIX + receivedItem;
        synchronized (writeLock) {
            outputStream.write(ackMessage.getBytes());
            outputStream.flush();
            Log.d(TAG, "Sent ACK: " + ackMessage);
        }
    }

    public void cancel() {
        stopRunning();
        cleanup();
    }

    private void stopRunning() {
        isRunning = false;
        interrupt(); // Interrupt any blocking read operations
    }

    private void cleanup() {
        try {
            if (inputStream != null) inputStream.close();
        } catch (IOException e) {
            Log.e(TAG, "Error closing input stream", e);
        }

        try {
            if (outputStream != null) outputStream.close();
        } catch (IOException e) {
            Log.e(TAG, "Error closing output stream", e);
        }

        try {
            if (bluetoothSocket != null) bluetoothSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Error closing socket", e);
        }
    }
}