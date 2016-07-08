package vodka.develop.bleconnect;

import android.Manifest;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements Scanner.DeviceFoundEvent {
    private static final int MY_PERMISSION_ACCESS_LOCATION = 1;
    private Map<UUID, BluetoothGattCharacteristic> map = new HashMap<UUID, BluetoothGattCharacteristic>();
    private Scanner mScanner = null;
    private BluetoothDevice mDevice = null;

    private void sendSomething() {
        // Get Gett service and characteristics... or something
//        connected to device
//        stopped here
    }

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
    public void deviceFound(BluetoothDevice device) {
        mDevice = device;
        Log.v("BLEX", "Device found callback");
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

//        Intent gattServiceIntent = new Intent(this, RBLService.class);
//        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }
}
