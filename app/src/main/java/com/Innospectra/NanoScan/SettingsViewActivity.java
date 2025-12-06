package com.Innospectra.NanoScan;


import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.ISCSDK.ISCNIRScanSDK;
import com.Innospectra.ISCScanNano.R;

import static com.ISCSDK.ISCNIRScanSDK.storeStringPref;

// 简化的设置界面Activity：只保留设备过滤器设置
public class SettingsViewActivity extends Activity {

    // 简化的设置页面：只保留设备过滤器设置
    private EditText et_devicefilter;
    private static Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        mContext = this;
        InitComponent();

        // 设置ActionBar返回按钮
        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }
    }
    private void InitComponent()
    {
        et_devicefilter = (EditText)findViewById(R.id.et_devicefilter);
        String devicename = ISCNIRScanSDK.getStringPref(mContext,ISCNIRScanSDK.SharedPreferencesKeys.DeviceFilter,"NIR");
        et_devicefilter.setText(devicename);
        et_devicefilter.setOnEditorActionListener(Device_Filter_OnEditor);
    }
    private EditText.OnEditorActionListener Device_Filter_OnEditor = new EditText.OnEditorActionListener()
    {
        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                ISCNIRScanSDK.storeStringPref(mContext,ISCNIRScanSDK.SharedPreferencesKeys.DeviceFilter,et_devicefilter.getText().toString());
            }
            return false;
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        // 刷新设备过滤器显示
        String devicename = ISCNIRScanSDK.getStringPref(mContext,ISCNIRScanSDK.SharedPreferencesKeys.DeviceFilter,"NIR");
        et_devicefilter.setText(devicename);
    }

    // Activity销毁时调用父类方法
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    // 创建选项菜单（此Activity没有菜单，只有返回按钮，始终返回true）
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    // 处理菜单项选择（此Activity只有返回按钮和设置按钮）
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == android.R.id.home) {
            this.finish();
        }


        return super.onOptionsItemSelected(item);
    }

}
