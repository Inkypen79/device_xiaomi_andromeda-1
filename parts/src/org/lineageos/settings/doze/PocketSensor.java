/*
 * Copyright (C) 2021 MB <dataoutputstream>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lineageos.settings.d.doze;

import android.app.KeyguardManager;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import java.lang.Thread;

public class PocketSensor implements SensorEventListener {


    private static final String TAG = "PocketProtection";
    private static final boolean DEBUG = false;
    
    private static final int F_TIME = 3000;
    
    //Max Proximity Sensor distance in cm
    private float DISTANCE_MAX_VALUE;
    //Last proximity event value
    private float lpe = -1;
    //Last light event value
    private float lle = -1;
    
    private boolean mPickedUp, mPocketProtection;

    private SensorManager mSensorManager;
    private Sensor mProximitySensor, mLightSensor, mPickUpSensor;
    private Context mContext;

    public PocketSensor(Context context) {
    
        mContext = context;
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mProximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mPickUpSensor = DozeUtils.getSensor(mSensorManager, "xiaomi.sensor.pickup");
        
        DISTANCE_MAX_VALUE=mProximitySensor.getMaximumRange();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
    
        if(event.sensor.getType()==Sensor.TYPE_PROXIMITY){
            lpe=event.values[0];
        }else if(event.sensor.getType() == Sensor.TYPE_LIGHT){
            lle=event.values[0];
        }else {
            mPickedUp = true && event.values[0] == 1;
        }
        
        if(mPocketProtection){
        
            boolean isNear = lpe < (DISTANCE_MAX_VALUE/4);
            boolean isLowLight = lle < 2;
        
            if (mPickedUp) return;
            if (isNear && isLowLight) {
                mPocketProtection=false;
                if (DEBUG) Log.d(TAG, "Into the Pocket");
                wait(F_TIME);
                if(PhoneStateReceiver.CUR_STATE == PhoneStateReceiver.IDLE){
                PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
                KeyguardManager myKM = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
                    if( myKM.inKeyguardRestrictedInputMode() ) {
                        pm.goToSleep(SystemClock.uptimeMillis());
                    } else{ return; 
                    }
                }
            }
            
        }
    }
   
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    
    }

    protected void enable() {
        mPocketProtection=true;
        if (DEBUG) Log.d(TAG, "Enabling");
        mSensorManager.registerListener(this, mProximitySensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mLightSensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
    }

    protected void disable() {
            if (DEBUG) Log.d(TAG, "Disabling");
            mPickedUp=false;
            mSensorManager.unregisterListener(this, mProximitySensor);
            mSensorManager.unregisterListener(this, mLightSensor);
            mSensorManager.registerListener(this, mPickUpSensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
    }
    
    protected void disable_On() {
            if (DEBUG) Log.d(TAG, "Disabling on");
            mPickedUp=true;
            mSensorManager.unregisterListener(this, mPickUpSensor);
            mSensorManager.unregisterListener(this, mProximitySensor);
            mSensorManager.unregisterListener(this, mLightSensor);
            
    }
    
    public static void wait(int ms){
        try{
            Thread.sleep(ms);
        }catch(InterruptedException ex){
            Thread.currentThread().interrupt();
        }
    }

}
 
