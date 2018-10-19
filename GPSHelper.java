package com.sezam.gbsfo.sezam.Helpers;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public class GPSHelper {

    private Context mContext;
    // flag for GPS Status
    private boolean mIsGPSEnabled = false;
    // flag for network status
    private boolean mIsNetworkEnabled = false;
    private LocationManager mLocationManager;

    public GPSHelper(@NonNull Context context) {
        if (context == null) {
            LogHelper.e("Context is NULL.");
            return;
        }
        this.mContext = context;
        mLocationManager = (LocationManager) context
                .getSystemService(Context.LOCATION_SERVICE);
    }

    /**
     * Perform getting last saved gps position of device
     *
     * @return ->coordinates on map
     * <p>-> NULL, if any error, or no connection
     */
    public LatLng getMyLocation() {
        List<String> providers = mLocationManager.getProviders(true);

        Location l = null;
        for (int i = 0; i < providers.size(); i++) {
            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                LogHelper.e("No permissions granted");
                return null;
            }
            l = mLocationManager.getLastKnownLocation(providers.get(i));
            if (l != null)
                break;
        }
        if (l != null) {
            return new LatLng(l.getLatitude(), l.getLongitude());
        } else {
            LogHelper.w("Can't get location");
            return null;
        }
    }

    /**
     * Check if can get current location
     *
     * @return true -> location can be identify
     * <p>false -> no connection
     */
    public boolean isGPSEnabled() {
        mIsGPSEnabled = mLocationManager
                .isProviderEnabled(LocationManager.GPS_PROVIDER);

        // getting network status
        mIsNetworkEnabled = mLocationManager
                .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        return (mIsGPSEnabled || mIsNetworkEnabled);
    }

    /**
     * Calculate distance in kilometers, meters between coordinates (by air). Calculate in line, doesn't include buildings and other.
     *
     * @param StartP start point, doesn't matter order
     * @param EndP   en point, doesn't matter order
     * @return -> filled {@link GPSHelper.Distance}
     */
    public Distance CalculationByDistance(LatLng StartP, LatLng EndP) {
        int Radius = 6371;// radius of earth in Km
        double lat1 = StartP.latitude;
        double lat2 = EndP.latitude;
        double lon1 = StartP.longitude;
        double lon2 = EndP.longitude;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);
        double c = 2 * Math.asin(Math.sqrt(a));
        double valueResult = Radius * c;
        int km = (int) (valueResult / 1);
        int meter = (int) ((valueResult - km) * 1000);

        return new Distance(valueResult, km, meter);
    }


    /**
     * Object to describe distance
     */
    public class Distance {
        private double mTotalDistance;
        private int mKm; //kilometers
        /**
         * meters, consists of 3 digits
         */
        private int mMeter;

        /**
         * Constructor
         *
         * @param total number of general distance
         * @param km    amt of kilometers
         * @param meter amt of meters
         */
        public Distance(double total, int km, int meter) {
            mTotalDistance = total;
            mKm = km;
            mMeter = meter;
        }

        public double getTotalDistance() {
            return mTotalDistance;
        }

        public int getKm() {
            return mKm;
        }

        public int getM() {
            return mMeter;
        }
    }
}

