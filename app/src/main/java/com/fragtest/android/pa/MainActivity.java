package com.fragtest.android.pa;

import android.Manifest;
import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.UserManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.fragtest.android.pa.AppStates.AppState;
import com.fragtest.android.pa.AppStates.StateCharging;
import com.fragtest.android.pa.AppStates.StateConnecting;
import com.fragtest.android.pa.AppStates.StateError;
import com.fragtest.android.pa.AppStates.StateProposing;
import com.fragtest.android.pa.AppStates.StateQuest;
import com.fragtest.android.pa.AppStates.StateRunning;
import com.fragtest.android.pa.Core.FileIO;
import com.fragtest.android.pa.Core.LogIHAB;
import com.fragtest.android.pa.Core.MessageList;
import com.fragtest.android.pa.DataTypes.INPUT_CONFIG;
import com.fragtest.android.pa.Questionnaire.QuestionnairePagerAdapter;

import org.pmw.tinylog.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;


public class MainActivity extends AppCompatActivity {

    private boolean USE_KIOSK_MODE = true;
    public static boolean USE_DEVELOPER_MODE = false;
    //private Locale LANGUAGE_CODE = Locale.GERMANY;
    private Locale LANGUAGE_CODE = Locale.ENGLISH;

    static final String LOG = "MainActivity";
    private static final String KEY_PREFS_IN_FOREGROUND = "prefsInForeGround";

    // RELEVANT FOR PERMISSIONS (Android 6+, just in case)
    private int requestIterator = 0;
    private boolean permissionGranted = false;
    private int nPermissions = 8;
    private int iPermission;
    private String[] requestString;
    public final static int MY_PERMISSIONS_READ_EXTERNAL_STORAGE = 0;
    public final static int MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 1;
    public final static int MY_PERMISSIONS_RECEIVE_BOOT_COMPLETED = 2;
    public final static int MY_PERMISSIONS_RECORD_AUDIO = 3;
    public final static int MY_PERMISSIONS_VIBRATE = 4;
    public final static int MY_PERMISSIONS_WAKE_LOCK = 5;
    public final static int MY_PERMISSIONS_DISABLE_KEYGUARD = 6;
    public final static int MY_PERMISSIONS_CAMERA = 7;
    public final static int MY_PERMISSIONS_USB = 8;
    public final static int MY_PERMISSIONS_WRITE_SETTINGS = 9;

    final Messenger mMessageHandler = new Messenger(new MessageHandler());
    private Messenger mServiceMessenger;
    public Handler mTaskHandler = new Handler();
    private MessageList mMessageList;

    public ViewPager mViewPager;
    private QuestionnairePagerAdapter mAdapter;
    public TextView mLogo;
    public View mRecord, mArrowBack, mArrowForward, mRevert, mProgress, mRegress, mConfig,
            mBatteryReg, mBatteryProg, mCharging;

    private boolean mServiceIsBound;
    private boolean mServiceIsRecording;
    private boolean isPrefsInForeGround = false;
    private boolean isActivityRunning = false;
    private boolean isQuestionnairePresent = true;
    //private boolean isCharging = false;
    //private boolean isTimer = false;
    private boolean showConfigButton = false;
    private boolean showRecordingButton = true;
    private boolean isBTPresent = false;
    private boolean isUSBPresent = false;

    // RELEVANT FOR KIOSK MODE
    private FileIO mFileIO;
    private ComponentName mAdminComponentName;
    private DevicePolicyManager mDevicePolicyManager;
    private final List blockedKeys = new ArrayList(Arrays.asList(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_UP));

    private ContentResolver mResolver;
    private Window mWindow;
    private int mBrightness;

    // Forced Answers (no answer no swipe)
    private int falseSwipes = 0;
    private int mCurrentItemBeforeMessage;
    private static boolean bRecordSwipes = true;

    // Preferences
    private SharedPreferences sharedPreferences;

    // States
    public AppState mAppState;
    private StateCharging mStateCharging;
    private StateConnecting mStateConnecting;
    private StateError mStateError;
    private StateProposing mStateProposing;
    private StateQuest mStateQuest;
    private StateRunning mStateRunning;

    // Context
    private static Context mStatContext;

    public static Context getContext() {
        return mStatContext;
    }

    public INPUT_CONFIG mServiceState;

    // Errors
    public enum AppErrors {
        ERROR_NO_BT, ERROR_BATT_LOW, ERROR_BATT_CRITICAL, ERROR_NO_QUEST, ERROR_NO_USB;

