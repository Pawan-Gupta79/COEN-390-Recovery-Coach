package com.team11.smartgym.data;

import android.content.Context;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserRepo {

    private final UserDao userDao;
    private final ExecutorService executorService;

    public UserRepo(Context context) {
        AppDb db = AppDb.get(context);
        userDao = db.userDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    public void insertUser(User user) {
        executorService.execute(() -> userDao.insertUser(user));
    }

    public interface LoginCallback {
        void onResult(User user);
    }

    /**
     * Login by email + plaintext password.
     * This method hashes the plaintext and compares it to the stored hash+salt.
     */
    public void login(String email, String plainPassword, LoginCallback callback) {
        executorService.execute(() -> {
            User user = userDao.getUserByEmail(email);
            if (user == null) {
                callback.onResult(null);
                return;
            }

            boolean ok = PasswordHasher.verifyPassword(
                    plainPassword,
                    user.passwordHash,
                    user.passwordSalt,
                    user.passwordIterations
            );
            callback.onResult(ok ? user : null);
        });
    }

    public interface GetUserCallback {
        void onResult(User user);
    }

    public void getUserById(int id, GetUserCallback callback) {
        executorService.execute(() -> {
            User user = userDao.getUserById(id);
            callback.onResult(user);
        });
    }

    public void updateUser(User user) {
        executorService.execute(() -> userDao.updateUser(user));
    }

    public interface EmailCheckCallback {
        void onResult(boolean exists);
    }

    public void isEmailTaken(String email, EmailCheckCallback callback) {
        executorService.execute(() -> {
            boolean exists = userDao.countUsersByEmail(email) > 0;
            callback.onResult(exists);
        });
    }

    public void insertDefaultUser() {
        executorService.execute(() -> {

            boolean exists = userDao.countUsersByEmail("admin@example.com") > 0;
            if (!exists) {

                String defaultPassword = "123456";
                PasswordHasher.HashedPassword hashed = PasswordHasher.hashPassword(defaultPassword);

                User defaultUser = new User();
                defaultUser.email = "a@email.com";
                defaultUser.name = "Admin";
                defaultUser.passwordHash = hashed.hashBase64;
                defaultUser.passwordSalt = hashed.saltBase64;
                defaultUser.passwordIterations = hashed.iterations;

                userDao.insertUser(defaultUser);
            }
        });
    }
}
