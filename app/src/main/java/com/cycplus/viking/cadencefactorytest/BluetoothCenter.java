package com.cycplus.viking.cadencefactorytest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Bluetooth center
 * Created by viking on 29/07/2017.
 */

public class BluetoothCenter {

    private static BluetoothCenter ourInstance = new BluetoothCenter();

    private Handler handler=new Handler();

    public static BluetoothCenter getInstance() {
        return ourInstance;
    }

    public BluetoothManager btManager;
    public BluetoothAdapter btAdapter;
    private HashMap<String,CadencePeripheral> peripheralMap;
//    private HashMap<String,Integer> rssiMap;
    private WeakReference<CadencePeripheral> isConnecting;
//    public List<BluetoothDevice> scanList;
    private CadencePeripheral connectedPeripheral;
//    private Handler mHandler;

    private BluetoothCenter() {

    }

    public List<CadencePeripheral>scanedList(){
        ArrayList<CadencePeripheral> result=new ArrayList<>();
        for (CadencePeripheral tail : peripheralMap.values()){
            if (tail.isAvailable()){
                result.add(tail);
            }
        }
        return result;
    }

    public CadencePeripheral getConnectedPeripheral() {
        return connectedPeripheral;
    }

    public void setConnectedPeripheral(CadencePeripheral connectedPeripheral) {
        this.connectedPeripheral = connectedPeripheral;
        if (connectedPeripheral==null){
            scanDevices();
        }else {
            if (isConnecting!=null&&isConnecting.get().bleDevice.getAddress().equalsIgnoreCase(connectedPeripheral.bleDevice.getAddress())){
                isConnecting=null;
            }
            stopScan();
        }
    }

    public void setup() {
        btManager=(BluetoothManager)App.sharedApp().getSystemService(Context.BLUETOOTH_SERVICE);
        if (btManager==null){
            System.out.println("No Bluetooth manager at all");
        }

        btAdapter=btManager.getAdapter();
        if (btAdapter==null){
            btAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        peripheralMap= new HashMap<>();
//        isConnecting=new ArrayList<>();
//        scanList=new ArrayList<>();
//        rssiMap=new HashMap<>();
        if (btAdapter == null) {
            System.out.println("No BLE at all");
        }
//        mHandler = new Handler();
    }


//    public int getRssiForDevice(BluetoothDevice device){
//        String address=device.getAddress();
//        if (rssiMap.containsKey(address)){
//            return rssiMap.get(address);
//        }else{
//            return -1;
//        }
//    }

    /**
     * 搜索设备
     */
    public void scanDevices() {
        if (btAdapter != null && btAdapter.isEnabled()) {
            peripheralMap.clear();
//            UUID[] uuidList=new UUID[1];
            btAdapter.startLeScan(BluetoothFilterAttributes.scanFilter,scanCallback);
            handler.postDelayed(selfCheck,3000);
        }
    }

    /**
     * 停止搜索
     */
    public void stopScan() {
        if (btAdapter != null && btAdapter.isEnabled()) {
            btAdapter.stopLeScan(scanCallback);
        }
        handler.removeCallbacks(selfCheck);
    }


    public void cancenConnection(final CadencePeripheral peripheral){
        if (peripheral!=null)
            handler.post(new Runnable() {
                @Override
                public void run() {
                    peripheral.disconnect();
                }
            });
    }

    /**
     * 连接
     *
     * @param tail 尾灯蓝牙实例
     */
    public void connectXuanTail(final BluetoothDevice tail) {
        if (isConnecting!=null&&isConnecting.get()!=null){
            return;
        }
        CadencePeripheral peripheral;
        if (peripheralMap.containsKey(tail.getAddress())){
            peripheral=(CadencePeripheral) peripheralMap.get(tail.getAddress());
        }else {
            peripheral=new CadencePeripheral(tail);
        }
        isConnecting=new WeakReference<CadencePeripheral>(peripheral);
        peripheral.state=1;
        tail.connectGatt(App.sharedApp(), false, peripheral.gattCallback);
    }
    // methods below are private

    private final BluetoothAdapter.LeScanCallback scanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice bluetoothDevice,final int rssi, byte[] bytes) {
            handler.post(new Runnable() {
                @Override
                public void run() {
//                    Log.d("Bluetooth scanned",bluetoothDevice.getAddress()+" rssi "+rssi);
                    if (rssi<-65){
                        return;
                    }
                    if (!peripheralMap.containsKey(bluetoothDevice.getAddress())){
                        CadencePeripheral peripheral=new CadencePeripheral(bluetoothDevice);
                        peripheral.setRssi(rssi);
                        peripheralMap.put(bluetoothDevice.getAddress(),peripheral);
                        EventBus.getDefault().post(CenterEvent.newEvent());
                    }else{
                        CadencePeripheral peripheral=peripheralMap.get(bluetoothDevice.getAddress());
                        peripheral.setRssi(rssi);
                        EventBus.getDefault().post(CenterEvent.rssiEvent(peripheral));
                    }
                }
            });
        }
    };

    public void reset() {
        stopScan();
        peripheralMap.clear();
        isConnecting=null;
        connectedPeripheral=null;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                scanDevices();
            }
        },1000);
    }

    Runnable selfCheck=new Runnable() {
        @Override
        public void run() {
            ArrayList<CadencePeripheral> delete=new ArrayList<>();
            for (CadencePeripheral tail : peripheralMap.values()){
                if (!tail.isAvailable()){
                    delete.add(tail);
                }
            }
            if (delete.size()>0){
                for (CadencePeripheral tail:delete){
                    peripheralMap.remove(tail.bleDevice.getAddress());
                }
                EventBus.getDefault().post(CenterEvent.newEvent());
            }
            handler.postDelayed(selfCheck,3000);
        }
    };

}
