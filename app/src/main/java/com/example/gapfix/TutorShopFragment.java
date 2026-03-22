package com.example.gapfix;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link TutorShopFragment#newInstance} factory method to
 * create an instance of this fragment.
 */

public class TutorShopFragment extends Fragment {

    public TutorShopFragment() {
        // Required empty public constructor
    }

    public static TutorShopFragment newInstance(String param1, String param2) {
        return new TutorShopFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tutor_shop_fragmeent, container, false);
    }
}