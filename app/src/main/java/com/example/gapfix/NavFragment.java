package com.example.gapfix;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link NavFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class NavFragment extends Fragment {

    public NavFragment() {
        // Required empty public constructor
    }
    // TODO: Rename and change types and number of parameters
    public static NavFragment newInstance() {
        return new NavFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_nav, container, false);
    }
}