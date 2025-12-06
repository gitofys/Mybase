package com.Innospectra.NanoScan;


import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.ISCSDK.ISCNIRScanSDK;
import com.Innospectra.ISCScanNano.R;

import static com.ISCSDK.ISCNIRScanSDK.getStringPref;
import static com.ISCSDK.ISCNIRScanSDK.storeStringPref;

// 设置界面Activity：控制全局设置视图，这些设置不需要Nano设备连接
// 用户可以更改温度和空间频率单位，以及设置和清除首选Nano设备
public class SettingsViewActivity extends Activity {

    // 版本号显示（已移除）
    private Button btn_set;
    private Button btn_forget;
    private AlertDialog alertDialog;
    private TextView tv_pref_nano;
    private EditText et_devicefilter;
    private String preferredNano;
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
        // tv_version = (TextView) findViewById(R.id.tv_version);
        btn_set = (Button) findViewById(R.id.btn_set);
        btn_forget = (Button) findViewById(R.id.btn_forget);
        tv_pref_nano = (TextView) findViewById(R.id.tv_pref_nano);
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
        // 初始化首选设备
        preferredNano = getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.preferredDevice, null);
        // 获取包信息以显示版本信息（已移除，不再使用）
//        try {
//            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
//            String version = pInfo.versionName;
//            int versionCode = pInfo.versionCode;
//            .setText(getString(R.string.version, version, versionCode));
//        } catch (PackageManager.NameNotFoundException e) {
//            .setText("");
//        }

        btn_set.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(CheckPermission())
                    startActivity(new Intent(mContext, SelectDeviceViewActivity.class));
            }
        });
        tv_pref_nano.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(CheckPermission())
                    startActivity(new Intent(mContext, SelectDeviceViewActivity.class));
            }
        });

        if(preferredNano == null){
            btn_forget.setEnabled(false);
        }else{
            btn_forget.setEnabled(true);
        }
        btn_forget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (preferredNano != null) {
                    confirmationDialog(preferredNano);
                }
            }
        });
        // 根据是否已设置首选设备来更新设置按钮和字段
        if (preferredNano != null) {
            btn_set.setVisibility(View.INVISIBLE);
            tv_pref_nano.setText(getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.preferredDeviceModel, null));
            tv_pref_nano.setVisibility(View.VISIBLE);
        } else {
            btn_set.setVisibility(View.VISIBLE);
            tv_pref_nano.setVisibility(View.INVISIBLE);
        }
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

        else if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // 显示确认清除已存储Nano设备的对话框
    // mac: 已存储Nano设备的MAC地址
    public void confirmationDialog(String mac) {

        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        alertDialogBuilder.setTitle(mContext.getResources().getString(R.string.nano_confirmation_title));
        alertDialogBuilder.setMessage(mContext.getResources().getString(R.string.nano_forget_msg, mac));

        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                alertDialog.dismiss();
                storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.preferredDevice, null);
                storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.preferredDeviceModel, null);
                btn_set.setVisibility(View.VISIBLE);
                tv_pref_nano.setVisibility(View.INVISIBLE);
                btn_forget.setEnabled(false);
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
    private Boolean CheckPermission()
    {
        if( android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        {
            Boolean hasPermission1 = (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED);
            Boolean hasPermission2 = (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED);
            if(!hasPermission1 || !hasPermission2)
            {
                Dialog_Pane("Warning","Will go to the application information page.\nShould allow nearby devices permission.");
                return false;
            }
        }
        return true;
    }
    private void Dialog_Pane(String title,String content)
    {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        alertDialogBuilder.setTitle(title);
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setMessage(content);

        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                Intent localIntent = new Intent();
                localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (Build.VERSION.SDK_INT >= 9) {
                    localIntent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
                    localIntent.setData(Uri.fromParts("package", SettingsViewActivity.this.getPackageName(), null));
                } else if (Build.VERSION.SDK_INT <= 8) {
                    localIntent.setAction(Intent.ACTION_VIEW);
                    localIntent.setClassName("com.android.settings", "com.android.settings.InstalledAppDetails");
                    localIntent.putExtra("com.android.settings.ApplicationPkgName", SettingsViewActivity.this.getPackageName());
                }
                startActivity(localIntent);
                alertDialog.dismiss();
            }
        });
        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
}
