package com.example.clonemdp;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class ConnectDeviceActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_BLUETOOTH_PERMISSION = 2;
    private static final int REQUEST_DISCOVERABLE_BT = 3;
    private BluetoothAdapter bluetoothAdapter;
    private Switch switchBluetooth;
    private Button btnDiscoverable;

    // Receiver to sync switch when Bluetooth state changes
    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        switchBluetooth.setChecked(false);
                        break;
                    case BluetoothAdapter.STATE_ON:
                        switchBluetooth.setChecked(true);
                        break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect_device);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        switchBluetooth = findViewById(R.id.switchBluetooth);
        btnDiscoverable = findViewById(R.id.btnDiscoverable);

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set initial switch state
        switchBluetooth.setChecked(bluetoothAdapter.isEnabled());

        // Register Bluetooth state receiver
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothStateReceiver, filter);

        // Switch toggle listener
        switchBluetooth.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (checkBluetoothPermission()) {
                    if (!bluetoothAdapter.isEnabled()) {
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    }
                } else {
                    requestBluetoothPermission();
                }
            } else {
                if (checkBluetoothPermission()) {
                    bluetoothAdapter.disable();
                    Toast.makeText(this, "Bluetooth turned off", Toast.LENGTH_SHORT).show();
                } else {
                    requestBluetoothPermission();
                }
            }
        });

        btnDiscoverable.setOnClickListener(v->{
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
                if (checkBluetoothPermission()) {
                    // Ask system to make device discoverable for 120 seconds
                    Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120);
                    startActivityForResult(discoverableIntent, REQUEST_DISCOVERABLE_BT);
                } else {
                    requestBluetoothPermission();
                }
            } else {
                Toast.makeText(this, "Enable Bluetooth first", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean checkBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Older versions don't need runtime permission
    }

    private void requestBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                    REQUEST_BLUETOOTH_PERMISSION);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            // Sync switch after enabling
            switchBluetooth.setChecked(bluetoothAdapter.isEnabled());
        }
        else if (requestCode == REQUEST_DISCOVERABLE_BT) {
            if (resultCode == 120) {
                Toast.makeText(this, "Device is now discoverable for 120 seconds", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Discoverability request canceled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(bluetoothStateReceiver);
    }
}
