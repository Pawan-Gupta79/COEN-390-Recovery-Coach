package com.team11.smartgym.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.util.Log;
import android.widget.Toast;

import com.team11.smartgym.R;
import com.team11.smartgym.data.User;
import com.team11.smartgym.data.UserRepo;


public class ViewProfileFragment extends Fragment {

    TextView userEmail, userNameSurname, userUsername, userHeight, userWeight, userAge, userGender, userActivityFrequency;
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
        userHeight = view.findViewById(R.id.userHeight);
        userWeight = view.findViewById(R.id.userWeight);
        userAge = view.findViewById(R.id.userAge);
        userGender = view.findViewById(R.id.userGender);
        userActivityFrequency = view.findViewById(R.id.userActivityFrequency);

        if (userId != -1) {
            try {
                repo.getUserById(userId, user -> {
                    try {
                        if (user != null) {
                            currentUser = user;
                            requireActivity().runOnUiThread(() -> {
                                try {
                                    // Only update UI when fragment is added to activity
                                    if (!isAdded() || getActivity() == null) {
                                        Log.w("ViewProfileFragment", "Fragment not attached; skipping UI update");
                                        return;
                                    }

                                    // Update fields individually, guarding each with try/catch so one bad value won't crash the fragment
                                    try {
                                        if (userEmail != null) userEmail.setText(user.email != null ? user.email : "");
                                    } catch (Exception e) {
                                        Log.e("ViewProfileFragment", "Failed to set userEmail", e);
                                    }

                                    try {
                                        if (userNameSurname != null) {
                                            String fullName = (user.name != null ? user.name : "") + " " + (user.surname != null ? user.surname : "");
                                            userNameSurname.setText(fullName.trim());
                                        }
                                    } catch (Exception e) {
                                        Log.e("ViewProfileFragment", "Failed to set userNameSurname", e);
                                    }

                                    try {
                                        if (userUsername != null) userUsername.setText(user.username != null ? user.username : "");
                                    } catch (Exception e) {
                                        Log.e("ViewProfileFragment", "Failed to set userUsername", e);
                                    }

                                    try {
                                        if (userHeight != null) {
                                            if (user.height > 0) {
                                                userHeight.setText(String.format(java.util.Locale.getDefault(), "%d cm", user.height));
                                            } else {
                                                userHeight.setText("--");
                                            }
                                        }
                                    } catch (Exception e) {
                                        Log.e("ViewProfileFragment", "Failed to set userHeight", e);
                                    }

                                    try {
                                        if (userWeight != null) {
                                            if (user.weight > 0) {
                                                userWeight.setText(String.format(java.util.Locale.getDefault(), "%d lbs", user.weight));
                                            } else {
                                                userWeight.setText("--");
                                            }
                                        }
                                    } catch (Exception e) {
                                        Log.e("ViewProfileFragment", "Failed to set userWeight", e);
                                    }

                                    try {
                                        if (userAge != null) {
                                            if (user.age > 0) {
                                                userAge.setText(String.format(java.util.Locale.getDefault(), "%d yrs", user.age));
                                            } else {
                                                userAge.setText("--");
                                            }
                                        }
                                    } catch (Exception e) {
                                        Log.e("ViewProfileFragment", "Failed to set userAge", e);
                                    }

                                    try {
                                        if (userGender != null) userGender.setText(user.gender != null ? user.gender : "");
                                    } catch (Exception e) {
                                        Log.e("ViewProfileFragment", "Failed to set userGender", e);
                                    }

                                    try {
                                        if (userActivityFrequency != null) {
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
                                        }
                                    } catch (Exception e) {
                                        Log.e("ViewProfileFragment", "Failed to set userActivityFrequency", e);
                                    }
                                } catch (Exception uiEx) {
                                    Log.e("ViewProfileFragment", "UI update failed", uiEx);
                                    Toast.makeText(requireContext(), "Failed to display profile", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } catch (Exception cbEx) {
                        Log.e("ViewProfileFragment", "Error in getUserById callback", cbEx);
                        requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show());
                    }
                });
            } catch (Exception ex) {
                Log.e("ViewProfileFragment", "Failed to request user", ex);
                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show());
            }
        }

        // Inflate the layout for this fragment
        return view;
    }

}