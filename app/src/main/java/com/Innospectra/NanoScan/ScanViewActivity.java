package com.Innospectra.NanoScan;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.Manifest;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.ISCSDK.ISCNIRScanSDK;
import com.Innospectra.ISCScanNano.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.opencsv.CSVWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static com.ISCSDK.ISCNIRScanSDK.Interpret_intensity;
import static com.ISCSDK.ISCNIRScanSDK.Interpret_length;
import static com.ISCSDK.ISCNIRScanSDK.Interpret_uncalibratedIntensity;
import static com.ISCSDK.ISCNIRScanSDK.Interpret_wavelength;
import static com.ISCSDK.ISCNIRScanSDK.LAMP_ON_OFF;
import static com.ISCSDK.ISCNIRScanSDK.Reference_Info;
import static com.ISCSDK.ISCNIRScanSDK.Scan_Config_Info;
import static com.ISCSDK.ISCNIRScanSDK.getBooleanPref;
import static com.ISCSDK.ISCNIRScanSDK.getStringPref;
import static com.ISCSDK.ISCNIRScanSDK.storeBooleanPref;
import static com.ISCSDK.ISCNIRScanSDK.storeStringPref;
import static com.Innospectra.NanoScan.DeviceStatusViewActivity.GetLampTimeString;

// 扫描视图Activity：控制设备连接后的扫描操作和数据显示
public class ScanViewActivity extends Activity {


    // 参数定义
    private static Context mContext;
    private ProgressDialog barProgressDialog;
    private ProgressBar calProgress;
    private TextView progressBarinsideText;
    private AlertDialog alertDialog;
    private Menu mMenu;

    private LineChart mMainChart; // 主图表，用于显示模式切换
    private Spinner spinner_display_mode; // 显示模式选择器（反射率/吸光度/强度）
    private Button btn_toggle_config; // 配置显示切换按钮
    private LinearLayout layout_config_collapsible; // 可折叠的配置区域
    private String GraphLabel = "ISC Scan";
    private ArrayList<String> mXValues;
    private ArrayList<Entry> mIntensityFloat;
    private ArrayList<Entry> mAbsorbanceFloat;
    private ArrayList<Entry> mReflectanceFloat;
    private ArrayList<Entry> mReferenceFloat;
    private ArrayList<Float> mWavelengthFloat;
    
    // 显示模式枚举
    public enum DisplayMode {
        INTENSITY(0, "强度"),
        ABSORBANCE(1, "吸光度"),
        REFLECTANCE(2, "反射率");
        
        private final int index;
        private final String label;
        
        DisplayMode(int index, String label) {
            this.index = index;
            this.label = label;
        }
        
        public int getIndex() { return index; }
        public String getLabel() { return label; }
    }
    private DisplayMode currentDisplayMode = DisplayMode.INTENSITY;

    // Tiva版本是否为扩展波长版本
    public static Boolean isExtendVer = false;
    public static Boolean isExtendVer_PLUS = false;
    // 固件级别控制
    public static ISCNIRScanSDK.FW_LEVEL_STANDARD fw_level_standard  = ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_0;
    public static ISCNIRScanSDK.FW_LEVEL_EXT fw_level_ext  = ISCNIRScanSDK.FW_LEVEL_EXT.LEVEL_EXT_1;
    public static ISCNIRScanSDK.FW_LEVEL_EXT_PLUS fw_level_ext_plus  = ISCNIRScanSDK.FW_LEVEL_EXT_PLUS.LEVEL_EXT_PLUS_1;

    public enum ScanMethod {
        Manual  // 仅使用手动模式
    }
    LampInfo Lamp_Info = LampInfo.ManualLamp;
    public enum LampInfo{
        WarmDevice,        ManualLamp,CloseWarmUpLampInScan
    }

    // 广播接收器
    private final BroadcastReceiver ScanDataReadyReceiver = new ScanDataReadyReceiver();
    private final BroadcastReceiver RefDataReadyReceiver = new RefDataReadyReceiver();
    private final BroadcastReceiver NotifyCompleteReceiver = new NotifyCompleteReceiver();
    private final BroadcastReceiver ScanStartedReceiver = new ScanStartedReceiver();
    private final BroadcastReceiver RefCoeffDataProgressReceiver = new RefCoeffDataProgressReceiver();
    private final BroadcastReceiver CalMatrixDataProgressReceiver = new CalMatrixDataProgressReceiver();
    private final BroadcastReceiver DisconnReceiver = new DisconnReceiver();
    private final BroadcastReceiver SpectrumCalCoefficientsReadyReceiver = new SpectrumCalCoefficientsReadyReceiver();
    private final BroadcastReceiver RetrunReadActivateStatusReceiver = new RetrunReadActivateStatusReceiver();
    private final BroadcastReceiver RetrunActivateStatusReceiver = new RetrunActivateStatusReceiver();
    private final BroadcastReceiver ReturnCurrentScanConfigurationDataReceiver = new ReturnCurrentScanConfigurationDataReceiver();
    private final BroadcastReceiver DeviceInfoReceiver = new DeviceInfoReceiver();
    private final BroadcastReceiver GetUUIDReceiver = new GetUUIDReceiver();
    private final BroadcastReceiver GetDeviceStatusReceiver = new GetDeviceStatusReceiver();
    private final BroadcastReceiver ScanConfReceiver = new ScanConfReceiver();
    private final BroadcastReceiver WriteScanConfigStatusReceiver = new WriteScanConfigStatusReceiver();
    private final BroadcastReceiver ScanConfSizeReceiver=  new ScanConfSizeReceiver();
    private final BroadcastReceiver GetActiveScanConfReceiver = new GetActiveScanConfReceiver();
    private final BroadcastReceiver ReturnLampRampUpADCReceiver = new ReturnLampRampUpADCReceiver();
    private final BroadcastReceiver ReturnLampADCAverageReceiver = new ReturnLampADCAverageReceiver();
    private final BroadcastReceiver ReturnLampRampUpADCTimeStampReceiver = new ReturnLampRampUpADCTimeStampReceiver();
    private final BroadcastReceiver ReturnMFGNumReceiver = new ReturnMFGNumReceiver();
    private final BroadcastReceiver ReturnSetLampReceiver = new ReturnSetLampReceiver();
    private final BroadcastReceiver ReturnSetPGAReceiver = new ReturnSetPGAReceiver();
    private final BroadcastReceiver ReturnSetScanRepeatsReceiver = new ReturnSetScanRepeatsReceiver();
    private final BroadcastReceiver ReturnHWModelReceiver = new ReturnHWModelReceiver();
    private final BroadcastReceiver BackgroundReciver = new BackGroundReciver();
    private final BroadcastReceiver GetPGAReceiver = new GetPGAReceiver();

    private final IntentFilter scanDataReadyFilter = new IntentFilter(ISCNIRScanSDK.SCAN_DATA);
    private final IntentFilter refReadyFilter = new IntentFilter(ISCNIRScanSDK.REF_CONF_DATA);
    private final IntentFilter notifyCompleteFilter = new IntentFilter(ISCNIRScanSDK.ACTION_NOTIFY_DONE);
    private final IntentFilter requestCalCoeffFilter = new IntentFilter(ISCNIRScanSDK.ACTION_REQ_CAL_COEFF);
    private final IntentFilter requestCalMatrixFilter = new IntentFilter(ISCNIRScanSDK.ACTION_REQ_CAL_MATRIX);
    private final IntentFilter disconnFilter = new IntentFilter(ISCNIRScanSDK.ACTION_GATT_DISCONNECTED);
    private final IntentFilter scanStartedFilter = new IntentFilter(ISCNIRScanSDK.ACTION_SCAN_STARTED);
    private final IntentFilter SpectrumCalCoefficientsReadyFilter = new IntentFilter(ISCNIRScanSDK.SPEC_CONF_DATA);
    private final IntentFilter RetrunReadActivateStatusFilter = new IntentFilter(ISCNIRScanSDK.ACTION_RETURN_READ_ACTIVATE_STATE);
    private final IntentFilter scanConfFilter = new IntentFilter(ISCNIRScanSDK.SCAN_CONF_DATA);
    private final IntentFilter RetrunActivateStatusFilter = new IntentFilter(ISCNIRScanSDK.ACTION_RETURN_ACTIVATE);
    private final IntentFilter ReturnCurrentScanConfigurationDataFilter = new IntentFilter(ISCNIRScanSDK.RETURN_CURRENT_CONFIG_DATA);
    private final IntentFilter WriteScanConfigStatusFilter = new IntentFilter(ISCNIRScanSDK.ACTION_RETURN_WRITE_SCAN_CONFIG_STATUS);
    private final IntentFilter ReturnLampRampUpFilter = new IntentFilter(ISCNIRScanSDK.ACTION_RETURN_LAMP_RAMPUP_ADC);
    private final IntentFilter ReturnLampADCAverageFilter = new IntentFilter(ISCNIRScanSDK.ACTION_RETURN_LAMP_AVERAGE_ADC);
    private final IntentFilter ReturnLampRampUpADCTimeStampFilter = new IntentFilter(ISCNIRScanSDK.ACTION_RETURN_LAMP_ADC_TIMESTAMP);
    private final IntentFilter ReturnMFGNumFilter = new IntentFilter(ISCNIRScanSDK.ACTION_RETURN_MFGNUM);
    private final IntentFilter ReturnHWModelFilter = new IntentFilter(ISCNIRScanSDK.ACTION_RETURN_HWMODEL);
    public static final String NOTIFY_BACKGROUND = "com.Innospectra.NanoScan.ScanViewActivity.notifybackground";
    private String  NOTIFY_ISEXTVER = "com.Innospectra.NanoScan.ISEXTVER";
    
    // 扫描数据和服务
    private ISCNIRScanSDK.ScanResults Scan_Spectrum_Data;
    private ISCNIRScanSDK mNanoBLEService;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private Handler mHandler;

    // 图表数据数组是否已初始化
    Boolean chartDataInitialized = false;

    // BLE设备名称过滤
    private static String DEVICE_NAME = "NIR";
    // 目标设备常量
    private static final String TARGET_DEVICE_NAME = "NIR-M-R2";
    private static final String TARGET_DEVICE_MAC_PART = "D11R013";
    // 设备连接状态
    private boolean connected;
    // 从设置页面获取的要连接的设备名称
    private String preferredDevice;
    // 设备的活动配置
    private ISCNIRScanSDK.ScanConfiguration activeConf;
    // 设备中接收到的配置数量
    private int receivedConfSize=-1;
    // 设备中配置的数量
    private int storedConfSize;
    // 扫描配置列表详情
    private ArrayList<ISCNIRScanSDK.ScanConfiguration> ScanConfigList = new ArrayList<ISCNIRScanSDK.ScanConfiguration>();
    // 从扫描配置页面获取的扫描配置列表详情
    private ArrayList<ISCNIRScanSDK.ScanConfiguration> ScanConfigList_from_ScanConfiguration = new ArrayList<ISCNIRScanSDK.ScanConfiguration>();
    // 活动配置字节数组
    private byte ActiveConfigByte[];
    // 扫描配置列表字节数组
    private ArrayList <byte []> ScanConfig_Byte_List = new ArrayList<>();
    // 从扫描配置页面获取的扫描配置列表字节数组
    private ArrayList <byte []> ScanConfig_Byte_List_from_ScanConfiuration = new ArrayList<>();
    // 活动配置索引
    int ActiveConfigindex;

    private float minWavelength=900;
    private float maxWavelength=1700;
    private int MINWAV=900;
    private int MAXWAV=1700;
    private float minAbsorbance=0;
    private float maxAbsorbance=2;
    private float minReflectance=-2;
    private float maxReflectance=2;
    private float minIntensity=-7000;
    private float maxIntensity=7000;
    private float minReference=-7000;
    private float maxReference=7000;
    private int numSections=0;

    private Button btn_scan;
    // 手动扫描设置
    private EditText filePrefix;
    private LinearLayout ly_lamp;
    private View view_lamp_onoff;
    private TextView tv_manual_scan_conf;
    private LinearLayout ly_manual_conf;
    private ToggleButton toggle_button_manual_scan_mode;
    private ToggleButton toggle_button_manual_lamp;
    private EditText et_manual_lamptime;
    private EditText et_manual_pga;
    private EditText et_manual_repead;
    private ScanMethod Current_Scan_Method = ScanMethod.Manual;
    
    ArrayAdapter<CharSequence> adapter_width;
    // 是否显示活动配置页面
    public static boolean showActiveconfigpage = false;

    // 从HomeViewActivity->ScanViewActivity触发时检查设备激活状态
    private  String mainflag = "";
    // 检查光谱校准系数是否已接收
    Boolean downloadspecFlag = false;
    // 记录光谱校准系数
    byte[] SpectrumCalCoefficients = new byte[144];
    // 允许AddScanConfigViewActivity获取光谱校准系数以计算最大模式数
    public static byte []passSpectrumCalCoefficients = new byte[144];

    private byte[] refCoeff;
    private byte[] refMatrix;
    // 最大模式数
    int MaxPattern = 0;
    // 是否为参考扫描设置配置
    boolean reference_set_config = false;

    // 是否前往扫描配置页面
    public static boolean GotoScanConfigFlag = false;
    // 暂停事件触发时是否前往其他页面
    private static Boolean GotoOtherPage = false;
    public  static Boolean isOldTiva = false;
    private Boolean WarmUp = false;
    
    // 灯控制变量
    public enum LampMode {
        ON, OFF, AUTO
    }
    private LampMode currentLampMode = LampMode.AUTO;
    private int warmupTimeSeconds = 0; // Warmup time in seconds
    private boolean isWarmupInProgress = false;
    private Handler warmupHandler = new Handler();
    private Runnable warmupCountdownRunnable;
    private TextView tv_connection_status;
    private TextView tv_warmup_countdown;
    private Button btn_lamp_settings;
    private Button btn_connect_device;
    private static final int REQUEST_WRITE_STORAGE = 112;
    private AlertDialog permissionAlertDialog;
    
    // 校准存储（从HomeViewActivity迁移）
    public static StoreCalibration storeCalibration = new StoreCalibration();
    public static class StoreCalibration
    {
        String device;
        byte[] storrefCoeff;
        byte[] storerefMatrix;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_scan);
        mContext = this;
        DEVICE_NAME = ISCNIRScanSDK.getStringPref(mContext,ISCNIRScanSDK.SharedPreferencesKeys.DeviceFilter,"NIR");
        
        // Check permissions first
        checkPermissions();
        
