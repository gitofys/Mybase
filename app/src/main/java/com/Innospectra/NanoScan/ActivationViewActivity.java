package com.Innospectra.NanoScan;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.ISCSDK.ISCNIRScanSDK;
import com.Innospectra.ISCScanNano.R;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.ISCSDK.ISCNIRScanSDK.getStringPref;
import static com.ISCSDK.ISCNIRScanSDK.storeStringPref;

// 激活界面Activity：用于输入许可证密钥以激活设备的高级功能

public class  ActivationViewActivity extends Activity {
    private static Context mContext;
    Button btn_submit;
    Button btn_clear;
    Button btn_unactivate;
    TextView et_status;
    TextView et_license_key;
    private AlertDialog alertDialog;
    private final BroadcastReceiver RetrunActivateStatusReceiver = new RetrunActivateStatusReceiver();
    private final IntentFilter RetrunActivateStatusFilter = new IntentFilter(ISCNIRScanSDK.ACTION_RETURN_ACTIVATE);
    private static Boolean GotoOtherPage = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.license_key);
        mContext = this;

        // 设置ActionBar标题并启用返回按钮
        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }
        initComponent();
        LocalBroadcastManager.getInstance(mContext).registerReceiver(RetrunActivateStatusReceiver, RetrunActivateStatusFilter);
    }
    private void initComponent()
    {
        btn_clear = (Button)findViewById(R.id.btn_clear);
        btn_submit = (Button)findViewById(R.id.btn_submit);
        btn_unactivate = (Button)findViewById(R.id.btn_unactivate);
        et_license_key = (TextView)findViewById(R.id.et_license_key);
        et_status = (TextView)findViewById(R.id.et_status);
        btn_submit.setOnClickListener(ButtonListenser);
        btn_clear.setOnClickListener(ButtonListenser);
        btn_unactivate.setOnClickListener(ButtonListenser);

        String licensekey = getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.licensekey, "");
        if(licensekey!=null)
        {
           et_license_key.setText(licensekey);
        }
        String avticavateStatus =  getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.Activacatestatus, "Function is locked.");
        et_status.setText(avticavateStatus);
    }
    private Button.OnClickListener ButtonListenser = new Button.OnClickListener()
    {
        @Override
        public void onClick(View view) {
            int id = view.getId();
            if (id == R.id.btn_submit) {
                Boolean checklength = checkLicenseKeyLength();
                if (!checklength) {
                    Dialog_Pane("Error", "License key length is not correct.");
                } else {
                    String filterdata = filterDate(et_license_key.getText().toString());
                    byte data[] = hexToBytes(filterdata);
                    ISCNIRScanSDK.SetLicenseKey(data);
                }
            }
            else if (id == R.id.btn_clear) {
                et_license_key.setText("");
            }
            else if (id== R.id.btn_unactivate){
                    Lock_Device_Dialog_Pane("Warning","Do you want to lock the device?");
            }
        }
    };
    private Boolean checkLicenseKeyLength()
    {
        String filterdata = filterDate(et_license_key.getText().toString());
        if(filterdata.length()!=24)
        {
            return false;
        }
        return true;
    }

    // 发送ACTION_ACTIVATE_STATE广播，通过RetrunActivateStatusReceiver获取设备的激活状态（需要调用ISCNIRScanSDK.SetLicenseKey(data)）
    public class RetrunActivateStatusReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            byte state[] = intent.getByteArrayExtra(ISCNIRScanSDK.RETURN_ACTIVATE_STATUS);
            if(state[0] == 1)
            {
                Submit_Dialog_Pane("","Set activation key is completed.");
                et_status.setText("Activated");
                storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.licensekey, et_license_key.getText().toString());
                storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.Activacatestatus, "Activated.");
            }
            else
            {
                Submit_Dialog_Pane("","The device is locked.");
                et_status.setText("Function is locked.");
                storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.Activacatestatus, "Function is locked.");
            }
        }
    }
    // Activity恢复时调用父类方法
    @Override
    public void onResume() {
        super.onResume();
        GotoOtherPage = false;
    }

    // Activity销毁时，注销处理激活状态的BroadcastReceiver
    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(RetrunActivateStatusReceiver);
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

    // 过滤掉所有非数字、/和-字符
    public static String filterDate(String Str) {
        String filter = "[^0-9^A-Z^a-z]"; // Specify the characters to be filtered
        Pattern p = Pattern.compile(filter);
        Matcher m = p.matcher(Str);
        return m.replaceAll("").trim(); // Replace all characters other than those set above
    }
    public static byte[] hexToBytes(String hexString) {

        char[] hex = hexString.toCharArray();
        //change to raw data, the length should divided by 2
        int length = hex.length / 2;
        byte[] rawData = new byte[length];
        for (int i = 0; i < length; i++) {
            //Convert hex data to decimal value
            int high = Character.digit(hex[i * 2], 16);
            int low = Character.digit(hex[i * 2 + 1], 16);
            //Shift the binary value of the first value by 4 bits to the left,ex: 00001000 => 10000000 (8=>128)
            //Then concatenate with the binary value of the second value ex: 10000000 | 00001100 => 10001100 (137)
            int value = (high << 4) | low;
            // 与FFFFFFFF进行补码运算
            if (value > 127)
                value -= 256;
            //Finally change to byte
            rawData [i] = (byte) value;
        }
        return rawData ;
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
                alertDialog.dismiss();
            }
        });
        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
    private void Submit_Dialog_Pane(String title,String content)
    {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        alertDialogBuilder.setTitle(title);
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setMessage(content);

        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                alertDialog.dismiss();
            }
        });
        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
    private void Lock_Device_Dialog_Pane(String title,String content)
    {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        alertDialogBuilder.setTitle(title);
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setMessage(content);
        alertDialogBuilder.setNegativeButton(getResources().getString(R.string.no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                alertDialog.dismiss();
            }
        });
        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                et_license_key.setText("");
                storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.licensekey,"");
                String filterdata = "000000000000000000000000";
                byte data[] = hexToBytes(filterdata);
                //Lock device
                ISCNIRScanSDK.SetLicenseKey(data);
                alertDialog.dismiss();
            }
        });
        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
}

