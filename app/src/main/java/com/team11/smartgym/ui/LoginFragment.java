package com.team11.smartgym.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.team11.smartgym.R;
import com.team11.smartgym.data.UserRepo;

public class LoginFragment extends Fragment {

    EditText email, password;
    Button login;
    TextView createAccount;
    private UserRepo repo;

    public LoginFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_login, container, false);
        EditText email = view.findViewById(R.id.insertEmail);
        EditText password = view.findViewById(R.id.insertPassword);
        Button login = view.findViewById(R.id.login);
        TextView createAccount = view.findViewById(R.id.createAccount);
        repo = new UserRepo(requireContext());
        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host);

        login.setOnClickListener(v -> {
            String emailText = email.getText().toString();
            String passwordText = password.getText().toString();
            if (TextUtils.isEmpty(emailText) || TextUtils.isEmpty(passwordText)) {
                Toast.makeText(requireContext(), "Please enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            repo.login(emailText, passwordText, user -> {
                requireActivity().runOnUiThread(() -> {
                    if (user != null) {
                        SharedPreferences prefs = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE);
                        prefs.edit()
                                .putInt("user_id", user.id)
                                .putString("user_email", user.email)
                                .apply();
                        Toast.makeText(requireContext(), "Login successful! Welcome " + user.name, Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(requireActivity(), MainActivity.class);
                        startActivity(intent);
                        requireActivity().finish();

                    } else {
                        Toast.makeText(requireContext(), "Invalid email or password", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });

        createAccount.setOnClickListener(v -> {
            navController.navigate(R.id.action_loginFragment_to_createAccountFragment);

        });


            // Inflate the layout for this fragment
        return view;
    }
    
}