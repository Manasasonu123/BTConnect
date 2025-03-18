// SendReceive.java
package com.example.bluetoothconnect;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SendReceive extends Thread {
    private final BluetoothSocket bluetoothSocket;
    private final InputStream inputStream;
    private final OutputStream outputStream;
    private Handler handler;

    public SendReceive(BluetoothSocket socket, Handler handler) {
        bluetoothSocket = socket;
        this.handler = handler;
        InputStream tempIn = null;
        OutputStream tempOut = null;

        try {
            tempIn = bluetoothSocket.getInputStream();
            tempOut = bluetoothSocket.getOutputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        inputStream = tempIn;
        outputStream = tempOut;

    }

    public void run() {
        byte[] buffer = new byte[1024];
        int bytes;
        while (true) {
            try {
                bytes = inputStream.read(buffer);
                handler.obtainMessage(MainActivity.STATE_MESSAGE_RECIEVED, bytes, -1, buffer).sendToTarget();
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    public void write(byte[] bytes) {
        try {
            outputStream.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}