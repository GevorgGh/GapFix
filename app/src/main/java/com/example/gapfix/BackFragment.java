package com.example.gapfix;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link BackFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BackFragment extends Fragment {



    public BackFragment() {
        // Required empty public constructor
    }



    public static BackFragment newInstance() {
        BackFragment fragment = new BackFragment();
        Bundle args = new Bundle();

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_back, container, false);

        LinearLayout back = view.findViewById(R.id.back);
        back.setOnClickListener(v -> {
            requireActivity().finish();
        });

        return view;

    }


}