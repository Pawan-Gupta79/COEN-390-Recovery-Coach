package com.team11.smartgym.ui;

import android.os.Bundle;

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


public class CreateAccountFragment extends Fragment {

    EditText email, password, name, surname, username, height, gender, activityFrequency;
    RadioButton maleOption, femaleOption;
    RadioButton inactiveOption, moderateActiveOption, activeOption;
    RadioGroup genderGroup, activityFrequencyGroup;
    TextView createAccount;
    private UserRepo repo;





    public CreateAccountFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_create_account, container, false);
        repo = new UserRepo(requireContext());
        email = view.findViewById(R.id.insertEmail);
        password = view.findViewById(R.id.insertPassword);
        name = view.findViewById(R.id.insertName);
        surname = view.findViewById(R.id.insertSurname);
        username = view.findViewById(R.id.insertUsername);
        height = view.findViewById(R.id.insertHeight);
        maleOption = view.findViewById(R.id.maleOption);
        femaleOption = view.findViewById(R.id.femaleOption);
        inactiveOption = view.findViewById(R.id.inactiveOption);
        moderateActiveOption = view.findViewById(R.id.moderateActiveOption);
        activeOption = view.findViewById(R.id.activeOption);
        createAccount = view.findViewById(R.id.createAccount);

        createAccount.setOnClickListener(v -> {
            if (validateInputs()) {
                // Create a new user
                User user = new User();
                user.email = email.getText().toString();
                user.password = password.getText().toString();
                user.name = name.getText().toString();
                user.surname = surname.getText().toString();
                user.username = username.getText().toString();
                user.height = Integer.parseInt(height.getText().toString());
                user.gender = maleOption.isChecked() ? "Male" : "Female";
                user.activityFrequency = inactiveOption.isChecked() ? 1 :
                        moderateActiveOption.isChecked() ? 2 : 3;
                repo.insertUser(user);
                Toast.makeText(requireContext(), "Account created successfully!", Toast.LENGTH_SHORT).show();
                requireActivity().getOnBackPressedDispatcher().onBackPressed();
            }
        });


        return view;
    }

    private boolean validateInputs() {
        boolean areTextFieldsEmpty = email.getText().toString().isEmpty() ||
                password.getText().toString().isEmpty() ||
                name.getText().toString().isEmpty() ||
                surname.getText().toString().isEmpty() ||
                username.getText().toString().isEmpty() ||
                height.getText().toString().isEmpty();
        if (areTextFieldsEmpty) {
            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!maleOption.isChecked() && !femaleOption.isChecked()) {
            Toast.makeText(requireContext(), "Please select a gender", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!inactiveOption.isChecked() && !moderateActiveOption.isChecked() && !activeOption.isChecked()) {
            Toast.makeText(requireContext(), "Please select an activity frequency", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
}