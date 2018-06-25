package com.cycplus.viking.cadencefactorytest;

/**
 * Created by viking on 29/07/2017.
 */

public class DataUpdatedEvent {
    String msg;
    public DataUpdatedEvent(CadencePeripheral cadence,int type){
        if (type==0){
            msg=cadence.getLatestedData();
        }else {
            msg=App.sharedApp().getString(R.string.NO_DATA)+"\n";
        }
    }


}
