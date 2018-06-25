package com.cycplus.viking.cadencefactorytest;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.scan_list_view)
    ListView scanResult;

    @BindView(R.id.console)
    TextView console;

    List<CadencePeripheral> scanlist;
    private scanAdapter adapter;
    private Handler mHandler = new Handler();
    private boolean refreshFlag = false;
    long lastUpdate;


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode==34){
            if (grantResults[0]==PackageManager.PERMISSION_GRANTED){
                BluetoothCenter.getInstance().scanDevices();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        scanlist = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int i = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
            if (i == PackageManager.PERMISSION_GRANTED) {
                BluetoothCenter.getInstance().scanDevices();
            }else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 34);
            }
        }else {
            BluetoothCenter.getInstance().scanDevices();
        }


        adapter = new scanAdapter(this);
        scanResult.setAdapter(adapter);
        mHandler.post(runnable);
//        mHandler.postDelayed(reloadListRunnable,6000);


        scanResult.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (i < 1) {
                    return;
                } else {
                    CadencePeripheral p = scanlist.get(i - 1);
                    if (p.isAvailable()) {
                        BluetoothCenter.getInstance().connectXuanTail(p.bleDevice);
                        refreshList();
                    }
                }
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (refreshFlag == true) {
                refreshList();
            }
            mHandler.postDelayed(runnable, 1000);
        }
    };

    private void refreshList() {
        if (scanlist.size()>1){
            Collections.sort(scanlist,new Comparator<CadencePeripheral>() {
                @Override
                public int compare(CadencePeripheral cadencePeripheral, CadencePeripheral t1) {
                    if (t1.getRssi()>=cadencePeripheral.getRssi()||t1.state>1)
                        return 1;
                    else
                        return -1;
                }
            });
        }
        adapter.notifyDataSetChanged();
        refreshFlag = false;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void handler(Object event) {
        if (event instanceof BluetoothEvent) {
            BluetoothEvent e = (BluetoothEvent) event;
            if (e.identifier.equals(BluetoothEvent.BLUETOOTH_CONNECTED_PERIPHERAL)) {
                refreshList();
            } else {
                console.setText("");
            }
        } else if (event instanceof CenterEvent) {
            CenterEvent e = (CenterEvent) event;
            if (e.cmd == 2) {
                scanlist = BluetoothCenter.getInstance().scanedList();
                lastUpdate = System.currentTimeMillis();
                refreshList();
            } else if (e.cmd == 1) {
                refreshFlag = true;
            }
        } else if (event instanceof MSGEvent) {
            MSGEvent me = (MSGEvent) event;
            console.setText(me.msg + "\n" + console.getText());
        } else if (event instanceof DataUpdatedEvent) {
            DataUpdatedEvent e = (DataUpdatedEvent) event;
            console.setText(e.msg + console.getText());
        }
    }


    @OnClick(R.id.finish)
    public void end_test() {
        BluetoothCenter.getInstance().cancenConnection(BluetoothCenter.getInstance().getConnectedPeripheral());

    }

    @OnClick(R.id.reset)
    public void reset(){
        BluetoothCenter.getInstance().cancenConnection(BluetoothCenter.getInstance().getConnectedPeripheral());
        BluetoothCenter.getInstance().reset();

    }


    private class scanAdapter extends BaseAdapter {

        private LayoutInflater layoutInflater;

        public scanAdapter(Context context) {
            this.layoutInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return scanlist.size() + 1;
        }

        @Override
        public Object getItem(int i) {
            return scanlist.get(i);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ScanCellView cell = null;
            if (view == null) {
                cell = new ScanCellView();
                view = layoutInflater.inflate(R.layout.scan_item, null);
                cell.mark = (ImageView) view.findViewById(R.id.scan_mark);
                cell.name = (TextView) view.findViewById(R.id.scan_name);
                cell.rssi = (TextView) view.findViewById(R.id.scan_rssi);
                view.setTag(cell);
            } else {
                cell = (ScanCellView) view.getTag();
            }
            if (i > 0) {
                CadencePeripheral device = (CadencePeripheral) this.getItem(i - 1);
                String name = device.bleDevice.getName();
                if (name == null) {
                    name = "Cycplus Cadence";
                }
                if (device.state == 2) {
                    cell.rssi.setText(R.string.connect);
                    cell.rssi.setTextColor(Color.GREEN);
                    if (device.isLegacy()){
                        name=name+" 老版本";
                    }else {
                        name=name+" "+device.getHard_version()+"("+device.getSoft_version()+")";
                    }
                } else if (device.state == 1) {
                    cell.rssi.setText(R.string.connecting);
                    cell.rssi.setTextColor(Color.YELLOW);
                } else {
                    cell.rssi.setText(getString(R.string.signal_strength, device.getRssi()));
                    cell.rssi.setTextColor(Color.BLACK);
                }
                cell.name.setText(name);

                cell.mark.setVisibility(View.VISIBLE);
            } else {
                cell.name.setText(R.string.scanning);
                cell.mark.setVisibility(View.INVISIBLE);
                cell.rssi.setText("");
            }
            return view;
        }
    }

}
