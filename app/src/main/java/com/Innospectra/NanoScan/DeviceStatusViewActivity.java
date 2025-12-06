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
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.ISCSDK.ISCNIRScanSDK;
import com.Innospectra.ISCScanNano.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.Innospectra.NanoScan.ScanViewActivity.isExtendVer;
import static com.Innospectra.NanoScan.ScanViewActivity.isExtendVer_PLUS;

// 统一设备状态界面Activity：合并设备信息、设备状态和错误状态
public class DeviceStatusViewActivity extends Activity {
    private static Context mContext;
    // 设备信息
    private TextView tv_manuf;
    private TextView tv_model;
    private TextView tv_serial;
    private TextView tv_hw;
    private TextView tv_tiva;
    // 设备状态
    private TextView tv_batt;
    private TextView tv_temp;
    private TextView tv_humid;
    private TextView tv_lamptime;
    Button btn_update_thresholds;
    private Button btn_device_status;
    private Button btn_error_status;
    private Button btn_clear_error;
    // 列表视图
    private ListView device_status_listview;
    private ListView error_status_listview;
    private LinearLayout ll_error_status_container;
    private BroadcastReceiver mStatusReceiver;
    private BroadcastReceiver DeviceInfoReceiver;
    private final BroadcastReceiver DisconnReceiver = new DisconnReceiver();
    private final BroadcastReceiver BackgroundReciver = new BackGroundReciver();
    private final IntentFilter disconnFilter = new IntentFilter(ISCNIRScanSDK.ACTION_GATT_DISCONNECTED);
    public static final String NOTIFY_BACKGROUND = "com.Innospectra.NanoScan.DeviceStatusViewActivity.notifybackground";
    String devStatus = "";
    byte[] devbyte;
    byte[] errbyte;
    String errorStatus = "";
    private static Boolean GotoOtherPage = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unified_device_status);
        mContext = this;
        // 设置ActionBar标题并启用返回按钮
        ActionBar ab = getActionBar();
        if(ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setTitle(getString(R.string.device_status));
        }
        // 初始化设备信息视图
        tv_manuf = (TextView) findViewById(R.id.tv_manuf);
        tv_model = (TextView) findViewById(R.id.tv_model);
        tv_serial = (TextView) findViewById(R.id.tv_serial);
        tv_hw = (TextView) findViewById(R.id.tv_hw);
        tv_tiva = (TextView) findViewById(R.id.tv_tiva);
        
        // 初始化设备状态视图
        tv_batt = (TextView)findViewById(R.id.tv_batt);
        tv_temp = (TextView)findViewById(R.id.tv_temp);
        tv_humid = (TextView)findViewById(R.id.tv_humid);
        tv_lamptime = (TextView)findViewById(R.id.tv_lamptime);
        btn_update_thresholds = (Button) findViewById(R.id.btn_update_thresholds);
        btn_device_status = (Button)findViewById(R.id.btn_device_status);
        btn_error_status = (Button)findViewById(R.id.btn_error_status);
        btn_clear_error = (Button)findViewById(R.id.btn_clear_error);
        
        // 初始化列表视图
        device_status_listview = (ListView) findViewById(R.id.device_status_listview);
        error_status_listview = (ListView) findViewById(R.id.error_status_listview);
        ll_error_status_container = (LinearLayout) findViewById(R.id.ll_error_status_container);

        btn_update_thresholds.setOnClickListener(Update_Threshold_Click);
        btn_device_status.setOnClickListener(Device_Status_Click);
        btn_error_status.setOnClickListener(Error_Status_Click);
        btn_clear_error.setOnClickListener(Clear_Error_Click);

        // 同时获取设备信息和设备状态
        ISCNIRScanSDK.GetDeviceInfo();
        ISCNIRScanSDK.GetDeviceStatus();
        setActivityTouchDisable(true);
        
        // 初始化设备信息接收器
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
            }
        };
        // 设置设备状态信息接收器（需要调用ISCNIRScanSDK.GetDeviceStatus()）
        mStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int batt = intent.getIntExtra(ISCNIRScanSDK.EXTRA_BATT, 0);
                float temp = intent.getFloatExtra(ISCNIRScanSDK.EXTRA_TEMP, 0);
                float humid = intent.getFloatExtra(ISCNIRScanSDK.EXTRA_HUMID, 0);
                long lamptime = intent.getLongExtra(ISCNIRScanSDK.EXTRA_LAMPTIME,0);
                devStatus = intent.getStringExtra(ISCNIRScanSDK.EXTRA_DEV_STATUS);
                errorStatus = intent.getStringExtra(ISCNIRScanSDK.EXTRA_ERR_STATUS);
                devbyte = intent.getByteArrayExtra(ISCNIRScanSDK.EXTRA_DEV_STATUS_BYTE);
                errbyte = intent.getByteArrayExtra(ISCNIRScanSDK.EXTRA_ERR_BYTE);
                tv_batt.setText(getString(R.string.batt_level_value, batt));
                tv_temp.setText(getString(R.string.temp_value_c, Integer.toString((int) temp)));
                tv_humid.setText(getString(R.string.humid_value,Integer.toString((int) humid)));
                tv_lamptime.setText(GetLampTimeString(lamptime));
                ProgressBar pb = (ProgressBar)findViewById(R.id.pb_status);
                pb.setVisibility(View.INVISIBLE);
                setActivityTouchDisable(false);
            }
        };
        // 注册所有接收器
        LocalBroadcastManager.getInstance(mContext).registerReceiver(DeviceInfoReceiver, new IntentFilter(ISCNIRScanSDK.ACTION_INFO));
        LocalBroadcastManager.getInstance(mContext).registerReceiver(mStatusReceiver, new IntentFilter(ISCNIRScanSDK.ACTION_STATUS));
        LocalBroadcastManager.getInstance(mContext).registerReceiver(DisconnReceiver, disconnFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(BackgroundReciver, new IntentFilter(NOTIFY_BACKGROUND));
    }
    public static String GetLampTimeString(long lamptime)
    {
        String lampusage = "";
        if (lamptime / 86400 != 0)
        {
            lampusage += lamptime / 86400 + "day ";
            lamptime -= 86400 * (lamptime / 86400);
        }
        if (lamptime / 3600 != 0)
        {
            lampusage += lamptime / 3600 + "hr ";
            lamptime -= 3600 * (lamptime / 3600);
        }
        if (lamptime / 60 != 0)
        {
            lampusage += lamptime / 60 + "min ";
            lamptime -= 60 * (lamptime / 60);
        }
        lampusage += lamptime + "sec ";
        return lampusage;
    }
    // 按钮事件处理
    private Button.OnClickListener Update_Threshold_Click = new Button.OnClickListener()
    {
        @Override
        public void onClick(View view) {
            ProgressBar pb = (ProgressBar)findViewById(R.id.pb_status);
            pb.setVisibility(View.VISIBLE);
            setActivityTouchDisable(true);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(ISCNIRScanSDK.GET_STATUS));
        }
    };
    private Button.OnClickListener Device_Status_Click = new Button.OnClickListener()
    {
        @Override
        public void onClick(View view) {
            // 切换设备状态详情列表的显示/隐藏
            if (device_status_listview.getVisibility() == View.VISIBLE) {
                device_status_listview.setVisibility(View.GONE);
            } else {
                device_status_listview.setVisibility(View.VISIBLE);
                showDeviceStatusDetail();
            }
        }
    };

    private Button.OnClickListener Error_Status_Click = new Button.OnClickListener()
    {
        @Override
        public void onClick(View view) {
            // 切换错误状态列表的显示/隐藏
            if (ll_error_status_container.getVisibility() == View.VISIBLE) {
                ll_error_status_container.setVisibility(View.GONE);
            } else {
                ll_error_status_container.setVisibility(View.VISIBLE);
                showErrorStatusDetail();
            }
        }
    };
    
    private Button.OnClickListener Clear_Error_Click = new Button.OnClickListener()
    {
        @Override
        public void onClick(View view) {
            ISCNIRScanSDK.ClearDeviceError();
            showErrorStatusDetail(); // 刷新错误状态显示
        }
    };
    
    // 显示设备状态详情
    private void showDeviceStatusDetail() {
        if (devbyte == null || devbyte.length == 0) return;
        
        int data = devbyte[0];
        int tiva = 0x00000001;
        int[] images = { R.drawable.leg_gray, R.drawable.leg_gray, R.drawable.leg_gray,
                R.drawable.leg_gray, R.drawable.leg_gray, R.drawable.leg_gray, R.drawable.leg_gray};

        for(int j=0;j<2;j++)
        {
            int ret = data & tiva;
            if(ret == tiva)
            {
                images[j] = R.drawable.led_g;
            }
            tiva = tiva<<1;
        }
        tiva = tiva<<2;
        for(int j=2;j<6;j++)
        {
            int ret = data & tiva;
            if(ret == tiva)
            {
                images[j] = R.drawable.led_g;
            }
            tiva = tiva<<1;
        }
        data = devbyte[1];
        if(data!=0)
            images[6] = R.drawable.led_g;
        
        String[] title = { "Tiva", "Scanning", "BLE stack", "BLE connection", "Scan Data Interpreting", "Scan Button Pressed", "Battery in charge"};
        images[0] = R.drawable.led_g;
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < images.length; i++) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("images", images[i]);
            map.put("title", title[i]);
            list.add(map);
        }
        SimpleAdapter adapter = new SimpleAdapter(this, list,
                R.layout.advance_device_status_item, new String[] { "images", "title" }, new int[] {
                R.id.image, R.id.text });
        device_status_listview.setAdapter(adapter);
    }
    
    // 显示错误状态详情
    private void showErrorStatusDetail() {
        if (errbyte == null || errbyte.length == 0) return;
        
        int[] images = { R.drawable.leg_gray, R.drawable.leg_gray, R.drawable.leg_gray,
                R.drawable.leg_gray, R.drawable.leg_gray, R.drawable.leg_gray, R.drawable.leg_gray,R.drawable.leg_gray,R.drawable.leg_gray,R.drawable.leg_gray,R.drawable.leg_gray,R.drawable.leg_gray};
        String[] title = { "Scan", "ADC", "EEPROM", "Bluetooth", "Spectrum Library", "Hardware","TMP006" ,"HDC1000","Battery","Memory","UART","System"};
        
        int data = errbyte[0]&0xFF | (errbyte[1] << 8);
        int error_scan = 0x00000001;
        for(int j=0;j<2;j++)
        {
            int ret = data & error_scan;
            if(ret == error_scan)
            {
                images[j] = R.drawable.led_r;
            }
            error_scan = error_scan<<1;
        }
        error_scan = error_scan<<1;
        for(int j=2;j<12;j++)
        {
            int ret = data & error_scan;
            if(ret == error_scan)
            {
                images[j] = R.drawable.led_r;
            }
            error_scan = error_scan<<1;
        }
        
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < images.length; i++) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("images", images[i]);
            map.put("title", title[i]);
            list.add(map);
        }
        SimpleAdapter adapter = new SimpleAdapter(this, list,
                R.layout.activity_error_status_item, new String[] { "images", "title" }, new int[] {
                R.id.image, R.id.error_test });
        error_status_listview.setAdapter(adapter);
    }
    // 按钮事件处理结束
    // Activity恢复时调用父类方法
    @Override
    public void onResume(){
        super.onResume();
        GotoOtherPage = false;
    }

    // Activity销毁时，注销所有BroadcastReceiver
    @Override
    public void onDestroy(){
        super.onDestroy();
        if (DeviceInfoReceiver != null) {
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(DeviceInfoReceiver);
        }
        if (mStatusReceiver != null) {
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mStatusReceiver);
        }
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(DisconnReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(BackgroundReciver);
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
        if(id == android.R.id.home){
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
            Toast.makeText(mContext, R.string.nano_disconnected, Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    private void setActivityTouchDisable(boolean value) {
        if (value) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        }
    }
}
