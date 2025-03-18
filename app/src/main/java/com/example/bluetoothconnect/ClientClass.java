//// ClientClass.java
//package com.example.bluetoothconnect;
//
//import android.Manifest;
//import android.annotation.SuppressLint;
//import android.bluetooth.BluetoothDevice;
//import android.bluetooth.BluetoothSocket;
//import android.content.Context;
//import android.content.pm.PackageManager;
//import android.os.Handler;
//import android.os.Message;
//import android.util.Log;
//
//import androidx.core.content.ContextCompat;
//
//import java.io.IOException;
//import java.util.UUID;
//
//public class ClientClass extends Thread {
//    private BluetoothDevice device;
//    private BluetoothSocket socket;
//    private Handler handler;
//    private Context context;
//    private static final UUID MY_UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
//
//    @SuppressLint("MissingPermission")
//    public ClientClass(BluetoothDevice device1, Handler handler, Context context) {
//        this.device = device1;
//        this.handler = handler;
//        this.context = context;
//        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//            return;
//        }
//        try {
//            Log.d("ClientClass", "Creating client socket...");
//            socket = device.createRfcommSocketToServiceRecord(MY_UUID);
//            Log.d("ClientClass", "Client socket created.");
//        } catch (IOException e) {
//            Log.e("ClientClass", "Error creating client socket: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
//    @SuppressLint("MissingPermission")
//    public void run() {
//        try {
//            Log.d("ClientClass", "Connecting to device...");
//            socket.connect();
//            Log.d("ClientClass", "Connected!");
//            Message message = Message.obtain();
//            message.what = MainActivity.STATE_CONNECTED;
//            handler.sendMessage(message);
//
//            SendReceive sendReceive = new SendReceive(socket, handler);
//            sendReceive.start();
//
//        } catch (IOException e) {
//            Log.e("ClientClass", "Connection failed: " + e.getMessage());
//            e.printStackTrace();
//            Message message = Message.obtain();
//            message.what = MainActivity.STATE_CONNECTION_FAILED;
//            handler.sendMessage(message);
//        }
//    }
//}
package com.example.bluetoothconnect;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.UUID;

public class ClientClass extends Thread {
    private BluetoothDevice device;
    private BluetoothSocket socket;
    private Handler handler;
    private Context context;
    private static final UUID MY_UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    @SuppressLint("MissingPermission")
    public ClientClass(BluetoothDevice device1, Handler handler, Context context) {
        this.device = device1;
        this.handler = handler;
        this.context = context;
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        try {
            Log.d("ClientClass", "Creating client socket...");
            socket = device.createRfcommSocketToServiceRecord(MY_UUID);
            Log.d("ClientClass", "Client socket created.");
        } catch (IOException e) {
            Log.e("ClientClass", "Error creating client socket: " + e.getMessage());
            e.printStackTrace();
            Message message = Message.obtain();
            message.what = MainActivity.STATE_CONNECTION_FAILED;
            handler.sendMessage(message);
            socket = null; // Set socket to null to prevent further errors
        }
    }

    @SuppressLint("MissingPermission")
    public void run() {
        try {
            if (socket != null) { // Check if socket is valid
                Log.d("ClientClass", "Connecting to device...");
                socket.connect();
                Log.d("ClientClass", "Connected!");
                Message message = Message.obtain();
                message.what = MainActivity.STATE_CONNECTED;
                message.obj = socket; // Pass the socket
                handler.sendMessage(message);
            } else {
                Log.e("ClientClass", "Socket is null, connection not attempted.");
                Message message = Message.obtain();
                message.what = MainActivity.STATE_CONNECTION_FAILED;
                handler.sendMessage(message);
            }
        } catch (IOException e) {
            Log.e("ClientClass", "Connection failed: " + e.getMessage());
            e.printStackTrace();
            Message message = Message.obtain();
            message.what = MainActivity.STATE_CONNECTION_FAILED;
            handler.sendMessage(message);
        }
    }
}