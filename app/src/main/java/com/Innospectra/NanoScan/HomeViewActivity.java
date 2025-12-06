package com.Innospectra.NanoScan;

import static com.ISCSDK.ISCNIRScanSDK.getStringPref;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Switch;

import com.ISCSDK.ISCNIRScanSDK;
import com.Innospectra.ISCScanNano.R;

/**
 * Created by iris.lin on 2018/2/2.
 */

public class HomeViewActivity  extends Activity {
    private static Context mContext;
    private ImageButton main_connect;
    // private ImageButton main_info;
    private ImageButton main_setting;
    private Switch switch_Warmup;
    private static final int REQUEST_WRITE_STORAGE = 112;
    AlertDialog alertDialog;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_page);
        mContext = this;
        initComponent();

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            boolean hasPermission = (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);
            boolean hasPermission1 = (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
            if(!hasPermission || !hasPermission1)
                DialogPane_LocationPermission();
            else
            {
                boolean hasPermission2 = (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
                if (!hasPermission2) {
                    ActivityCompat.requestPermissions(HomeViewActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.INTERNET
                            },
                            REQUEST_WRITE_STORAGE);
                }
            }
        }
        else if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        {
            boolean hasPermission = (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
            if (!hasPermission) {
                ActivityCompat.requestPermissions(HomeViewActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.INTERNET,
                        },
                        REQUEST_WRITE_STORAGE);
            }
        }
    }
    private void CheckPermission()
    {
        boolean hasPermission1;
        boolean hasPermission2;
        boolean hasPermission3;
        boolean hasPermission4;
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            hasPermission1 = (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
            hasPermission2 = (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
            hasPermission3 = (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);
            hasPermission4 = (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);

            if(!hasPermission1 || !hasPermission2 || !hasPermission3 || !hasPermission4)
            {
               Dialog_Pane("Warning","Will go to the application information page.\nRequiring permission for location and storage.");
            }
            else
            {
                String DeviceName = getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.preferredDevice, null);
                if(DeviceName == null || TextUtils.isEmpty(DeviceName))
                    Dialog_Pane_GoToSettingPage("Warning","The device has not been selected yet, it will automatically go to the settings page.");
                else
                {
                    Intent newscanhIntent = new Intent(mContext, ScanViewActivity.class);
                    newscanhIntent.putExtra("main","main");
                    newscanhIntent.putExtra("warmup",switch_Warmup.isChecked());
                    startActivity(newscanhIntent);
                }
            }
        }
        else if( android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        {
            hasPermission1 = (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED);
            hasPermission2 = (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED);
            if(!hasPermission1 || !hasPermission2)
            {
                Dialog_Pane("Warning","Will go to the application information page.\nShould allow nearby devices permission.");
            }
            else
            {
                String DeviceName = getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.preferredDevice, null);
                if(DeviceName == null || TextUtils.isEmpty(DeviceName))
                    Dialog_Pane_GoToSettingPage("Warning","The device has not been selected yet, it will automatically go to the settings page.");
                else
                {
                    Intent newscanhIntent = new Intent(mContext, ScanViewActivity.class);
                    newscanhIntent.putExtra("main","main");
                    newscanhIntent.putExtra("warmup",switch_Warmup.isChecked());
                    startActivity(newscanhIntent);
                }
            }
        }
    }
    private void initComponent()
    {
        main_connect = (ImageButton)findViewById(R.id.main_connect);
        // main_info = (ImageButton)findViewById(R.id.main_info);
        main_setting = (ImageButton)findViewById(R.id.main_setting);
        switch_Warmup = (Switch)findViewById(R.id.switch_Warmup);

        main_connect.setOnClickListener(main_connect_listenser);
        // main_info.setOnClickListener(main_info_listenser);
        main_setting.setOnClickListener(main_setting_listenser);
        switch_Warmup.setOnClickListener(switch_Warmup_listenser);
    }
    private Switch.OnClickListener switch_Warmup_listenser = new Button.OnClickListener()
    {
        @Override
        public void onClick(View view) {
            if(switch_Warmup.isChecked())
                switch_Warmup.setText("Connect with Lamp-On");
            else
                switch_Warmup.setText("Connect with Lamp-Off");
        }
    };
    private Button.OnClickListener main_connect_listenser = new Button.OnClickListener()
    {

        @Override
        public void onClick(View view) {
            CheckPermission();
        }
    };
// 主界面按钮进入公司介绍页，删除
//    private Button.OnClickListener main_info_listenser = new Button.OnClickListener()
//    {
//
//        @Override
//        public void onClick(View view) {
//            Intent infoIntent = new Intent(mContext, InformationViewActivity.class);
//            startActivity(infoIntent);
//        }
//    };

    private Button.OnClickListener main_setting_listenser = new Button.OnClickListener()
    {

        @Override
        public void onClick(View view) {
            Intent settingsIntent = new Intent(mContext, SettingsViewActivity.class);
            startActivity(settingsIntent);
        }
    };

    public static StoreCalibration storeCalibration = new StoreCalibration();
    public static class StoreCalibration
    {
        String device;
        byte[] storrefCoeff;
        byte[] storerefMatrix;
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
                    localIntent.setData(Uri.fromParts("package", HomeViewActivity.this.getPackageName(), null));
                } else if (Build.VERSION.SDK_INT <= 8) {
                    localIntent.setAction(Intent.ACTION_VIEW);
                    localIntent.setClassName("com.android.settings", "com.android.settings.InstalledAppDetails");
                    localIntent.putExtra("com.android.settings.ApplicationPkgName", HomeViewActivity.this.getPackageName());
                }
                startActivity(localIntent);
                alertDialog.dismiss();
            }
        });
        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
    private void Dialog_Pane_GoToSettingPage(String title,String content)
    {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        alertDialogBuilder.setTitle(title);
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setMessage(content);
        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                alertDialog.dismiss();
                Intent settingsIntent = new Intent(mContext, SettingsViewActivity.class);
                startActivity(settingsIntent);
            }
        });
        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
    private void DialogPane_AllowPermission(String Title, String Content) {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setTitle(Title);
        alertDialogBuilder.setMessage(Content);
        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                alertDialog.dismiss();
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
            }
        });

        alertDialog = alertDialogBuilder.create();
        alertDialog.show();

    }
    private void DialogPane_LocationPermission()
    {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        alertDialogBuilder.setTitle("Permission");
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setMessage("ISC NIRScan App collect location data to enable BLE scan device normally even when the app is closed or not in use.");

        alertDialogBuilder.setPositiveButton("Accept", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                boolean hasPermission = (ContextCompat.checkSelfPermission(HomeViewActivity.this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);
                boolean hasPermission1 = (ContextCompat.checkSelfPermission(HomeViewActivity.this,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
                if (!hasPermission || !hasPermission1) {
                    ActivityCompat.requestPermissions(HomeViewActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.INTERNET, Manifest.permission.ACCESS_COARSE_LOCATION
                                    , Manifest.permission.ACCESS_FINE_LOCATION
                            },
                            REQUEST_WRITE_STORAGE);
                }
                alertDialog.dismiss();
            }
        });
        alertDialogBuilder.setNegativeButton("Deny", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                boolean hasPermission = (ContextCompat.checkSelfPermission(HomeViewActivity.this,
                        Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
                if (!hasPermission) {
                    ActivityCompat.requestPermissions(HomeViewActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.INTERNET
                            },
                            REQUEST_WRITE_STORAGE);
                }
                alertDialog.dismiss();
            }
        });
        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
}
