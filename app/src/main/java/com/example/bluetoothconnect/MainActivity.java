package com.example.bluetoothconnect;

import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.Manifest;
import android.os.Parcelable;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Set;


public class MainActivity extends AppCompatActivity {
    Button listen, send, listDevice,start;
    TextView msg_box, status;
    EditText writeMsg;
    private boolean listDevicesPending = false;

    BluetoothAdapter bluetoothAdapter;
    BluetoothDevice[] btarray;
    private SendReceive sendReceive;
    private BluetoothForegroundService bluetoothForegroundService;
    private boolean isBound = false;

    static final int STATE_LISTENING = 1;
    static final int STATE_CONNECTING = 2;
    static final int STATE_CONNECTED = 3;
    static final int STATE_CONNECTION_FAILED = 4;
    static final int STATE_MESSAGE_RECIEVED = 5;

    private static final int BLUETOOTH_PERMISSION_REQUEST_CODE = 101;
    private static final int BLUETOOTH_ADVERTISE_PERMISSION_REQUEST_CODE = 103;

    int REUEST_ENABLE_BLUETOOTH = 1;

    private RecyclerView recyclerView;
    private RecyclerViewAdapter recyclerViewAdapter;
    private List<List<String>> itemList = Arrays.asList(
            Arrays.asList("1a", "2n", "3g", "5j", "6k"),
            Arrays.asList("7l", "8m", "9o", "10p", "11q"),
            Arrays.asList("8r", "5s", "3t", "5j", "9k"),
            Arrays.asList("19a", "23h", "34g", "58j", "61k")// Example of a second list
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewByIde();
        setupRecyclerView();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        send.setEnabled(false);

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

    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewAdapter = new RecyclerViewAdapter(itemList);
        recyclerView.setAdapter(recyclerViewAdapter);
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
                ServerClass serverClass = new ServerClass(bluetoothAdapter, handler, this,bluetoothForegroundService);
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
                    showPairedDevicesDialog();
                    listDevicesPending = false; // Reset the flag
                }
            } else {
                // Permission denied, handle accordingly (e.g., show a message)
            }
        } else if (requestCode == BLUETOOTH_ADVERTISE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "BLUETOOTH_ADVERTISE granted, starting ServerClass.");
                ServerClass serverClass = new ServerClass(bluetoothAdapter, handler, this,bluetoothForegroundService);
                serverClass.start();
                Toast.makeText(this, "Server started", Toast.LENGTH_SHORT).show();
            } else {
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
                showPairedDevicesDialog();
            }
        });
        listen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("MainActivity", "Listen button clicked.");
                checkBluetoothAdvertisePermission();
            }
        });
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (sendReceive != null) {
                    String string = writeMsg.getText().toString();
                    if (!string.isEmpty()) {
                        sendReceive.write(string.getBytes());
                        writeMsg.setText("");
                    } else {
                        Toast.makeText(MainActivity.this, "Enter a message to send!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Not connected", Toast.LENGTH_SHORT).show();
                    Log.e("MainActivity", "sendReceive is null, connection not established.");
                }
            }
        });

        start.setOnClickListener(v -> {
            if (sendReceive == null) {
                Toast.makeText(MainActivity.this, "Not connected", Toast.LENGTH_SHORT).show();
                return;
            }
            for(int i=0;i<itemList.size();i++){
                Intent serviceIntent=new Intent(MainActivity.this,BluetoothForegroundService.class);
                serviceIntent.putExtra("itemList",(Serializable) itemList);
                bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
                if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
                    startForegroundService(serviceIntent);
                }else{
                    startService(serviceIntent);
                }
            }
        });


    }

    private void showPairedDevicesDialog() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Set<BluetoothDevice> bt = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
        String[] deviceNames = new String[bt.size()];
        btarray = new BluetoothDevice[bt.size()];
        int idx = 0;

        if (bt.size() > 0) {
            for (BluetoothDevice device : bt) {
                btarray[idx] = device;
                deviceNames[idx] = device.getName();
                idx++;
            }
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, deviceNames);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Paired Devices");
            builder.setAdapter(arrayAdapter, (dialog, which) -> {
                ClientClass clientClass = new ClientClass(btarray[which], handler, this);
                clientClass.start();
                status.setText("Connecting");
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
            builder.show();
        } else {
            Toast.makeText(this, "No paired devices found.", Toast.LENGTH_SHORT).show();
        }
    }
    public void setSendReceive(SendReceive sendReceive) {
        this.sendReceive = sendReceive;
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BluetoothForegroundService.LocalBinder binder = (BluetoothForegroundService.LocalBinder) service;
            bluetoothForegroundService = binder.getService();
            isBound = true;
            // Connect the SendReceive instance to the service
            if (sendReceive != null) {
                bluetoothForegroundService.setSendReceive(sendReceive);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bluetoothForegroundService = null;
            isBound = false;
        }
    };

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case STATE_LISTENING:
                    status.setText("Listening");
                    break;
                case STATE_CONNECTING:
                    status.setText("Connecting");
                    break;
                case STATE_CONNECTED:
                    status.setText("Connected");
                    send.setEnabled(true);
                    if (msg.obj != null && msg.obj instanceof BluetoothSocket) { // Check if socket is passed
                        sendReceive = new SendReceive((BluetoothSocket) msg.obj, handler,bluetoothForegroundService); // Initialize sendReceive
                        sendReceive.start();
                    }
                    break;
                case STATE_CONNECTION_FAILED:
                    status.setText("Connection failed");
                    break;
                case STATE_MESSAGE_RECIEVED:
                    byte[] readBuff = (byte[]) msg.obj;
                    String tempMsg = new String(readBuff, 0, msg.arg1);
                    msg_box.setText(tempMsg);

                    break;
            }
            return true;
        }
    });

    private void findViewByIde() {
        listen = findViewById(R.id.listen);
        send = findViewById(R.id.send);
        msg_box = findViewById(R.id.msg);
        status = findViewById(R.id.status);
        writeMsg = findViewById(R.id.writemsg);
        listDevice = findViewById(R.id.listDevices);
        start=findViewById(R.id.start);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
        if (sendReceive != null) {
            sendReceive.cancel();
        }
    }
}