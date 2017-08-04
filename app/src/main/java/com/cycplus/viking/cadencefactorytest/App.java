package com.cycplus.viking.cadencefactorytest;

import android.app.Application;
import android.os.Handler;

/**
 * Created by viking on 20/07/2017.
 */

public class App extends Application {
    private static App shared;
    private Handler handler=new Handler();
    public Handler getMainHandler(){return handler;}
    @Override
    public void onCreate() {
        super.onCreate();
        shared=this;
        BluetoothCenter.getInstance().setup();
    }

    public static App sharedApp(){
        return shared;
    }
}
