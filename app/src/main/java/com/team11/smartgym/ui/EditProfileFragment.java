package com.team11.smartgym.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import com.team11.smartgym.data.PasswordHasher;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.team11.smartgym.R;
import com.team11.smartgym.data.User;
import com.team11.smartgym.data.UserRepo;

public class EditProfileFragment extends Fragment {

    EditText email, password, name, surname, username, height, weight, age;
    RadioButton maleOption, femaleOption;
    RadioButton inactiveOption, moderateActiveOption, activeOption;
    RadioGroup genderGroup, activityFrequencyGroup;
    TextView createAccount;
    Button saveButton;
    private UserRepo repo;
    private User currentUser;


    public EditProfileFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);

        repo = new UserRepo(requireContext());
        SharedPreferences prefs = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE);
        int userId = prefs.getInt("user_id", -1);
        email = view.findViewById(R.id.insertEmail);
        password = view.findViewById(R.id.insertPassword);
        name = view.findViewById(R.id.insertName);
        surname = view.findViewById(R.id.insertSurname);
        username = view.findViewById(R.id.insertUsername);
        height = view.findViewById(R.id.insertHeight);
        // weight/age fields may be absent in some layouts; handle nulls gracefully
        weight = view.findViewById(R.id.insertWeight);
        age = view.findViewById(R.id.insertAge);
        maleOption = view.findViewById(R.id.maleOption);
        femaleOption = view.findViewById(R.id.femaleOption);
        inactiveOption = view.findViewById(R.id.inactiveOption);
        moderateActiveOption = view.findViewById(R.id.moderateActiveOption);
        activeOption = view.findViewById(R.id.activeOption);

        saveButton = view.findViewById(R.id.save);
        // Disable save until current user is loaded
        saveButton.setEnabled(false);

        if (userId != -1) {
            repo.getUserById(userId, user -> {
                if (user != null) {
                    currentUser = user;
                    requireActivity().runOnUiThread(() -> {
                        email.setText(user.email);
                        password.setText("");
                        name.setText(user.name);
                        surname.setText(user.surname);
                        username.setText(user.username);
                        if (height != null) height.setText(String.valueOf(user.height));
                        if (weight != null) weight.setText(String.valueOf(user.weight));
                        if (age != null) age.setText(String.valueOf(user.age));

                        if ("Male".equals(user.gender)) {
                            maleOption.setChecked(true);
                        } else {
                            femaleOption.setChecked(true);
                        }

                        switch (user.activityFrequency) {
                            case 3:
                                activeOption.setChecked(true);
                                break;
                            case 2:
                                moderateActiveOption.setChecked(true);
                                break;
                            default:
                                inactiveOption.setChecked(true);
                        }

                        // user loaded, allow saving
                        if (saveButton != null) saveButton.setEnabled(true);
                    });
                }
            });
        }


        saveButton.setOnClickListener(v -> {
            if (currentUser == null) {
                Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT).show();
                return;
            }
            // Validate numeric fields only if the corresponding input views exist and are non-empty.
            if (height != null) {
                String h = height.getText().toString().trim();
                if (!h.isEmpty()) {
                    try {
                        Integer.parseInt(h);
                    } catch (NumberFormatException e) {
                        Toast.makeText(requireContext(), "Height must be a whole number (in cm)", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            }

            if (weight != null) {
                String w = weight.getText().toString().trim();
                if (!w.isEmpty()) {
                    try {
                        Integer.parseInt(w);
                    } catch (NumberFormatException e) {
                        Toast.makeText(requireContext(), "Weight must be a number (in lbs)", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            }

            if (age != null) {
                String a = age.getText().toString().trim();
                if (!a.isEmpty()) {
                    try {
                        Integer.parseInt(a);
                    } catch (NumberFormatException e) {
                        Toast.makeText(requireContext(), "Age must be a whole number", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            }
            String newEmail = email.getText().toString();

            repo.isEmailTaken(newEmail, isTaken -> {
                requireActivity().runOnUiThread(() -> {
                    if (isTaken && !newEmail.equals(currentUser.email)) {
                        Toast.makeText(requireContext(), "Email already in use by another account", Toast.LENGTH_SHORT).show();
                    } else {
                        saveProfileChanges();
                    }
                });
            });
        });



        return view;
    }
    private void saveProfileChanges() {
        currentUser.email = email.getText().toString();

        String newPassword = password.getText().toString();
        if (!newPassword.isEmpty()) {
            PasswordHasher.HashedPassword hp = PasswordHasher.hashPassword(newPassword);
            currentUser.passwordHash = hp.hashBase64;
            currentUser.passwordSalt = hp.saltBase64;
            currentUser.passwordIterations = hp.iterations;
        }

        currentUser.name = name.getText().toString();
        currentUser.surname = surname.getText().toString();
        currentUser.username = username.getText().toString();
        if (height != null) {
            String h = height.getText().toString().trim();
            if (!h.isEmpty()) currentUser.height = Integer.parseInt(h);
        }
        if (weight != null) {
            String w = weight.getText().toString().trim();
            if (!w.isEmpty()) currentUser.weight = Integer.parseInt(w);
        }
        if (age != null) {
            String a = age.getText().toString().trim();
            if (!a.isEmpty()) currentUser.age = Integer.parseInt(a);
        }
        currentUser.gender = (maleOption.isChecked() ? "Male" : "Female");
        currentUser.activityFrequency =
                (activeOption.isChecked() ? 3 :
                        (moderateActiveOption.isChecked() ? 2 : 1));

        repo.updateUser(currentUser);
        // Update stored session email/username if changed so app reflects edits immediately
        try {
            SharedPreferences prefs = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE);
            prefs.edit()
                    .putString("user_email", currentUser.email)
                    .putString("user_username", currentUser.username)
                    .apply();
        } catch (Exception ignored) {}

        Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
    }
}