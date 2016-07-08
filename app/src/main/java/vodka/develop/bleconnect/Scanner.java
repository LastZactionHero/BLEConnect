package vodka.develop.bleconnect;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by zach on 7/8/16.
 */
public class Scanner {
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mDevice = null;
    private final String DEVICE_NAME = "TXRX";
    private final long SCAN_PERIOD = 3000;
    private RBLService mBluetoothLeService = null;
    private DeviceFoundEvent mCallback = null;

    public Scanner(DeviceFoundEvent callback) {
        mCallback = callback;
    }

    public interface DeviceFoundEvent {
        public void deviceFound(BluetoothDevice device, RBLService leService);
        public void scanError(String message);
    }

    public void findDevice(BluetoothManager manager) {
        mBluetoothAdapter = manager.getAdapter();
        if(mBluetoothAdapter == null){
            mCallback.scanError("BLE not supported");
            return;
        }

        if(!mBluetoothAdapter.isEnabled()) {
            mCallback.scanError("Bluetooth not enabled");
            return;
        }

        scanLeDevice();
    }

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

                mCallback.deviceFound(mDevice, mBluetoothLeService);
            }
        }.start();
    }

}
