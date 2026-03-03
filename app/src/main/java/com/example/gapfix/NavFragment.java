package com.example.gapfix;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class NavFragment extends Fragment {

    private ImageView imgShop, imgArchive, imgUser, imgCal, imgPerson;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_nav, container, false);

        imgShop = view.findViewById(R.id.imageView3);
        imgArchive = view.findViewById(R.id.imageView6);
        imgUser = view.findViewById(R.id.imageView4);
        imgCal = view.findViewById(R.id.imageView5);
        imgPerson = view.findViewById(R.id.imageView7);

        imgShop.setOnClickListener(v -> updateMenu(R.id.imageView3));
        imgArchive.setOnClickListener(v -> updateMenu(R.id.imageView6));
        imgUser.setOnClickListener(v -> updateMenu(R.id.imageView4));
        imgCal.setOnClickListener(v -> updateMenu(R.id.imageView5));
        imgPerson.setOnClickListener(v -> updateMenu(R.id.imageView7));

        updateMenu(R.id.imageView3);

        return view;
    }

    public void updateMenu(int clickedId) {
        imgShop.setImageResource(R.drawable.shop);
        imgArchive.setImageResource(R.drawable.archive);
        imgUser.setImageResource(R.drawable.user_act);
        imgCal.setImageResource(R.drawable.cal);
        imgPerson.setImageResource(R.drawable.person);

        if (clickedId == R.id.imageView3) {
            imgShop.setImageResource(R.drawable.shop_clicked);
        } else if (clickedId == R.id.imageView6) {
            imgArchive.setImageResource(R.drawable.archive_clicked);
        } else if (clickedId == R.id.imageView4) {
            imgUser.setImageResource(R.drawable.user_act_clicked);
        } else if (clickedId == R.id.imageView5) {
            imgCal.setImageResource(R.drawable.cal_clicked);
        } else if (clickedId == R.id.imageView7) {
            imgPerson.setImageResource(R.drawable.person_clicked);
        }
    }
}