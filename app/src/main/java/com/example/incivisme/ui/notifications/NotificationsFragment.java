package com.example.incivisme.ui.notifications;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import com.bumptech.glide.Glide;
import com.example.incivisme.Incidencia;
import com.example.incivisme.R;
import com.example.incivisme.SharedViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.model.Marker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationsFragment extends Fragment {

    private NotificationsViewModel notificationsViewModel;
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    static final int REQUEST_TAKE_PHOTO = 1;
    private final String TAG = this.getClass().getSimpleName();

    private ProgressBar mLoading;
    private SharedViewModel model;
    private TextInputEditText txtLatitud;
    private TextInputEditText txtLongitud;
    private TextInputEditText txtDireccio;
    private TextInputEditText txtDescripcio;
    private FusedLocationProviderClient mFusedLocationClient;
    private TextView mLocationTextView;

    private Button buttonNotificar;

    private String mCurrentPhotoPath;
    private Button buttonLocation;
    private LocationCallback mLocationCallback;
    private String downloadUrl;

    private Uri photoURI;
    private ImageView foto;

    // Les primitives booleanes són predeterminades a false
    private boolean mTrackingLocation;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        notificationsViewModel =
                new ViewModelProvider(this).get(NotificationsViewModel.class);
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        mLoading = view.findViewById(R.id.loading);

        model = ViewModelProviders.of(getActivity()).get(SharedViewModel.class);

        txtLatitud =  view.findViewById(R.id.txtLatitud);
        txtLongitud = view.findViewById(R.id.txtLongitud);
        txtDireccio = view.findViewById(R.id.txtDireccio);
        txtDescripcio = view.findViewById(R.id.txtDescripcio);
        buttonNotificar = view.findViewById(R.id.button_notificar);

        ImageView Foto = view.findViewById(R.id.foto);
        Button buttonFoto = view.findViewById(R.id.button_foto);
        buttonFoto.setOnClickListener(button -> {
            dispatchTakePictureIntent();
        });


        model.getCurrentAddress().observe(getViewLifecycleOwner(), address -> {
            txtDireccio.setText(getString(R.string.address_text,
                    address, System.currentTimeMillis()));
        });

        model.getCurrentLatLng().observe(getViewLifecycleOwner(), latlng -> {
            txtLatitud.setText(String.valueOf(latlng.latitude));
            txtLongitud.setText(String.valueOf(latlng.longitude));
        });


        model.getProgressBar().observe(getViewLifecycleOwner(), visible -> {
            if(visible)
                mLoading.setVisibility(ProgressBar.VISIBLE);
            else
                mLoading.setVisibility(ProgressBar.INVISIBLE);
        });

        model.switchTrackingLocation();


        buttonNotificar.setOnClickListener(button -> {
            Incidencia incidencia = new Incidencia();
            incidencia.setDireccio(txtDireccio.getText().toString());
            incidencia.setLatitud(txtLatitud.getText().toString());
            incidencia.setLongitud(txtLongitud.getText().toString());
            incidencia.setProblema(txtDescripcio.getText().toString());
            incidencia.setUrl(downloadUrl);

            FirebaseAuth auth = FirebaseAuth.getInstance();
            DatabaseReference base = FirebaseDatabase.getInstance("https://incivisme-b9ce2-default-rtdb.firebaseio.com/").getReference();

            DatabaseReference users = base.child("users");
            DatabaseReference uid = users.child(auth.getUid());
            DatabaseReference incidencies = uid.child("incidencies");

            DatabaseReference reference = incidencies.push();
            reference.setValue(incidencia);

            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReference();

            StorageReference imageRef = storageRef.child(mCurrentPhotoPath);
            UploadTask uploadTask = imageRef.putFile(photoURI);

            uploadTask.addOnSuccessListener(taskSnapshot -> {
                imageRef.getDownloadUrl().addOnCompleteListener(task -> {
                    Uri downloadUri = task.getResult();
                    Glide.with(this).load(downloadUri).into(foto);

                    downloadUrl = downloadUri.toString();
                });
            });
        });



        return view;
    }

    private void startTrackingLocation() {
        if (ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]
                            {Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        } else {
            mFusedLocationClient.requestLocationUpdates(
                    getLocationRequest(),
                    mLocationCallback,
                    null
            );
        }

        mLocationTextView.setText(getString(R.string.address_text, "Carregant...",
                System.currentTimeMillis()));

        mLoading.setVisibility(ProgressBar.VISIBLE);
    }

    private void stopTrackingLocation() {
        if (mTrackingLocation) {
            mLoading.setVisibility(ProgressBar.INVISIBLE);
            mTrackingLocation = false;
            buttonLocation.setText("Comença a seguir la ubicació");
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
    }

    private LocationRequest getLocationRequest() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION_PERMISSION:
                // Si es concedeix permís, obté la ubicació, d'una altra manera, mostra un Toast

                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startTrackingLocation();
                } else {
                    Toast.makeText(getContext(),
                            "Permís denegat",
                            Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private class FetchAddressTask extends AsyncTask<Location, Void, String> {
        private final String TAG = FetchAddressTask.class.getSimpleName();
        private Context mContext;

        public FetchAddressTask(Context applicationContext) {
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

            if (addresses == null || addresses.size() == 0) {
                if (resultMessage.isEmpty()) {
                    resultMessage = "No s'ha trobat cap adreça";
                    Log.e(TAG, resultMessage);
                }
            } else {
                Address address = addresses.get(0);
                ArrayList<String> addressParts = new ArrayList<>();

                for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                    addressParts.add(address.getAddressLine(i));
                }

                resultMessage = TextUtils.join("\n", addressParts);
            }

            return resultMessage;
        }

        @Override
        protected void onPostExecute(String address) {
            super.onPostExecute(address);
            if (mTrackingLocation) {
                mLocationTextView.setText(getString(R.string.address_text, address, System.currentTimeMillis()));
            }
        }

    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );

        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(
                getContext().getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ignored) {
            }

            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(getContext(),
                        "com.example.android.incivismefileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_TAKE_PHOTO) {
            if (resultCode == Activity.RESULT_OK) {
                Glide.with(getActivity()).load(photoURI).into(foto);
            } else {
                Toast.makeText(getContext(),
                        "Picture wasn't taken!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}