package com.example.bluetoothconnect;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SendReceive extends Thread {
    private static final String TAG = "SendReceive";
    private final BluetoothSocket bluetoothSocket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private final Handler handler;
    private volatile boolean isRunning = true;

    public SendReceive(BluetoothSocket socket, Handler handler) {
        this.bluetoothSocket = socket;
        this.handler = handler;
        try {
            inputStream = bluetoothSocket.getInputStream();
            outputStream = bluetoothSocket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "Error getting input or output stream", e);
            cancel(); // Close socket and streams on error
        }
    }

    @Override
    public void run() {
        byte[] buffer = new byte[1024];
        int bytes;

        if (inputStream == null) {
            Log.e(TAG, "Input stream is null. Exiting thread.");
            handler.obtainMessage(MainActivity.STATE_CONNECTION_FAILED).sendToTarget();
            return;
        }

        while (isRunning) {
            try {
                bytes = inputStream.read(buffer);
                if (bytes > 0) {
                    handler.obtainMessage(MainActivity.STATE_MESSAGE_RECIEVED, bytes, -1, buffer).sendToTarget();
                } else if (bytes == -1) {
                    Log.d(TAG, "End of stream reached or socket closed.");
                    break; // Exit loop when stream ends or socket is closed.
                }
            } catch (IOException e) {
                Log.e(TAG, "Error reading from input stream", e);
                break;
            }
        }

        handler.obtainMessage(MainActivity.STATE_CONNECTION_FAILED).sendToTarget();
        cancel();
    }

    public synchronized void write(byte[] bytes) {
        if (outputStream == null) {
            Log.e(TAG, "Output stream is null. Cannot write.");
            return;
        }
        try {
            outputStream.write(bytes);
        } catch (IOException e) {
            Log.e(TAG, "Error writing to output stream", e);
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