package com.cycplus.viking.cadencefactorytest;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothGatt.GATT_FAILURE;
import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTED;

/**
 * Created by viking on 29/07/2017.
 */

public class CadencePeripheral {

    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic notifyCharacteristic;
    private BluetoothGattService notifyService;
    public BluetoothDevice bleDevice;
    public int state = 0;
    private int rssi;
    private long timestamp;
    private byte mode = 0;
    private long speed_round = 0;
    private long speed_time = 0;
    private int cadence_round = 0;
    private long cadence_time = 0;
    private boolean dataChanged = false;

    public CadencePeripheral(BluetoothDevice device) {
        bleDevice = device;
        timestamp = System.currentTimeMillis();
    }

    public void disconnect() {
        if (gatt != null) {
            gatt.disconnect();
            gatt = null;
            notifyCharacteristic = null;
            state=-1;
        }
    }


    public boolean isAvailable() {
        return state==1||state == 2 || state == 3 || (System.currentTimeMillis() - timestamp < 5000);
    }


    public void setRssi(int r) {
        rssi = r;
    }

    public int getRssi() {
        return rssi;
    }


    private void handleRX(byte[] data) {
        if (data == null) {
            return;
        }
        byte flag = data[0];
        mode = flag;
        long new_speed_round = speed_round;
        long new_speed_time = speed_time;
        int new_cadence_round = cadence_round;
        long new_cadence_time = cadence_time;

        switch (flag) {
            case 0x03: {
                if (data.length >= 11) {
                    new_speed_round = ((data[4]&0xff) << 24) + ((data[3]&0xff) << 16) + ((data[2]&0xff) << 8) + (data[1]&0xff);
                    new_speed_time = ((data[6]&0xff) << 8) + (data[5]&0xff);
                    new_cadence_round = ((data[8]&0xff) << 8) + (data[7]&0xff);
                    new_cadence_time = ((data[10]&0xff) << 8) + (data[9]&0xff);
                }
                break;
            }
            case 0x01: {
                if (data.length >= 7) {
                    new_speed_round = ((data[4]&0xff) << 24) + ((data[3]&0xff) << 16) + ((data[2]&0xff) << 8) + (data[1]&0xff);
                    new_speed_time = ((data[6]&0xff) << 8) + (data[5]&0xff);
                }
                break;
            }
            case 0x02: {
                if (data.length >= 5) {
                    new_cadence_round = ((data[2]&0xff) << 8) + (data[1]&0xff);
                    new_cadence_time = ((data[4]&0xff) << 8) + (data[3]&0xff);
                }
                break;
            }
            default: {
                break;
            }
        }
        if (new_cadence_round == cadence_round && new_cadence_time == cadence_time && new_speed_round == speed_round && new_speed_time == speed_time) {
            Log.e("BLE","Same data");
        }else {
            dataChanged = true;
            cadence_time = new_cadence_time;
            cadence_round = new_cadence_round;
            speed_time = new_speed_time;
            speed_round = new_speed_round;
            DataUpdatedEvent event = new DataUpdatedEvent(this);
            EventBus.getDefault().post(event);

        }
    }


    public String getLatestedData() {
        String res;
        switch (mode) {
            case 0x03: {
                res=String.format(App.sharedApp().getResources().getString(R.string.mode3),speed_round,speed_time/1024.f,cadence_round,cadence_time/1024.f)+"\n";
//                res =  "轮圈 " + speed_round + " 时间 " + speed_time/1024.f + " ; 曲柄" + cadence_round + " 时间 " + cadence_time/1024.f + "\n";
                break;
            }
            case 0x01: {
                res=String.format(App.sharedApp().getResources().getString(R.string.mode1),speed_round,speed_time/1024.f)+"\n";
//                res = "轮圈 " + speed_round + " 时间 " + speed_time/1024.f + "\n";
                break;
            }
            case 0x02: {
                res=String.format(App.sharedApp().getResources().getString(R.string.mode2),cadence_round,cadence_time/1024.f)+"\n";
//                res = "曲柄" + cadence_round + " 时间 " + cadence_time/1024.f + "\n";
                break;
            }
            default: {
                res = "error\n";
                break;
            }
        }
        return res;
    }


