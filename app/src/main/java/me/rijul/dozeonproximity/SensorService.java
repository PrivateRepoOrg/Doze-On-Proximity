package me.rijul.dozeonproximity;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

public class SensorService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String ACTION_DOZE = "com.android.systemui.doze.pulse";
    public static final String TAG = "DozeOnProximity";
    private int DELAY_BETWEEN_DOZES_IN_MS = MainActivity.DEFAULT_TIME;
    public static boolean isRunning;
    private Context mContext;
    private ProximitySensor mSensor;
    private PowerManager mPowerManager;
    private long mLastDoze;
    private boolean displayTurnedOff = false;
    private BroadcastReceiver mScreenStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                onDisplayOff();
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                onDisplayOn();
            }
        }
    };

    @Override
    public void onCreate() {
        mContext = this;
        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mSensor = new ProximitySensor(mContext);

        if (!isInteractive()) {
            mSensor.enable();
        }

        isRunning = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        IntentFilter screenStateFilter = new IntentFilter(
                Intent.ACTION_SCREEN_ON);
        screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF);
        mContext.registerReceiver(mScreenStateReceiver, screenStateFilter);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        preferences.registerOnSharedPreferenceChangeListener(this);
        onSharedPreferenceChanged(preferences, null);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        mContext.unregisterReceiver(mScreenStateReceiver);
        PreferenceManager.getDefaultSharedPreferences(mContext).unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void launchDozePulse() {
        mContext.sendBroadcast(new Intent(ACTION_DOZE));
        mLastDoze = System.currentTimeMillis();
        Log.d(TAG, "Launching pulse!");
    }

    private boolean isInteractive() {
        return mPowerManager.isInteractive();
    }

    private void onDisplayOn() {
        mSensor.disable();
    }

    private void onDisplayOff() {
        mSensor.enable();
        displayTurnedOff = true;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        DELAY_BETWEEN_DOZES_IN_MS = sharedPreferences.getInt(MainActivity.SERVICE_CHECK_TIME, MainActivity.DEFAULT_TIME);
        if (!sharedPreferences.getBoolean(MainActivity.SERVICE_STATUS, false))
            stopSelf();
    }

    class ProximitySensor implements SensorEventListener {
        private SensorManager mSensorManager;
        private Sensor mSensor;

        private boolean mIsNear = false;

        public ProximitySensor(Context context) {
            mSensorManager = (SensorManager) context
                    .getSystemService(Context.SENSOR_SERVICE);
            mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            long now = System.currentTimeMillis();
            mIsNear = event.values[0] < mSensor.getMaximumRange();

            if (!mIsNear && (now - mLastDoze > DELAY_BETWEEN_DOZES_IN_MS)
                    && !displayTurnedOff) {
                launchDozePulse();
            }

            displayTurnedOff = false;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
			/* Empty */
        }

        public void enable() {
            mSensorManager.registerListener(this, mSensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }

        public void disable() {
            mSensorManager.unregisterListener(this, mSensor);
        }
    }
}