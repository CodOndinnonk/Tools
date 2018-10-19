package com.sezam.gbsfo.sezam.Helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import com.sezam.gbsfo.sezam.Vars;

/**
 * Class that performs saving  and reading from SharedPreferences file
 *
 * @param <T> {String, Integer, Float, Boolean,Long} type of object to save or get
 */
public class PreferencesHelper<T> {
    private static SharedPreferences mPreferences;
    private Context mContext;

    /**
     * SharedPreferences file name
     */
    public static final String APP_PREFERENCES_FILE = Vars.APP_PREFERENCES_FILE;
    public static final String ON_ERROR_STRING = "errorWithPrefOperation";
    public static final Float ON_ERROR_FLOAT = -927454234f;
    public static final Integer ON_ERROR_INTEGER = -927453654;
    public static final Long ON_ERROR_LONG = -928373654l;
    public static final Boolean ON_ERROR_BOOLEAN = false;


    public static final String TOKEN = "AuthToken";


    public PreferencesHelper(@NonNull Context context) {
        if (context == null){
            LogHelper.e("Context is null");
            return;
        }
        mContext = context;
        mPreferences = mContext.getSharedPreferences(APP_PREFERENCES_FILE, Context.MODE_PRIVATE);
    }

    /**
     * Save record to the app preferences file
     *
     * @param key   String key of record
     * @param value {String, Integer, Long, Float, Boolean} value of record
     */
    public void save(@NonNull String key, @NonNull T value) {
        if (key == null || value == null || key.length() == 0) {
            LogHelper.e("Can't save pref, as some argument is NULL, or key length = 0. key = " + key + ". value = " + value);
            return;
        }

        if (value instanceof String) {
            if (((String) value).length() == 0) {
                LogHelper.w("Warning, value argument length = 0.");
            }
        }

        //check for existence of key
        if (value instanceof String) {
            if (!get(key, (T) ON_ERROR_STRING).equals(ON_ERROR_STRING)) {
                LogHelper.i("Preference with key " + key + " will be override. Previous value = " + get(key, (T) ON_ERROR_STRING) + " New value = " + value);
            }
        } else if (value instanceof Float) {
            if (!get(key, (T) ON_ERROR_FLOAT).equals(ON_ERROR_FLOAT)) {
                LogHelper.i("Preference with key " + key + " will be override. Previous value = " + get(key, (T) ON_ERROR_FLOAT) + " New value = " + value);
            }
        } else if (value instanceof Integer) {
            if (!get(key, (T) ON_ERROR_INTEGER).equals(ON_ERROR_INTEGER)) {
                LogHelper.i("Preference with key " + key + " will be override. Previous value = " + get(key, (T) ON_ERROR_INTEGER) + " New value = " + value);
            }
        } else if (value instanceof Long) {
            if (!get(key, (T) ON_ERROR_LONG).equals(ON_ERROR_LONG)) {
                LogHelper.i("Preference with key " + key + " will be override. Previous value = " + get(key, (T) ON_ERROR_LONG) + " New value = " + value);
            }
        } else if (value instanceof Boolean) {
            if (!get(key, (T) ON_ERROR_BOOLEAN).equals(ON_ERROR_BOOLEAN)) {
                LogHelper.i("Preference with key " + key + " will be override. Previous value = " + get(key, (T) ON_ERROR_BOOLEAN) + " New value = " + value);
            }
        }

        SharedPreferences.Editor editor = mPreferences.edit();

        if (value instanceof String) {
            editor.putString(key, (String) value);
        } else if (value instanceof Float) {
            editor.putFloat(key, (Float) value);
        } else if (value instanceof Integer) {
            editor.putInt(key, (Integer) value);
        } else if (value instanceof Long) {
            editor.putLong(key, (Long) value);
        } else if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value);
        } else {
            LogHelper.e("Unexpected instance of value. Expected {String, Float, Boolean}, get value instance = " + value.getClass());
        }

        editor.commit();
        editor.apply();
    }

    /**
     * Get record from the app preferences file
     *
     * @param key          String key of record
     * @param defaultValue {String, Float, Boolean} value to return on some issue. It will be returned if:
     *                     1) key has some issues
     *                     2) no such key in file
     *                     3) on error in base getString() method
     *                     4) any other error
     * @return {String, Integer, Float, Long, Boolean} value bonded to the key, or {@param defaultValue}
     */
    public T get(@NonNull String key, T defaultValue) {
        if (key == null) {
            LogHelper.e("Can't save pref, as some argument is NULL. key = " + key);
            return defaultValue;
        }
        if (key.length() == 0) {
            LogHelper.e("Warning, some of arguments length = 0. key = " + key.length());
            return defaultValue;
        }
        try {
            if (defaultValue instanceof String || defaultValue == null) {
                //is needed to return null as default value. Because of use String.valueOf(), returned "null" (as string)
                if (mPreferences.getString(key, String.valueOf(defaultValue)).equals("null") && defaultValue == null) {
                    return null;
                }
                return (T) mPreferences.getString(key, String.valueOf(defaultValue));
            } else if (defaultValue instanceof Float) {
                return (T) (Float) mPreferences.getFloat(key, (Float) defaultValue);
            } else if (defaultValue instanceof Integer) {
                return (T) (Integer) mPreferences.getInt(key, (Integer) defaultValue);
            } else if (defaultValue instanceof Long) {
                return (T) (Long) mPreferences.getLong(key, (Long) defaultValue);
            } else if (defaultValue instanceof Boolean) {
                return (T) (Boolean) mPreferences.getBoolean(key, (Boolean) defaultValue);
            } else {
                LogHelper.e("Unexpected instance of defaultValue. Expected {String, Float, Boolean}, get defaultValue instance = " + defaultValue);
                return defaultValue;
            }
        } catch (Exception e) {
            LogHelper.e("Unexpected instance of defaultValue. defaultValue has different instance from contains in record by key. key = " + key + " defaultValue instance = " + defaultValue);
            return defaultValue;
        }
    }


    /**
     * Delete record from the app preferences file
     *
     * @param key String key of record
     */
    public static void delete(@NonNull String key) {
        if (key == null || key.length() == 0) {
            LogHelper.e("Can't delete pref, as key argument is NULL, or key length = 0. key = " + key);
            return;
        }
        SharedPreferences.Editor editor = mPreferences.edit();

        editor.remove(key);
        editor.commit();
        editor.apply();
    }

    /**
     * Perform deletion all records from {@link PreferencesHelper#APP_PREFERENCES_FILE}
     */
    public void deleteAll() {
        SharedPreferences.Editor editor = mPreferences.edit();

        editor.clear();
        editor.commit();
        editor.apply();
    }


    /**
     * Search pref file for set key
     *
     * @param key key of record to search
     * @return -> true, if file contains record
     * <p>-> false, record hasn't found
     */
    public static boolean contains(@NonNull String key) {
        if (key == null || key.length() == 0) {
            LogHelper.e("Can't search pref, as key argument is NULL, or key length = 0. key = " + key);
        }
        return mPreferences.contains(key);
    }

}
