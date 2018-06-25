package com.cycplus.viking.cadencefactorytest;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.text.DecimalFormat;
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

    private BluetoothGattCharacteristic softwareCharacteristic;
    private BluetoothGattCharacteristic hardwareCharacteristic;
    private BluetoothGattCharacteristic sleepCharacteristic;



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

    private String soft_version;
    private String hard_version;

    private float delta_cadence;
    private float delta_speed;
    private int sameCount = 0;

    public CadencePeripheral(BluetoothDevice device) {
        bleDevice = device;
        timestamp = System.currentTimeMillis();
    }

    public void disconnect() {
        if (gatt != null) {
            gatt.disconnect();
            gatt = null;
            notifyCharacteristic = null;
            state = -1;
        }
    }


    public boolean isAvailable() {
        return state == 1 || state == 2 || state == 3 || (System.currentTimeMillis() - timestamp < 5000);
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
                    new_speed_round = ((data[4] & 0xff) << 24) + ((data[3] & 0xff) << 16) + ((data[2] & 0xff) << 8) + (data[1] & 0xff);
                    new_speed_time = ((data[6] & 0xff) << 8) + (data[5] & 0xff);
                    new_cadence_round = ((data[8] & 0xff) << 8) + (data[7] & 0xff);
                    new_cadence_time = ((data[10] & 0xff) << 8) + (data[9] & 0xff);
                }
                break;
            }
            case 0x01: {
                if (data.length >= 7) {
                    new_speed_round = ((data[4] & 0xff) << 24) + ((data[3] & 0xff) << 16) + ((data[2] & 0xff) << 8) + (data[1] & 0xff);
                    new_speed_time = ((data[6] & 0xff) << 8) + (data[5] & 0xff);
                }
                break;
            }
            case 0x02: {
                if (data.length >= 5) {
                    new_cadence_round = ((data[2] & 0xff) << 8) + (data[1] & 0xff);
                    new_cadence_time = ((data[4] & 0xff) << 8) + (data[3] & 0xff);
                }
                break;
            }
            default: {
                break;
            }
        }
        if (new_cadence_round == cadence_round && new_cadence_time == cadence_time && new_speed_round == speed_round && new_speed_time == speed_time) {
            Log.e("BLE", "Same data");
            delta_cadence = 0;
            delta_speed = 0;
            sameCount++;
            if (sameCount == 3) {
                sameCount = 0;
                DataUpdatedEvent event = new DataUpdatedEvent(this, 1);
                EventBus.getDefault().post(event);
            }
        } else {
            dataChanged = true;
            if (cadence_round != 0)
                delta_cadence = ((new_cadence_round - cadence_round) / 1.f / ((new_cadence_time - cadence_time) / 1024.f)) * 60.f;
            if (speed_round != 0)
                delta_speed = ((new_speed_round - speed_round) / 1.f / ((new_speed_time - speed_time) / 1024.f)) * 2.077f * 3.6f;
            cadence_time = new_cadence_time;
            cadence_round = new_cadence_round;
            speed_time = new_speed_time;
            speed_round = new_speed_round;
            DataUpdatedEvent event = new DataUpdatedEvent(this, 0);
            EventBus.getDefault().post(event);

        }
    }


    public String getLatestedData() {
        String res;
        DecimalFormat a = new DecimalFormat("###.##");
        switch (mode) {
            case 0x03: {
                res = String.format(App.sharedApp().getResources().getString(R.string.mode3), speed_round, speed_time / 1024.f, cadence_round, cadence_time / 1024.f);
//                res =  "轮圈 " + speed_round + " 时间 " + speed_time/1024.f + " ; 曲柄" + cadence_round + " 时间 " + cadence_time/1024.f + "\n";
                res = res + "   --->  (" + a.format(delta_speed) + "," + a.format(delta_cadence) + ")\n";
                break;
            }
            case 0x01: {
                res = String.format(App.sharedApp().getResources().getString(R.string.mode1), speed_round, speed_time / 1024.f);
//                res = "轮圈 " + speed_round + " 时间 " + speed_time/1024.f + "\n";
                res = res + "   --->  (" + a.format(delta_speed) + ")速度\n";
                break;
            }
            case 0x02: {
                res = String.format(App.sharedApp().getResources().getString(R.string.mode2), cadence_round, cadence_time / 1024.f);
//                res = "曲柄" + cadence_round + " 时间 " + cadence_time/1024.f + "\n";
                res = res + "   --->  (" + a.format(delta_cadence) + ")踏频\n";
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

            if (newState == STATE_CONNECTED) {
                if (status == GATT_SUCCESS) {
                    //connection has been established
                    CadencePeripheral.this.state = 2;
                    Log.d("Bluetooth", "Connection has been established");
                    MSGEvent mevent = new MSGEvent(App.sharedApp().getString(R.string.connected));
                    EventBus.getDefault().post(mevent);

                    CadencePeripheral.this.gatt = gatt;
                    BluetoothCenter.getInstance().setConnectedPeripheral(CadencePeripheral.this);
                    if (notifyCharacteristic == null) {
                        gatt.discoverServices();
                    }
                }else {
                    BluetoothCenter.getInstance().setConnectedPeripheral(null);
                }
            } else if (newState == STATE_DISCONNECTED) {
                //connection has been lost
                notifyCharacteristic = null;
                notifyService = null;
                gatt.close();
                CadencePeripheral.this.state = 0;
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

                BluetoothGattService deviceService=gatt.getService(UUID.fromString(BluetoothFilterAttributes.kDeviceInfoServer));
                for (BluetoothGattCharacteristic ch:deviceService.getCharacteristics()){
                    if (ch.getUuid().toString().equalsIgnoreCase(BluetoothFilterAttributes.kDeviceSoftInfoCharacteristic)){
                        softwareCharacteristic=ch;
                        continue;
                    }
                    if (ch.getUuid().toString().equalsIgnoreCase(BluetoothFilterAttributes.kDeviceHardInfoCharacteristic)){
                        hardwareCharacteristic=ch;
                        continue;
                    }
                }

                BluetoothGattService sleepService=gatt.getService(UUID.fromString(BluetoothFilterAttributes.kDeviceSleepService));
                if (sleepService!=null){
                    for (BluetoothGattCharacteristic ch:sleepService.getCharacteristics()){
                        if (ch.getUuid().toString().equalsIgnoreCase(BluetoothFilterAttributes.kDeviceSleepCharactierstic)){
                            sleepCharacteristic=ch;
                            break;
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
                    if (hardwareCharacteristic!=null&&softwareCharacteristic!=null){
                        App.sharedApp().getMainHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                CadencePeripheral.this.gatt.readCharacteristic(hardwareCharacteristic);
                            }
                        });
                    }
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
            Log.e("BLE", "did read value");
            if (characteristic == notifyCharacteristic) {
//                Log.e("rx", Arrays.toString(characteristic.getValue()));
                handleRX(notifyCharacteristic.getValue());
            }else if (characteristic==hardwareCharacteristic){
                hard_version=new String(hardwareCharacteristic.getValue());
                version_updated();
                if (gatt!=null&&softwareCharacteristic!=null){
                    gatt.readCharacteristic(softwareCharacteristic);
                }
            }else if (characteristic==softwareCharacteristic){
                soft_version=new String(softwareCharacteristic.getValue());
                version_updated();
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

    void version_updated(){
        String msg="";
        if (hard_version!=null){
            msg=msg+"HW:"+hard_version+"\n";
        }
        if (soft_version!=null){
            msg=msg+"SW:"+soft_version+"\n";
        }
        MSGEvent event=new MSGEvent(msg);
        EventBus.getDefault().post(event);
        CenterEvent event1=CenterEvent.rssiEvent(this);
        EventBus.getDefault().post(event1);
    }

    public String getSoft_version() {
        if (soft_version!=null) return soft_version;
        return "???";
    }

    public String getHard_version() {
        if (hard_version!=null) return hard_version;
        return "???";
    }

    public boolean isLegacy(){
        if (state>=2){
            if (hardwareCharacteristic==null||softwareCharacteristic==null){
                return true;
            }
        }
        return false;
    }

    public void sleep(){

    }

}
