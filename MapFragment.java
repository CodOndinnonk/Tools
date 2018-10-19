package com.sezam.gbsfo.sezam.Fragments;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.sezam.gbsfo.sezam.Activities.MainActivity;
import com.sezam.gbsfo.sezam.Entities.Header;
import com.sezam.gbsfo.sezam.Entities.Restaurant;
import com.sezam.gbsfo.sezam.Helpers.ImageHelper;
import com.sezam.gbsfo.sezam.Helpers.LoadingInfoHelper;
import com.sezam.gbsfo.sezam.Helpers.LogHelper;
import com.sezam.gbsfo.sezam.Helpers.PreferencesHelper;
import com.sezam.gbsfo.sezam.Helpers.ServerHelper;
import com.sezam.gbsfo.sezam.Models.OrderModel;
import com.sezam.gbsfo.sezam.Models.RestaurantModel;
import com.sezam.gbsfo.sezam.NavigationHandler;
import com.sezam.gbsfo.sezam.R;
import com.sezam.gbsfo.sezam.Utils.ActivitiesHandler;
import com.sezam.gbsfo.sezam.Utils.URL;

import java.util.ArrayList;


public class MapFragment extends Fragment implements OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener, NavigationHandler.OnToolbarNavigationActionListener {


    public interface CallbackLocation {
        /**
         * Define the process performing after some actions(in methods)
         */
        void action(Location location);
    }

    private Context mContext;
    private GoogleMap mGoogleMap;
    SupportMapFragment mMapFragment;
    private MapView mMapView;
    private View mLayout;
    private Location mCurrentLocation;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private boolean mIsCurrentLocationEnable;
    public static final int LOCATION_PERMISSION_REQUEST_CODE = 32452;
    private ArrayList<Runnable> mDelayedTasks = new ArrayList<>();
    private ArrayList<MarkerOptions> mMarkersList = new ArrayList<>();
    private GoogleMap.OnMarkerClickListener mOnMarkerClickListener;

    public static final String USE_DEVICE_LOCATION = "useDeviceLocation";

    //is using for child threads
    private Handler mainHandler = new Handler(Looper.getMainLooper());


    public static MapFragment newInstance(@NonNull Context context, boolean isCurrentLocationEnable) {
        if (context == null) {
            LogHelper.e("Context is NULL.");
            return null;
        }
        MapFragment fragment = new MapFragment();
        //set arguments
        Bundle bundle = new Bundle();
        bundle.putBoolean(USE_DEVICE_LOCATION, isCurrentLocationEnable);
        fragment.setContext(context);

        fragment.setArguments(bundle);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LoadingInfoHelper.setLoading(getActivity());

        Bundle bundle = getArguments();

        if (bundle == null) {
            bundle = new Bundle();
        }

        mIsCurrentLocationEnable = bundle.getBoolean(USE_DEVICE_LOCATION, false);
    }

