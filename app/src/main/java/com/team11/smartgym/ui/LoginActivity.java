package com.team11.smartgym.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.team11.smartgym.R;
import com.team11.smartgym.data.PasswordHasher;
import com.team11.smartgym.data.SessionManager;
import com.team11.smartgym.data.User;
import com.team11.smartgym.data.UserRepo;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private TextView tvCreateAccount;

    private SessionManager session;
    private UserRepo userRepo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new SessionManager(this);
        userRepo = new UserRepo(this);

        // If already logged in, go straight to main app
        if (session.isLoggedIn()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);
        //insert admin account email "a@email.com" pass "123456"
        userRepo.insertDefaultUser();
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvCreateAccount = findViewById(R.id.tvCreateAccount);

        btnLogin.setOnClickListener(this::attemptLogin);

        // Open account creation screen
        tvCreateAccount.setOnClickListener(v -> {
            Intent i = new Intent(this, CreateAccountActivity.class);
            startActivity(i);
        });
    }

    private void attemptLogin(View anchor) {
        // Clear previous errors
        tilEmail.setError(null);
        tilPassword.setError(null);

        String email = etEmail.getText() == null ? "" : etEmail.getText().toString().trim();
        String pass  = etPassword.getText() == null ? "" : etPassword.getText().toString();

        boolean ok = true;

        // Basic input validation
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

        // Disable button while we verify credentials
        btnLogin.setEnabled(false);

        // Real DB-backed login using hashed credentials
        userRepo.login(email, pass, user -> runOnUiThread(() -> {
            // Re-enable button when result arrives
            btnLogin.setEnabled(true);

            if (isFinishing() || isDestroyed()) {
                return;
            }

            if (user == null) {
                // Incorrect email/password combination → clean failure
                tilPassword.setError(getString(R.string.err_bad_credentials));
                Snackbar.make(anchor, R.string.err_bad_credentials, Snackbar.LENGTH_SHORT).show();
                return;
            }

            // Correct credentials → success path

            // 1) Mark session as logged in
            session.setLoggedIn(email);

            // 2) Store user_id + email for settings/profile
            SharedPreferences prefs =
                    getSharedPreferences("user_session", MODE_PRIVATE);
            prefs.edit()
                    .putInt("user_id", user.id)
                    .putString("user_email", user.email)
                    .apply();

            // 3) Navigate to MainActivity
            Snackbar.make(anchor, R.string.login_success, Snackbar.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }));
    }
}
