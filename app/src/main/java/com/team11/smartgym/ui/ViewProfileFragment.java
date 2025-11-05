package com.team11.smartgym.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.team11.smartgym.R;
import com.team11.smartgym.data.User;
import com.team11.smartgym.data.UserRepo;


public class ViewProfileFragment extends Fragment {

    TextView userEmail, userNameSurname, userUsername, userHeight, userGender, userActivityFrequency;
    private UserRepo repo;
    private User currentUser;


    public ViewProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_view_profile, container, false);
        repo = new UserRepo(requireContext());
        SharedPreferences prefs = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE);
        int userId = prefs.getInt("user_id", -1);
        userEmail = view.findViewById(R.id.userEmail);
        userNameSurname = view.findViewById(R.id.userNameSurname);
        userUsername = view.findViewById(R.id.userUsername);
        userHeight = view.findViewById(R.id.usertHeight);
        userGender = view.findViewById(R.id.userGender);
        userActivityFrequency = view.findViewById(R.id.userActivityFrequency);

        if (userId != -1) {
            repo.getUserById(userId, user -> {
                if (user != null) {
                    currentUser = user;
                    requireActivity().runOnUiThread(() -> {
                        userEmail.setText(user.email);
                        String fullName = user.name + " " + user.surname;
                        userNameSurname.setText(fullName);
                        userUsername.setText(user.username);
                        userHeight.setText(String.valueOf(user.height));
                        userGender.setText(user.gender);
                        switch (user.activityFrequency) {
                            case 3:
                                userActivityFrequency.setText("Active");
                                break;
                            case 2:
                                userActivityFrequency.setText("Moderate Active");
                                break;
                            default:
                                userActivityFrequency.setText("Inactive");
                        }


                    });
                }
            });
        }

        // Inflate the layout for this fragment
        return view;
    }
}