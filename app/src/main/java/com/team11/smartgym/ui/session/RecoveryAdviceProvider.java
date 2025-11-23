package com.team11.smartgym.ui.session;

import androidx.annotation.Nullable;

import com.team11.smartgym.data.AdviceEngine;

/**
 * Wrapper class used by UI or ViewModels to format recovery advice.
 * Simply delegates to AdviceEngine.computeAdvice() with null-safe parameters.
 */
public final class RecoveryAdviceProvider {

    private RecoveryAdviceProvider() {}

    /**
     * Compute human-readable advice given RMSSD + LF/HF.
     *
     * rmssdMs  – HRV RMSSD (milliseconds)
     * lfHf     – LF/HF stress ratio
     *
     * Returns fallback messages for insufficient data.
     */
    public static String getAdvice(@Nullable Double rmssdMs,
                                   @Nullable Double lfHf) {

        // Delegate to the DS-07 AdviceEngine
        return AdviceEngine.computeAdvice(rmssdMs, lfHf);
    }
}

