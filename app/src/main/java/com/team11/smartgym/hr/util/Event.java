package com.team11.smartgym.hr.util;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

public class Event<T> extends MutableLiveData<T> {

        super.observe(owner, t -> {
                observer.onChanged(t);
        });
    }

}
