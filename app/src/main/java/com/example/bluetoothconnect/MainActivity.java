package com.example.bluetoothconnect;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.Manifest;
import android.os.OutcomeReceiver;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {
    Button listen, send, listDevice;
    ListView listView;
    TextView msg_box, status;
    EditText writeMsg;
    private boolean listDevicesPending = false;

    BluetoothAdapter bluetoothAdapter;
    BluetoothDevice[] btarray;
    SendReceive sendReceive;

    static final int STATE_LISTENING = 1;
    static final int STATE_CONNECTING = 2;
    static final int STATE_CONNECTED = 3;
    static final int STATE_CONNECTION_FAILED = 4;
    static final int STATE_MESSAGE_RECIEVED = 5;

    private static final int BLUETOOTH_PERMISSION_REQUEST_CODE = 101;
    private static final int BLUETOOTH_ADVERTISE_PERMISSION_REQUEST_CODE = 103;

    int REUEST_ENABLE_BLUETOOTH = 1;
    private static final String APP_NAME="BTChat";
    private static final UUID MY_UUID=UUID.fromString("123e4567-e89b-12d3-a456-426614174000");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewByIde();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestBluetoothConnectPermission();
                return;
            }
            startActivityForResult(enableIntent, REUEST_ENABLE_BLUETOOTH);
        }
        implementListener();
    }

    private void checkBluetoothConnectPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestBluetoothConnectPermission();
        }
    }

    private void requestBluetoothConnectPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, BLUETOOTH_PERMISSION_REQUEST_CODE);
    }

    private void checkBluetoothAdvertisePermission() {
        Log.d("MainActivity", "checkBluetoothAdvertisePermission called.");
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
                requestBluetoothAdvertisePermission();
            } else {
                Log.d("MainActivity", "BLUETOOTH_ADVERTISE permission already granted");
                ServerClass serverClass = new ServerClass();
                serverClass.start();
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error in checkBluetoothAdvertisePermission: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void requestBluetoothAdvertisePermission() {
        Log.d("MainActivity", "requestBluetoothAdvertisePermission called.");
        try {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_ADVERTISE}, BLUETOOTH_ADVERTISE_PERMISSION_REQUEST_CODE);
        } catch (Exception e) {
            Log.e("MainActivity", "Error in requestBluetoothAdvertisePermission: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d("MainActivity", "onRequestPermissionsResult called.");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with Bluetooth operations
                if (listDevicesPending) {
                    listPairedDevices();
                    listDevicesPending = false; // Reset the flag
                }
            } else {
                // Permission denied, handle accordingly (e.g., show a message)
            }
        } else if (requestCode == BLUETOOTH_ADVERTISE_PERMISSION_REQUEST_CODE){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Log.d("MainActivity", "BLUETOOTH_ADVERTISE granted, starting ServerClass.");
                ServerClass serverClass = new ServerClass();
                serverClass.start();
                Toast.makeText(this,"Server started",Toast.LENGTH_SHORT).show();
            }else{
                Log.e("MainActivity", "BLUETOOTH_ADVERTISE denied.");
            }
        }
    }

    private void implementListener() {
        listDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    listDevicesPending = true; // Set the flag
                    requestBluetoothConnectPermission();
                    return;
                }
                listPairedDevices();
            }
        });
        listen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("MainActivity", "Listen button clicked.");
                checkBluetoothAdvertisePermission();
