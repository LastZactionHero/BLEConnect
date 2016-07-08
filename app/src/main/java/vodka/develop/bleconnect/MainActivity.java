package vodka.develop.bleconnect;

import android.Manifest;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements Scanner.DeviceFoundEvent {
    private static final int MY_PERMISSION_ACCESS_LOCATION = 1;
    private static final int MAX_MESSAGE_LEN = 14;
    private Map<UUID, BluetoothGattCharacteristic> map = new HashMap<UUID, BluetoothGattCharacteristic>();
    private Scanner mScanner = null;
    private BluetoothDevice mDevice = null;
    private RBLService mBluetoothLeService = null;


    private void sendSomething() {
        // Get Gett service and characteristics... or something
//        connected to device
//        stopped here
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v("BLEX", "GATT Update Receiver: onReceive");

            final String action = intent.getAction();
            if(RBLService.ACTION_GATT_DISCONNECTED.equals(action)){
                Log.v("BLEX", "GATT Disconnected");
            } else if(RBLService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)){
                Log.v("BLEX", "GATT Services Discovered");
                getGattService(mBluetoothLeService.getSupportedGattService());
            } else if(RBLService.ACTION_DATA_AVAILABLE.equals(action)) {
                Log.v("BLEX", "Action Data Available");
            }
        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        Log.v("BLEX", "Starting Up");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mScanner = new Scanner(this);

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSION_ACCESS_LOCATION);
        } else {
            BluetoothManager manager = (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE);
            mScanner.findDevice(manager);
        }

        Button buttonSendNotification = (Button)findViewById(R.id.buttonSendNotification);
        buttonSendNotification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendNotification("Hello World!0123456789 0123456789 0123456789 0123456789");
            }
        });

    }

    protected void onResume() {
        super.onResume();
        startConnection();
        registerGattReceiver();
    }

    private void registerGattReceiver() {
        Log.v("BLEX", "Registering GATT Receiver");
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch(requestCode){
            case MY_PERMISSION_ACCESS_LOCATION: {
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    BluetoothManager manager = (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE);
                    mScanner.findDevice(manager);
                }
            }
        }
    }

    @Override
    public void deviceFound(BluetoothDevice device, RBLService leService) {
        Log.v("BLEX", "Device found callback");
        mDevice = device;
        mBluetoothLeService = leService;
        startConnection();
        registerGattReceiver();
    }

    @Override
    public void scanError(String message) {
        Log.e("BLEX", message);
        finish();
    }

    private void startConnection() {
        if(mDevice == null){
            Log.e("BLEX", "Could not find device.");
            return;
        }

        Intent gattServiceIntent = new Intent(this, RBLService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(RBLService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(RBLService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(RBLService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(RBLService.ACTION_DATA_AVAILABLE);

        return intentFilter;
    }

    private void getGattService(BluetoothGattService gattService){
        Log.v("BLEX", "getGattService: " + gattService.getUuid());

        if(gattService == null){
            return;
        }

        BluetoothGattCharacteristic characteristicTx = gattService.getCharacteristic(RBLService.UUID_BLE_SHIELD_TX);
        map.put(characteristicTx.getUuid(), characteristicTx);

        BluetoothGattCharacteristic characteristicRx = gattService.getCharacteristic(RBLService.UUID_BLE_SHIELD_RX);
        mBluetoothLeService.setCharacteristicNotification(characteristicRx, true);
        mBluetoothLeService.readCharacteristic(characteristicRx);
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((RBLService.LocalBinder) service).getService();
            if(!mBluetoothLeService.initialize()){
                Log.v("BLEX", "Unable to init Bluetooth");
                finish();
                return;
            }

            Log.v("BLEX", "Connecting...");
            mBluetoothLeService.connect(mDevice.getAddress());
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private void sendNotification(final String message){
        new Thread(){
            public void run() {
                Log.v("BLEX", "Sending Notification: " + message);
                String notificationMsg = "N" + message + "\n";

                int messageCount = notificationMsg.length() / MAX_MESSAGE_LEN + 1;
                for(int messageIdx = 0; messageIdx < messageCount; messageIdx++){
                    int startIdx = messageIdx * MAX_MESSAGE_LEN;
                    int endIdx = Math.min((messageIdx + 1) * MAX_MESSAGE_LEN, notificationMsg.length());
                    String messagePart = notificationMsg.substring(startIdx, endIdx);
                    Log.v("BLEX", messagePart);

                    byte[] tmp = messagePart.getBytes();
//                    byte[] tx = new byte[tmp.length + 1];
//                    tx[0] = 0x00;
//                    for(int i = 1; i < tmp.length + 1; i++){
//                        tx[i] = tmp[i - 1];
//                    }
                    BluetoothGattCharacteristic characteristic = map.get(RBLService.UUID_BLE_SHIELD_TX);
                    characteristic.setValue(tmp);
                    mBluetoothLeService.writeCharacteristic(characteristic);

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }
}
