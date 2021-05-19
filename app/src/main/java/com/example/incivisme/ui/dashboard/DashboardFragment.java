package com.example.incivisme.ui.dashboard;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import com.example.incivisme.Incidencia;
import com.example.incivisme.IncidenciesInfoWindowAdapter;
import com.example.incivisme.R;
import com.example.incivisme.SharedViewModel;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DashboardFragment extends Fragment {

    private DashboardViewModel dashboardViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        dashboardViewModel =
                new ViewModelProvider(this).get(DashboardViewModel.class);
        View root = inflater.inflate(R.layout.fragment_dashboard, container, false);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.g_map);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        DatabaseReference base = FirebaseDatabase.getInstance("https://incivisme-b9ce2-default-rtdb.firebaseio.com/").getReference();

        DatabaseReference users = base.child("users");
        DatabaseReference uid = users.child(auth.getUid());
        DatabaseReference incidencies = uid.child("incidencies");

        SharedViewModel model = ViewModelProviders.of(getActivity()).get(SharedViewModel.class);

        mapFragment.getMapAsync(map -> {
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                map.setMyLocationEnabled(true);

                MutableLiveData<LatLng> currentLatLng = model.getCurrentLatLng();
                LifecycleOwner owner = getViewLifecycleOwner();
                currentLatLng.observe(owner, latLng -> {
                    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15);
                    map.animateCamera(cameraUpdate);
                    currentLatLng.removeObservers(owner);
                });

                incidencies.addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                        Incidencia incidencia = dataSnapshot.getValue(Incidencia.class);

                        LatLng aux = new LatLng(
                                Double.valueOf(incidencia.getLatitud()),
                                Double.valueOf(incidencia.getLongitud())
                        );

                        map.addMarker(new MarkerOptions()
                                .title(incidencia.getProblema())
                                .snippet(incidencia.getDireccio())
                                .position(aux));

                        IncidenciesInfoWindowAdapter customInfoWindow = new IncidenciesInfoWindowAdapter(
                                getActivity()
                        );

                        Marker marker = map.addMarker(new MarkerOptions()
                                .title(incidencia.getProblema())
                                .snippet(incidencia.getDireccio())
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                                .position(aux));
                        marker.setTag(incidencia);

                        map.setInfoWindowAdapter(customInfoWindow);
                    }

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                    }

                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                    }

                    @Override
                    public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                    }
                });
            }
        });

        return root;
    }
}