        public String getErrorMessage() {
            switch (this) {
                case ERROR_NO_BT:
                    return mStatContext.getResources().getString(R.string.noBluetooth);
                case ERROR_BATT_LOW:
                    return mStatContext.getResources().getString(R.string.batteryWarning);
                case ERROR_BATT_CRITICAL:
                    return mStatContext.getResources().getString(R.string.batteryCritical);
                case ERROR_NO_QUEST:
                    return mStatContext.getResources().getString(R.string.noQuestionnaires);
                case ERROR_NO_USB:
                    return mStatContext.getResources().getString(R.string.noUSB);
                default:
                    return "generic error message";
            }
        }
    }

    public ArrayList<String> mErrorList = new ArrayList<>();

    public void addError(AppErrors error) {
        if (!mErrorList.contains(error.getErrorMessage())) {
            // In case of Standalone Mode, no BT error is needed
            if (!(ControlService.INPUT == INPUT_CONFIG.STANDALONE && error == AppErrors.ERROR_NO_BT)) {
                mErrorList.add(error.getErrorMessage());
                LogIHAB.log("Error added: " + error.getErrorMessage());
            }
        }
    }

    public void removeError(AppErrors error) {
        if (mErrorList.contains(error.getErrorMessage())) {
            mErrorList.remove(error.getErrorMessage());
            LogIHAB.log("Error removed: " + error.getErrorMessage());
        }
    }

