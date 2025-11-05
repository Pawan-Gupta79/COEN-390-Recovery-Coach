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

    public void login(String email, String password, LoginCallback callback) {
        executorService.execute(() -> {
            User user = userDao.login(email, password);
            callback.onResult(user);
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
}
