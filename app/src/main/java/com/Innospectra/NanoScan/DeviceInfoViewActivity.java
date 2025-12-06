package com.Innospectra.NanoScan;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ISCSDK.ISCNIRScanSDK;
import com.Innospectra.ISCScanNano.R;

import static com.Innospectra.NanoScan.ScanViewActivity.isExtendVer;
import static com.Innospectra.NanoScan.ScanViewActivity.isExtendVer_PLUS;

// 设备信息界面Activity：控制Nano连接后的设备信息视图
// 当Activity创建时，会向ISCNIRScanSDK发送广播以开始获取设备信息

public class DeviceInfoViewActivity extends Activity {

    private static Context mContext;
    private TextView tv_manuf;
    private TextView tv_model;
    private TextView tv_serial;
    private TextView tv_hw;
    private TextView tv_tiva;
    private BroadcastReceiver DeviceInfoReceiver;
    private final BroadcastReceiver DisconnReceiver = new DisconnReceiver();
    private final IntentFilter disconnFilter = new IntentFilter(ISCNIRScanSDK.ACTION_GATT_DISCONNECTED);
    private static Boolean GotoOtherPage = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_info);
        mContext = this;
        // 设置ActionBar标题并启用返回按钮
        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setTitle(getString(R.string.device_information));
        }

        tv_manuf = (TextView) findViewById(R.id.tv_manuf);
        tv_model = (TextView) findViewById(R.id.tv_model);
        tv_serial = (TextView) findViewById(R.id.tv_serial);
        tv_hw = (TextView) findViewById(R.id.tv_hw);
        tv_tiva = (TextView) findViewById(R.id.tv_tiva);

        // 获取设备信息
        ISCNIRScanSDK.GetDeviceInfo();
        // 初始化设备信息广播接收器
        // 所有设备信息在一个广播中发送
        // 一旦接收到信息，使进度条不可见（需要调用ISCNIRScanSDK.GetDeviceInfo()）
        DeviceInfoReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                tv_manuf.setText(intent.getStringExtra(ISCNIRScanSDK.EXTRA_MANUF_NAME).replace("\n", ""));
                tv_model.setText(intent.getStringExtra(ISCNIRScanSDK.EXTRA_MODEL_NUM).replace("\n", ""));
                String SerialNumber = intent.getStringExtra(ISCNIRScanSDK.EXTRA_SERIAL_NUM);
                if((isExtendVer||isExtendVer_PLUS)&& SerialNumber.length()>8)
                    SerialNumber = SerialNumber.substring(0,8);
                else if(!isExtendVer_PLUS &&!isExtendVer && SerialNumber.length()>7)
                    SerialNumber = SerialNumber.substring(0,7);
                tv_serial.setText(SerialNumber);
                tv_hw.setText(intent.getStringExtra(ISCNIRScanSDK.EXTRA_HW_REV));
                tv_tiva.setText(intent.getStringExtra(ISCNIRScanSDK.EXTRA_TIVA_REV));

                ProgressBar pb = (ProgressBar) findViewById(R.id.pb_info);
                pb.setVisibility(View.INVISIBLE);
            }
        };
        // 注册广播接收器
        LocalBroadcastManager.getInstance(mContext).registerReceiver(DeviceInfoReceiver, new IntentFilter(ISCNIRScanSDK.ACTION_INFO));
        LocalBroadcastManager.getInstance(mContext).registerReceiver(DisconnReceiver, disconnFilter);
    }

    // Activity恢复时调用父类方法
    @Override
    public void onResume() {
        super.onResume();
        GotoOtherPage = false;
    }

    // Activity销毁时，注销处理断开连接事件和设备信息的BroadcastReceiver
    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(DeviceInfoReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(DisconnReceiver);
    }
    @Override
    public void onPause() {
        super.onPause();
        if(!GotoOtherPage)
        {
            Intent notifybackground = new Intent(ScanViewActivity.NOTIFY_BACKGROUND);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(notifybackground);
            Intent notifybackground2 = new Intent(ConfigureViewActivity.NOTIFY_BACKGROUND);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(notifybackground2);
            finish();
        }
    }
    @Override
    public void onBackPressed() {
        GotoOtherPage = true;
        super.onBackPressed();
    }
    // 创建选项菜单（此Activity没有菜单，只有返回按钮，始终返回true）
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    // 处理菜单项选择（此Activity只有返回按钮，选择后应结束Activity）
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            GotoOtherPage = true;
            this.finish();
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Broadcast Receiver handling the disconnect event. If the Nano disconnects,
     * this activity should finish so that the user is taken back to the previous activity.
     * A toast message should appear so that the user knows why the activity is finishing.
     */
    public class DisconnReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(mContext, R.string.nano_disconnected, Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
