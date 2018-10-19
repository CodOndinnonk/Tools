package com.sezam.gbsfo.sezam.Helpers;

import android.content.Context;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.annotation.NonNull;

public class VibrationHelper {

    //VIB -> seconds of vibration; INTERVAL -> seconds of waiting
    public static final long[] VIBR_1_INTERVAL_5 = {1000, 5000};


    private Vibrator mVibrator;


    public VibrationHelper(@NonNull Context context) {
        if (context == null) {
            LogHelper.e("Context is NULL.");
            return;
        }
        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }


    /**
     * Turn ON vibration in set mode and auto stop after effect finished
     *
     * @param effect  array of {@code {VIB,INTERVAL}} VIB -> milliseconds of vibration; INTERVAL -> milliseconds of waiting.
     *                Array can contains more than 2 elements.
     * @param repeats is repeat. If true -> will repeat effect from start until call {@link Vibrator#cancel()}
     */
    public void turnOnVibrationEffect(long[] effect, boolean repeats) {
        if (effect.length < 2) {
            LogHelper.e("Effect array has to be at least with 2 elemts");
        }

        if (repeats) {
            //set to repeat from first position of effect
            mVibrator.vibrate(effect, 0);
        } else {
            //no repeats
            mVibrator.vibrate(effect, -1);
        }
    }

    /**
     * Turn ON vibration for set milliseconds
     *
     * @param millisecondsOn time in milliseconds to vibrate
     */
    public void turnOnVibration(long millisecondsOn) {
        mVibrator.vibrate(millisecondsOn);
    }

    /**
     * Turn ON vibration, will not stop by it self
     */
    public void turnOnVibration() {
        mVibrator.vibrate(VibrationEffect.DEFAULT_AMPLITUDE);
    }

    /**
     * Turn OFF vibration
     */
    public void offVibration() {
        mVibrator.cancel();
    }

}