//                ServerClass serverClass=new ServerClass();
//                serverClass.start();
            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, BLUETOOTH_PERMISSION_REQUEST_CODE);
                    return;
                }
                ClientClass clientClass=new ClientClass(btarray[i]);
                clientClass.start();
                status.setText("Connecting");
            }
        });
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String string= String.valueOf(writeMsg.getText());
                sendReceive.write(string.getBytes());
                writeMsg.setText("");
            }
        });
    }
    private void listPairedDevices() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Set<BluetoothDevice> bt = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
        String[] string = new String[bt.size()];
        btarray = new BluetoothDevice[bt.size()];
        int idx = 0;

        if (bt.size() > 0) {
            for (BluetoothDevice device : bt) {
                btarray[idx] = device;
                string[idx] = device.getName();
                idx++;
            }
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, string);
            listView.setAdapter(arrayAdapter);
        }
    }

    Handler handler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what){
                case STATE_LISTENING:status.setText("Listening");
                                    break;
                case STATE_CONNECTING:status.setText("Connecting");
                                    break;
                case STATE_CONNECTED:status.setText("Connected");
                                        break;
                case STATE_CONNECTION_FAILED:status.setText("Connection failed");
                                            break;
                case STATE_MESSAGE_RECIEVED:
                    byte[] readBuff=(byte[]) msg.obj;
                    String tempMsg=new String(readBuff,0,msg.arg1);
                    msg_box.setText(tempMsg);

                    break;
            }
            return true;
        }
    });

    private void findViewByIde(){
        listen=findViewById(R.id.listen);
        send=findViewById(R.id.send);
        listView=findViewById(R.id.listview);
        msg_box=findViewById(R.id.msg);
        status=findViewById(R.id.status);
        writeMsg=findViewById(R.id.writemsg);
        listDevice=findViewById(R.id.listDevices);
    }

    private class ServerClass extends Thread{
        private BluetoothServerSocket serverSocket;
        @SuppressLint("MissingPermission")
        public ServerClass(){
            Log.d("ServerClass", "ServerClass constructor called.");
            if (bluetoothAdapter == null){
                Log.e("ServerClass", "Bluetooth Adapter is null");
                return;
            }
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Log.e("ServerClass", "BLUETOOTH_CONNECT permission not granted.");
                return;
            }
            Log.d("ServerClass", "BLUETOOTH_CONNECT permission granted.");
            try {
                Log.d("ServerClass", "Creating server socket...");
                serverSocket = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(APP_NAME, MY_UUID);
                Log.d("ServerClass", "Server socket created.");
            } catch (IOException e) {
                Log.e("ServerClass", "Error creating server socket: " + e.getMessage());
                e.printStackTrace();
            }
        }
        public void run(){
            BluetoothSocket socket=null;
            while(socket==null){
                try {
                    Log.d("ServerClass", "Server listening for connections...");
                    Message message = Message.obtain();
                    message.what = STATE_CONNECTING;
                    handler.sendMessage(message);

                    socket = serverSocket.accept();
                    Log.d("ServerClass", "Server connection accepted!");
                } catch (IOException e) {
                    Log.e("ServerClass", "Server accept failed: " + e.getMessage());
                    e.printStackTrace();
                    Message message=Message.obtain();
                    message.what=STATE_CONNECTION_FAILED;
                    handler.sendMessage(message);
                }
                if(socket!=null){
                    Message message=Message.obtain();
                    message.what=STATE_CONNECTED;
                    handler.sendMessage(message);

                    sendReceive=new SendReceive(socket);
                    sendReceive.start();
                    break;
                }
            }
        }
    }
    private class ClientClass extends Thread{
        private BluetoothDevice device;
        private BluetoothSocket socket;

        @SuppressLint("MissingPermission")
        public ClientClass(BluetoothDevice device1) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            device = device1;
            try {
                Log.d("ClientClass", "Creating client socket...");
                socket = device.createRfcommSocketToServiceRecord(MY_UUID);
                Log.d("ClientClass", "Client socket created.");
            } catch (IOException e) {
                Log.e("ClientClass", "Error creating client socket: " + e.getMessage());
                e.printStackTrace();
            }
        }
        @SuppressLint("MissingPermission")
        public void run(){
            try {
                Log.d("ClientClass", "Connecting to device...");
                socket.connect();
                Log.d("ClientClass", "Connected!");
                Message message=Message.obtain();
                message.what=STATE_CONNECTED;
                handler.sendMessage(message);

                sendReceive=new SendReceive(socket);
                sendReceive.start();

            } catch (IOException e) {
                Log.e("ClientClass", "Connection failed: " + e.getMessage());
                e.printStackTrace();
                Message message=Message.obtain();
                message.what=STATE_CONNECTION_FAILED;
                handler.sendMessage(message);
            }
        }
    }

    private class SendReceive extends Thread{
        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public SendReceive (BluetoothSocket socket){
            bluetoothSocket=socket;
            InputStream tempIn=null;
            OutputStream tempOut=null;

            try {
                tempIn=bluetoothSocket.getInputStream();
                tempOut=bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            inputStream=tempIn;
            outputStream=tempOut;

        }
        public void run(){
            byte[] buffer=new byte[1024];
            int bytes;
            while (true){
                try {
                    bytes=inputStream.read(buffer);
                    handler.obtainMessage(STATE_MESSAGE_RECIEVED,bytes,-1,buffer).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        public void write(byte[] bytes){
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }
}