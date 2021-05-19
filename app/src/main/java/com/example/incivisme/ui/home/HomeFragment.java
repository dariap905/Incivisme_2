package com.example.incivisme.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.incivisme.Incidencia;
import com.example.incivisme.R;
import com.firebase.ui.database.FirebaseListAdapter;
import com.firebase.ui.database.FirebaseListOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class HomeFragment extends Fragment {

    private HomeViewModel homeViewModel;

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        DatabaseReference base = FirebaseDatabase.getInstance("https://incivisme-b9ce2-default-rtdb.firebaseio.com/").getReference();

        DatabaseReference users = base.child("users");
        DatabaseReference uid = users.child(auth.getUid());
        DatabaseReference incidencies = uid.child("incidencies");


        FirebaseListOptions<Incidencia> options = new FirebaseListOptions.Builder<Incidencia>()
                .setQuery(incidencies, Incidencia.class)
                .setLayout(R.layout.lv_incidencies_item)
                .setLifecycleOwner(this)
                .build();


        FirebaseListAdapter<Incidencia> adapter = new FirebaseListAdapter<Incidencia>(options) {
            @Override
            protected void populateView(View v, Incidencia model, int position) {
                TextView txtDescripcio = v.findViewById(R.id.txtDescripcio);
                TextView txtAdreca = v.findViewById(R.id.txtAdreca);

                txtDescripcio.setText(model.getProblema());
                txtAdreca.setText(model.getDireccio());
            }
        };

        ListView lvIncidencies = view.findViewById(R.id.lvIncidencies);
        lvIncidencies.setAdapter(adapter);


        return view;
    }
}