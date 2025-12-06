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
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ToggleButton;

import com.ISCSDK.ISCNIRScanSDK;
import com.Innospectra.ISCScanNano.R;

import static com.ISCSDK.ISCNIRScanSDK.getBooleanPref;
import static com.ISCSDK.ISCNIRScanSDK.storeBooleanPref;
import static com.Innospectra.NanoScan.ScanViewActivity.isExtendVer;
import static com.Innospectra.NanoScan.ScanViewActivity.isExtendVer_PLUS;

// 配置界面Activity：控制Nano连接后的设置视图
// 提供四个选项，每个选项都会启动一个新的Activity
// 由于每个选项都需要Nano连接才能执行GATT操作
public class ConfigureViewActivity extends Activity {

    private static Context mContext;
    private final BroadcastReceiver DisconnReceiver = new DisconnReceiver();
    private final BroadcastReceiver BackgroundReciver = new BackGroundReciver();
    private final IntentFilter disconnFilter = new IntentFilter(ISCNIRScanSDK.ACTION_GATT_DISCONNECTED);
    public static final String NOTIFY_BACKGROUND = "com.Innospectra.NanoScan.Configuration.notifybackground";
    private LinearLayout ll_device_info;
    private LinearLayout ll_device_status;
    private LinearLayout ll_scan_config;
    private LinearLayout ll_lock_button;
    private ToggleButton toggle_btn_lock_button;
    private View view_lock_button;
    private static Boolean GotoOtherPage = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuration);
        mContext = this;
        // 设置ActionBar标题并启用返回按钮
        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setTitle(getString(R.string.configure));
        }
        InitComponent();
        // 注册断开连接广播接收器
        LocalBroadcastManager.getInstance(mContext).registerReceiver(DisconnReceiver, disconnFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(BackgroundReciver, new IntentFilter(NOTIFY_BACKGROUND));
    }
    private void InitComponent()
    {
        ll_device_info = (LinearLayout)findViewById(R.id.ll_device_info);
        ll_device_status = (LinearLayout)findViewById(R.id.ll_device_status);
        ll_scan_config = (LinearLayout)findViewById(R.id.ll_scan_config);
        ll_lock_button = (LinearLayout)findViewById(R.id.ll_lock_button);
        toggle_btn_lock_button = (ToggleButton)findViewById(R.id.btn_lock_button);
        view_lock_button = (View)findViewById(R.id.view_lock_button);

        ll_device_info.setOnClickListener(View_Click);
        ll_device_status.setOnClickListener(View_Click);
        ll_scan_config.setOnClickListener(View_Click);
        toggle_btn_lock_button.setOnCheckedChangeListener(Toggle_Button_OnCheckedChanged);

        if((!isExtendVer_PLUS && !isExtendVer && ScanViewActivity.fw_level_standard.compareTo(ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_1)<=0 )
                || ISCNIRScanSDK.getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.Activacatestatus, "").contains("Activated") ==false|| ScanViewActivity.isOldTiva)
        {
            ll_lock_button.setVisibility(View.GONE);
            view_lock_button.setVisibility(View.GONE);
        }
        else
        {
            Boolean isLockScan = getBooleanPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.LockButton,false);
            toggle_btn_lock_button.setChecked(isLockScan);
        }
    }
    private LinearLayout.OnClickListener View_Click = new LinearLayout.OnClickListener()
    {
        @Override
        public void onClick(View v) {
            int id =v.getId();
            if (id == R.id.ll_device_info) {
                GotoOtherPage = true;
                Intent infoIntent = new Intent(mContext, DeviceInfoViewActivity.class);
                startActivity(infoIntent);
            }
            else if (id == R.id.ll_device_status) {
                GotoOtherPage = true;
                Intent statusIntent = new Intent(mContext, DeviceStatusViewActivity.class);
                startActivity(statusIntent);
            }
            else if(id == R.id.ll_scan_config){
                    GotoOtherPage = true;
                    Intent confIntent = new Intent(mContext, ScanConfigurationsViewActivity.class);
                    startActivity(confIntent);
            }
        }
    };
    ToggleButton.OnCheckedChangeListener Toggle_Button_OnCheckedChanged = new ToggleButton.OnCheckedChangeListener()
    {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            storeBooleanPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.LockButton,toggle_btn_lock_button.isChecked());
            SetDeviceButtonStatus(toggle_btn_lock_button.isChecked());
        }
    };
    // Activity恢复时调用父类方法
    @Override
    public void onResume() {
        super.onResume();
        GotoOtherPage = false;
    }
    // Activity销毁时，注销处理断开连接事件的BroadcastReceiver
    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(DisconnReceiver);
    }
    @Override
    public void onPause() {
        super.onPause();
        if(!GotoOtherPage)
        {
            Intent notifybackground = new Intent(ScanViewActivity.NOTIFY_BACKGROUND);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(notifybackground);
            finish();
        }
    }
    @Override
    public void onBackPressed() {
        GotoOtherPage = true;
        super.onBackPressed();
    }
    private class  BackGroundReciver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
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
     * this activity should finish so that the user is taken back to the previous activity
     */
    public class DisconnReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    }

    // 设置设备物理按钮状态
    private void SetDeviceButtonStatus(Boolean isLockButton)
    {
        //User open lock button on Configure page
        if(isLockButton)
        {
            ISCNIRScanSDK.ControlPhysicalButton(ISCNIRScanSDK.PhysicalButton.Lock);
        }
        else
        {
            ISCNIRScanSDK.ControlPhysicalButton(ISCNIRScanSDK.PhysicalButton.Unlock);
        }
    }
}