        // Initialize components
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            mainflag = bundle.getString("main");
            WarmUp = bundle.getBoolean("warmup", false);
        } else {
            mainflag = "";
            WarmUp = false;
        }
        storeBooleanPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.LockButton,false);

        calProgress = (ProgressBar) findViewById(R.id.calProgress);
        calProgress.setVisibility(View.VISIBLE);
        progressBarinsideText = (TextView)findViewById(R.id.progressBarinsideText);
        connected = false;
        Disable_Stop_Continous_button();

        // 初始化连接状态和灯控制UI
        tv_connection_status = (TextView) findViewById(R.id.tv_connection_status);
        tv_warmup_countdown = (TextView) findViewById(R.id.tv_warmup_countdown);
        btn_lamp_settings = (Button) findViewById(R.id.btn_lamp_settings);
        btn_connect_device = (Button) findViewById(R.id.btn_connect_device);
        
        if (tv_connection_status != null) {
            updateConnectionStatus("未连接");
        }
        if (btn_connect_device != null) {
            btn_connect_device.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onConnectButtonClick();
                }
            });
        }
        if (btn_lamp_settings != null) {
            btn_lamp_settings.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showLampSettingsMenu();
                }
            });
        }

        filePrefix = (EditText) findViewById(R.id.et_prefix);
        btn_scan = (Button) findViewById(R.id.btn_scan);

        btn_scan.setClickable(false);
        btn_scan.setBackgroundColor(ContextCompat.getColor(mContext, R.color.btn_unavailable));
        btn_scan.setOnClickListener(Button_Scan_Click);
        setActivityTouchDisable(true);

        // 初始化手动模式组件
        InitialManualComponent();
        InitialDisplayModeSelector();
        InitialConfigToggle();
        
        // 设置手动模式为默认且唯一的模式
        Current_Scan_Method = ScanMethod.Manual;

        // 绑定服务，这将启动服务并调用启动命令函数
        Intent gattServiceIntent = new Intent(this, ISCNIRScanSDK.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        
        // 注册所有需要的广播接收器
        LocalBroadcastManager.getInstance(mContext).registerReceiver(ScanDataReadyReceiver, scanDataReadyFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(RefDataReadyReceiver, refReadyFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(NotifyCompleteReceiver, notifyCompleteFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(RefCoeffDataProgressReceiver, requestCalCoeffFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(CalMatrixDataProgressReceiver, requestCalMatrixFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(DisconnReceiver, disconnFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(ScanConfReceiver, scanConfFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(ScanStartedReceiver, scanStartedFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(SpectrumCalCoefficientsReadyReceiver, SpectrumCalCoefficientsReadyFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(RetrunReadActivateStatusReceiver, RetrunReadActivateStatusFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(RetrunActivateStatusReceiver, RetrunActivateStatusFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(ReturnCurrentScanConfigurationDataReceiver, ReturnCurrentScanConfigurationDataFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(DeviceInfoReceiver, new IntentFilter(ISCNIRScanSDK.ACTION_INFO));
        LocalBroadcastManager.getInstance(mContext).registerReceiver(GetUUIDReceiver, new IntentFilter(ISCNIRScanSDK.SEND_DEVICE_UUID));
        LocalBroadcastManager.getInstance(mContext).registerReceiver(ReturnLampRampUpADCReceiver, ReturnLampRampUpFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(ReturnLampADCAverageReceiver, ReturnLampADCAverageFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(ReturnLampRampUpADCTimeStampReceiver, ReturnLampRampUpADCTimeStampFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(ReturnMFGNumReceiver, ReturnMFGNumFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(ReturnHWModelReceiver, ReturnHWModelFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(BackgroundReciver, new IntentFilter(NOTIFY_BACKGROUND));
        LocalBroadcastManager.getInstance(mContext).registerReceiver(ReturnSetLampReceiver, new IntentFilter(ISCNIRScanSDK.SET_LAMPSTATE_COMPLETE));
        LocalBroadcastManager.getInstance(mContext).registerReceiver(ReturnSetPGAReceiver, new IntentFilter(ISCNIRScanSDK.SET_PGA_COMPLETE));
        LocalBroadcastManager.getInstance(mContext).registerReceiver(ReturnSetScanRepeatsReceiver, new IntentFilter(ISCNIRScanSDK.SET_SCANREPEATS_COMPLETE));
        LocalBroadcastManager.getInstance(mContext).registerReceiver(GetPGAReceiver, new IntentFilter(ISCNIRScanSDK.SEND_PGA));
    }
    
    // 权限检查方法
    private void checkPermissions() {
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
                    ActivityCompat.requestPermissions(ScanViewActivity.this,
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
                ActivityCompat.requestPermissions(ScanViewActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.INTERNET,
                        },
                        REQUEST_WRITE_STORAGE);
            }
        }
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
                boolean hasPermission = (ContextCompat.checkSelfPermission(ScanViewActivity.this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);
                boolean hasPermission1 = (ContextCompat.checkSelfPermission(ScanViewActivity.this,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
                if (!hasPermission || !hasPermission1) {
                    ActivityCompat.requestPermissions(ScanViewActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.INTERNET, Manifest.permission.ACCESS_COARSE_LOCATION
                                    , Manifest.permission.ACCESS_FINE_LOCATION
                            },
                            REQUEST_WRITE_STORAGE);
                }
                if (permissionAlertDialog != null) {
                    permissionAlertDialog.dismiss();
                }
            }
        });
        alertDialogBuilder.setNegativeButton("Deny", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                boolean hasPermission = (ContextCompat.checkSelfPermission(ScanViewActivity.this,
                        Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
                if (!hasPermission) {
                    ActivityCompat.requestPermissions(ScanViewActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.INTERNET
                            },
                            REQUEST_WRITE_STORAGE);
                }
                if (permissionAlertDialog != null) {
                    permissionAlertDialog.dismiss();
                }
            }
        });
        permissionAlertDialog = alertDialogBuilder.create();
        permissionAlertDialog.show();
    }
    
    // 扫描设备并连接
    // 管理服务生命周期
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            // 从服务连接获取服务引用
            mNanoBLEService = ((ISCNIRScanSDK.LocalBinder) service).getService();

            // 初始化蓝牙，如果BLE不可用则结束
            if (!mNanoBLEService.initialize()) {
                finish();
            }
            // 开始扫描匹配DEVICE_NAME的设备
            final BluetoothManager bluetoothManager =
                    (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = bluetoothManager.getAdapter();
            mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
            if(mBluetoothLeScanner == null){
                finish();
                Toast.makeText(ScanViewActivity.this, "请确保蓝牙已开启并重试", Toast.LENGTH_SHORT).show();
            }
            mHandler = new Handler();
            // 始终直接扫描目标设备
            updateConnectionStatus("扫描中...");
            scanTargetDevice(true);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mNanoBLEService = null;
        }
    };

    // 蓝牙扫描回调函数
    private final ScanCallback mLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
            String name = device.getName();
            String address = device.getAddress();

            if (name != null && address != null) {
                // 检查设备名称是否包含目标名称，MAC地址是否包含目标MAC部分
                boolean nameMatches = name.contains(TARGET_DEVICE_NAME);
                boolean macMatches = address.toUpperCase().contains(TARGET_DEVICE_MAC_PART.toUpperCase());
                
                if (nameMatches && macMatches) {
                    updateConnectionStatus("连接中...");
                    mNanoBLEService.connect(device.getAddress());
                    preferredDevice = device.getAddress();
                    // 存储首选设备以供将来使用
                    storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.preferredDevice, device.getAddress());
                    storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.preferredDeviceModel, name);
                    connected = true;
                    scanTargetDevice(false);
                }
            }
        }
        
        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            updateConnectionStatus("扫描失败");
            Toast.makeText(ScanViewActivity.this, "扫描失败，错误代码: " + errorCode, Toast.LENGTH_SHORT).show();
        }
    };

    // 专门扫描目标设备
    private void scanTargetDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(mBluetoothLeScanner != null) {
                        mBluetoothLeScanner.stopScan(mLeScanCallback);
                        if (!connected) {
                            updateConnectionStatus("未连接");
                            notConnectedDialog();
                        }
                    }
                }
            }, ISCNIRScanSDK.SCAN_PERIOD);
            if(mBluetoothLeScanner != null) {
                mBluetoothLeScanner.startScan(mLeScanCallback);
            } else {
                finish();
                Toast.makeText(ScanViewActivity.this, "请确保蓝牙已开启并重试", Toast.LENGTH_SHORT).show();
            }
        } else {
            if(mBluetoothLeScanner != null) {
                mBluetoothLeScanner.stopScan(mLeScanCallback);
            }
        }
    }

    // 在指定间隔扫描蓝牙设备
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(mBluetoothLeScanner != null) {
                        mBluetoothLeScanner.stopScan(mLeScanCallback);
                        if (!connected) {
                            notConnectedDialog();
                        }
                    }
                }
            }, ISCNIRScanSDK.SCAN_PERIOD);
            if(mBluetoothLeScanner != null) {
                mBluetoothLeScanner.startScan(mLeScanCallback);
            }else{
                finish();
                Toast.makeText(ScanViewActivity.this, "请确保蓝牙已开启并重试", Toast.LENGTH_SHORT).show();
            }
        } else {
            mBluetoothLeScanner.stopScan(mLeScanCallback);
        }
    }

    // 扫描首选Nano设备
    private void scanPreferredLeDevice(final boolean enable) {
        // 使用scanTargetDevice代替，直接扫描目标设备
        scanTargetDevice(enable);
    }
    
    // 连接设备后
    // 自定义接收器：在所有GATT通知订阅后请求时间
    public class NotifyCompleteReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            connected = true;
            updateConnectionStatus("已连接");
            if (btn_connect_device != null) {
                btn_connect_device.setEnabled(false);
            }
            if(WarmUp)
            {
                ISCNIRScanSDK.ControlLamp(ISCNIRScanSDK.LampState.ON);
                Lamp_Info = LampInfo.WarmDevice;
            }
            else
            {
                Boolean reference = false;
                if(getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.ReferenceScan, "Not").equals("ReferenceScan"))
                {
                    reference = true;
                }
                // 连接后始终下载校准信息
                if(preferredDevice != null && preferredDevice.equals(storeCalibration.device) && reference == false)
                {
                    refCoeff = storeCalibration.storrefCoeff;
                    refMatrix = storeCalibration.storerefMatrix;
                    ArrayList<ISCNIRScanSDK.ReferenceCalibration> refCal = new ArrayList<>();
                    refCal.add(new ISCNIRScanSDK.ReferenceCalibration(refCoeff, refMatrix));
                    ISCNIRScanSDK.ReferenceCalibration.writeRefCalFile(mContext, refCal);
                    calProgress.setVisibility(View.INVISIBLE);
                    barProgressDialog = new ProgressDialog(ScanViewActivity.this);
                     // 获取活动配置
		            ISCNIRScanSDK.ShouldDownloadCoefficient = false;
		            ISCNIRScanSDK.SetCurrentTime();
                }
                else
                {
                    if(reference == true)
                    {
                        storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.ReferenceScan, "Not");
                    }
                    //Synchronize time and download calibration coefficient and calibration matrix
                	ISCNIRScanSDK.ShouldDownloadCoefficient = true;
                	ISCNIRScanSDK.SetCurrentTime();
                }
            }
        }
    }
    // 接收校准系数数据（必须调用ISCNIRScanSDK.SetCurrentTime()）
    public class RefCoeffDataProgressReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            intent.getIntExtra(ISCNIRScanSDK.EXTRA_REF_CAL_COEFF_SIZE, 0);
            Boolean size = intent.getBooleanExtra(ISCNIRScanSDK.EXTRA_REF_CAL_COEFF_SIZE_PACKET, false);
            if (size) {
                calProgress.setVisibility(View.INVISIBLE);
                barProgressDialog = new ProgressDialog(ScanViewActivity.this);
                barProgressDialog.setTitle(getString(R.string.dl_ref_cal));
                barProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                barProgressDialog.setProgress(0);
                barProgressDialog.setMax(intent.getIntExtra(ISCNIRScanSDK.EXTRA_REF_CAL_COEFF_SIZE, 0));
                barProgressDialog.setCancelable(false);
                barProgressDialog.show();
            } else {
                barProgressDialog.setProgress(barProgressDialog.getProgress() + intent.getIntExtra(ISCNIRScanSDK.EXTRA_REF_CAL_COEFF_SIZE, 0));
            }
        }
    }
    // 接收校准矩阵数据，完成后请求活动配置（必须调用ISCNIRScanSDK.SetCurrentTime()）
    public class CalMatrixDataProgressReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            intent.getIntExtra(ISCNIRScanSDK.EXTRA_REF_CAL_MATRIX_SIZE, 0);
            Boolean size = intent.getBooleanExtra(ISCNIRScanSDK.EXTRA_REF_CAL_MATRIX_SIZE_PACKET, false);
            if (size) {
                barProgressDialog.dismiss();
                barProgressDialog = new ProgressDialog(ScanViewActivity.this);
                barProgressDialog.setTitle(getString(R.string.dl_cal_matrix));
                barProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                barProgressDialog.setProgress(0);
                barProgressDialog.setMax(intent.getIntExtra(ISCNIRScanSDK.EXTRA_REF_CAL_MATRIX_SIZE, 0));
                barProgressDialog.setCancelable(false);
                barProgressDialog.show();
            } else {
                barProgressDialog.setProgress(barProgressDialog.getProgress() + intent.getIntExtra(ISCNIRScanSDK.EXTRA_REF_CAL_MATRIX_SIZE, 0));
            }
            if (barProgressDialog.getProgress() == barProgressDialog.getMax()) {
                // 发送GET_ACTIVE_CONF广播以获取活动配置
                ISCNIRScanSDK.GetActiveConfig();
            }
        }
    }
    // 下载参考校准矩阵后通知并保存（必须调用ISCNIRScanSDK.SetCurrentTime()）
    public class RefDataReadyReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            refCoeff = intent.getByteArrayExtra(ISCNIRScanSDK.EXTRA_REF_COEF_DATA);
            refMatrix = intent.getByteArrayExtra(ISCNIRScanSDK.EXTRA_REF_MATRIX_DATA);
            ArrayList<ISCNIRScanSDK.ReferenceCalibration> refCal = new ArrayList<>();
            refCal.add(new ISCNIRScanSDK.ReferenceCalibration(refCoeff, refMatrix));
            ISCNIRScanSDK.ReferenceCalibration.writeRefCalFile(mContext, refCal);
            calProgress.setVisibility(View.GONE);
            //------------------------------------------------------------------
            storeCalibration.device = preferredDevice;
            storeCalibration.storrefCoeff = refCoeff;
            storeCalibration.storerefMatrix = refMatrix;
        }
    }
    // 发送GET_ACTIVE_CONF广播，获取活动配置（需要调用ISCNIRScanSDK.GetActiveConfig()）
    private class  GetActiveScanConfReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ActiveConfigindex = intent.getByteArrayExtra(ISCNIRScanSDK.EXTRA_ACTIVE_CONF)[0];
            if(ScanConfigList.size()!=0)
            {
                GetActiveConfigOnResume();
            }
            else
            {
                // 获取扫描配置数量和扫描配置数据
                ISCNIRScanSDK.GetScanConfig();
            }
        }
    }
    // 获取扫描配置数量（需要调用ISCNIRScanSDK.GetScanConfig()）
    private class  ScanConfSizeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            storedConfSize = intent.getIntExtra(ISCNIRScanSDK.EXTRA_CONF_SIZE, 0);
        }
    }
    // 获取扫描配置数据（需要调用ISCNIRScanSDK.GetScanConfig()）
    private class ScanConfReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            receivedConfSize++;
            ScanConfig_Byte_List.add(intent.getByteArrayExtra(ISCNIRScanSDK.EXTRA_DATA));
            ScanConfigList.add(ISCNIRScanSDK.scanConf);

            if (storedConfSize>0 && receivedConfSize==0) {
                barProgressDialog.dismiss();
                barProgressDialog = new ProgressDialog(ScanViewActivity.this);
                barProgressDialog.setTitle(getString(R.string.reading_configurations));
                barProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                barProgressDialog.setProgress(0);
                barProgressDialog.setMax(storedConfSize);
                barProgressDialog.setCancelable(false);
                barProgressDialog.show();
            } else {
                barProgressDialog.setProgress(receivedConfSize+1);
            }
            if (barProgressDialog.getProgress() == barProgressDialog.getMax() || barProgressDialog.getMax()==1)
            {
                for(int i=0;i<ScanConfigList.size();i++)
                {
                    int ScanConfigIndextoByte = (byte)ScanConfigList.get(i).getScanConfigIndex();
                    if(ActiveConfigindex == ScanConfigIndextoByte )
                    {
                        activeConf = ScanConfigList.get(i);
                        ActiveConfigByte = ScanConfig_Byte_List.get(i);
                    }
                }
                barProgressDialog.dismiss();
                mMenu.findItem(R.id.action_settings).setEnabled(true);
                storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.scanConfiguration, ISCNIRScanSDK.scanConf.getConfigName());
                tv_manual_scan_conf.setText(activeConf.getConfigName());
                if(downloadspecFlag ==false)
                {
                    // 获取光谱校准系数
                    ISCNIRScanSDK.GetSpectrumCoef();
                    downloadspecFlag = true;
                }
            }
        }
    }
    // 从设备获取光谱校准系数，然后请求设备信息（需要调用ISCNIRScanSDK.GetSpectrumCoef()）
    public class SpectrumCalCoefficientsReadyReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            SpectrumCalCoefficients = intent.getByteArrayExtra(ISCNIRScanSDK.EXTRA_SPEC_COEF_DATA);
            passSpectrumCalCoefficients = SpectrumCalCoefficients;
            // 请求设备信息
            ISCNIRScanSDK.GetDeviceInfo();
        }
    }

    String model_name="";
    String serial_num = "";
    String HWrev = "";
    String Tivarev ="";
    String Specrev = "";
    // 发送GET_INFO广播，获取设备信息（需要调用ISCNIRScanSDK.GetDeviceInfo()）
    public class DeviceInfoReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            model_name = intent.getStringExtra(ISCNIRScanSDK.EXTRA_MODEL_NUM);
            serial_num = intent.getStringExtra(ISCNIRScanSDK.EXTRA_SERIAL_NUM);
            HWrev = intent.getStringExtra(ISCNIRScanSDK.EXTRA_HW_REV);
            Tivarev = intent.getStringExtra(ISCNIRScanSDK.EXTRA_TIVA_REV);
            Specrev = intent.getStringExtra(ISCNIRScanSDK.EXTRA_SPECTRUM_REV);
            if(Tivarev.substring(0,1) .equals("5"))
            {
                isExtendVer_PLUS = true;
                isExtendVer = false;
            }
            else if(Tivarev.substring(0,1) .equals("3") && (HWrev.substring(0,1).equals("E")|| HWrev.substring(0,1).equals("O")))
            {
                isExtendVer_PLUS = false;
                isExtendVer = true;
            }
            else
            {
                isExtendVer_PLUS = false;
                isExtendVer = false;
            }
            if((isExtendVer||isExtendVer_PLUS) && serial_num.length()>8)
                serial_num = serial_num.substring(0,8);
            else if(!isExtendVer_PLUS&&!isExtendVer && serial_num.length()>7)
                serial_num = serial_num.substring(0,7);
            if(HWrev.substring(0,1).equals("N"))
                Dialog_Pane_Finish("Not support","Not to support the N version of the main board.\nWill go to the home page.");
            else
            {
                if(isExtendVer_PLUS)
                {
                    adapter_width = ArrayAdapter.createFromResource(mContext,
                            R.array.scan_width_plus, android.R.layout.simple_spinner_item);
                    adapter_width.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                }
                else
                {
                }
                if(isExtendVer)
                    ISCNIRScanSDK.TIVAFW_EXT =  GetFWLevelEXT(Tivarev);
                else if(isExtendVer_PLUS)
                    ISCNIRScanSDK.TIVAFW_EXT_PLUS = GetFWLevelEXTPLUS(Tivarev);
                else
                    ISCNIRScanSDK.TIVAFW_STANDARD = GetFWLevelStandard(Tivarev);
                InitParameter();
                if(!isExtendVer_PLUS && !isExtendVer && fw_level_standard.compareTo(ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_0)==0)
                    Dialog_Pane_Finish("Firmware Out of Date","You must update the firmware on your NIRScan Nano to make this App working correctly!\n" +
                            "FW required version at least V2.4.4.\nDetected version is V" + Tivarev +".");
                else
                    ISCNIRScanSDK.GetMFGNumber();
            }
        }
    }
    // 获取MFG编号（需要调用ISCNIRScanSDK.GetMFGNumber()）
    private byte MFG_NUM[];
    public class ReturnMFGNumReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            MFG_NUM = intent.getByteArrayExtra(ISCNIRScanSDK.MFGNUM_DATA);
            // Tiva 2.5.x
            if((!isExtendVer_PLUS && !isExtendVer && fw_level_standard.compareTo(ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_4)>=0) || (isExtendVer && fw_level_ext.compareTo(ISCNIRScanSDK.FW_LEVEL_EXT.LEVEL_EXT_3)>=0))
                ISCNIRScanSDK.GetHWModel();
            else
                ISCNIRScanSDK.GetUUID();
        }
    }
    // 获取硬件型号（需要调用ISCNIRScanSDK.GetHWModel()）
    private String HW_Model="";
    public class ReturnHWModelReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            byte[]byteHWMDEL = intent.getByteArrayExtra(ISCNIRScanSDK.HWMODEL_DATA);
            int len = 0;
            for(int i=0;i<byteHWMDEL.length;i++)
            {
                if(byteHWMDEL[i]==0)
                    break;
                else
                    len ++;
            }
            byte[] HWModel = new byte[len];
            for(int i=0;i<len;i++)
                HWModel[i] = byteHWMDEL[i];
            HW_Model = new String(HWModel, StandardCharsets.UTF_8);
            // 获取设备的UUID
            ISCNIRScanSDK.GetUUID();
        }
    }
    // 根据Tiva版本定义固件级别
    private ISCNIRScanSDK.FW_LEVEL_STANDARD GetFWLevelStandard(String Tivarev)
    {
        String[] TivaArray= Tivarev.split(Pattern.quote("."));
        String split_hw[] = HWrev.split("\\.");
        fw_level_standard = ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_0;
        if(Integer.parseInt(TivaArray[1])>=5 && split_hw[0].equals("F"))
        {
                // 新应用：使用新命令读取ADC值和时间戳
            fw_level_standard = ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_5;//>=2.5.X and main board ="F"
        }
        else if(Integer.parseInt(TivaArray[1])>=5)
        {
                // 新应用：支持获取PGA
            fw_level_standard = ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_4;//>=2.5.X
        }
        else if(Integer.parseInt(TivaArray[1])>=4 && Integer.parseInt(TivaArray[2])>=3 &&split_hw[0].equals("F"))
        {
                // 新应用：支持读取ADC值
            fw_level_standard = ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_3;//>=2.4.4 and main board ="F"
        }
        else if((Integer.parseInt(TivaArray[1])>=4 && Integer.parseInt(TivaArray[2])>=3) || Integer.parseInt(TivaArray[1])>=5)
        {
                // 新应用：添加锁定按钮
            fw_level_standard = ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_2;//>=2.4.4
        }
        else if((TivaArray.length==3 && Integer.parseInt(TivaArray[1])>=1)|| (TivaArray.length==4 &&  Integer.parseInt(TivaArray[3])>=67)) // >=2.1.0.67
        {
            // 新应用：支持激活状态
            fw_level_standard = ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_1;
        }
        else
        {
            fw_level_standard = ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_0;
        }
        return fw_level_standard;
    }
    private ISCNIRScanSDK.FW_LEVEL_EXT GetFWLevelEXT(String Tivarev)
    {
        String[] TivaArray= Tivarev.split(Pattern.quote("."));
        String split_hw[] = HWrev.split("\\.");
        fw_level_ext = ISCNIRScanSDK.FW_LEVEL_EXT.LEVEL_EXT_1;
        if(Integer.parseInt(TivaArray[1])>=5 && split_hw[0].equals("O"))
        {
            // 新应用：使用新命令读取ADC值和时间戳
            fw_level_ext = ISCNIRScanSDK.FW_LEVEL_EXT.LEVEL_EXT_4; // >=3.5.X 且主板="O"
        }
        else if(Integer.parseInt(TivaArray[1])>=5)
        {
            // 新应用：支持获取PGA
            fw_level_ext = ISCNIRScanSDK.FW_LEVEL_EXT.LEVEL_EXT_3; // >=3.5.X
        }
        else if(Integer.parseInt(TivaArray[1])>=3 && split_hw[0].equals("O"))
        {
            // 新应用：支持读取ADC值
            fw_level_ext = ISCNIRScanSDK.FW_LEVEL_EXT.LEVEL_EXT_2; // >=3.3.0 且主板="O"
        }
        else if(Integer.parseInt(TivaArray[1])>=3)
        {
            // 新应用：添加锁定按钮
            fw_level_ext = ISCNIRScanSDK.FW_LEVEL_EXT.LEVEL_EXT_1; // >=3.3.0
        }
        else if(Integer.parseInt(TivaArray[1])==2 && Integer.parseInt(TivaArray[2])==1 )
            fw_level_ext = ISCNIRScanSDK.FW_LEVEL_EXT.LEVEL_EXT_1;//==3.2.1

        return fw_level_ext;
    }
    private ISCNIRScanSDK.FW_LEVEL_EXT_PLUS GetFWLevelEXTPLUS(String Tivarev)
    {
        String[] TivaArray= Tivarev.split(Pattern.quote("."));
        String split_hw[] = HWrev.split("\\.");
        fw_level_ext_plus = ISCNIRScanSDK.FW_LEVEL_EXT_PLUS.LEVEL_EXT_PLUS_1;
        return fw_level_ext_plus;
    }
    // 确定设备的波长范围并初始化参数
    private void InitParameter()
    {
        if(isExtendVer)
        {
            minWavelength = 1350;
            maxWavelength = 2150;
            MINWAV = 1350;
            MAXWAV = 2150;
        }
        else if(isExtendVer_PLUS)
        {
            minWavelength = 1600;
            maxWavelength = 2400;
            MINWAV = 1600;
            MAXWAV = 2400;
        }
        else
        {
            minWavelength = 900;
            maxWavelength = 1700;
            MINWAV = 900;
            MAXWAV = 1700;
        }
        // 不支持锁定按钮
        if(!isExtendVer_PLUS && !isExtendVer && fw_level_standard.compareTo(ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_1) <=0)
            storeBooleanPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.LockButton,false);
    }
    // 获取设备UUID（需要调用ISCNIRScanSDK.GetUUID()）
    String uuid="";
    public class GetUUIDReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {

            byte buf[] = intent.getByteArrayExtra(ISCNIRScanSDK.EXTRA_DEVICE_UUID);
            for(int i=0;i<buf.length;i++)
            {
                uuid += Integer.toHexString( 0xff & buf[i] );
                if(i!= buf.length-1)
                {
                    uuid +=":";
                }
            }
            CheckIsOldTIVA();
            if(!isOldTiva)
            {
                // 获取设备是否已激活
                ISCNIRScanSDK.ReadActivateState();
            }
            else
            {
                closeFunction();
                mMenu.findItem(R.id.action_key).setVisible(false);
            }
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(DeviceInfoReceiver);
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(GetUUIDReceiver);
        }
    }

    // 获取设备的激活状态（需要调用ISCNIRScanSDK.ReadActivateState()）
    public class RetrunReadActivateStatusReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            if(mainflag!="")// 仅从HomeViewActivity->ScanViewActivity时执行
            {
                // 设置活动扫描配置，避免设备使用wpf或winform本地配置
                ISCNIRScanSDK.SetActiveConfig();
                mainflag = "";
            }
            byte state[] = intent.getByteArrayExtra(ISCNIRScanSDK.RETURN_READ_ACTIVATE_STATE);
            if(state[0] == 1)
            {
                Handler handler = new Handler();
                handler.postDelayed(new Runnable(){

                    @Override
                    public void run() {
                        SetDeviceButtonStatus();
                        Dialog_Pane_OpenFunction("Device Activated","Device advanced functions are all unlocked.");
                    }}, 200);
                mMenu.findItem(R.id.action_settings).setEnabled(true);
                mMenu.findItem(R.id.action_key).setEnabled(true);
                storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.Activacatestatus, "Activated.");
            }
            else
            {
                String licensekey = getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.licensekey, null);
                // 设备已锁定但保存了许可证密钥
                if(licensekey!=null && licensekey!="")
                {
                    calProgress.setVisibility(View.VISIBLE);
                    String filterdata = filterDate(licensekey);
                    final byte data[] = hexToBytes(filterdata);
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable(){

                        @Override
                        public void run() {
                            ISCNIRScanSDK.SetLicenseKey(data);
                        }}, 200);
                }
                else
                {
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable(){

                        @Override
                        public void run() {
                            SetDeviceButtonStatus();
                            Dialog_Pane("Unlock device","Some functions are locked.");
                        }}, 200);
                    mMenu.findItem(R.id.action_settings).setEnabled(true);
                    mMenu.findItem(R.id.action_key).setEnabled(true);
                    storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.Activacatestatus, "Function is locked.");
                    closeFunction();
                }
            }
        }
    }
    // 获取设备的激活状态（需要调用ISCNIRScanSDK.SetLicenseKey(data)）
    public class RetrunActivateStatusReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            mMenu.findItem(R.id.action_settings).setEnabled(true);
            mMenu.findItem(R.id.action_key).setEnabled(true);
            calProgress.setVisibility(View.GONE);
            byte state[] = intent.getByteArrayExtra(ISCNIRScanSDK.RETURN_ACTIVATE_STATUS);
            if(state[0] == 1)
            {
                SetDeviceButtonStatus();
                Dialog_Pane_OpenFunction("Device Activated","Device advanced functions are all unlocked.");
                storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.Activacatestatus, "Activated.");
            }
            else
            {
                SetDeviceButtonStatus();
                Dialog_Pane("Unlock device","Some functions are locked.");
                storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.Activacatestatus, "Function is locked.");
                closeFunction();
            }
        }
    }
    private void CheckIsOldTIVA()
    {
        String[] TivaArray= Tivarev.split(Pattern.quote("."));
        try {
            if(!isExtendVer_PLUS && !isExtendVer && (Integer.parseInt(TivaArray[1])<4 || Integer.parseInt(TivaArray[1])<4))//Tiva <2.4.4(the newest version)
            {
                isOldTiva = true;
                Dialog_Pane_OldTIVA("Firmware Out of Date", "You must update the firmware on your NIRScan Nano to make this App working correctly!\n" +
                        "FW required version at least V2.4.4\nDetected version is V" + Tivarev + "\nDo you still want to continue?");
            }
            else
                isOldTiva = false;
        }catch (Exception e)
        {

        };
    }
    // 标题栏
    // 初始化图表视图和标题栏事件
    private void TitleBarEvent()
    {
        // 设置标题栏
        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setTitle(getString(R.string.new_scan));
        }
    }
    // 创建选项菜单
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_new_scan, menu);
        mMenu = menu;
        mMenu.findItem(R.id.action_settings).setEnabled(false);
        mMenu.findItem(R.id.action_key).setEnabled(false);
        return true;
    }

    // 处理菜单项选择
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            GotoOtherPage = true;
            ChangeLampState();
            // 避免前往扫描配置页面时冲突
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(ScanConfReceiver);
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(ScanConfSizeReceiver);
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(GetActiveScanConfReceiver);
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(WriteScanConfigStatusReceiver);
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(GetDeviceStatusReceiver);

            Intent configureIntent = new Intent(mContext, ConfigureViewActivity.class);
            startActivity(configureIntent);
        }
        if (id == R.id.action_key) {
            GotoOtherPage = true;
            ChangeLampState();
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(RetrunReadActivateStatusReceiver);
            Intent configureIntent = new Intent(mContext, ActivationViewActivity.class);
            startActivity(configureIntent);
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(RetrunActivateStatusReceiver);
        }
        if (id == android.R.id.home) {
            this.finish();
        }
        return super.onOptionsItemSelected(item);
    }
    
    // 初始化组件和控制
    // 初始化显示模式选择器（替代ViewPager标签）
    private void InitialDisplayModeSelector() {
        spinner_display_mode = (Spinner) findViewById(R.id.spinner_display_mode);
        mMainChart = (LineChart) findViewById(R.id.lineChartMain);
        
        if (spinner_display_mode != null) {
            String[] modes = {
                DisplayMode.INTENSITY.getLabel(),
                DisplayMode.ABSORBANCE.getLabel(),
                DisplayMode.REFLECTANCE.getLabel()
            };
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_spinner_item, modes);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner_display_mode.setAdapter(adapter);
            spinner_display_mode.setSelection(currentDisplayMode.getIndex());
            
            spinner_display_mode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    currentDisplayMode = DisplayMode.values()[position];
                    updateChartDisplay();
                }
                
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }
        
        // Initialize main chart
        if (mMainChart != null) {
            initializeChart(mMainChart);
        }
    }
    
    // 初始化配置切换按钮
    private void InitialConfigToggle() {
        btn_toggle_config = (Button) findViewById(R.id.btn_toggle_config);
        layout_config_collapsible = (LinearLayout) findViewById(R.id.layout_config_collapsible);
        
        if (btn_toggle_config != null && layout_config_collapsible != null) {
            btn_toggle_config.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (layout_config_collapsible.getVisibility() == View.VISIBLE) {
                        layout_config_collapsible.setVisibility(View.GONE);
                        btn_toggle_config.setText("显示配置");
                    } else {
                        layout_config_collapsible.setVisibility(View.VISIBLE);
                        btn_toggle_config.setText("隐藏配置");
                    }
                }
            });
        }
    }
    
    // 初始化图表设置
    private void initializeChart(LineChart chart) {
        chart.setDrawGridBackground(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setAxisMaximum(maxWavelength);
        xAxis.setAxisMinimum(minWavelength);
        
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.enableGridDashedLine(10f, 10f, 0f);
        leftAxis.setDrawLimitLinesBehindData(true);
        // 设置强度模式的默认Y轴范围（数据到达时将更新）
        leftAxis.setAxisMaximum(1000f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setStartAtZero(true);
        
        chart.getAxisRight().setEnabled(false);
        
        Legend l = chart.getLegend();
        l.setForm(Legend.LegendForm.LINE);
        chart.getLegend().setEnabled(false);
        
        // 添加默认占位线（y=0的水平线），实际扫描数据到达时将被替换
        ArrayList<Entry> defaultEntries = new ArrayList<>();
        // Create two points to draw a horizontal line across the wavelength range
        defaultEntries.add(new Entry(minWavelength, 0f));
        defaultEntries.add(new Entry(maxWavelength, 0f));
        
        LineDataSet defaultDataSet = new LineDataSet(defaultEntries, "");
        defaultDataSet.setColor(Color.GRAY);
        defaultDataSet.setLineWidth(1f);
        defaultDataSet.setDrawCircles(false);
        defaultDataSet.setDrawValues(false);
        defaultDataSet.setDrawFilled(false);
        
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(defaultDataSet);
        LineData defaultData = new LineData(dataSets);
        chart.setData(defaultData);
        chart.invalidate();
    }
    
    // 根据选中的模式更新图表显示
    private void updateChartDisplay() {
        if (mMainChart == null || mXValues == null || mXValues.isEmpty()) return;
        
        try {
            int numSections = 1;
            if (Scan_Config_Info != null && Scan_Config_Info.numSections != null && Scan_Config_Info.numSections.length > 0) {
                numSections = Scan_Config_Info.numSections[0];
            }
            
            switch (currentDisplayMode) {
                case INTENSITY:
                    mMainChart.getAxisLeft().setAxisMaximum(maxIntensity);
                    mMainChart.getAxisLeft().setAxisMinimum(minIntensity);
                    mMainChart.getAxisLeft().setStartAtZero(true);
                    if (numSections >= 2 && !Float.isNaN(minIntensity) && !Float.isNaN(maxIntensity) && mIntensityFloat != null && !mIntensityFloat.isEmpty()) {
                        setDataSlew(mMainChart, mIntensityFloat, numSections);
                    } else if (!Float.isNaN(minIntensity) && !Float.isNaN(maxIntensity) && mIntensityFloat != null && !mIntensityFloat.isEmpty()) {
                        setData(mMainChart, mXValues, mIntensityFloat, ChartType.INTENSITY);
                    }
                    break;
                case ABSORBANCE:
                    mMainChart.getAxisLeft().setAxisMaximum(maxAbsorbance);
                    mMainChart.getAxisLeft().setAxisMinimum(minAbsorbance);
                    mMainChart.getAxisLeft().setStartAtZero(false);
                    if (numSections >= 2 && !Float.isNaN(minAbsorbance) && !Float.isNaN(maxAbsorbance) && mAbsorbanceFloat != null && !mAbsorbanceFloat.isEmpty()) {
                        setDataSlew(mMainChart, mAbsorbanceFloat, numSections);
                    } else if (!Float.isNaN(minAbsorbance) && !Float.isNaN(maxAbsorbance) && mAbsorbanceFloat != null && !mAbsorbanceFloat.isEmpty()) {
                        setData(mMainChart, mXValues, mAbsorbanceFloat, ChartType.ABSORBANCE);
                    }
                    break;
                case REFLECTANCE:
                    mMainChart.getAxisLeft().setAxisMaximum(maxReflectance);
                    mMainChart.getAxisLeft().setAxisMinimum(minReflectance);
                    mMainChart.getAxisLeft().setStartAtZero(false);
                    if (numSections >= 2 && !Float.isNaN(minReflectance) && !Float.isNaN(maxReflectance) && mReflectanceFloat != null && !mReflectanceFloat.isEmpty()) {
                        setDataSlew(mMainChart, mReflectanceFloat, numSections);
                    } else if (!Float.isNaN(minReflectance) && !Float.isNaN(maxReflectance) && mReflectanceFloat != null && !mReflectanceFloat.isEmpty()) {
                        setData(mMainChart, mXValues, mReflectanceFloat, ChartType.REFLECTANCE);
                    }
                    break;
            }
            
            mMainChart.invalidate();
        } catch (Exception e) {
            // Ignore errors during chart update
        }
    }
    
    // 初始化扫描模式按钮组件（已移除，仅使用手动模式）
    private void InitialScanMethodButtonComponent()
    {
    }
    
    // 初始化普通扫描模式组件（已移除）
    private void InitialNormalComponent()
    {
    }
    
    // 初始化快速设置扫描模式组件（已移除）
    private void InitialQuicksetComponent()
    {
    }
    // 初始化手动扫描模式组件
    private void InitialManualComponent()
    {
        ly_lamp = (LinearLayout)findViewById(R.id.ly_lamp);
        view_lamp_onoff = (View) findViewById(R.id.view_lamp_onoff);
        tv_manual_scan_conf = (TextView)findViewById(R.id.tv_manual_scan_conf);
        toggle_button_manual_scan_mode = (ToggleButton) findViewById(R.id.toggle_button_manual_scan_mode);
        toggle_button_manual_lamp = (ToggleButton) findViewById(R.id.toggle_button_manual_lamp);
        et_manual_lamptime = (EditText) findViewById(R.id.et_manual_lamptime);
        et_manual_pga = (EditText) findViewById(R.id.et_manual_pga);
        et_manual_repead = (EditText) findViewById(R.id.et_manual_repead);
        ly_manual_conf = (LinearLayout)findViewById(R.id.ly_conf_manual);

        toggle_button_manual_scan_mode.setOnClickListener(Toggle_Button_Manual_ScanMode_Click);
        toggle_button_manual_lamp.setOnCheckedChangeListener(Toggle_Button_Manual_Lamp_Changed);
        et_manual_pga.setOnEditorActionListener(Manual_PGA_OnEditor);
        et_manual_repead.setOnEditorActionListener(Manual_Repeat_OnEditor);
        et_manual_lamptime.setOnEditorActionListener(Manual_Lamptime_OnEditor);
        ly_manual_conf.setOnClickListener(Manual_Config_Click);
        toggle_button_manual_lamp.setEnabled(false);
        et_manual_repead.setEnabled(false);
        et_manual_pga.setEnabled(false);
        et_manual_lamptime.setEnabled(false);
    }
    // 初始化维护（参考）扫描模式组件（已移除）
    private void InitialMaintainComponent()
    {
    }
    private void DisableLinearComponet(LinearLayout layout)
    {

        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            child.setEnabled(false);
        }
    }
    private void DisableAllComponent()
    {
        // Only manual mode is used now - disable manual mode components only
        LinearLayout layout;
        
        // 手动模式组件（在可折叠布局内）
        layout = (LinearLayout) findViewById(R.id.ll_prefix_manual);
        if (layout != null) DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_scan_mode);
        if (layout != null) DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_lamp);
        if (layout != null) DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_pga);
        if (layout != null) DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_repeat);
        if (layout != null) DisableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_conf_manual);
        if (layout != null) DisableLinearComponet(layout);
        
        // Disable scan button and menu
        btn_scan.setClickable(false);
        if (mMenu != null) {
            mMenu.findItem(R.id.action_settings).setEnabled(false);
            mMenu.findItem(R.id.action_key).setEnabled(false);
        }
    }

    // 禁用连续扫描停止按钮（已移除）
    private void Disable_Stop_Continous_button()
    {
    }

    // 启用连续扫描停止按钮（已移除）
    private void Enable_Stop_Continous_button()
    {
    }

    private void EnableLinearComponet(LinearLayout layout)
    {

        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            child.setEnabled(true);
        }
    }
    // 启用所有组件（仅手动模式）
    private void EnableAllComponent()
    {
        LinearLayout layout;
        
        // 手动模式组件（在可折叠布局内）
        layout = (LinearLayout) findViewById(R.id.ll_prefix_manual);
        if (layout != null) EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_scan_mode);
        if (layout != null) EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_lamp);
        if (layout != null) EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_pga);
        if (layout != null) EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_repeat);
        if (layout != null) EnableLinearComponet(layout);
        layout = (LinearLayout) findViewById(R.id.ly_conf_manual);
        if (layout != null) EnableLinearComponet(layout);
        
        if(toggle_button_manual_scan_mode != null && toggle_button_manual_scan_mode.isChecked() == false)
        {
            if (toggle_button_manual_lamp != null) toggle_button_manual_lamp.setEnabled(false);
            if (et_manual_repead != null) et_manual_repead.setEnabled(false);
            if (et_manual_pga != null) et_manual_pga.setEnabled(false);
            if (et_manual_lamptime != null) et_manual_lamptime.setEnabled(true);
        }
        
        // Enable scan button and menu
        btn_scan.setClickable(true);
        setActivityTouchDisable(false);
        if (mMenu != null) {
            mMenu.findItem(R.id.action_settings).setEnabled(true);
            mMenu.findItem(R.id.action_key).setEnabled(true);
        }
    }
    // 解锁设备后将打开所有扫描模式（仅手动模式）
    private void openFunction()
    {
        Current_Scan_Method = ScanMethod.Manual;
        btn_scan.setClickable(true);
        btn_scan.setBackgroundColor(ContextCompat.getColor(mContext, R.color.red));
        setActivityTouchDisable(false);

        if((HW_Model.equals("R") && isExtendVer) || HW_Model.equals("R3")||HW_Model.equals("R11"))
        {
            if (ly_lamp != null) ly_lamp.setVisibility(View.GONE);
            if (view_lamp_onoff != null) view_lamp_onoff.setVisibility(View.GONE);
        }
    }
    // 锁定设备后只能使用普通扫描（仅手动模式）
    private void closeFunction()
    {
        Current_Scan_Method = ScanMethod.Manual;
        btn_scan.setClickable(true);
        btn_scan.setBackgroundColor(ContextCompat.getColor(mContext, R.color.red));
        setActivityTouchDisable(false);
    }
    
    // 扫描模式按钮事件（已移除，仅使用手动模式）
    private void ChangeLampState()
    {
        if(WarmUp)
        {
            ISCNIRScanSDK.ControlLamp(ISCNIRScanSDK.LampState.AUTO);
            WarmUp = false;
        }
        if(Current_Scan_Method == ScanMethod.Manual && toggle_button_manual_scan_mode.isChecked())//Manual->Normal,Quickset,Maintain
        {
            if(toggle_button_manual_lamp.getText().toString().toUpperCase().equals("ON"))
            {
                toggle_button_manual_lamp.setChecked(false);//close lamp
            }
            ISCNIRScanSDK.ControlLamp(ISCNIRScanSDK.LampState.AUTO);
        }
    }
    
    // 普通UI组件事件
    private LinearLayout.OnClickListener Normal_Config_Click = new LinearLayout.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(activeConf != null) {
                GotoOtherPage = true;
                Intent activeConfIntent = new Intent(mContext, ActiveConfigDetailViewActivity.class);
                activeConfIntent.putExtra("conf",activeConf);
                startActivity(activeConfIntent);
            }
        }
    };
    
    // QuickSet UI组件事件（已移除）


    // 发送ACTION_WRITE_SCAN_CONFIG广播，获取设置配置的状态（需要调用SetConfig）
    public class WriteScanConfigStatusReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            calProgress.setVisibility(View.GONE);
            byte status[] = intent.getByteArrayExtra(ISCNIRScanSDK.RETURN_WRITE_SCAN_CONFIG_STATUS);
            btn_scan.setClickable(true);
            if((int)status[0] == 1)
            {
                if((int)status[2] == -1 && (int)status[3]==-1)
                {
                    Dialog_Pane("Fail","Set configuration fail!");
                }
                else
                {
                    //Get the scan config of the device
                    ISCNIRScanSDK.ReadCurrentScanConfig();
                }
            }
            else if((int)status[0] == -1)
            {
                Dialog_Pane("Fail","Set configuration fail!");
            }
            else if((int)status[0] == -2)
            {
                Dialog_Pane("Fail","Set configuration fail! Hardware not compatible!");
            }
            else if((int)status[0] == -3)
            {
                Dialog_Pane("Fail","Set configuration fail! Function is currently locked!" );
            }
        }
    }

    // 获取设备中的当前扫描配置（需要调用ISCNIRScanSDK.ReadCurrentScanConfig(data)）
    public class ReturnCurrentScanConfigurationDataReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            Boolean flag = Compareconfig(intent.getByteArrayExtra(ISCNIRScanSDK.EXTRA_CURRENT_CONFIG_DATA));
            calProgress.setVisibility(View.GONE);
            if(flag)
            {
                if(saveReference == true)
                {
                    saveReference = false;
                    ISCNIRScanSDK.ClearDeviceError();
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable(){

                        @Override
                        public void run() {
                            finish();
                        }}, 100);
                }
                else
                {
                    Dialog_Pane("Success","Complete to set configuration.");
                }
            }
            else
            {
                if(saveReference == true)
                {
                    Dialog_Pane("Fail","Restore config fail, should re-open device.");
                    saveReference = false;
                    ISCNIRScanSDK.ClearDeviceError();
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable(){

                        @Override
                        public void run() {
                            finish();
                        }}, 100);
                }
                else
                {
                    Dialog_Pane("Fail","Set configuration fail.");
                }
            }
        }
    }
    
    // 手动模式UI组件事件
    private Button.OnClickListener Toggle_Button_Manual_ScanMode_Click = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(toggle_button_manual_scan_mode.getText().toString().toUpperCase().equals("ON"))
            {
                toggle_button_manual_lamp.setEnabled(true);
                et_manual_repead.setEnabled(true);
                et_manual_pga.setEnabled(true);
                et_manual_lamptime.setEnabled(false);
                if((HW_Model.equals("R")&&isExtendVer)|| HW_Model.equals("R3")||HW_Model.equals("R11"))
                    toggle_button_manual_lamp.setVisibility(View.GONE);
                else
                    toggle_button_manual_lamp.setChecked(true);
            }
            else
            {
                toggle_button_manual_lamp.setEnabled(false);
                et_manual_repead.setEnabled(false);
                et_manual_pga.setEnabled(false);
                et_manual_lamptime.setEnabled(true);
                toggle_button_manual_lamp.setChecked(false);
                ISCNIRScanSDK.ControlLamp(ISCNIRScanSDK.LampState.AUTO);
            }
        }
    };
    private ToggleButton.OnCheckedChangeListener Toggle_Button_Manual_Lamp_Changed = new ToggleButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

            if(toggle_button_manual_lamp.getText().toString().toUpperCase().equals("OFF"))//OFF->ON
            {
                ISCNIRScanSDK.ControlLamp(ISCNIRScanSDK.LampState.ON);
            }
            else
            {
                ISCNIRScanSDK.ControlLamp(ISCNIRScanSDK.LampState.OFF);
            }
        }
    };
    private EditText.OnEditorActionListener Manual_Lamptime_OnEditor = new EditText.OnEditorActionListener()
    {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if(Integer.parseInt(et_manual_lamptime.getText().toString())!=625)
                {
                    int lamptime = Integer.parseInt(et_manual_lamptime.getText().toString());
                    ISCNIRScanSDK.SetLampStableTime(lamptime);
                }
                return false;
            }
            return false;
        }
    };

    private EditText.OnEditorActionListener Manual_PGA_OnEditor = new EditText.OnEditorActionListener()
    {
        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if( checkValidPga()==true)
                {
                    int pga = Integer.parseInt(et_manual_pga.getText().toString());
                    ISCNIRScanSDK.SetPGA(pga);
                    if( (!isExtendVer_PLUS && !isExtendVer && fw_level_standard.compareTo(ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_4)>=0)
                        || (isExtendVer && fw_level_ext.compareTo(ISCNIRScanSDK.FW_LEVEL_EXT.LEVEL_EXT_3)>=0))
                        ISCNIRScanSDK.GetPGA();
                    return false;
                }
            }
            return false;
        }
    };
    /**
     * Get PGA(ISCNIRScanSDK.GetPGA()should be called and TIVA should � 2.5.x)
     */
    public class GetPGAReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {

            byte buf[] = intent.getByteArrayExtra(ISCNIRScanSDK.EXTRA_PGA);
            int pga = Integer.parseInt(et_manual_pga.getText().toString());
            int getpga = (int) buf[0];
            if(pga == getpga)
                Dialog_Pane("Success", "Set PGA : " + pga);
            else
                Dialog_Pane("Fail", "Set PGA : " + pga);
        }
    }

    private EditText.OnEditorActionListener Manual_Repeat_OnEditor = new EditText.OnEditorActionListener()
    {
        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if(checkValidRepeat())
                {
                    int scan_repeat = Integer.parseInt(et_manual_repead.getText().toString());
                    ISCNIRScanSDK.SetScanRepeat(scan_repeat);
                    return false;
                }

            }
            return false;
        }
    };
    private LinearLayout.OnClickListener Manual_Config_Click = new LinearLayout.OnClickListener()
    {
        @Override
        public void onClick(View view) {
            if(activeConf != null) {
                GotoOtherPage = true;
                Intent activeConfIntent = new Intent(mContext, ActiveConfigDetailViewActivity.class);
                activeConfIntent.putExtra("conf",activeConf);
                startActivity(activeConfIntent);
            }
        }
    };
    
    // 扫描相关
    private Button.OnClickListener Button_Scan_Click = new Button.OnClickListener()
    {
        @Override
        public void onClick(View view) {
            // Check if warmup is in progress
            if (isWarmupInProgress) {
                Toast.makeText(ScanViewActivity.this, "预热进行中，请稍候", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 处理灯自动模式 - 扫描前打开灯
            if (currentLampMode == LampMode.AUTO && connected) {
                ISCNIRScanSDK.ControlLamp(ISCNIRScanSDK.LampState.ON);
            }
            
            storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.prefix, filePrefix.getText().toString());
            long delaytime = 300;
            // 仅支持手动模式
            if(Current_Scan_Method == ScanMethod.Manual)
            {
                if( checkValidPga()==false)
                {
                    NotValidValueDialog("错误","PGA值必须是 1,2,4,8,16,32,64 之一。");
                    return;
                }
                else if( checkValidRepeat()==false)
                {
                    NotValidValueDialog("错误","扫描重复次数范围是 1~50。");
                    return;
                }
                else if(toggle_button_manual_scan_mode.getText().toString().equals("On"))
                {
                    DisableAllComponent();
                    btn_scan.setText("扫描中...");
                    calProgress.setVisibility(View.VISIBLE);
                    PerformScan(delaytime);
                }
                else
                {
                    PerformScan(delaytime);
                }
                
                // 扫描期间禁用UI
                DisableAllComponent();
                calProgress.setVisibility(View.VISIBLE);
                btn_scan.setText("扫描中...");
            }
        }
    };

    // 发送START_SCAN广播，通知扫描开始（需要调用PerformScan）
    public class ScanStartedReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            calProgress.setVisibility(View.VISIBLE);
            btn_scan.setText("扫描中...");
        }
    }
    
    ISCNIRScanSDK.ReferenceCalibration reference_calibration;
    String CurrentTime;
    long MesureScanTime=0;
    
    // 处理扫描数据并设置图表（需要调用ISCNIRScanSDK.StartScan()）
    public class ScanDataReadyReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            long endtime = System.currentTimeMillis();
            MesureScanTime = endtime - ISCNIRScanSDK.startScanTime;
            reference_calibration = ISCNIRScanSDK.ReferenceCalibration.currentCalibration.get(0);
            if(Interpret_length<=0)
            {
                Dialog_Pane_Finish("Error","The scan interpret fail. Please check your device.");
            }
            else
            {
                //Get scan spectrum data
                Scan_Spectrum_Data = new ISCNIRScanSDK.ScanResults(Interpret_wavelength,Interpret_intensity,Interpret_uncalibratedIntensity,Interpret_length);

                mXValues.clear();
                mIntensityFloat.clear();
                mAbsorbanceFloat.clear();
                mReflectanceFloat.clear();
                mWavelengthFloat.clear();
                mReferenceFloat.clear();
                int index;
                for (index = 0; index < Scan_Spectrum_Data.getLength(); index++) {
                    mXValues.add(String.format("%.02f", ISCNIRScanSDK.ScanResults.getSpatialFreq(mContext, Scan_Spectrum_Data.getWavelength()[index])));
                    mIntensityFloat.add(new Entry((float) Scan_Spectrum_Data.getWavelength()[index],(float) Scan_Spectrum_Data.getUncalibratedIntensity()[index]));
                    mAbsorbanceFloat.add(new Entry((float) Scan_Spectrum_Data.getWavelength()[index],(-1) * (float) Math.log10((double) Scan_Spectrum_Data.getUncalibratedIntensity()[index] / (double) Scan_Spectrum_Data.getIntensity()[index])));
                    mReflectanceFloat.add(new Entry((float) Scan_Spectrum_Data.getWavelength()[index],(float) Scan_Spectrum_Data.getUncalibratedIntensity()[index] / Scan_Spectrum_Data.getIntensity()[index]));
                    mWavelengthFloat.add((float) Scan_Spectrum_Data.getWavelength()[index]);
                    mReferenceFloat.add(new Entry((float) Scan_Spectrum_Data.getWavelength()[index],(float) Scan_Spectrum_Data.getIntensity()[index]));
                }
                minWavelength = mWavelengthFloat.get(0);
                maxWavelength = mWavelengthFloat.get(0);

                for (Float f : mWavelengthFloat) {
                    if (f < minWavelength) minWavelength = f;
                    if (f > maxWavelength) maxWavelength = f;
                }
                minAbsorbance = mAbsorbanceFloat.get(0).getY();
                maxAbsorbance = mAbsorbanceFloat.get(0).getY();
                for (Entry e : mAbsorbanceFloat) {
                    if (e.getY() < minAbsorbance || Float.isNaN(minAbsorbance)) minAbsorbance = e.getY();
                    if (e.getY() > maxAbsorbance || Float.isNaN(maxAbsorbance)) maxAbsorbance = e.getY();
                }
                if(minAbsorbance==0 && maxAbsorbance==0)
                {
                    maxAbsorbance=2;
                }
                minReflectance = mReflectanceFloat.get(0).getY();
                maxReflectance = mReflectanceFloat.get(0).getY();

                for (Entry e : mReflectanceFloat) {
                    if (e.getY() < minReflectance|| Float.isNaN(minReflectance) ) minReflectance = e.getY();
                    if (e.getY() > maxReflectance|| Float.isNaN(maxReflectance) ) maxReflectance = e.getY();
                }
                if(minReflectance==0 && maxReflectance==0)
                {
                    maxReflectance=2;
                }
                minIntensity = mIntensityFloat.get(0).getY();
                maxIntensity = mIntensityFloat.get(0).getY();

                for (Entry e : mIntensityFloat) {
                    if (e.getY() < minIntensity|| Float.isNaN(minIntensity)) minIntensity = e.getY();
                    if (e.getY() > maxIntensity|| Float.isNaN(maxIntensity)) maxIntensity = e.getY();
                }
                if(minIntensity==0 && maxIntensity==0)
                {
                    maxIntensity=1000;
                }
                minReference = mReferenceFloat.get(0).getY();
                maxReference = mReferenceFloat.get(0).getY();

                for (Entry e : mReferenceFloat) {
                    if (e.getY() < minReference || Float.isNaN(minReference)) minReference = e.getY();
                    if (e.getY() > maxReference || Float.isNaN(maxReference)) maxReference = e.getY();
                }
                if(minReference==0 && maxReference==0)
                {
                    maxReference=1000;
                }
                // 更新单个图表显示
                updateChartDisplay();
                //number of slew
                String slew="";
                if(activeConf != null && activeConf.getScanType().equals("Slew")){
                    int numSections = activeConf.getSlewNumSections();
                    int i;
                    for(i = 0; i < numSections; i++){
                        slew = slew + activeConf.getSectionNumPatterns()[i]+"%";
                    }
                }
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", java.util.Locale.getDefault());
                SimpleDateFormat filesimpleDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault());
                String ts = simpleDateFormat.format(new Date());
                CurrentTime = filesimpleDateFormat.format(new Date());
                ActionBar ab = getActionBar();
                if (ab != null) {
                    if (filePrefix.getText().toString().equals("")) {
                        ab.setTitle("ISC" + ts);
                    } else {
                        ab.setTitle(filePrefix.getText().toString() + ts);
                    }
                    ab.setSelectedNavigationItem(0);
                }
                
                storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.prefix, filePrefix.getText().toString());
                
                // 处理灯自动模式 - 扫描完成后关闭灯
                if (currentLampMode == LampMode.AUTO && connected) {
                    ISCNIRScanSDK.ControlLamp(ISCNIRScanSDK.LampState.OFF);
                }
                
                if(WarmUp)
                {
                    ISCNIRScanSDK.ControlLamp(ISCNIRScanSDK.LampState.AUTO);
                    Lamp_Info = LampInfo.CloseWarmUpLampInScan;
                }
                else
                    // 从设备获取设备信息
                    ISCNIRScanSDK.GetDeviceStatus();
            }
        }
    }
    
    // 获取设备状态（需要调用ISCNIRScanSDK.GetDeviceStatus()）
    String battery="";
    String TotalLampTime;
    byte[] devbyte;
    byte[] errbyte;
    float temprature;
    float humidity;
    public class GetDeviceStatusReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            battery = Integer.toString( intent.getIntExtra(ISCNIRScanSDK.EXTRA_BATT, 0));
            long lamptime = intent.getLongExtra(ISCNIRScanSDK.EXTRA_LAMPTIME,0);
            TotalLampTime = GetLampTimeString(lamptime);
            devbyte = intent.getByteArrayExtra(ISCNIRScanSDK.EXTRA_DEV_STATUS_BYTE);
            errbyte = intent.getByteArrayExtra(ISCNIRScanSDK.EXTRA_ERR_BYTE);
            if(isExtendVer_PLUS ||(isExtendVer && (fw_level_ext.compareTo(ISCNIRScanSDK.FW_LEVEL_EXT.LEVEL_EXT_2)==0|| fw_level_ext.compareTo(ISCNIRScanSDK.FW_LEVEL_EXT.LEVEL_EXT_4)==0))
                    || (!isExtendVer_PLUS && !isExtendVer && (fw_level_standard.compareTo(ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_3)==0 || fw_level_standard.compareTo(ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_5)==0 ) ))
                ISCNIRScanSDK.GetScanLampRampUpADC();
            else
                DoScanComplete();
        }
    }

    // 获取灯预热ADC数据（需要调用ISCNIRScanSDK.GetScanLampRampUpADC()）
    private byte Lamp_RAMPUP_ADC_DATA[];
    public class ReturnLampRampUpADCReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            Lamp_RAMPUP_ADC_DATA = intent.getByteArrayExtra(ISCNIRScanSDK.LAMP_RAMPUP_DATA);
            ISCNIRScanSDK.GetLampADCAverage();
        }
    }
    // 获取灯平均ADC数据（需要调用ISCNIRScanSDK.GetLampADCAverage()）
    private byte Lamp_AVERAGE_ADC_DATA[];
    public class ReturnLampADCAverageReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            Lamp_AVERAGE_ADC_DATA = intent.getByteArrayExtra(ISCNIRScanSDK.LAMP_ADC_AVERAGE_DATA);
            if(isExtendVer_PLUS ||(!isExtendVer_PLUS && !isExtendVer && fw_level_standard.compareTo(ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_5)==0)
            || (isExtendVer && fw_level_ext.compareTo(ISCNIRScanSDK.FW_LEVEL_EXT.LEVEL_EXT_4)==0))
                ISCNIRScanSDK.GetLampADCTimeStamp();
            else
                DoScanComplete();
        }
    }
    // 获取灯预热ADC时间戳（需要调用ISCNIRScanSDK.GetLampADCTimeStamp()）
    private byte Lamp_RAMPUP_ADC_TIMESTAMP[];
    public class ReturnLampRampUpADCTimeStampReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            Lamp_RAMPUP_ADC_TIMESTAMP = intent.getByteArrayExtra(ISCNIRScanSDK.LAMP_ADC_TIMESTAMP);
            DoScanComplete();
        }
    }
    // 完成扫描，将扫描数据写入CSV文件并设置UI
    private void DoScanComplete()
    {
        long delaytime = 0;
        Boolean isLockButton = getBooleanPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.LockButton,false);
        // 用户在扫描设置中打开锁定按钮
        if(isLockButton)
            ISCNIRScanSDK.ControlPhysicalButton(ISCNIRScanSDK.PhysicalButton.Lock);
        else
            ISCNIRScanSDK.ControlPhysicalButton(ISCNIRScanSDK.PhysicalButton.Unlock);
        delaytime = 300;
        if(getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.Activacatestatus, "").contains("Activated") == false)
        {
            closeFunction();
        }
        // 写入CSV文件
        writeCSV(Scan_Spectrum_Data);
        
        calProgress.setVisibility(View.GONE);
        progressBarinsideText.setVisibility(View.GONE);
        btn_scan.setText(getString(R.string.scan));
        EnableAllComponent();
        Disable_Stop_Continous_button();
        // Tiva版本 <2.1.0.67
        if(!isExtendVer_PLUS && !isExtendVer && fw_level_standard.compareTo(ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_0)==0)
            closeFunction();
    }
    private String ErrorByteTransfer()
    {
        String ErrorMsg = "";
        int ErrorInt = errbyte[0]&0xFF | (errbyte[1] << 8);
        if((ErrorInt & 0x00000001) > 0)//Scan Error
        {
            ErrorMsg += "Scan Error : ";
            int ErrDetailInt = errbyte[4]&0xFF;
            if ((ErrDetailInt & 0x01) > 0)
                ErrorMsg += "DLPC150 Boot Error Detected.    ";
            if ((ErrDetailInt & 0x02) > 0)
                ErrorMsg += "DLPC150 Init Error Detected.    ";
            if ((ErrDetailInt & 0x04) > 0)
                ErrorMsg += "DLPC150 Lamp Driver Error Detected.    ";
            if ((ErrDetailInt & 0x08) > 0)
                ErrorMsg += "DLPC150 Crop Image Failed.    ";
            if ((ErrDetailInt & 0x10) > 0)
                ErrorMsg += "ADC Data Error.    ";
            if ((ErrDetailInt & 0x20) > 0)
                ErrorMsg += "Scan Config Invalid.    ";
            if ((ErrDetailInt & 0x40) > 0)
                ErrorMsg += "Scan Pattern Streaming Error.    ";
            if ((ErrDetailInt & 0x80) > 0)
                ErrorMsg += "DLPC150 Read Error.    ";
            ErrorMsg += ",";
        }
        if ((ErrorInt & 0x00000002) > 0)  // ADC Error
        {
            ErrorMsg += "ADC Error : ";
            int ErrDetailInt = errbyte[5]&0xFF;
            if (ErrDetailInt == 1)
                ErrorMsg += "Timeout Error.    ";
            else if (ErrDetailInt == 2)
                ErrorMsg += "PowerDown Error.    ";
            else if (ErrDetailInt == 3)
                ErrorMsg += "PowerUp Error.    ";
            else if (ErrDetailInt == 4)
                ErrorMsg += "Standby Error.    ";
            else if (ErrDetailInt == 5)
                ErrorMsg += "WakeUp Error.    ";
            else if (ErrDetailInt == 6)
                ErrorMsg += "Read Register Error.    ";
            else if (ErrDetailInt == 7)
                ErrorMsg += "Write Register Error.    ";
            else if (ErrDetailInt == 8)
                ErrorMsg += "Configure Error.    ";
            else if (ErrDetailInt == 9)
                ErrorMsg += "Set Buffer Error.    ";
            else if (ErrDetailInt == 10)
                ErrorMsg += "Command Error.    ";
            else if (ErrDetailInt == 11)
                ErrorMsg += "Set PGA Error.    ";
            ErrorMsg += ",";
        }
        if ((ErrorInt & 0x00000004) > 0)  // SD Card Error
        {
            ErrorMsg += "SD Card Error.";
            ErrorMsg += ",";
        }
        if ((ErrorInt & 0x00000008) > 0)  // EEPROM Error
        {
            ErrorMsg += "EEPROM Error.";
            ErrorMsg += ",";
        }
        if ((ErrorInt & 0x00000010) > 0)  // BLE Error
        {
            ErrorMsg += "Bluetooth Error.";
            ErrorMsg += ",";
        }
        if ((ErrorInt & 0x00000020) > 0)  // Spectrum Library Error
        {
            ErrorMsg += "Spectrum Library Error.";
            ErrorMsg += ",";
        }
        if ((ErrorInt & 0x00000040) > 0)  // Hardware Error
        {
            ErrorMsg += "HW Error : ";
            int ErrDetailInt = errbyte[11]&0xFF;
            if (ErrDetailInt == 1)
                ErrorMsg += "DLPC150 Error.    ";
            else if (ErrDetailInt == 2)
                ErrorMsg += "Read UUID Error.    ";
            else if (ErrDetailInt == 3)
                ErrorMsg += "Flash Initial Error.    ";
            ErrorMsg += ",";
        }
        if ((ErrorInt & 0x00000080) > 0)  // TMP Sensor Error
        {
            ErrorMsg += "TMP Error : ";
            int ErrDetailInt = errbyte[12]&0xFF;
            if (ErrDetailInt == 1)
                ErrorMsg += "Invalid Manufacturing ID.    ";
            else if (ErrDetailInt == 2)
                ErrorMsg += "Invalid Device ID.    ";
            else if (ErrDetailInt == 3)
                ErrorMsg += "Reset Error.    ";
            else if (ErrDetailInt == 4)
                ErrorMsg += "Read Register Error.    ";
            else if (ErrDetailInt == 5)
                ErrorMsg += "Write Register Error.    ";
            else if (ErrDetailInt == 6)
                ErrorMsg += "Timeout Error.    ";
            else if (ErrDetailInt == 7)
                ErrorMsg += "I2C Error.    ";
            ErrorMsg += ",";
        }
        if ((ErrorInt & 0x00000100) > 0)  // HDC Sensor Error
        {
            ErrorMsg += "HDC Error : ";
            int ErrDetailInt = errbyte[13]&0xFF;
            if (ErrDetailInt == 1)
                ErrorMsg += "Invalid Manufacturing ID.    ";
            else if (ErrDetailInt == 2)
                ErrorMsg += "Invalid Device ID.    ";
            else if (ErrDetailInt == 3)
                ErrorMsg += "Reset Error.    ";
            else if (ErrDetailInt == 4)
                ErrorMsg += "Read Register Error.    ";
            else if (ErrDetailInt == 5)
                ErrorMsg += "Write Register Error.    ";
            else if (ErrDetailInt == 6)
                ErrorMsg += "Timeout Error.    ";
            else if (ErrDetailInt == 7)
                ErrorMsg += "I2C Error.    ";
            ErrorMsg += ",";
        }
        if ((ErrorInt & 0x00000200) > 0)  // Battery Error
        {
            ErrorMsg += "Battery Error : ";
            int ErrDetailInt = errbyte[14]&0xFF;
            if (ErrDetailInt == 0x01)
                ErrorMsg += "Battery Low.    ";
            ErrorMsg += ",";
        }
        if ((ErrorInt & 0x00000400) > 0)  // Insufficient Memory Error
        {
            ErrorMsg += "Not Enough Memory.";
            ErrorMsg += ",";
        }
        if ((ErrorInt & 0x00000800) > 0)  // UART Error
        {
            ErrorMsg += "UART Error.";
            ErrorMsg += ",";
        }
        if ((ErrorInt & 0x00001000) > 0)   // System Error
        {
            ErrorMsg += "System Error : ";
            int ErrDetailInt = errbyte[17]&0xFF;
            if ((ErrDetailInt & 0x01) > 0)
                ErrorMsg += "Unstable Lamp ADC.    ";
            if ((ErrDetailInt & 0x02) > 0)
                ErrorMsg += "Unstable Peak Intensity.    ";
            if ((ErrDetailInt & 0x04) > 0)
                ErrorMsg += "ADS1255 Error.    ";
            if ((ErrDetailInt & 0x08) > 0)
                ErrorMsg += "Auto PGA Error.    ";

            ErrDetailInt = errbyte[18]&0xFF;
            if ((ErrDetailInt & 0x01) > 0)
                ErrorMsg += "Unstable Scan in Repeated times.    ";
            ErrorMsg += ",";
        }
        if(ErrorMsg.equals(""))
            ErrorMsg = "Not Found";
        return ErrorMsg;
    }
    // 保存扫描数据为CSV文件，只保存三列：波长、信号强度、参比
    private void writeCSV(ISCNIRScanSDK.ScanResults scanResults) {
        String prefix = filePrefix.getText().toString();
        if (prefix.equals("")) {
            prefix = "ISC";
        }
        
        if(android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_REMOVED))
        {
            Toast.makeText(ScanViewActivity.this , "未检测到SD卡" , Toast.LENGTH_SHORT ).show();
            return ;
        }
        
        File mSDFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File mFile = new File(mSDFile.getParent() + "/" + mSDFile.getName() + "/ISC_Report");
        if(!mFile.exists())
        {
            mFile.mkdirs();
        }
        mFile.setExecutable(true);
        mFile.setReadable(true);
        mFile.setWritable(true);

        MediaScannerConnection.scanFile(this, new String[] {mFile.toString()}, null, null);
        
        String configname = getBytetoString(Scan_Config_Info.configName);
        String csvOS = mSDFile.getParent() + "/" + mSDFile.getName() + "/ISC_Report/" + prefix + "_" + configname + "_" + CurrentTime + ".csv";

        try {
            List<String[]> data = new ArrayList<String[]>();
            
            // 表头：波长、信号强度、参比
            data.add(new String[]{"波长(nm)", "信号强度", "参比"});
            
            // 数据行：只保存三列数据
            for (int csvIndex = 0; csvIndex < scanResults.getLength(); csvIndex++) {
                double waves = scanResults.getWavelength()[csvIndex];
                int signalIntensity = scanResults.getUncalibratedIntensity()[csvIndex];
                int reference = scanResults.getIntensity()[csvIndex];
                data.add(new String[]{String.valueOf(waves), String.valueOf(signalIntensity), String.valueOf(reference)});
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            {
                MediaStoreWriteCSV(data, configname, prefix);
            }
            else
            {
                MediaScannerConnection.scanFile(this, new String[] {csvOS}, null, null);
                CSVWriter writer = new CSVWriter(new FileWriter(csvOS), ',', CSVWriter.NO_QUOTE_CHARACTER);
                writer.writeAll(data);
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(ScanViewActivity.this, "保存CSV文件失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    // 使用MediaStore API保存CSV文件（Android 11+）
    private void MediaStoreWriteCSV(List<String[]> data, String configname, String prefix)
    {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, prefix + "_" + configname + "_" + CurrentTime + ".csv");
            values.put(MediaStore.MediaColumns.MIME_TYPE, "text/comma-separated-values");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/ISC_Report/");
            Uri uri = getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);
            OutputStream outputStream = getContentResolver().openOutputStream(uri);
            int Len = data.size();
            for(int i=0; i<Len; i++)
            {
                String datacontent = "";
                for(int j=0; j<data.get(i).length; j++)
                {
                    datacontent += data.get(i)[j];
                    if(j < data.get(i).length - 1)
                        datacontent += ",";
                }
                datacontent += "\r\n";
                outputStream.write(datacontent.getBytes());
            }
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(ScanViewActivity.this, "保存CSV文件失败", Toast.LENGTH_SHORT).show();
        }
    }

    
    // 绘制光谱图

    private void setData(LineChart mChart, ArrayList<String> xValues, ArrayList<Entry> yValues, ChartType type) {

        if (type == ChartType.REFLECTANCE) {
            //init yvalues
            int size = yValues.size();
            if(size == 0)
            {
                return;
            }
            //---------------------------------------------------------
            // create a dataset and give it a type
            LineDataSet set1 = new LineDataSet(yValues,GraphLabel);
            // set the line to be drawn like this "- - - - - -"
            set1.enableDashedLine(10f, 5f, 0f);
            set1.enableDashedHighlightLine(10f, 5f, 0f);
            set1.setColor(Color.BLACK);
            set1.setCircleColor(Color.RED);
            set1.setLineWidth(1f);
            set1.setCircleSize(2f);
            set1.setDrawCircleHole(false);
            set1.setValueTextSize(9f);
            set1.setFillAlpha(65);
            set1.setFillColor(Color.RED);
            set1.setDrawFilled(true);
            set1.setValues(yValues);

            ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
            dataSets.add(set1);
            LineData data = new LineData(dataSets);
            mChart.setData(data);

            mChart.setMaxVisibleValueCount(20);
        } else if (type == ChartType.ABSORBANCE) {
            int size = yValues.size();
            if(size == 0)
            {
                return;
            }
            // create a dataset and give it a type
            LineDataSet set1 = new LineDataSet(yValues, GraphLabel);

            // set the line to be drawn like this "- - - - - -"
            set1.enableDashedLine(10f, 5f, 0f);
            set1.enableDashedHighlightLine(10f, 5f, 0f);
            set1.setColor(Color.BLACK);
            set1.setCircleColor(Color.GREEN);
            set1.setLineWidth(1f);
            set1.setCircleSize(2f);
            set1.setDrawCircleHole(false);
            set1.setValueTextSize(9f);
            set1.setFillAlpha(65);
            set1.setFillColor(Color.GREEN);
            set1.setDrawFilled(true);
            set1.setValues(yValues);

            ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
            dataSets.add(set1);
            LineData data = new LineData(dataSets);
            mChart.setData(data);

            mChart.setMaxVisibleValueCount(20);
        } else if (type == ChartType.INTENSITY) {
            int size = yValues.size();
            if(size == 0)
            {
                return;
            }
            // create a dataset and give it a type
            LineDataSet set1 = new LineDataSet(yValues, GraphLabel);

            // set the line to be drawn like this "- - - - - -"
            set1.enableDashedLine(10f, 5f, 0f);
            set1.enableDashedHighlightLine(10f, 5f, 0f);
            set1.setColor(Color.BLACK);
            set1.setCircleColor(Color.BLUE);
            set1.setLineWidth(1f);
            set1.setCircleSize(2f);
            set1.setDrawCircleHole(false);
            set1.setValueTextSize(9f);
            set1.setFillAlpha(65);
            set1.setFillColor(Color.BLUE);
            set1.setDrawFilled(true);
            set1.setValues(yValues);

            ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
            dataSets.add(set1);
            LineData data = new LineData(dataSets);
            mChart.setData(data);


            mChart.setMaxVisibleValueCount(20);
        } else {
            int size = yValues.size();
            if(size == 0)
            {
                yValues.add(new Entry((float) -10, (float) -10));
            }
            // create a dataset and give it a type
            LineDataSet set1 = new LineDataSet(yValues, GraphLabel);

            // set the line to be drawn like this "- - - - - -"
            set1.enableDashedLine(10f, 5f, 0f);
            set1.enableDashedHighlightLine(10f, 5f, 0f);
            set1.setColor(Color.BLACK);
            set1.setCircleColor(Color.BLACK);
            set1.setLineWidth(1f);
            set1.setCircleSize(3f);
            set1.setDrawCircleHole(true);
            set1.setValueTextSize(9f);
            set1.setFillAlpha(65);
            set1.setFillColor(Color.BLACK);
            set1.setDrawFilled(true);
            set1.setValues(yValues);

            ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
            dataSets.add(set1);
            LineData data = new LineData(dataSets);
            mChart.setData(data);

            mChart.setMaxVisibleValueCount(10);
        }
    }

    // 设置Slew扫描数据
    private void setDataSlew(LineChart mChart, ArrayList<Entry> yValues, int slewnum)
    {
        if(yValues.size()<=1)
        {
            return;
        }
        ArrayList<Entry> yValues1 = new ArrayList<Entry>();
        ArrayList<Entry> yValues2 = new ArrayList<Entry>();
        ArrayList<Entry> yValues3 = new ArrayList<Entry>();
        ArrayList<Entry> yValues4 = new ArrayList<Entry>();
        ArrayList<Entry> yValues5 = new ArrayList<Entry>();

        for(int i=0;i<activeConf.getSectionNumPatterns()[0];i++)
        {
            if(Float.isInfinite(yValues.get(i).getY()) == false)
            {
                yValues1.add(new Entry(yValues.get(i).getX(),yValues.get(i).getY()));
            }
        }
        int offset = activeConf.getSectionNumPatterns()[0];
        for(int i=0;i<activeConf.getSectionNumPatterns()[1];i++)
        {
            if(Float.isInfinite(yValues.get(offset+ i).getY()) == false)
            {
                yValues2.add(new Entry(yValues.get(offset + i).getX(),yValues.get(offset+ i).getY()));
            }
        }
        if(slewnum>=3)
        {
            offset = activeConf.getSectionNumPatterns()[0] + activeConf.getSectionNumPatterns()[1];
            for(int i=0;i<activeConf.getSectionNumPatterns()[2];i++)
            {
                if(Float.isInfinite(yValues.get(offset+ i).getY()) == false)
                {
                    yValues3.add(new Entry(yValues.get(offset + i).getX(),yValues.get(offset+ i).getY()));
                }

            }
        }
        if(slewnum>=4)
        {
            offset = activeConf.getSectionNumPatterns()[0] + activeConf.getSectionNumPatterns()[1]+ activeConf.getSectionNumPatterns()[2];
            for(int i=0;i<activeConf.getSectionNumPatterns()[3];i++)
            {
                if(Float.isInfinite(yValues.get(offset+ i).getY()) == false)
                {
                    yValues4.add(new Entry(yValues.get(offset + i).getX(),yValues.get(offset+ i).getY()));
                }
            }
        }
        if(slewnum==5)
        {
            offset = activeConf.getSectionNumPatterns()[0] + activeConf.getSectionNumPatterns()[1]+ activeConf.getSectionNumPatterns()[2]+ activeConf.getSectionNumPatterns()[3];
            for(int i=0;i<activeConf.getSectionNumPatterns()[4];i++)
            {
                if(Float.isInfinite(yValues.get(offset+ i).getY()) == false)
                {
                    yValues5.add(new Entry(yValues.get(offset + i).getX(),yValues.get(offset+ i).getY()));
                }
            }
        }
        // create a dataset and give it a type
        LineDataSet set1 = new LineDataSet(yValues1, "Slew1");
        LineDataSet set2 = new LineDataSet(yValues2, "Slew2");
        LineDataSet set3 = new LineDataSet(yValues3, "Slew3");
        LineDataSet set4 = new LineDataSet(yValues4, "Slew4");
        LineDataSet set5 = new LineDataSet(yValues5, "Slew5");

        // set the line to be drawn like this "- - - - - -"
        set1.enableDashedLine(10f, 5f, 0f);
        set1.enableDashedHighlightLine(10f, 5f, 0f);
        set1.setColor(Color.BLUE);
        set1.setCircleColor(Color.BLUE);
        set1.setLineWidth(1f);
        set1.setCircleSize(2f);
        set1.setDrawCircleHole(false);
        set1.setValueTextSize(9f);
        set1.setFillAlpha(65);
        set1.setFillColor(Color.BLUE);
        set1.setDrawFilled(true);
        set1.setValues(yValues1);

        // set the line to be drawn like this "- - - - - -"
        set2.enableDashedLine(10f, 5f, 0f);
        set2.enableDashedHighlightLine(10f, 5f, 0f);
        set2.setColor(Color.RED);
        set2.setCircleColor(Color.RED);
        set2.setLineWidth(1f);
        set2.setCircleSize(2f);
        set2.setDrawCircleHole(false);
        set2.setValueTextSize(9f);
        set2.setFillAlpha(65);
        set2.setFillColor(Color.RED);
        set2.setDrawFilled(true);
        set2.setValues(yValues2);
        // set the line to be drawn like this "- - - - - -"
        set3.enableDashedLine(10f, 5f, 0f);
        set3.enableDashedHighlightLine(10f, 5f, 0f);
        set3.setColor(Color.GREEN);
        set3.setCircleColor(Color.GREEN);
        set3.setLineWidth(1f);
        set3.setCircleSize(2f);
        set3.setDrawCircleHole(false);
        set3.setValueTextSize(9f);
        set3.setFillAlpha(65);
        set3.setFillColor(Color.GREEN);
        set3.setDrawFilled(true);
        set3.setValues(yValues3);
        // set the line to be drawn like this "- - - - - -"
        set4.enableDashedLine(10f, 5f, 0f);
        set4.enableDashedHighlightLine(10f, 5f, 0f);
        set4.setColor(Color.YELLOW);
        set4.setCircleColor(Color.YELLOW);
        set4.setLineWidth(1f);
        set4.setCircleSize(2f);
        set4.setDrawCircleHole(false);
        set4.setValueTextSize(9f);
        set4.setFillAlpha(65);
        set4.setFillColor(Color.YELLOW);
        set4.setDrawFilled(true);
        set4.setValues(yValues4);

        // set the line to be drawn like this "- - - - - -"
        set5.enableDashedLine(10f, 5f, 0f);
        set5.enableDashedHighlightLine(10f, 5f, 0f);
        set5.setColor(Color.LTGRAY);
        set5.setCircleColor(Color.LTGRAY);
        set5.setLineWidth(1f);
        set5.setCircleSize(2f);
        set5.setDrawCircleHole(false);
        set5.setValueTextSize(9f);
        set5.setFillAlpha(65);
        set5.setFillColor(Color.LTGRAY);
        set5.setDrawFilled(true);
        set5.setValues(yValues5);

        if(slewnum==2)
        {
            LineData data = new LineData(set1, set2);
            mChart.setData(data);
            mChart.setMaxVisibleValueCount(20);
        }
        if(slewnum==3)
        {
            LineData data = new LineData(set1, set2,set3);
            mChart.setData(data);
            mChart.setMaxVisibleValueCount(20);
        }

        if(slewnum==4)
        {
            LineData data = new LineData(set1, set2,set3,set4);
            mChart.setData(data);
            mChart.setMaxVisibleValueCount(20);
        }

        if(slewnum==5)
        {
            LineData data = new LineData(set1, set2,set3,set4,set5);
            mChart.setData(data);
            mChart.setMaxVisibleValueCount(20);
        }
    }
    
    // 图表类型枚举
    public enum ChartType {
        REFLECTANCE,
        ABSORBANCE,
        INTENSITY
    }
    
    // 通用函数
    private Boolean checkValidRepeat()
    {
        try
        {
            int value = Integer.parseInt(et_manual_repead.getText().toString());
            if(value>=1&&value<=50)
            {
                return true;
            }
        }
        catch (NumberFormatException ex)
        {
        }

        return false;
    }
    private Boolean checkValidPga()
    {
        try
        {
            int value = Integer.parseInt(et_manual_pga.getText().toString());
            if(value==1 || value == 2 || value == 4 || value==8 || value==16 || value==32 ||value==64)
            {
                return true;
            }
        }
        catch (NumberFormatException ex)
        {
        }
        return false;
    }

    // 检查QuickSet值（已移除，始终返回true以保持兼容性）
    private Boolean checkQuicksetValue()
    {
        return true;
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
        //change to rawData length by half
        int length = hex.length / 2;
        byte[] rawData = new byte[length];
        for (int i = 0; i < length; i++) {
            //Convert hex data to decimal value
            int high = Character.digit(hex[i * 2], 16);
            int low = Character.digit(hex[i * 2 + 1], 16);
            // 将第一个值的二进制值左移4位，然后与第二个值的二进制值连接
            int value = (high << 4) | low;
            //Complementary with FFFFFFFF
            if (value > 127)
                value -= 256;
            //Finally change to byte
            rawData [i] = (byte) value;
        }
        return rawData;
    }
    
    // 将字节数组转换为字符串
    public static String getBytetoString(byte configName[]) {
        byte[] byteChars = new byte[40];
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] var3 = byteChars;
        int i = byteChars.length;
        for(int var5 = 0; var5 < i; ++var5) {
            byte b = var3[var5];
            byteChars[b] = 0;
        }
        String s = null;
        for(i = 0; i < configName.length; ++i) {
            byteChars[i] = configName[i];
            if(configName[i] == 0) {
                break;
            }
            os.write(configName[i]);
        }
        try {
            s = new String(os.toByteArray(), "UTF-8");
        } catch (UnsupportedEncodingException var7) {
            var7.printStackTrace();
        }
        return s;
    }
    
    // 对话框相关
    private void Dialog_Pane_Finish(String title,String content)
    {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        alertDialogBuilder.setTitle(title);
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setMessage(content);
        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                alertDialog.dismiss();
                finish();
            }
        });
        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
    private void Dialog_Pane_maintain(String title,String content)
    {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        alertDialogBuilder.setTitle(title);
        alertDialogBuilder.setMessage(content);

        alertDialogBuilder.setNegativeButton(getResources().getString(R.string.yes_i_know), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                calProgress.setVisibility(View.VISIBLE);
                SetReferenceParameter();
                alertDialog.dismiss();
            }
        });
        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                alertDialog.dismiss();
            }
        });
        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void ReferenceConfigSaveSuccess() {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        alertDialogBuilder.setTitle("Finish");
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setMessage("Complete save reference config, start scan");
        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                PerformScan(0);
                DisableAllComponent();
                calProgress.setVisibility(View.VISIBLE);
                btn_scan.setText(getString(R.string.scanning));
                alertDialog.dismiss();
            }
        });
        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
    private void NotValidValueDialog(String title,String content) {
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
    
    private void Dialog_Pane_OpenFunction(String title, String content)
    {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        alertDialogBuilder.setTitle(title);
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setMessage(content);

        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                alertDialog.dismiss();
                openFunction();
            }
        });
        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
    private void Dialog_Pane_OldTIVA(String title,String content)
    {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        alertDialogBuilder.setTitle(title);
        alertDialogBuilder.setMessage(content);

        alertDialogBuilder.setNegativeButton(getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                alertDialog.dismiss();
                NotValidValueDialog("Limited Functions","Running with older Tiva firmware\nis not recommended and functions\nwill be limited!");
            }
        });
        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                alertDialog.dismiss();
                finish();
            }
        });
        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
    
    // 显示对话框，告知用户Nano未连接
    private void notConnectedDialog() {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        alertDialogBuilder.setTitle("设备未连接");
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setMessage("未找到目标设备 " + TARGET_DEVICE_NAME + " <" + TARGET_DEVICE_MAC_PART + ">。请确保设备已开机并在范围内。");

        alertDialogBuilder.setPositiveButton("重试", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                alertDialog.dismiss();
                updateConnectionStatus("扫描中...");
                scanTargetDevice(true);
            }
        });
        
        alertDialogBuilder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                alertDialog.dismiss();
            }
        });

        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
    
    // 更新连接状态显示
    private void updateConnectionStatus(String status) {
        if (tv_connection_status != null) {
            tv_connection_status.setText(status);
            // 更新状态颜色
            if (status.contains("已连接")) {
                tv_connection_status.setTextColor(Color.GREEN);
            } else if (status.contains("连接中") || status.contains("扫描中")) {
                tv_connection_status.setTextColor(Color.BLUE);
            } else if (status.contains("未连接") || status.contains("失败")) {
                tv_connection_status.setTextColor(Color.RED);
            } else {
                tv_connection_status.setTextColor(Color.BLACK);
            }
        }
    }
    
    // 连接按钮点击事件
    private void onConnectButtonClick() {
        if (!connected) {
            updateConnectionStatus("扫描中...");
            scanTargetDevice(true);
        }
    }
    
    // 灯控制设置菜单
    // 显示灯设置菜单
    private void showLampSettingsMenu() {
        if (!connected) {
            Toast.makeText(this, "请先连接设备", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create dialog view programmatically
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);
        
        // Warmup time input
        TextView tv_warmup_label = new TextView(this);
        tv_warmup_label.setText("预热时间（秒）:");
        layout.addView(tv_warmup_label);
        
        final EditText et_warmup_time = new EditText(this);
        et_warmup_time.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        et_warmup_time.setText(String.valueOf(warmupTimeSeconds));
        et_warmup_time.setHint("输入预热时间");
        layout.addView(et_warmup_time);
        
        // Lamp mode spinner
        TextView tv_mode_label = new TextView(this);
        tv_mode_label.setText("灯模式:");
        tv_mode_label.setPadding(0, 20, 0, 10);
        layout.addView(tv_mode_label);
        
        final Spinner spinner_lamp_mode = new Spinner(this);
        String[] modes = {"打开", "关闭", "自动"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, modes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_lamp_mode.setAdapter(adapter);
        spinner_lamp_mode.setSelection(currentLampMode.ordinal());
        layout.addView(spinner_lamp_mode);
        
        // Start warmup button
        Button btn_start_warmup = new Button(this);
        btn_start_warmup.setText("开始预热");
        btn_start_warmup.setPadding(0, 20, 0, 0);
        layout.addView(btn_start_warmup);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("灯控制设置");
        builder.setView(layout);
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    warmupTimeSeconds = Integer.parseInt(et_warmup_time.getText().toString());
                    currentLampMode = LampMode.values()[spinner_lamp_mode.getSelectedItemPosition()];
                    applyLampMode();
                } catch (NumberFormatException e) {
                    Toast.makeText(ScanViewActivity.this, "预热时间必须是数字", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("取消", null);
        
        final AlertDialog dialog = builder.create();
        
        btn_start_warmup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    int time = Integer.parseInt(et_warmup_time.getText().toString());
                    if (time > 0) {
                        startWarmup(time);
                        dialog.dismiss();
                    } else {
                        Toast.makeText(ScanViewActivity.this, "预热时间必须大于0", Toast.LENGTH_SHORT).show();
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(ScanViewActivity.this, "预热时间必须是数字", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        dialog.show();
    }
    
    /**
     * 应用灯模式
     */
    private void applyLampMode() {
        if (!connected) return;
        
        switch (currentLampMode) {
            case ON:
                ISCNIRScanSDK.ControlLamp(ISCNIRScanSDK.LampState.ON);
                break;
            case OFF:
                ISCNIRScanSDK.ControlLamp(ISCNIRScanSDK.LampState.OFF);
                break;
            case AUTO:
                ISCNIRScanSDK.ControlLamp(ISCNIRScanSDK.LampState.AUTO);
                break;
        }
    }
    
    /**
     * 开始预热
     */
    private void startWarmup(int seconds) {
        if (!connected) {
            Toast.makeText(this, "请先连接设备", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (isWarmupInProgress) {
            Toast.makeText(this, "预热正在进行中", Toast.LENGTH_SHORT).show();
            return;
        }
        
        isWarmupInProgress = true;
        warmupTimeSeconds = seconds;
        
        // Turn on lamp
        ISCNIRScanSDK.ControlLamp(ISCNIRScanSDK.LampState.ON);
        
        // Disable UI interactions
        setActivityTouchDisable(true);
        if (btn_scan != null) {
            btn_scan.setEnabled(false);
        }
        if (btn_lamp_settings != null) {
            btn_lamp_settings.setEnabled(false);
        }
        
        // Show countdown
        if (tv_warmup_countdown != null) {
            tv_warmup_countdown.setVisibility(View.VISIBLE);
        }
        
        // Start countdown
        final int[] remainingSeconds = {seconds};
        warmupCountdownRunnable = new Runnable() {
            @Override
            public void run() {
                if (remainingSeconds[0] > 0) {
                    if (tv_warmup_countdown != null) {
                        tv_warmup_countdown.setText("预热中: " + remainingSeconds[0] + "秒");
                    }
                    remainingSeconds[0]--;
                    warmupHandler.postDelayed(this, 1000);
                } else {
                    // Warmup complete
                    isWarmupInProgress = false;
                    if (tv_warmup_countdown != null) {
                        tv_warmup_countdown.setVisibility(View.GONE);
                    }
                    
                    // Re-enable UI interactions
                    setActivityTouchDisable(false);
                    if (btn_scan != null && connected) {
                        btn_scan.setEnabled(true);
                    }
                    if (btn_lamp_settings != null) {
                        btn_lamp_settings.setEnabled(true);
                    }
                    
                    Toast.makeText(ScanViewActivity.this, "预热完成", Toast.LENGTH_SHORT).show();
                }
            }
        };
        warmupHandler.post(warmupCountdownRunnable);
    }

    Boolean saveReference = false;
    private void SaveReferenceDialog() {
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        alertDialogBuilder.setTitle("Finish");
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setMessage("Replace Factory Reference is complete.\nShould reconnect bluetooth to reload reference.");

        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                alertDialog.dismiss();
                storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.ReferenceScan, "ReferenceScan");
                ISCNIRScanSDK.ScanConfig(ActiveConfigByte,ISCNIRScanSDK.ScanConfig.SET);
                saveReference = true;
            }
        });

        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
    
    /**
     * 返回此页面时应获取活动配置索引
     */
    private void GetActiveConfigOnResume()
    {
        ScanConfigList_from_ScanConfiguration = ScanConfigurationsViewActivity.bufconfigs;//from scan configuration
        ScanConfig_Byte_List_from_ScanConfiuration = ScanConfigurationsViewActivity.bufEXTRADATA_fromScanConfigurationsViewActivity;
        int storenum = ScanConfigList_from_ScanConfiguration.size();
        if(storenum !=0)
        {
            if(storenum!=ScanConfigList.size())
            {
                ScanConfigList.clear();
                ScanConfig_Byte_List.clear();
                for(int i=0;i<ScanConfigList_from_ScanConfiguration.size();i++)
                {
                    ScanConfigList.add(ScanConfigList_from_ScanConfiguration.get(i));
                    ScanConfig_Byte_List.add(ScanConfig_Byte_List_from_ScanConfiuration.get(i));
                }
            }
            for(int i=0;i<ScanConfigList.size();i++)
            {
                int ScanConfigIndextoByte = (byte)ScanConfigList.get(i).getScanConfigIndex();
                if(ActiveConfigindex == ScanConfigIndextoByte )
                {
                    activeConf = ScanConfigList.get(i);
                    ActiveConfigByte = ScanConfig_Byte_List.get(i);
                    tv_manual_scan_conf.setText(activeConf.getConfigName());
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        GotoOtherPage = false;
        numSections=0;
        // 首次恢复时初始化图表数据数组
        if(!chartDataInitialized)
        {
            chartDataInitialized = true;
            // 初始化图表显示
            updateChartDisplay();

            mXValues = new ArrayList<>();
            mIntensityFloat = new ArrayList<>();
            mAbsorbanceFloat = new ArrayList<>();
            mReflectanceFloat = new ArrayList<>();
            mWavelengthFloat = new ArrayList<>();
            mReferenceFloat = new ArrayList<>();
        }
        else
        {
            if((!isExtendVer_PLUS && !isExtendVer && fw_level_standard.compareTo(ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_0)==0) || isOldTiva)
                closeFunction();
            else if(getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.Activacatestatus, null).contains("Activated"))
            {
                if(!showActiveconfigpage)
                    openFunction();
            }
            else
                closeFunction();
        }
        if(!showActiveconfigpage)
        {
            LocalBroadcastManager.getInstance(mContext).registerReceiver(ScanConfSizeReceiver, new IntentFilter(ISCNIRScanSDK.SCAN_CONF_SIZE));
            LocalBroadcastManager.getInstance(mContext).registerReceiver(GetActiveScanConfReceiver, new IntentFilter(ISCNIRScanSDK.SEND_ACTIVE_CONF));
            LocalBroadcastManager.getInstance(mContext).registerReceiver(WriteScanConfigStatusReceiver, WriteScanConfigStatusFilter);
            LocalBroadcastManager.getInstance(mContext).registerReceiver(GetDeviceStatusReceiver,new IntentFilter(ISCNIRScanSDK.ACTION_STATUS));

        }
        // 从活动页面返回此页面时，不执行任何操作，不初始化扫描配置文本
        if(showActiveconfigpage)
        {
            showActiveconfigpage = false;
        }
        // 首次连接
        else
        {
            if (tv_manual_scan_conf != null) {
                tv_manual_scan_conf.setText(getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.scanConfiguration, "Column 1"));
            }
        }
        if(!GotoScanConfigFlag && activeConf != null)
        {
            if (tv_manual_scan_conf != null) {
                tv_manual_scan_conf.setText(activeConf.getConfigName());
            }
        }
        else if(GotoScanConfigFlag)
        {
            ISCNIRScanSDK.GetActiveConfig();
        }
    }
    /*
     * When the activity is destroyed, unregister all broadcast receivers, remove handler callbacks,
     * and store all user preferences
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        ISCNIRScanSDK.ControlPhysicalButton(ISCNIRScanSDK.PhysicalButton.Unlock);
        ChangeLampState();
        try {
            Thread.sleep(200);
        }
        catch (Exception e)
        {

        }
        unbindService(mServiceConnection);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(ScanDataReadyReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(RefDataReadyReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(NotifyCompleteReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(RefCoeffDataProgressReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(CalMatrixDataProgressReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(DisconnReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(ScanConfReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(ScanConfSizeReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(GetActiveScanConfReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(SpectrumCalCoefficientsReadyReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(RetrunReadActivateStatusReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(RetrunActivateStatusReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(ReturnCurrentScanConfigurationDataReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(WriteScanConfigStatusReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(DeviceInfoReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(GetUUIDReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(GetDeviceStatusReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(ReturnLampRampUpADCReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(ReturnLampADCAverageReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(ReturnLampRampUpADCTimeStampReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(ReturnMFGNumReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(ReturnHWModelReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(ReturnSetLampReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(ReturnSetPGAReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(ReturnSetScanRepeatsReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(GetPGAReceiver);
        mHandler.removeCallbacksAndMessages(null);
    }
    @Override
    public void onPause() {
        super.onPause();
        //back to desktop,should disconnect to device
        if(!GotoOtherPage)
            finish();
    }
    private class  BackGroundReciver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
           finish();
        }
    }

    /**
     * 处理断开连接事件的广播接收器
     * 如果Nano断开连接，将显示提示消息，允许用户重新连接
     */
    public class DisconnReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            connected = false;
            updateConnectionStatus("已断开");
            Toast.makeText(mContext, R.string.nano_disconnected, Toast.LENGTH_SHORT).show();
            // 不结束Activity，允许用户重新连接
            if (btn_connect_device != null) {
                btn_connect_device.setEnabled(true);
            }
        }
    }
    
    /**
     * 设置Activity触摸禁用状态
     */
    private void setActivityTouchDisable(boolean value) {
        if (value) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        }
    }
    
    /**
     * 显示最大模式数（已移除，为保持兼容性保留但无操作）
     */
    private void UI_ShowMaxPattern()
    {
    }
    
    /**
     * 设置设备物理按钮状态
     */
    private void SetDeviceButtonStatus()
    {
        if(isExtendVer_PLUS || isExtendVer || (!isExtendVer_PLUS && !isExtendVer && fw_level_standard.compareTo(ISCNIRScanSDK.FW_LEVEL_STANDARD.LEVEL_1)>0))
        {
            Boolean isLockButton = getBooleanPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.LockButton,false);
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
    /**
     * 获取用户可以设置的最大模式数
     */
    private int GetMaxPattern(int start_nm, int end_nm, int width_index, int num_repeat, int scan_type, int IsEXTver)
    {
        return ISCNIRScanSDK.GetMaxPatternJNI(scan_type,start_nm,end_nm,width_index,num_repeat,SpectrumCalCoefficients,IsEXTver);
    }
    /**
     * 将扫描配置转换为字节数组（已移除，为保持兼容性返回null）
     */
    public byte[] ChangeScanConfigToByte()
    {
        return null;
    }
    /**
     * 比较设备的配置和用户设置是否相同
     * @param EXTRA_DATA 扫描配置字节数据
     */
    public Boolean Compareconfig(byte EXTRA_DATA[])
    {
        if(EXTRA_DATA.length != 155)
        {
            return false;
        }
        String model = "";
        if(!model_name.isEmpty() && model_name != null)
        {
            String[] SplitModel = model_name.split("-");
            if(SplitModel.length > 0)
                model = SplitModel[SplitModel.length - 1];
        }
        ISCNIRScanSDK.ScanConfiguration config = ISCNIRScanSDK.current_scanConf;
        // 检查参考设置
        if(reference_set_config)
        {
            reference_set_config = false;
            if(model.equals("R11"))
            {
                if(config.getSectionScanType()[0]!= (byte)1)
                    return false;
                if(config.getSectionWidthPx()[0]!=(byte)9)
                    return false;
                if(config.getSectionNumPatterns()[0]!=160)
                    return false;
                if(config.getSectionNumRepeats()[0]!=12)
                    return false;
                if( config.getSectionExposureTime()[0]!=0)
                    return false;
            }
            else if(model.equals("R13"))
            {
                if(config.getSectionScanType()[0]!= (byte)1)
                    return false;
                if(config.getSectionWidthPx()[0]!=(byte)9)
                    return false;
                if(config.getSectionNumPatterns()[0]!=228)
                    return false;
                if(config.getSectionNumRepeats()[0]!=12)
                    return false;
                if( config.getSectionExposureTime()[0]!=1)
                    return false;
            }
            else if(model.equals("T11") || model.equals("F13"))
            {
                if(config.getSectionScanType()[0]!= (byte)0)
                    return false;
                if(config.getSectionWidthPx()[0]!=(byte)9)
                    return false;
                if(config.getSectionNumPatterns()[0]!=228)
                    return false;
                if(config.getSectionNumRepeats()[0]!=30)
                    return false;
                if( config.getSectionExposureTime()[0]!=0)
                    return false;
            }
            else
            {
                if(config.getSectionScanType()[0]!= (byte)0)
                    return false;
                if(config.getSectionWidthPx()[0]!=(byte)6)
                    return false;
                if(config.getSectionNumPatterns()[0]!=228)
                    return false;
                if(config.getSectionNumRepeats()[0]!=30)
                    return false;
                if( config.getSectionExposureTime()[0]!=0)
                    return false;
            }
        }
        else if(saveReference)//after save reference, should set active config and compare
        {
            if(config.getSectionScanType()[0]!= activeConf.getSectionScanType()[0])
                return false;
            if(config.getSectionWavelengthStartNm()[0]!=activeConf.getSectionWavelengthStartNm()[0])
                return false;
            if(config.getSectionWavelengthEndNm()[0]!=activeConf.getSectionWavelengthEndNm()[0])
                return false;
            if(activeConf.getSectionWidthPx()[0] != config.getSectionWidthPx()[0])
                return false;
            if(config.getSectionNumPatterns()[0]!=activeConf.getSectionNumPatterns()[0])
                return false;
            if(config.getSectionNumRepeats()[0]!=activeConf.getSectionNumRepeats()[0])
                return false;
            if( config.getSectionExposureTime()[0]!=activeConf.getSectionExposureTime()[0])
                return false;
        }
        else
        {
            // 仅使用手动模式，始终返回true以保持兼容性
            return true;
        }
        return true;
    }
    public void SetReferenceParameter()
    {
        reference_set_config = true;
        ISCNIRScanSDK.SetReferenceParameter(MINWAV,MAXWAV);
    }
    /**
     * 对设备执行扫描
     * @param delaytime 设置延迟时间以避免BLE挂起
     */
    private void PerformScan(long delaytime)
    {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable(){

            @Override
            public void run() {
                //Send broadcast START_SCAN will trigger to scan data
                ISCNIRScanSDK.StartScan();
            }}, delaytime);
    }
    
    // 接收器
    /**
     * 成功设置灯状态（需要调用ISCNIRScanSDK.LampState）
     */
    public class ReturnSetLampReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            //Complete set lamp on,off,auto
            switch (Lamp_Info)
            {
                case ManualLamp:
                    break;
                case WarmDevice:
                    Lamp_Info = LampInfo.ManualLamp;
                    Boolean reference = false;
                    if(getStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.ReferenceScan, "Not").equals("ReferenceScan"))
                        reference = true;
                    if(reference == true)
                        storeStringPref(mContext, ISCNIRScanSDK.SharedPreferencesKeys.ReferenceScan, "Not");
                    //Synchronize time and download calibration coefficient and calibration matrix
                    ISCNIRScanSDK.SetCurrentTime();
                    break;
                case CloseWarmUpLampInScan:
                    WarmUp = false;
                    ISCNIRScanSDK.GetDeviceStatus();
                    break;
            }
        }
    }
    /**
     * 成功设置PGA（需要调用ISCNIRScanSDK.SetPGA）
     */
    public class ReturnSetPGAReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            //Complete set pga
        }
    }
    /**
     * 成功设置扫描重复次数（需要调用ISCNIRScanSDK.setScanAverage）
     */
    public class ReturnSetScanRepeatsReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            // 完成设置扫描重复次数
        }
    }
}
