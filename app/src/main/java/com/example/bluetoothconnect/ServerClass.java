//// ServerClass.java
//package com.example.bluetoothconnect;
//
//import android.Manifest;
//import android.annotation.SuppressLint;
//import android.bluetooth.BluetoothAdapter;
//import android.bluetooth.BluetoothServerSocket;
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
//import static com.example.bluetoothconnect.MainActivity.STATE_CONNECTED;
//import static com.example.bluetoothconnect.MainActivity.STATE_CONNECTION_FAILED;
//import static com.example.bluetoothconnect.MainActivity.STATE_CONNECTING;
//
//public class ServerClass extends Thread {
//
//    private BluetoothServerSocket serverSocket;
//    private BluetoothAdapter bluetoothAdapter;
//    private Handler handler;
//    private Context context;
//    private static final String APP_NAME = "BTChat";
//    private static final UUID MY_UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
//
//    @SuppressLint("MissingPermission")
//    public ServerClass(BluetoothAdapter bluetoothAdapter, Handler handler, Context context) {
//        this.bluetoothAdapter = bluetoothAdapter;
//        this.handler = handler;
//        this.context = context;
//
//        if (bluetoothAdapter == null) {
//            Log.e("ServerClass", "Bluetooth Adapter is null");
//            return;
//        }
//
//        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//            Log.e("ServerClass", "BLUETOOTH_CONNECT permission not granted.");
//            return;
//        }
//
//        try {
//            Log.d("ServerClass", "Creating server socket...");
//            serverSocket = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(APP_NAME, MY_UUID);
//            Log.d("ServerClass", "Server socket created.");
//        } catch (IOException e) {
//            Log.e("ServerClass", "Error creating server socket: " + e.getMessage());
//            e.printStackTrace();
//            Message message = Message.obtain();
//            message.what = STATE_CONNECTION_FAILED;
//            handler.sendMessage(message); // notify main thread
//        }
//    }
//
//    public void run() {
//        BluetoothSocket socket = null;
//        while (socket == null) {
//            try {
//                Log.d("ServerClass", "Server listening for connections...");
//                Message message = Message.obtain();
//                message.what = STATE_CONNECTING;
//                handler.sendMessage(message);
//
//                socket = serverSocket.accept();
//                Log.d("ServerClass", Thread.currentThread().getName() + ": Server connection accepted!");
//            } catch (IOException e) {
//                Log.e("ServerClass", "Server accept failed: " + e.getMessage());
//                e.printStackTrace();
//                Message message = Message.obtain();
//                message.what = STATE_CONNECTION_FAILED;
//                handler.sendMessage(message);
//            }finally {
//                if (serverSocket != null) {
//                    try {
//                        serverSocket.close();
//                        Log.d("ServerClass", "ServerSocket Closed");
//                    } catch (IOException e) {
//                        Log.e("ServerClass", "Error closing server socket: " + e.getMessage());
//                        e.printStackTrace();
//                    }
//                }
//            }
//
//            if (socket != null) {
//                Message message = Message.obtain();
//                message.what = MainActivity.STATE_CONNECTED;
//                message.obj = socket; // Pass the socket.
//                handler.sendMessage(message);
//                SendReceive sendReceive = new SendReceive(socket, handler);
//                sendReceive.start();
//                // store sendReceive as a class member if needed
//                break;
//            }
//        }
//    }
//}
package com.example.bluetoothconnect;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.UUID;

import static com.example.bluetoothconnect.MainActivity.STATE_CONNECTION_FAILED;
import static com.example.bluetoothconnect.MainActivity.STATE_CONNECTING;

public class ServerClass extends Thread {
    private BluetoothServerSocket serverSocket;
    private BluetoothAdapter bluetoothAdapter;
    private Handler handler;
    private Context context;
    private BluetoothForegroundService service;
    private static final String APP_NAME = "BTChat";
    private static final UUID MY_UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    @SuppressLint("MissingPermission")
    public ServerClass(BluetoothAdapter bluetoothAdapter, Handler handler, Context context, BluetoothForegroundService service) {
        this.bluetoothAdapter = bluetoothAdapter;
        this.handler = handler;
        this.context = context;
        this.service = service;

        if (bluetoothAdapter == null) {
            Log.e("ServerClass", "Bluetooth Adapter is null");
            return;
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.e("ServerClass", "BLUETOOTH_CONNECT permission not granted.");
            return;
        }

        try {
            Log.d("ServerClass", "Creating server socket...");
            serverSocket = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(APP_NAME, MY_UUID);
            Log.d("ServerClass", "Server socket created.");
        } catch (IOException e) {
            Log.e("ServerClass", "Error creating server socket: " + e.getMessage());
            e.printStackTrace();
            Message message = Message.obtain();
            message.what = STATE_CONNECTION_FAILED;
            handler.sendMessage(message);
        }
    }

    public void run() {
        BluetoothSocket socket = null;
        while (socket == null) {
            try {
                Log.d("ServerClass", "Server listening for connections...");
                Message message = Message.obtain();
                message.what = STATE_CONNECTING;
                handler.sendMessage(message);

                socket = serverSocket.accept();
                Log.d("ServerClass", Thread.currentThread().getName() + ": Server connection accepted!");
            } catch (IOException e) {
                Log.e("ServerClass", "Server accept failed: " + e.getMessage());
                e.printStackTrace();
                Message message = Message.obtain();
                message.what = STATE_CONNECTION_FAILED;
                handler.sendMessage(message);
            } finally {
                if (serverSocket != null) {
                    try {
                        serverSocket.close();
                        Log.d("ServerClass", "ServerSocket Closed");
                    } catch (IOException e) {
                        Log.e("ServerClass", "Error closing server socket: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }

            if (socket != null) {
                Message message = Message.obtain();
                message.what = MainActivity.STATE_CONNECTED;
                message.obj = socket;
                handler.sendMessage(message);

                // Initialize SendReceive with all required parameters
                if (context instanceof MainActivity && service != null) {
                    SendReceive sendReceive = new SendReceive(socket, handler, context, service);
                    sendReceive.start();

                    // Set the SendReceive instance in MainActivity
                    ((MainActivity) context).setSendReceive(sendReceive);
                }
                break;
            }
        }
    }
}