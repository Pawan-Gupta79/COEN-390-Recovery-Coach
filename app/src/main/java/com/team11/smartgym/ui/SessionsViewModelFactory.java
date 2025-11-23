package com.team11.smartgym.ui;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.team11.smartgym.data.SessionRepository;

public class SessionsViewModelFactory implements ViewModelProvider.Factory {

    private final SessionRepository repo;

    public SessionsViewModelFactory(SessionRepository repo) {
        this.repo = repo;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(SessionsViewModel.class)) {
            return (T) new SessionsViewModel(repo);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
