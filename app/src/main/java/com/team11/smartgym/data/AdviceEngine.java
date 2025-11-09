package com.team11.smartgym.data;

/**
 * DS-07: AdviceEngine
 *
 * Maps RMSSD & LF/HF metrics to simple recovery advice.
 * Tier-based rubric from DS-07.1 and DS-07.2.
 *
 * Handles null/missing/invalid inputs gracefully.
 */
public final class AdviceEngine {

    private AdviceEngine() {}

    /**
     * Compute advice from RMSSD + LF/HF.
     *
     * @param rmssd  Root-mean-square of successive differences (HRV)
     * @param lfHf   LF/HF ratio (stress indicator)
     * @return       Advice string
     */
    public static String computeAdvice(Double rmssd, Double lfHf) {

        // ------------------------------
        // Missing or invalid input
        // ------------------------------
        if (rmssd == null || lfHf == null || rmssd <= 0 || lfHf <= 0) {
            return "INSUFFICIENT DATA — Heartbeat variation too low or incomplete readings.";
        }

        // ------------------------------
        // Tier thresholds (simple rubric)
        // ------------------------------

        // RMSSD:
        // < 20 ms  → Low recovery
        // 20–40 ms → Moderate
        // > 40 ms  → High recovery

        // LF/HF ratio:
        // > 2.0 → Sympathetic dominant (stress)
        // 1.0–2.0 → Balanced / moderate stress
        // < 1.0 → Parasympathetic dominant (relaxed)

        boolean lowRecovery = rmssd < 20;
        boolean moderateRecovery = rmssd >= 20 && rmssd <= 40;
        boolean highRecovery = rmssd > 40;

        boolean highStress = lfHf > 2.0;
        boolean moderateStress = lfHf >= 1.0 && lfHf <= 2.0;
        boolean lowStress = lfHf < 1.0;

        // ------------------------------
        // Combine rubric into advice
        // ------------------------------
        if (lowRecovery && highStress) {
            return "REST NEEDED — High stress + low recovery. Avoid intense training today.";
        }

        if (lowRecovery && moderateStress) {
            return "LIGHT ACTIVITY — Recovery low. Prefer walking, stretching, or mobility work.";
        }

        if (lowRecovery && lowStress) {
            return "RECOVERY TRENDING UP — Stress low, but recovery still limited. Keep intensity low.";
        }

        if (moderateRecovery && highStress) {
            return "CAUTION — Recovery moderate but stress elevated. Consider shorter training.";
        }

        if (moderateRecovery && moderateStress) {
            return "GOOD TO TRAIN — Balanced profile. Moderate intensity is appropriate.";
        }

        if (moderateRecovery && lowStress) {
            return "SOLID READINESS — Low stress + moderate HRV. Training should feel good.";
        }

        if (highRecovery && highStress) {
            return "UNUSUAL MIX — High HRV with high stress. Warm up longer and monitor how you feel.";
        }

        if (highRecovery && moderateStress) {
            return "TRAINING READY — Strong recovery. Normal to high intensity is fine.";
        }

        if (highRecovery && lowStress) {
            return "OPTIMAL — High recovery and low stress. Great day for a strong workout!";
        }

        // fallback
        return "INSUFFICIENT DATA";
    }
}
