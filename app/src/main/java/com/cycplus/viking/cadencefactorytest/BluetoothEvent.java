package com.cycplus.viking.cadencefactorytest;

import android.bluetooth.BluetoothDevice;

/**
 * Created by viking on 20/07/2017.
 */

public class BluetoothEvent {

    public static String BLUETOOTH_CONNECTED_PERIPHERAL="com.viking.bluetooth.connected";
    public static String BLUETOOTH_DISCONNECTED_PERIPHERAL="com.viking.bluetooth.disconnected";
    //    public static String BLUETOOTH_FOUND_SERVICE="com.viking.bluetooth.service.found";
//    public static String BLUETOOTH_FOUND_CHARACTERISTIC="com.viking.bluetooth.characteristic.found";
    public static String BLUETOOTH_DATA_SENT="com.viking.bluetooth.sent";
//    public static String BLUETOOTH_TAIL_FOUND="com.viking.bluetooth.tail.found";

    public String MacAddress;
    public String DeviceName;
    public BluetoothDevice device;
    public String identifier;

}