    public BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (status == GATT_FAILURE) {
                BluetoothCenter.getInstance().setConnectedPeripheral(null);
            }

            if (newState == STATE_CONNECTED) {
                if (status == GATT_SUCCESS) {
                    //connection has been established
                    CadencePeripheral.this.state=2;
                    Log.d("Bluetooth", "Connection has been established");
                    MSGEvent mevent = new MSGEvent(App.sharedApp().getString(R.string.connected));
                    EventBus.getDefault().post(mevent);

                    CadencePeripheral.this.gatt = gatt;
                    BluetoothCenter.getInstance().setConnectedPeripheral(CadencePeripheral.this);
                    if (notifyCharacteristic == null) {
                        gatt.discoverServices();
                    }
                }
            } else if (newState == STATE_DISCONNECTED) {
                //connection has been lost
                notifyCharacteristic = null;
                notifyService = null;
                gatt.close();
                CadencePeripheral.this.state=0;
                CadencePeripheral.this.gatt = null;
                BluetoothCenter.getInstance().setConnectedPeripheral(null);

                BluetoothEvent event = new BluetoothEvent();
                event.identifier = BluetoothEvent.BLUETOOTH_DISCONNECTED_PERIPHERAL;
                event.device = CadencePeripheral.this.bleDevice;
                event.DeviceName = event.device.getName();
                event.MacAddress = event.device.getAddress();
                EventBus.getDefault().post(event);
                Log.d("Bluetooth", "Connection has been lost");
                MSGEvent mevent = new MSGEvent(App.sharedApp().getString(R.string.disconnected));
                EventBus.getDefault().post(mevent);
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == GATT_SUCCESS) {
                notifyService = gatt.getService(UUID.fromString(BluetoothFilterAttributes.kServer));
                for (BluetoothGattCharacteristic characteristic : notifyService.getCharacteristics()) {
                    Log.e("SERVICE", characteristic.getUuid().toString());
                    if (characteristic.getUuid().toString().equalsIgnoreCase(BluetoothFilterAttributes.KCharacteristic)) {
                        notifyCharacteristic = characteristic;
                        boolean listening = gatt.setCharacteristicNotification(notifyCharacteristic, true);
                        List<BluetoothGattDescriptor> descriptorList = notifyCharacteristic.getDescriptors();
                        if (descriptorList != null && descriptorList.size() > 0) {
                            for (BluetoothGattDescriptor descriptor : descriptorList) {
                                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                gatt.writeDescriptor(descriptor);
                            }
                        }
                    }
                }
                if (notifyCharacteristic != null) {
                    BluetoothEvent event = new BluetoothEvent();
                    CadencePeripheral.this.state = 2;
                    event.identifier = BluetoothEvent.BLUETOOTH_CONNECTED_PERIPHERAL;
                    event.device = CadencePeripheral.this.bleDevice;
                    event.DeviceName = event.device.getName();
                    event.MacAddress = event.device.getAddress();
                    EventBus.getDefault().post(event);
                    Log.d("Bluetooth", "Characteristic found");
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
//            Log.e("BLE", "did update value");
            if (characteristic == notifyCharacteristic) {
//                Log.e("rx", Arrays.toString(characteristic.getValue()));
                handleRX(notifyCharacteristic.getValue());
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
//            Log.e("BLE", "did update value");
            if (characteristic == notifyCharacteristic) {
//                Log.e("rx", Arrays.toString(characteristic.getValue()));
                handleRX(notifyCharacteristic.getValue());
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            if (status == GATT_SUCCESS) {
                CadencePeripheral.this.rssi = rssi;
            }
        }
    };

}
