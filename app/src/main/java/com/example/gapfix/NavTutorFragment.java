package com.example.gapfix;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class NavTutorFragment extends Fragment {

    private ImageView dashboard;
    private ImageView calendar;
    private ImageView subjects;
    private ImageView chat;
    private ImageView profile;

    public NavTutorFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_nav_tutor, container, false);

        // 1. Initialize Views
        dashboard = view.findViewById(R.id.dashboard);
        calendar = view.findViewById(R.id.calendar);
        subjects = view.findViewById(R.id.subjects);
        chat = view.findViewById(R.id.chat);
        profile = view.findViewById(R.id.profile);

        // 2. Add Click Listeners
        View.OnClickListener navClickListener = v -> updateMenu(v.getId());

        dashboard.setOnClickListener(navClickListener);
        calendar.setOnClickListener(navClickListener);
        subjects.setOnClickListener(navClickListener);
        chat.setOnClickListener(navClickListener);
        profile.setOnClickListener(navClickListener);

        // 3. Set Dashboard as DEFAULT clicked state on load
        updateMenu(R.id.dashboard);

        return view;
    }

    public void updateMenu(int clickedId) {
        // Reset all to unclicked state
        dashboard.setImageResource(R.drawable.user_act);
        calendar.setImageResource(R.drawable.cal);
        subjects.setImageResource(R.drawable.journal);
        chat.setImageResource(R.drawable.chat_unfilled);
        profile.setImageResource(R.drawable.person);

        if (clickedId == R.id.dashboard) {
            dashboard.setImageResource(R.drawable.user_act_clicked);
            switchFragment(new TutorDashboardFragment());
        } else if (clickedId == R.id.calendar) {
            calendar.setImageResource(R.drawable.cal_clicked);
            switchFragment(new TutorCalendarFragment());
        } else if (clickedId == R.id.subjects) {
            subjects.setImageResource(R.drawable.journal_clicked);
            switchFragment(new TutorSubjectFragment());
        } else if (clickedId == R.id.chat) {
            chat.setImageResource(R.drawable.chat_filled);
            switchFragment(new ChatListFragment());
        } else if (clickedId == R.id.profile) {
            profile.setImageResource(R.drawable.person_clicked);
            switchFragment(new TutorSettingsFragment());
        }
    }

    // Helper method to keep updateMenu clean
    private void switchFragment(Fragment fragment) {
        if (getActivity() != null) {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
        }
    }
}
