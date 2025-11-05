package com.team11.smartgym.hr.util;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

/** Very small one-shot event wrapper for LiveData (prevents duplicate snackbars). */
public class Event<T> extends MutableLiveData<T> {

    private boolean handled = false;

    @Override
    public void setValue(T value) {
        handled = false;
        super.setValue(value);
    }

    @Override
    public void postValue(T value) {
        handled = false;
        super.postValue(value);
    }

    public void observe(LifecycleOwner owner, Observer<? super T> observer) {
        super.observe(owner, t -> {
            if (handled) return;
            handled = true;
            observer.onChanged(t);
        });
    }

    /** Emit without removing existing observers. */
    public void emit(T value) { postValue(value); }
}
