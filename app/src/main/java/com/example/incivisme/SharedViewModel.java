package com.example.incivisme;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SharedViewModel extends AndroidViewModel {
    private final Application app;
    private final MutableLiveData<String> currentAddress = new MutableLiveData<>();
    private final MutableLiveData<String> checkPermission = new MutableLiveData<>();
    private final MutableLiveData<String> buttonText = new MutableLiveData<>();
    private final MutableLiveData<Boolean> progressBar = new MutableLiveData<>();

    private final MutableLiveData<LatLng> currentLatLng = new MutableLiveData<>();

    private boolean mTrackingLocation;
    FusedLocationProviderClient mFusedLocationClient;

    public MutableLiveData<LatLng> getCurrentLatLng() {
        return currentLatLng;
    }

    public SharedViewModel(@NonNull Application application) {
        super(application);

        this.app = application;
    }

    public void setFusedLocationClient(FusedLocationProviderClient mFusedLocationClient) {
        this.mFusedLocationClient = mFusedLocationClient;
    }

    public LiveData<String> getCurrentAddress() {
        return currentAddress;
    }

    public MutableLiveData<String> getButtonText() {
        return buttonText;
    }

    public MutableLiveData<Boolean> getProgressBar() {
        return progressBar;
    }

    public LiveData<String> getCheckPermission() {
        return checkPermission;
    }

    private LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (mTrackingLocation) {
                new FetchAddressTask(
                        getApplication().getApplicationContext()
                ).execute(
                        locationResult.getLastLocation()
                );
            }
        }
    };

    private LocationRequest getLocationRequest() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }

    public void switchTrackingLocation() {
        if (!mTrackingLocation) {
            startTrackingLocation(true);
        } else {
            stopTrackingLocation();
        }

    }

    @SuppressLint("MissingPermission")
    public void startTrackingLocation(boolean needsChecking) {
        if (needsChecking) {
            checkPermission.postValue("check");
        } else {
            mFusedLocationClient.requestLocationUpdates(
                    getLocationRequest(),
                    mLocationCallback,
                    null
            );

            currentAddress.postValue("Carregant...");

            progressBar.postValue(true);
            mTrackingLocation = true;
            buttonText.setValue("Aturar el seguiment de la ubicació");
        }
    }


    private void stopTrackingLocation() {
        if (mTrackingLocation) {
            mFusedLocationClient.removeLocationUpdates (mLocationCallback);
            mTrackingLocation = false;
            progressBar.postValue(false);
            buttonText.setValue("Comença a seguir la ubicació");
        }
    }

    private class FetchAddressTask extends AsyncTask<Location, Void, String> {
        private final String TAG = FetchAddressTask.class.getSimpleName();
        private Context mContext;

        FetchAddressTask(Context applicationContext) {
            mContext = applicationContext;
        }

        @Override
        protected String doInBackground(Location... locations) {
            Geocoder geocoder = new Geocoder(mContext, Locale.getDefault());
            Location location = locations[0];

            List<Address> addresses = null;
            String resultMessage = "";

            try {
                addresses = geocoder.getFromLocation(
                        location.getLatitude(),
                        location.getLongitude(),
                        // En aquest cas, sols volem una única adreça:
                        1);

                if (addresses == null || addresses.size() == 0) {
                    resultMessage = "No s'ha trobat cap adreça";
                    Log.e(TAG, resultMessage);
                } else {
                    Address address = addresses.get(0);
                    ArrayList<String> addressParts = new ArrayList<>();

                    for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                        addressParts.add(address.getAddressLine(i));
                    }

                    resultMessage = TextUtils.join("\n", addressParts);
                }
            } catch (IOException ioException) {
                resultMessage = "Servei no disponible";
                Log.e(TAG, resultMessage, ioException);
            } catch (IllegalArgumentException illegalArgumentException) {
                resultMessage = "Coordenades no vàlides";
                Log.e(TAG, resultMessage + ". " +
                        "Latitude = " + location.getLatitude() +
                        ", Longitude = " +
                        location.getLongitude(), illegalArgumentException);
            }
            return resultMessage;
        }

        @Override
        protected void onPostExecute(String address) {
            super.onPostExecute(address);
            currentAddress.postValue(address);
        }
    }
}
