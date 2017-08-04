package com.cycplus.viking.cadencefactorytest;

/**
 * Created by viking on 20/07/2017.
 */

public class CenterEvent {

    public int cmd;
    public CadencePeripheral obj;

    public static CenterEvent rssiEvent(CadencePeripheral tail){
        CenterEvent e=new CenterEvent();
        e.cmd=1;
        return e;
    }

    public static CenterEvent newEvent(){
        CenterEvent e=new CenterEvent();
        e.cmd=2;
        return e;
    }
}