    /**
     * Set specific toolbar, load restaurants and set markers on map
     */
    public void setAllRestaurants() {
        //if not attached to context (activity) add to delay task
        if (getContext() != null) {
            new ActivitiesHandler<MainActivity>(MainActivity.class).getActivityInstance().createToolbarText(this, mContext.getString(R.string.restaurants_text));
            new ActivitiesHandler<MainActivity>(MainActivity.class).getActivityInstance().setLeftToolbarButton(getContext().getDrawable(R.drawable.ic_three_lines));
            if (OrderModel.getCurrentOrder().getOrderedProducts().size() > 0) {
                new ActivitiesHandler<MainActivity>(MainActivity.class).getActivityInstance().setRightToolbarButton(getResources().getDrawable(R.drawable.ic_cart_read_dot));
            } else {
                new ActivitiesHandler<MainActivity>(MainActivity.class).getActivityInstance().setRightToolbarButton(getResources().getDrawable(R.drawable.ic_cart));
            }
            final Header header = new Header(Header.HEADER_KEY_AUTH, ServerHelper.bearerValue(new PreferencesHelper<String>(getContext()).get(PreferencesHelper.TOKEN, null)));
            //load list of restaurants
            RestaurantModel.getInstance(mContext).getList(new RestaurantModel.CallbackRestaurants() {
                @Override
                public void action(final ArrayList<Restaurant> restaurants) {
                    for (int i = 0; i < restaurants.size(); i++) {
                        final int finalI = i;
                        //load icons for restaurants
                        ServerHelper.loadImage(URL.STORAGE(restaurants.get(finalI).getLogo()), header, new ServerHelper.CallbackBitmap() {
                            @Override
                            public void action(Bitmap bitmap) {
                                //if fragment hasn't been detached from screen
                                if (!isDetached()) {
                                    //set markers, with resized icons
                                    addMarkerAndMoveCamera(
                                            new MarkerOptions()
                                                    .position(new LatLng(restaurants.get(finalI).getLatitude(), restaurants.get(finalI).getLongitude()))
                                                    .title(restaurants.get(finalI).getTitle()),
                                            ImageHelper.resizeImageProportional(bitmap, mContext.getResources().getDimension(R.dimen.map_restaurant_icon_size)),
                                            10);
                                }
                            }
                        });
                    }
                    //close loader
                    LoadingInfoHelper.closeLoading(getActivity());
                }
            });

            setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {
                    Restaurant restaurantToOpen = RestaurantModel.getInstance(getContext()).getRestaurantByTitle(marker.getTitle());
                    if (restaurantToOpen != null) {
                        ActivitiesHandler.getBaseActivityInstance().openFragment(RestaurantDetailsFragment.newInstance(mContext, restaurantToOpen));
                    } else {
                        LogHelper.w("Can't find restaurant to showToast");
                    }
                    return false;
                }
            });
        } else {
            //perform action later
            Runnable delayTask = new Runnable() {
                @Override
                public void run() {
                    setAllRestaurants();
                }
            };
            //will start on the end of onMapReady
            addTaskOnPostMapReady(delayTask);
            return;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mLayout = inflater.inflate(R.layout.fragment_map, container, false);


        // Inflate the layout for this fragment
        return mLayout;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //check the permissions
        if (isLocationPermissionsSet()) {
            //initialize the mapView
            initMap();
        } else {
            requestPermissions();
            //permission answer will be in MainActivity onRequestPermissionsResult()
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        MapsInitializer.initialize(getContext());
        mGoogleMap = googleMap;

        //set map img type
        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        //if client ask to showToast current location
        if (mIsCurrentLocationEnable) {
            //if has all permissions
            if (isLocationPermissionsSet()) {

                if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                mGoogleMap.setMyLocationEnabled(true);
                //set my location button
                mGoogleMap.getUiSettings().setMyLocationButtonEnabled(true);
                mGoogleMap.setOnMyLocationButtonClickListener(this);
            }
        }

        // start delayed tasks, if have such. Tasks that use mGoogleMap, and it was null
        for (Runnable t : mDelayedTasks) {
            mainHandler.post(t);
        }
    }

    @Override
    public boolean onMyLocationButtonClick() {
        return false;
    }


    /**
     * Perform adding standard marker to the map
     *
     * @param marker marker with coordinates and title
     *               <p>
     *               new MarkerOptions()
     *               .position(new LatLng(0,0))
     *               .title("test")
     *               .snippet("test Population: 4,627,300")
     *               .infoWindowAnchor(0.5f, 0.5f)
     */
    public void addMarker(final @NonNull MarkerOptions marker) {
        addMarker(marker, null);
    }

    /**
     * Perform adding standard marker to the map
     *
     * @param marker marker with coordinates and title
     * @param icon   bitmap of icon, to set. If no icon, set NULL
     *               <p>
     *               new MarkerOptions()
     *               .position(new LatLng(0,0))
     *               .title("test")
     *               .snippet("test Population: 4,627,300")
     *               .infoWindowAnchor(0.5f, 0.5f)
     */
    public void addMarker(final @NonNull MarkerOptions marker, final Bitmap icon) {
        if (marker == null) {
            LogHelper.e("marker is null");
        }
        //if method is called before map initialization
        if (mGoogleMap == null) {
            //perform action later
            Runnable delayTask = new Runnable() {
                @Override
                public void run() {
                    addMarker(marker, icon);
                }
            };
            //will start on the end of onMapReady
            addTaskOnPostMapReady(delayTask);
            return;
        }
        if (icon != null) {
            //todo **  доделать bitmap чтоб была иконка с фоном маркера
            marker.icon(BitmapDescriptorFactory.fromBitmap(icon));
        }
        mGoogleMap.addMarker(marker);
        mMarkersList.add(marker);
    }


    /**
     * Perform adding marker to the map and zoom the camera
     *
     * @param marker marker with coordinates and title
     * @param zoom   zoom of map (as big, so close) standard = 16, if has several markers, this ignore and use auto size
     */
    public void addMarkerAndMoveCamera(final @NonNull MarkerOptions marker, final Bitmap icon, final float zoom) {
        if (marker == null) {
            LogHelper.e("marker is null");
        }
        if (zoom <= 0) {
            LogHelper.w("zoom = " + zoom + ". It has to be > 0");
        }
        //if method is called before map initialization
        if (mGoogleMap == null) {
            //perform action later
            Runnable delayTask = new Runnable() {
                @Override
                public void run() {
                    addMarkerAndMoveCamera(marker, icon, zoom);
                }
            };
            //will start on the end of onMapReady
            addTaskOnPostMapReady(delayTask);
            return;
        }
        addMarker(marker, icon);

        //if has more than 1 point, do auto size map
        if (mMarkersList.size() > 1) {
            LatLngBounds.Builder builder = LatLngBounds.builder();

            for (MarkerOptions m : mMarkersList) {
                builder.include(m.getPosition());
            }

            LatLngBounds bounds = builder.build();

            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
        } else {
            CameraPosition position = CameraPosition.builder().target(marker.getPosition()).zoom(zoom).build();
            mGoogleMap.moveCamera(CameraUpdateFactory.newCameraPosition(position));
        }
    }


    /**
     * Check permissions. If no some permission, request it.
     * Use the next permissions:
     * {@link Manifest.permission#ACCESS_FINE_LOCATION}
     * {@link Manifest.permission#ACCESS_COARSE_LOCATION}
     *
     * @return true -> all permissions are granted
     * <p>false -> need to grant some permission
     */
    private boolean isLocationPermissionsSet() {
        if (ContextCompat.checkSelfPermission(getActivity().getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return ContextCompat.checkSelfPermission(getActivity().getApplicationContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        } else {
            return false;
        }
    }

    /**
     * Ask user the needed permissions.
     * Use the next permissions:
     * {@link Manifest.permission#ACCESS_FINE_LOCATION}
     * {@link Manifest.permission#ACCESS_COARSE_LOCATION}
     */
    private void requestPermissions() {
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};

        ActivityCompat.requestPermissions(getActivity(),
                permissions,
                LOCATION_PERMISSION_REQUEST_CODE);
    }


    /**
     * Get location of device, if all permissions are granted
     *
     * @param callback action on get location. Need to use {@link MapFragment#mCurrentLocation}
     */
    public void getDeviceLocation(@NonNull final CallbackLocation callback) {
        //try to get object, while it will be initialized
        try {
            mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getActivity());
        } catch (Exception e) {
            LogHelper.w(e.toString());
        }

        //if not initialized, create delay task
        if (mFusedLocationProviderClient == null) {
            //perform action later
            Runnable delayTask = new Runnable() {
                @Override
                public void run() {
                    getDeviceLocation(callback);
                }
            };
            //will start on the end of onMapReady
            addTaskOnPostMapReady(delayTask);
            return;
        }

        try {
            if (isLocationPermissionsSet()) {
                final Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            mCurrentLocation = (Location) task.getResult();
                            callback.action(mCurrentLocation);
                        } else {
                            Toast.makeText(getContext(), "Unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            LogHelper.e(e.getMessage());
        }
    }

    /**
     * Perform initialization if mapView and start methods, to begin working with it.
     */
    private void initMap() {
        //find map and set sync
        if (mMapFragment == null) {
            mMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapView);
            mMapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDelayedTasks.clear();
        mGoogleMap = null;
    }

    /**
     * Add task that needs to be done after onMapReady (initialized map)
     *
     * @param task Runnable with executing code
     */
    private void addTaskOnPostMapReady(Runnable task) {
        mDelayedTasks.add(task);
    }


    @Override
    public void onLeftButtonClick() {
        ActivitiesHandler.getBaseActivityInstance().openFragment(RestaurantsFragment.newInstance(mContext));
    }

    @Override
    public void onRightButtonClick() {
        new ActivitiesHandler<MainActivity>(MainActivity.class).getActivityInstance().openBasket();
    }

    @Override
    public void onDropDownSelected(@NonNull String selectedItem, int positionInList) {

    }

    public void setOnMarkerClickListener(@NonNull GoogleMap.OnMarkerClickListener listener) {
        if (listener == null) {
            LogHelper.e("OnMarkerClickListener is NULL, it has to be set.");
            return;
        }
        mOnMarkerClickListener = listener;
        //if method is called before map initialization
        if (mGoogleMap == null) {
            //perform action later
            Runnable delayTask = new Runnable() {
                @Override
                public void run() {
                    setOnMarkerClickListener(mOnMarkerClickListener);
                }
            };
            //will start on the end of onMapReady
            addTaskOnPostMapReady(delayTask);
            return;
        }
        mGoogleMap.setOnMarkerClickListener(mOnMarkerClickListener);
    }

    private void setContext(@NonNull Context mContext) {
        if (mContext == null) {
            LogHelper.e("Context is NULL, it has to be set");
            return;
        }
        this.mContext = mContext;
    }
}



