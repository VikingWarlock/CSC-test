package com.cycplus.viking.cadencefactorytest;

import java.util.UUID;

/**
 * Created by viking on 20/07/2017.
 */

public class BluetoothFilterAttributes {
    public static String kServer =                       "00001816-0000-1000-8000-00805f9b34fb";

    public static String kADServer =                     "00001816-0000-1000-8000-00805f9b34fb";

    public static String kDeviceInfoServer =             "0000180a-0000-1000-8000-00805f9b34fb";

    public static String kDeviceSoftInfoCharacteristic = "00002a28-0000-1000-8000-00805f9b34fb";

    public static String kDeviceHardInfoCharacteristic = "00002a27-0000-1000-8000-00805f9b34fb";

    public static String kDeviceSleepService=            "6e400001-b5a3-f393-e0a9-e50e24dcca9e";

    public static String kDeviceSleepCharactierstic=     "6e400002-b5a3-f393-e0a9-e50e24dcca9e";

    public static String KCharacteristic =               "00002a5b-0000-1000-8000-00805f9b34fb";


    public static UUID[] scanFilter =
            { UUID.fromString(kADServer) };

}
