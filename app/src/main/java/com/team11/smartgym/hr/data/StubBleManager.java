package com.team11.smartgym.hr.data;

import android.os.Handler;
import android.os.Looper;

import java.util.Random;

public class StubBleManager implements BleManager {

    private boolean connected = false;



        handler.postDelayed(() -> {
            connected = true;
            if (connectionListener != null) connectionListener.onConnected();
        }, 700);
    }

        connected = false;
    }

    }

    }

    }

}
