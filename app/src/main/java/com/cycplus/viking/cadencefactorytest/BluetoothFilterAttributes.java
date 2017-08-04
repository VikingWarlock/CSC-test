package com.cycplus.viking.cadencefactorytest;

import java.util.UUID;

/**
 * Created by viking on 20/07/2017.
 */

public class BluetoothFilterAttributes {
    public static String kServer = "00001816-0000-1000-8000-00805f9b34fb";

    public static String kADServer = "00001816-0000-1000-8000-00805f9b34fb";

    public static String KCharacteristic = "00002a5b-0000-1000-8000-00805f9b34fb";


    public static UUID[] scanFilter =
            { UUID.fromString(kADServer) };

}
