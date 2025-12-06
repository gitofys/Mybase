package com.Innospectra.NanoScan;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ISCSDK.ISCNIRScanSDK;
import com.Innospectra.ISCScanNano.R;

import java.util.ArrayList;

import static com.ISCSDK.ISCNIRScanSDK.storeStringPref;

// 设备选择界面Activity：通过BLE扫描广播的Nano设备
// 允许用户指定一个首选Nano设备供将来使用
// 在有多个Nano设备的环境中，将首先连接首选Nano设备
// 选择设备ID的列表，后期考虑自动识别
public class SelectDeviceViewActivity extends Activity {

    private Handler mHandler;
    private BluetoothAdapter mBluetoothAdapter;
    public BluetoothLeScanner mBluetoothLeScanner;
    private static String DEVICE_NAME = "NIR";
    private ArrayList<ISCNIRScanSDK.NanoDevice> nanoDeviceList = new ArrayList<>();
    private NanoScanAdapter nanoScanAdapter;
    private static Context mContext;
    private AlertDialog alertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        mContext = this;
        DEVICE_NAME = ISCNIRScanSDK.getStringPref(mContext,ISCNIRScanSDK.SharedPreferencesKeys.DeviceFilter,"NIR");

        // 设置ActionBar标题并启用返回按钮
        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setTitle(getString(R.string.select_nano));
            ab.setDisplayHomeAsUpEnabled(true);
        }
        final BluetoothManager bluetoothManager =
               (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        ListView lv_nanoDevices = (ListView) findViewById(R.id.lv_nanoDevices);
        // 为BLE扫描返回的NanoDevice对象创建适配器
        nanoScanAdapter = new NanoScanAdapter(this, nanoDeviceList);
        lv_nanoDevices.setAdapter(nanoScanAdapter);
        lv_nanoDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                confirmationDialog(nanoDeviceList.get(i).getNanoMac(),nanoDeviceList.get(i).getNanoName());
            }
        });

        mHandler = new Handler();
        scanLeDevice(true);
    }

    // 向用户显示对话框，询问是否确定要将指定MAC地址的Nano设为首选设备
    // mac: Nano设备的MAC地址
    public void confirmationDialog(String mac, final String name) {

        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        final String deviceMac = mac;
        alertDialogBuilder.setTitle(mContext.getResources().getString(R.string.nano_confirmation_title));
        alertDialogBuilder.setMessage(mContext.getResources().getString(R.string.nano_confirmation_msg, mac));

        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                alertDialog.dismiss();
                storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.preferredDevice, deviceMac);
                storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.preferredDeviceModel,name);
                finish();
            }
        });

        alertDialogBuilder.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                alertDialog.dismiss();
            }
        });

        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    // 蓝牙扫描回调函数：提供找到的BluetoothDevice实例、RSSI和广播数据（scanRecord）
    // 当找到广告名称匹配DEVICE_NAME的蓝牙设备时，会调用连接设备
    // 即使ISCNIRScanSDK.SCAN_PERIOD尚未到期，蓝牙也应停止扫描
    private ScanCallback mLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            BluetoothDevice device = result.getDevice();
            @SuppressLint("MissingPermission") String name = device.getName();
            if (name != null && name.contains(DEVICE_NAME) && result.getScanRecord() != null) {
                Boolean isDeviceInList = false;
                ISCNIRScanSDK.NanoDevice nanoDevice = new ISCNIRScanSDK.NanoDevice(device, result.getRssi(), result.getScanRecord().getBytes());
                for (ISCNIRScanSDK.NanoDevice d : nanoDeviceList) {
                    if (d.getNanoMac().equals(device.getAddress())) {
                        isDeviceInList = true;
                        d.setRssi(result.getRssi());
                        nanoScanAdapter.notifyDataSetChanged();
                    }
                }
                if (!isDeviceInList) {
                    nanoDeviceList.add(nanoDevice);
                    nanoScanAdapter.notifyDataSetChanged();
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            System.out.println("BLE// onScanFailed");
            Log.e("Scan Failed", "Error Code: " + errorCode);
        }

    };
    // 处理菜单项选择（此Activity只有返回按钮，选择后应结束Activity）
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            this.finish();
        }

        return super.onOptionsItemSelected(item);
    }

    // 在指定间隔ISCNIRScanSDK.SCAN_PERIOD扫描蓝牙设备
    // 此函数使用Handler延迟调用停止扫描，直到间隔到期
    // enable: 告诉BluetoothAdapter是否应该开始或停止扫描
    @SuppressLint("MissingPermission")
    private void scanLeDevice(final boolean enable) {
        if(mBluetoothLeScanner == null){
            Toast.makeText(SelectDeviceViewActivity.this, "Could not open LE scanner", Toast.LENGTH_SHORT).show();
        }else {
            if (enable) {
                // 在预定的扫描周期后停止扫描
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mBluetoothLeScanner.stopScan(mLeScanCallback);
                    }
                }, ISCNIRScanSDK.SCAN_PERIOD);
                mBluetoothLeScanner.startScan(mLeScanCallback);
            } else {
                mBluetoothLeScanner.stopScan(mLeScanCallback);
            }
        }
    }

    // 自定义适配器，用于在ListView中保存ISCNIRScanSDK.NanoDevice对象
    // 此适配器包含设备名称、MAC地址和RSSI
    private class NanoScanAdapter extends ArrayAdapter<ISCNIRScanSDK.NanoDevice> {
        private final ArrayList<ISCNIRScanSDK.NanoDevice> nanoDevices;
        public NanoScanAdapter(Context context, ArrayList<ISCNIRScanSDK.NanoDevice> values) {
            super(context, -1, values);
            this.nanoDevices = values;
        }
        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = LayoutInflater.from(this.getContext())
                        .inflate(R.layout.row_nano_scan_item, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.nanoName = (TextView) convertView.findViewById(R.id.tv_nano_name);
                viewHolder.nanoMac = (TextView) convertView.findViewById(R.id.tv_nano_mac);
                viewHolder.nanoRssi = (TextView) convertView.findViewById(R.id.tv_rssi);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            final ISCNIRScanSDK.NanoDevice device = getItem(position);
            if (device != null) {
                viewHolder.nanoName.setText(device.getNanoName());
                viewHolder.nanoMac.setText(device.getNanoMac());
                viewHolder.nanoRssi.setText(device.getRssiString());
            }
            return convertView;
        }
    }

    // ISCNIRScanSDK.NanoDevice对象的视图持有者
    private class ViewHolder {
        private TextView nanoName;
        private TextView nanoMac;
        private TextView nanoRssi;
    }
}
