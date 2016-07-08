package vodka.develop.bleconnect;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter;
    public static BluetoothDevice mDevice = null;
    private static final long SCAN_PERIOD = 3000;
    private static final int MY_PERMISSION_ACCESS_LOCATION = 1;
    private static final String DEVICE_NAME = "TXRX";
    private RBLService mBluetoothLeService = null;
    private Map<UUID, BluetoothGattCharacteristic> map = new HashMap<UUID, BluetoothGattCharacteristic>();

    private ScanCallback mLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if(mDevice == null && device.getName().equals(DEVICE_NAME)){
                Log.v("BLEX", "FOUND DEVICE");
                mDevice = device;
            }

        }
    };

    private final ServiceConnection mServiceConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((RBLService.LocalBinder) service).getService();
            if(!mBluetoothLeService.initialize()){
                Log.e("BLEX", "Unable to init Bluetooth");
                finish();
            }

            Log.v("BLEX", "Connecting...");
            mBluetoothLeService.connect(mDevice.getAddress());
            sendSomething();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private void sendSomething() {
        // Get Gett service and characteristics... or something
//        connected to device
//        stopped here
    }

    protected void onCreate(Bundle savedInstanceState) {
        Log.v("BLEX", "Starting Up");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        final BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if(mBluetoothAdapter == null){
            Toast.makeText(this, "BLE not supported", Toast.LENGTH_SHORT).show();
            Log.v("BLEX", "BLE not supported");
            finish();
            return;
        }

        if(!mBluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Bluetooth not enabled", Toast.LENGTH_SHORT).show();
            Log.v("BLEX", "Bluetooth not enabled");
            finish();
            return;
        }

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSION_ACCESS_LOCATION);
        } else {
            scanLeDevice();
        }

    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch(requestCode){
            case MY_PERMISSION_ACCESS_LOCATION: {
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    scanLeDevice();
                }
            }
        }
    }



    private void startConnection() {
        if(mDevice != null){
            Toast.makeText(this, "Connecting to device.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Could not find device.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent gattServiceIntent = new Intent(this, RBLService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    private void scanLeDevice() {
        new Thread(){
            public void run() {
                Log.v("BLEX", "Starting Scan");
                BluetoothLeScanner mLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
                mLeScanner.startScan(mLeScanCallback);

                try {
                    Thread.sleep(SCAN_PERIOD);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                Log.v("BLEX", "Done Scanning");
                mLeScanner.stopScan(mLeScanCallback);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        startConnection();
                    }
                });

            }
        }.start();

    }
}
