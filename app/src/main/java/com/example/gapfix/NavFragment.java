package com.example.gapfix;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.fragment.app.Fragment;

public class NavFragment extends Fragment {

    private ImageView imgShop, imgArchive, imgUser, imgCal, imgPerson;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_nav, container, false);

        imgShop = view.findViewById(R.id.dashboard);
        imgArchive = view.findViewById(R.id.calendar);
        imgUser = view.findViewById(R.id.chat);
        imgCal = view.findViewById(R.id.subjects);
        imgPerson = view.findViewById(R.id.profile);

        imgShop.setOnClickListener(v -> updateMenu(R.id.dashboard));
        imgArchive.setOnClickListener(v -> updateMenu(R.id.calendar));
        imgUser.setOnClickListener(v -> updateMenu(R.id.chat));
        imgCal.setOnClickListener(v -> updateMenu(R.id.subjects));
        imgPerson.setOnClickListener(v -> updateMenu(R.id.profile));

        updateMenu(R.id.dashboard);

        return view;
    }

    public void updateMenu(int clickedId) {
        imgShop.setImageResource(R.drawable.shop);
        imgArchive.setImageResource(R.drawable.archive);
        imgUser.setImageResource(R.drawable.chat_unfilled);
        imgCal.setImageResource(R.drawable.cal);
        imgPerson.setImageResource(R.drawable.person);

        if (clickedId == R.id.dashboard) {
            imgShop.setImageResource(R.drawable.shop_clicked);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new TutorShopFragment())
                    .commit();
        } else if (clickedId == R.id.calendar) {
            imgArchive.setImageResource(R.drawable.archive_clicked);
        } else if (clickedId == R.id.chat) {
            imgUser.setImageResource(R.drawable.chat_filled);
        } else if (clickedId == R.id.subjects) {
            imgCal.setImageResource(R.drawable.cal_clicked);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new StudentCalendarFragment())
                    .commit();
        } else if (clickedId == R.id.profile) {
            imgPerson.setImageResource(R.drawable.person_clicked);
        }
    }
}