    // Battery Status Receiver
    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctxt, Intent intent) {
            int tempPlugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            boolean plugged = tempPlugged == BatteryManager.BATTERY_PLUGGED_USB ||
                    tempPlugged == BatteryManager.BATTERY_PLUGGED_AC;

            // only announce on change
            if (plugged && !ControlService.isCharging) {
                mAppState.chargeOn();
                // a change towards charging
                messageService(ControlService.MSG_CHARGING_ON);
            } else if (!plugged && ControlService.isCharging) {
                mAppState.chargeOff();
                // a change towards not charging
                messageService(ControlService.MSG_CHARGING_OFF);
            }
            ControlService.isCharging = plugged;
            mAdapter.setIsCharging(plugged);

        }
    };


    public void setUSBPresent(boolean isPresent) {
        isUSBPresent = isPresent;
    }

    public boolean getIsUSBPresent() {
        return isUSBPresent;
    }

    public boolean getIsBTPresent() {
        return isBTPresent;
    }

    public void setIsBTPresent(boolean isPresent) {
        isBTPresent = isPresent;
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            Log.e(LOG, "ON SERVICE CONNECTED");

            mServiceMessenger = new Messenger(service);

            //TODO: Experimental - Grant permission and suppress "Bluetooth Permission" - problem
            //mDevicePolicyManager.setPermissionPolicy(mAdminComponentName, DevicePolicyManager.PERMISSION_POLICY_AUTO_GRANT);

            IntentFilter batteryFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = registerReceiver(null, batteryFilter);

            // Are we plugged in?
            int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            ControlService.isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_PLUGGED_USB;
            if (ControlService.isCharging) {
                messageService(ControlService.MSG_CHARGING_ON_PRE);
                mAppState.chargeOn();
            }

            messageService(ControlService.MSG_REGISTER_CLIENT);
            messageService(ControlService.MSG_GET_STATUS);

            LogIHAB.log("Processing message list of length: " + mMessageList.getLength());
            mMessageList.work();

            //mAppState.usbNotPresent();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mServiceMessenger = null;
        }
    };

    @SuppressWarnings("deprecation")
    private void setSystemLocale() {
        Resources resources = getResources();
        Configuration configuration = resources.getConfiguration();
        DisplayMetrics displayMetrics = resources.getDisplayMetrics();

        // Automatically set to specific language
        /*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1){
            configuration.setLocale(LANGUAGE_CODE);
        } else{
            configuration.locale = LANGUAGE_CODE;
        }
        */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            getApplicationContext().createConfigurationContext(configuration);
        } else {
            resources.updateConfiguration(configuration, displayMetrics);
        }
    }

    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service :
                manager.getRunningServices(Integer.MAX_VALUE)) {
            if (ControlService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    void doBindService() {
        if (!isServiceRunning() && permissionGranted) {
            startService(new Intent(this, ControlService.class));
        }
        if (permissionGranted) {
            bindService(new Intent(this, ControlService.class),
                    mConnection, Context.BIND_AUTO_CREATE);
            mServiceIsBound = true;
        }
    }

    void doUnbindService() {
        if (mServiceIsBound) {
            messageService(ControlService.MSG_UNREGISTER_CLIENT);
            unbindService(mConnection);
            mServiceIsBound = false;
        }
    }

    public void handleNewPagerAdapter() {
        mViewPager = null;
        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        mAdapter = new QuestionnairePagerAdapter(this, mViewPager, USE_KIOSK_MODE);
        mViewPager.setAdapter(mAdapter);
        mViewPager.setCurrentItem(0);
        mViewPager.addOnPageChangeListener(myOnPageChangeListener);
    }

    private void shipPreferencesToControlService() {
        // Load information from shared preferences and bundle them
        Bundle dataPreferences = new Bundle();
        // String
        dataPreferences.putString("whichQuest", sharedPreferences.getString("whichQuest", ""));
        dataPreferences.putString("samplerate", sharedPreferences.getString("samplerate", "" + InitValues.samplerate));
        dataPreferences.putString("chunklengthInS", sharedPreferences.getString("chunklengthInS", "" + InitValues.chunklengthInS));
        dataPreferences.putString("filterHpFrequency", sharedPreferences.getString("filterHpFrequency", "" + InitValues.filterHpFrequency));
        dataPreferences.putString("operationMode", sharedPreferences.getString("operationMode", "" + InitValues.operationMode));
        // Boolean
        dataPreferences.putBoolean("isWave", sharedPreferences.getBoolean("isWave", InitValues.isWave));
        //dataPreferences.putBoolean("isTimer", sharedPreferences.getBoolean("isTimer", InitValues.isTimer));
        //dataPreferences.putBoolean("isLocked", sharedPreferences.getBoolean("isLocked", InitValues.isLocked));
        dataPreferences.putBoolean("keepAudioCache", sharedPreferences.getBoolean("keepAudioCache", InitValues.keepAudioCache));
        dataPreferences.putBoolean("downsample", sharedPreferences.getBoolean("downsample", InitValues.downsample));
        dataPreferences.putBoolean("showConfigButton", sharedPreferences.getBoolean("showConfigButton", InitValues.showConfigButton));
        dataPreferences.putBoolean("showRecordingButton", sharedPreferences.getBoolean("showRecordingButton", InitValues.showRecordingButton));
        dataPreferences.putBoolean("filterHp", sharedPreferences.getBoolean("filterHp", InitValues.filterHp));
        // String Set
        Set<String> activeFeatures = sharedPreferences.getStringSet("features", null);
        // String Set cannot be bundled natively, cast to ArrayList
        ArrayList<String> listActiveFeatures = new ArrayList<>();
        listActiveFeatures.addAll(activeFeatures);
        dataPreferences.putStringArrayList("features", listActiveFeatures);

        messageService(ControlService.MSG_CHECK_FOR_PREFERENCES, dataPreferences);
    }

    public String getVersion() {
        try {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "";
        }
    }
    private ViewPager.OnPageChangeListener myOnPageChangeListener =
            new ViewPager.OnPageChangeListener() {

                @Override
                public void onPageScrollStateChanged(int state) {
                }

                @Override
                public void onPageScrolled(int position,
                                           float positionOffset, int positionOffsetPixels) {
                }

                @Override
                public void onPageSelected(int position) {
                    // If question has not been answered, no forward swiping is allowed
                    if (mAdapter.getHasQuestionBeenAnswered()) {
                        mAdapter.setQuestionnaireProgressBar(position);
                        mAdapter.setArrows(position);
                        mViewPager.setCurrentItem(position, true);
                    } else {
                        mAdapter.setQuestionnaireProgressBar(position - 1);
                        mAdapter.setArrows(position - 1);
                        mViewPager.setCurrentItem(position - 1, true);
                        if (bRecordSwipes) {
                            falseSwipes += 1;
                        }
                        Log.e(LOG, "False Swipes: " + falseSwipes);
                        if (bRecordSwipes && falseSwipes > 2) {
                            messageFalseSwipes();
                        }
                    }
                }
            };

    public void incrementPage() {
        mViewPager.setCurrentItem(mViewPager.getCurrentItem() + 1, true);
    }

    public static void stopRecordingFalseSwipes() {
        bRecordSwipes = false;
    }

    public static void startRecordingFalseSwipes() {
        bRecordSwipes = true;
    }

    private void messageFalseSwipes() {
        stopRecordingFalseSwipes();
        falseSwipes = 0;
        //int currentItemBeforeMessage = mViewPager.getCurrentItem();
        //mAdapter.createSwipeMessage(currentItemBeforeMessage);
        startActivity(new Intent(MainActivity.this, SwipingActivity.class));
        //overridePendingTransition(R.xml.enter_anim, R.xml.exit_anim);
    }


    /**
     * State Affairs
     **/


    public void setState(AppState newAppState) {
        mAppState = newAppState;
    }

    public AppState getStateCharging() {
        return mStateCharging;
    }

    public AppState getStateConnecting() {
        return mStateConnecting;
    }

    public AppState getStateError() {
        return mStateError;
    }

    public AppState getStateProposing() {
        return mStateProposing;
    }

    public AppState getStateQuest() {
        return mStateQuest;
    }

    public AppState getStateRunning() {
        return mStateRunning;
    }


    /**
     * QPA Modifiers
     **/


    public void finishQuestionnaire() {
        mAppState.finishQuest();
        messageService(ControlService.MSG_QUESTIONNAIRE_FINISHED);
    }

    public void setLogoActive() {

        switch (mServiceState) {

            case A2DP:
                mRecord.setBackgroundTintList(
                        ColorStateList.valueOf(ResourcesCompat.getColor(getResources(),
                                R.color.BatteryGreen, null)));
                break;

            case RFCOMM:
                mRecord.setBackgroundTintList(
                        ColorStateList.valueOf(ResourcesCompat.getColor(getResources(),
                                R.color.BatteryGreen, null)));
                break;

            case USB:
                mRecord.setBackgroundTintList(
                        ColorStateList.valueOf(ResourcesCompat.getColor(getResources(),
                                R.color.JadeRed, null)));
                break;

            case STANDALONE:
                mRecord.setBackgroundTintList(
                        ColorStateList.valueOf(ResourcesCompat.getColor(getResources(),
                                R.color.AirplaneBlue, null)));
                break;
        }
    }

    public void setLogoInactive() {
        mRecord.setBackgroundTintList(
                ColorStateList.valueOf(ResourcesCompat.getColor(getResources(),
                        R.color.darkerGray, null)));
    }


/*
    public void setBTLogoConnected() {
        if (ControlService.INPUT != INPUT_CONFIG.STANDALONE) {
            mRecord.setBackgroundTintList(
                    ColorStateList.valueOf(ResourcesCompat.getColor(getResources(),
                            R.color.BatteryGreen, null)));
        } else {
            setBTLogoAirplaneMode();
        }
    }

    public void setBTLogoDisconnected() {
        if (ControlService.INPUT != INPUT_CONFIG.STANDALONE) {
            mRecord.setBackgroundTintList(
                    ColorStateList.valueOf(ResourcesCompat.getColor(getResources(),
                            R.color.darkerGray, null)));
        } else {
            setBTLogoAirplaneMode();
        }
    }

    public void setBTLogoAirplaneMode() {
        mRecord.setBackgroundTintList(
                ColorStateList.valueOf(ResourcesCompat.getColor(getResources(),
                        R.color.AirplaneBlue, null)));
    }

    public void setBTLogoUSB() {
        mRecord.setBackgroundTintList(
                ColorStateList.valueOf(ResourcesCompat.getColor(getResources(),
                        R.color.JadeRed, null)));
    }
    */

    private void setConfigVisibility() {
        if (showConfigButton) {
            mConfig.setVisibility(View.VISIBLE);
        } else {
            mConfig.setVisibility(View.GONE);
        }
    }


    /**
     * Lifecycle methods
     */


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        mStatContext = this;
        mMessageList = new MessageList(this);

        setSystemLocale();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (!isActivityRunning) {
            super.onCreate(savedInstanceState);

            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().requestFeature(Window.FEATURE_NO_TITLE);

            for (iPermission = 0; iPermission < nPermissions; iPermission++) {
                requestPermissions(iPermission);
            }

            setContentView(R.layout.activity_main);
            mLogo = (TextView) findViewById(R.id.Action_Logo);
            mRecord = findViewById(R.id.Action_Record);
            mArrowBack = findViewById(R.id.Action_Back);
            mArrowForward = findViewById(R.id.Action_Forward);
            mRevert = findViewById(R.id.Action_Revert);
            mProgress = findViewById(R.id.progress);
            mRegress = findViewById(R.id.regress);
            mConfig = findViewById(R.id.Action_Config);
            mBatteryProg = findViewById(R.id.battery_prog);
            mBatteryReg = findViewById(R.id.battery_reg);
            mCharging = findViewById(R.id.charging);

            mFileIO = new FileIO();
            showConfigButton = mFileIO.checkConfigFile();
            if (showConfigButton) {
                USE_KIOSK_MODE = false;
                USE_DEVELOPER_MODE = true;
            }
            setConfigVisibility();

            mConfig.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(MainActivity.this, PreferencesActivity.class));
                    isPrefsInForeGround = true;
                    mAdapter.setPrefsInForeGround(isPrefsInForeGround);
                }
            });

            doBindService();
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

            registerReceiver(this.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

            handleNewPagerAdapter();

            mAdapter.createMenu();

            mStateCharging = new StateCharging(this, mAdapter);
            mStateError = new StateError(this, mAdapter);
            mStateProposing = new StateProposing(this, mAdapter);
            mStateQuest = new StateQuest(this, mAdapter);
            mStateRunning = new StateRunning(this, mAdapter);
            mStateConnecting = new StateConnecting(this, mAdapter);

            if (ControlService.INPUT == INPUT_CONFIG.STANDALONE) {
                mAppState = mStateRunning;
            } else {
                mAppState = mStateConnecting;
            }
            mAppState.setInterface();

            mAdapter.checkBatteryCritical();

            isActivityRunning = true;
        }

        if (!Settings.System.canWrite(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + mStatContext.getPackageName()));
            startActivity(intent);
        }

        // BRIGHTNESS ADJUSTMENT
        mResolver = getContentResolver();
        mWindow = getWindow();

        // KIOSK MODE
        ComponentName deviceAdmin = new ComponentName(this, AdminReceiver.class);
        mDevicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        mDevicePolicyManager.setLockTaskPackages(deviceAdmin, new String[]{getPackageName()});
        mAdminComponentName = deviceAdmin;
        mDevicePolicyManager = (DevicePolicyManager) getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        setDefaultCosuPolicies(USE_KIOSK_MODE);
        enableKioskMode(USE_KIOSK_MODE);

        // ONLY Needed in case of uninstall!!!
        //mDevicePolicyManager.clearDeviceOwnerApp(this.getPackageName());

    }

    @Override
    protected void onDestroy() {
        if (BuildConfig.DEBUG) {
            Log.i(LOG, "onDestroy");
        }
        super.onDestroy();
        messageService(ControlService.MSG_APPLICATION_SHUTDOWN);
        doUnbindService();
        unregisterReceiver(mBatInfoReceiver);
    }

    @Override
    protected void onStart() {
        if (BuildConfig.DEBUG) {
            Log.i(LOG, "onStart");
        }
        super.onStart();
    }

    @Override
    protected void onRestart() {
        if (BuildConfig.DEBUG) {
            Log.i(LOG, "onRestart");
        }
        super.onRestart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (BuildConfig.DEBUG) {
            Log.i(LOG, "onStop");
        }
    }

    @Override
    protected void onPause() {
        if (BuildConfig.DEBUG) {
            Log.i(LOG, "onPause");
        }
        mAdapter.onPause();
        super.onPause();

        // KIOSK MODE related
        ActivityManager activityManager = (ActivityManager) getApplicationContext()
                .getSystemService(Context.ACTIVITY_SERVICE);
        if (USE_KIOSK_MODE) {
            activityManager.moveTaskToFront(getTaskId(), 0);
        }
    }

    @Override
    protected void onResume() {
        if (BuildConfig.DEBUG) {
            Log.i(LOG, "onResume");
        }

        if (isPrefsInForeGround) {
            isPrefsInForeGround = false;
            mAdapter.setPrefsInForeGround(isPrefsInForeGround);

            Log.e(LOG, "HERE YOU GOT IT!!!");
            shipPreferencesToControlService();

        }
        mAdapter.onResume();
        super.onResume();

        hideSystemUI(USE_KIOSK_MODE);


        //Set the system brightness using the brightness variable value
        boolean maxBrightness = sharedPreferences.getBoolean("maxBrightness", false);
        if (maxBrightness) {
            LogIHAB.log(LOG + ": Setting display brightness to maximum.");
            //Settings.System.putInt(mResolver, Settings.System.SCREEN_BRIGHTNESS, 255);
            //Get the current window attributes
            WindowManager.LayoutParams layoutParams = mWindow.getAttributes();
            //Set the brightness of this window
            layoutParams.screenBrightness = 1f;//mBrightness / (float)255;
            //Apply attribute changes to this window
            mWindow.setAttributes(layoutParams);
        }
    }


    /**
     * KIOSK MODE RELATED STUFF
     */


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        // Little hack since the Power button seems to be inaccessible at this point
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus && USE_KIOSK_MODE) {
            Intent closeDialog = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            sendBroadcast(closeDialog);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
            enableKioskMode(true);
        }
    }

    // This disables the Volume Buttons
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {

        Log.e(LOG, "EVENT: " + event.getKeyCode());

        if (blockedKeys.contains(event.getKeyCode()) && USE_KIOSK_MODE) {
            return true;
        } else if ((event.getKeyCode() == KeyEvent.KEYCODE_POWER) && USE_KIOSK_MODE) {
            Log.e(LOG, "POWER BUTTON WAS PRESSED");
            return super.dispatchKeyEvent(event);
        } else {
            return super.dispatchKeyEvent(event);
        }
    }

    // When back button is pressed, questionnaire navigates one page backwards, menu does nothing
    @Override
    public void onBackPressed() {
        if (!mAdapter.isMenu() && USE_KIOSK_MODE) {
            if (mViewPager.getCurrentItem() != 0) {
                mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1);
            } else {
                // Might be unsafe because this accidentally resets the timer and starts a new cycle
                //mAdapter.createMenu();
            }
        }
    }

    private void setDefaultCosuPolicies(boolean active) {
        // set user restrictions
        setUserRestriction(UserManager.DISALLOW_ADD_USER, active);
        setUserRestriction(UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA, active);
        setUserRestriction(UserManager.DISALLOW_ADJUST_VOLUME, false);
        setUserRestriction(UserManager.DISALLOW_CREATE_WINDOWS, active);
        Log.i(LOG, "KIOSK MODE: " + active);
        // disable keyguard and status bar - needs API 23 (Damnit!)
        // TODO: CHECK IF THIS APPLIES
        mDevicePolicyManager.setKeyguardDisabled(mAdminComponentName, active);
        mDevicePolicyManager.setStatusBarDisabled(mAdminComponentName, active);
    }

    private void setUserRestriction(String restriction, boolean disallow) {
        if (disallow) {
            mDevicePolicyManager.addUserRestriction(mAdminComponentName,
                    restriction);
        } else {
            mDevicePolicyManager.clearUserRestriction(mAdminComponentName,
                    restriction);
        }
    }

    private void enableKioskMode(boolean enabled) {
        try {
            if (enabled && USE_KIOSK_MODE) {
                if (mDevicePolicyManager.isLockTaskPermitted(this.getPackageName())) {
                    startLockTask();
                } else {
                    Toast.makeText(this, "Kiosk not permitted", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Logger.info("Unable to start KIOSK mode");
            LogIHAB.log("Unable to start KIOSK mode");
        }
    }

    public void hideSystemUI(boolean isImmersive) {
        if (isImmersive) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_VISIBLE
            );
        }
    }


    /**
     * PERMISSION STUFF (ANDROID 6+)
     */

    // TODO: Need to implement this

    public void checkForPermissions() {

        if (ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)
                !=PackageManager.PERMISSION_GRANTED)
        {
            LogIHAB.log("Requesting permission to record audio.");
            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.RECORD_AUDIO)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        MY_PERMISSIONS_RECORD_AUDIO);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }

        }

        if (ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)
                !=PackageManager.PERMISSION_GRANTED)
        {
            LogIHAB.log("Requesting permission to record audio.");
            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_READ_EXTERNAL_STORAGE);
                permissionGranted = true;

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }

        if (ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)
                !=PackageManager.PERMISSION_GRANTED)
        {
            LogIHAB.log("Requesting permission to record audio.");
            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_SETTINGS)
                != PackageManager.PERMISSION_GRANTED) {
            LogIHAB.log("Requesting permission to record audio.");
            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.WRITE_SETTINGS)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.WRITE_SETTINGS},
                        MY_PERMISSIONS_WRITE_SETTINGS);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }

        if (ContextCompat.checkSelfPermission(this,Manifest.permission.VIBRATE)
                !=PackageManager.PERMISSION_GRANTED)
        {
            LogIHAB.log("Requesting permission to record audio.");
            requestPermissions(MY_PERMISSIONS_VIBRATE);
        }

        if (ContextCompat.checkSelfPermission(this,Manifest.permission.WAKE_LOCK)
                !=PackageManager.PERMISSION_GRANTED)
        {
            LogIHAB.log("Requesting permission to record audio.");
            requestPermissions(MY_PERMISSIONS_WAKE_LOCK);
        }

        if (ContextCompat.checkSelfPermission(this,Manifest.permission.RECEIVE_BOOT_COMPLETED)
                !=PackageManager.PERMISSION_GRANTED)
        {
            LogIHAB.log("Requesting permission to record audio.");
            requestPermissions(MY_PERMISSIONS_RECEIVE_BOOT_COMPLETED);
        }


    }


    public void requestPermissions(int iPermission) {

        // TODO: Make array
        switch (iPermission) {
            case 0:
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_READ_EXTERNAL_STORAGE);
                break;
            case 1:
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE);
                break;
            case 2:
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECEIVE_BOOT_COMPLETED},
                        MY_PERMISSIONS_RECEIVE_BOOT_COMPLETED);
                break;
            case 3:
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        MY_PERMISSIONS_RECORD_AUDIO);
                break;
            case 4:
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.VIBRATE},
                        MY_PERMISSIONS_VIBRATE);
                break;
            case 5:
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WAKE_LOCK},
                        MY_PERMISSIONS_WAKE_LOCK);
                break;
            case 6:
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.DISABLE_KEYGUARD},
                        MY_PERMISSIONS_DISABLE_KEYGUARD);
                break;
            case 7:
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        MY_PERMISSIONS_CAMERA);
                break;
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        requestString = getResources().getStringArray(R.array.permissionMessages);
        switch (requestCode) {

            case MY_PERMISSIONS_RECORD_AUDIO: {
                //Toast.makeText(this, "Thanks for permission to record audio.", Toast.LENGTH_SHORT).show();

                break;
            }
            case MY_PERMISSIONS_RECEIVE_BOOT_COMPLETED: {
                //Toast.makeText(this, "Thanks for permission to receive boot completed.", Toast.LENGTH_SHORT).show();

                break;
            }
            case MY_PERMISSIONS_VIBRATE: {
                //Toast.makeText(this, "Thanks for permission to vibrate.", Toast.LENGTH_SHORT).show();

                break;
            }
            case MY_PERMISSIONS_WAKE_LOCK: {
                //Toast.makeText(this, "Thanks for permission for wake lock.", Toast.LENGTH_SHORT).show();

                break;
            }
            case MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE: {
                //Toast.makeText(this, "Thanks for permission to write external storage.", Toast.LENGTH_SHORT).show();

                break;
            }
            case MY_PERMISSIONS_DISABLE_KEYGUARD: {
                //Toast.makeText(this, "Thanks for permission to disable keyguard.", Toast.LENGTH_SHORT).show();

                break;
            }
            case MY_PERMISSIONS_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permissionGranted = true;
                    //Toast.makeText(this, "Thanks for permission to read external storage.", Toast.LENGTH_SHORT).show();
                    doBindService();
                } else {
                    //Toast.makeText(this, requestString[requestIterator % (requestString.length)], Toast.LENGTH_SHORT).show();
                    requestPermissions(iPermission);
                    requestIterator++;
                }
                break;
            }
            case MY_PERMISSIONS_WRITE_SETTINGS: {
                //Toast.makeText(this, "Thanks for permission to record audio.", Toast.LENGTH_SHORT).show();

                break;
            }
        }
    }


    /** Message Handling */


    // Send message to connected client
    public void messageService(int what) {

        if (mServiceIsBound) {

            if (mServiceMessenger != null) {
                try {
                    Message msg = Message.obtain(null, what);
                    msg.replyTo = mMessageHandler;
                    mServiceMessenger.send(msg);
                } catch (RemoteException e) {
                }
            }
        } else {
            mMessageList.addMessage(what);
            LogIHAB.log("Message was added to MessageList: " + what);
        }
    }

    // Send message to connected client
    public void messageService(int what, Bundle data) {

        if (mServiceIsBound) {

            if (mServiceMessenger != null) {
                try {
                    Message msg = Message.obtain(null, what);
                    msg.setData(data);
                    msg.replyTo = mMessageHandler;
                    mServiceMessenger.send(msg);
                } catch (RemoteException e) {
                }
            }
        } else {
            mMessageList.addMessage(what, data);
            LogIHAB.log("Message was added to MessageList with data: " + what);
        }
    }

    class MessageHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {

            if (BuildConfig.DEBUG) {
                Log.d(LOG, "Message received: " + msg.what);
            }

            switch (msg.what) {

                case ControlService.MSG_NO_QUESTIONNAIRE_FOUND:

                    mAppState.noQuest();

                    break;

                /*case MSG_CHANGE_PREFERENCE:

                    Bundle data = msg.getData();
                    if (data.getString("type").equals("boolean")) {
                        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).
                                edit().putBoolean(data.getString("key"), data.getBoolean("value")).
                                apply();
                    }
                    shipPreferencesToControlService();

                    break;*/

                case ControlService.MSG_SET_COUNTDOWN_TIME:

                    int finalCountDown = msg.getData().getInt("finalCountDown");
                    int countDownInterval = msg.getData().getInt("countDownInterval");
                    mAdapter.setFinalCountDown(finalCountDown, countDownInterval);
                    mAppState.countdownStart();

                    Log.e(LOG, "SETTING COUNTDOWN TIME TO: " + finalCountDown);

                    break;

                case ControlService.MSG_START_QUESTIONNAIRE:

                    Bundle dataQuest = msg.getData();
                    ArrayList<String> questionList = dataQuest.getStringArrayList("questionList");
                    String head = dataQuest.getString("head");
                    String foot = dataQuest.getString("foot");
                    String surveyUri = dataQuest.getString("surveyUri");
                    String motivation = dataQuest.getString("motivation");
                    mAdapter.createQuestionnaire(questionList, head, foot, surveyUri, motivation);
                    mAppState.startQuest();

                    break;

                case ControlService.MSG_PROPOSE_QUESTIONNAIRE:

                    mAppState.countdownFinish();

                    break;

                case ControlService.MSG_GET_STATUS:

                    // Set UI to match ControlService's state
                    Bundle status = msg.getData();
                    mServiceIsRecording = status.getBoolean("isRecording");

                    Log.d(LOG, "recording state: " + mServiceIsRecording);

                    if (ControlService.INPUT == INPUT_CONFIG.A2DP || ControlService.INPUT == INPUT_CONFIG.RFCOMM) {
                        if (isBTPresent) {
                            mAppState.bluetoothPresent();
                        } else {
                            mAppState.bluetoothNotPresent();
                        }
                    }

                    break;

                case ControlService.MSG_PREFS_IN_FOREGROUND:

                    isPrefsInForeGround = msg.getData().getBoolean(KEY_PREFS_IN_FOREGROUND);
                    break;

                case ControlService.MSG_START_RECORDING:

                    if (ControlService.INPUT == INPUT_CONFIG.A2DP || ControlService.INPUT == INPUT_CONFIG.RFCOMM) {
                        mAppState.bluetoothPresent();
                        isBTPresent = true;
                    }

                    break;

                case ControlService.MSG_STOP_RECORDING:

                    if (ControlService.INPUT == INPUT_CONFIG.A2DP || ControlService.INPUT == INPUT_CONFIG.RFCOMM) {
                        mAppState.bluetoothNotPresent();
                        isBTPresent = false;
                    }

                    break;

                case ControlService.MSG_TIME_CORRECT:

                    mAppState.timeCorrect();

                    break;
                case ControlService.MSG_TIME_INCORRECT:

                    mAppState.timeIncorrect();

                    break;

                case ControlService.MSG_USB_CONNECT:

                    Log.e(LOG, "USB PRESENT!!!");

                        mAppState.usbPresent();
                        setUSBPresent(true);
                    break;

                case ControlService.MSG_USB_DISCONNECT:

                    Log.e(LOG, "USB NOT PRESENT!!!");

                        mAppState.usbNotPresent();
                        setUSBPresent(false);
                        setLogoInactive();
                        addError(AppErrors.ERROR_NO_USB);
                    break;

                case ControlService.MSG_BT_CONNECTED:
                    setIsBTPresent(true);
                    mAppState.bluetoothPresent();
                    break;

                case ControlService.MSG_BT_DISCONNECTED:
                    setIsBTPresent(false);
                    mAppState.bluetoothNotPresent();
                    break;

                case ControlService.MSG_STATE_CHANGE:

                    switch (msg.getData().getString("operationMode", "")) {
                        case "A2DP":
                            mServiceState = INPUT_CONFIG.A2DP;
                            removeError(AppErrors.ERROR_NO_USB);
                            break;
                        case "RFCOMM":
                            mServiceState = INPUT_CONFIG.RFCOMM;
                            removeError(AppErrors.ERROR_NO_USB);
                            break;
                        case "USB":
                            mServiceState = INPUT_CONFIG.USB;
                            removeError(AppErrors.ERROR_NO_BT);
                            setLogoActive();
                            break;
                        case "STANDALONE":
                            mServiceState = INPUT_CONFIG.STANDALONE;
                            removeError(AppErrors.ERROR_NO_BT);
                            removeError(AppErrors.ERROR_NO_USB);
                            setLogoActive();
                            break;
                    }

                    mAppState = mStateConnecting;
                    mAppState.setInterface();
                    break;

                case ControlService.MSG_REQUEST_PERMISSION:

                    Bundle bundle = msg.getData();
                    int permissionName = bundle.getInt("permissionName");
                    switch (permissionName) {
                        case MY_PERMISSIONS_RECORD_AUDIO:
                            Log.e(LOG, "Requesting permission to record Audio.");
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.RECORD_AUDIO},
                                    MY_PERMISSIONS_RECORD_AUDIO);

                            break;

                        default:
                            Log.e(LOG, "Unknown Permission requested: " + permissionName);
                    }

                    break;

                default:

                    super.handleMessage(msg);

                    break;
            }
        }
    }

}