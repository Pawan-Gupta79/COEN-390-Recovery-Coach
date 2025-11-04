package com.team11.smartgym.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.team11.smartgym.R;
import com.team11.smartgym.data.SessionManager;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private SessionManager session;

    // Demo credentials (replace with real auth later)
    private static final String DEMO_EMAIL = "user@example.com";
    private static final String DEMO_PASS  = "password123";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new SessionManager(this);

        // If already logged in, skip to Main/Dashboard
        if (session.isLoggedIn()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(this::attemptLogin);
    }

    private void attemptLogin(View anchor) {
        tilEmail.setError(null);
        tilPassword.setError(null);

        String email = etEmail.getText() == null ? "" : etEmail.getText().toString().trim();
        String pass  = etPassword.getText() == null ? "" : etPassword.getText().toString();

        boolean ok = true;

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.err_invalid_email));
            ok = false;
        }

        if (pass.isEmpty() || pass.length() < 6) {
            tilPassword.setError(getString(R.string.err_invalid_password));
            ok = false;
        }

        if (!ok) {
            Snackbar.make(anchor, R.string.err_fix_inputs, Snackbar.LENGTH_SHORT).show();
            return;
        }

        // Fake credential check
        if (!email.equalsIgnoreCase(DEMO_EMAIL) || !pass.equals(DEMO_PASS)) {
            Snackbar.make(anchor, R.string.err_bad_credentials, Snackbar.LENGTH_SHORT).show();
            return;
        }

        // Success → persist session and go to Main
        session.setLoggedIn(email);
        Snackbar.make(anchor, R.string.login_success, Snackbar.LENGTH_SHORT).show();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}

