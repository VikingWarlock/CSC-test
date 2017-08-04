package com.cycplus.viking.cadencefactorytest;

/**
 * Created by viking on 29/07/2017.
 */

public class DataUpdatedEvent {
    String msg;
    public DataUpdatedEvent(CadencePeripheral cadence){
        msg=cadence.getLatestedData();
    }

}
