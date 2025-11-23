package com.team11.smartgym.data;

import java.util.Locale;

/**
 * DS-07.1: Advice rubric + thresholds for HRV-based recovery guidance.
 *
 * Inputs you’ll eventually have:
 *  - rmssdMs: time-domain HRV (ms)
 *  - lfHf: LF/HF power ratio (unitless), optional if not available
 *
 * We map to tiers and short, actionable copy for the UI.
 *
 * NOTE: Thresholds are simple defaults and easy to tune later.
 * Add TODOs for Sprint 3 calibration against your datasets.
 */
public final class AdviceRubric {

    /** High-level advice tier for the UI. */
    public enum Tier {
        RECOVERED,     // good readiness; normal/harder session ok
        MODERATE,      // train but keep it steady / moderate
        STRAINED,      // prioritize recovery / light only
        INVALID        // insufficient or noisy data
    }

    /** Encapsulates the final mapping result. */
    public static final class Advice {
        public final Tier tier;
        public final String title;
        public final String shortText;

        public Advice(Tier tier, String title, String shortText) {
            this.tier = tier;
            this.title = title;
            this.shortText = shortText;
        }

        @Override public String toString() {
            return String.format(Locale.US, "[%s] %s — %s", tier, title, shortText);
        }
    }

    // ----------------- Thresholds (tunable) -----------------

    // Time-domain HRV (RMSSD, ms)
    // >= 60ms → strong parasympathetic tone / recovered (for many adults)
    // 35–59ms → moderate
    // < 35ms  → strained / lower readiness
    public static final int RMSSD_RECOVERED_MIN = 60;
    public static final int RMSSD_MODERATE_MIN  = 35;

    // LF/HF ratio (frequency-domain balance)
    // ~0.5–2.0 balanced; >2.5 suggests sympathetic load; <0.5 strong parasympath.
    public static final double LFHF_BALANCED_MIN = 0.5;
    public static final double LFHF_BALANCED_MAX = 2.0;
    public static final double LFHF_HIGH_SYMPATHETIC = 2.5;
    public static final double LFHF_LOW_SYMPATHETIC  = 0.4;

    private AdviceRubric() {}

    /**
     * Core rubric logic (DS-07.1).
     * If lfHf is NaN, we decide only by RMSSD.
     *
     * @param rmssdMs   RMSSD in ms (>=0), or <0 if unavailable
     * @param lfHfRatio LF/HF ratio (>=0), or NaN if unavailable
     */
    public static Advice evaluate(double rmssdMs, double lfHfRatio) {
        // Sanity checks
        boolean rmssdValid = rmssdMs >= 0 && rmssdMs < 3000; // reject wild values
        boolean lfHfValid = !Double.isNaN(lfHfRatio) && lfHfRatio >= 0 && lfHfRatio < 50;

        if (!rmssdValid && !lfHfValid) {
            return new Advice(Tier.INVALID, "Insufficient data",
                    "We couldn’t compute reliable HRV. Re-measure when you’re still for 60–90s.");
        }

        // Primary decision by RMSSD (available more often)
        Tier byRmssd;
        if (!rmssdValid) {
            byRmssd = null; // fall back to LF/HF only
        } else if (rmssdMs >= RMSSD_RECOVERED_MIN) {
            byRmssd = Tier.RECOVERED;
        } else if (rmssdMs >= RMSSD_MODERATE_MIN) {
            byRmssd = Tier.MODERATE;
        } else {
            byRmssd = Tier.STRAINED;
        }

        // Modifier by LF/HF (if available)
        Tier byLfHf = null;
        if (lfHfValid) {
            if (lfHfRatio > LFHF_HIGH_SYMPATHETIC) {
                byLfHf = Tier.STRAINED;  // sympathetic dominance
            } else if (lfHfRatio < LFHF_LOW_SYMPATHETIC) {
                byLfHf = Tier.MODERATE;  // very low LF/HF: caution (fatigue/over-parasymp)
            } else if (lfHfRatio >= LFHF_BALANCED_MIN && lfHfRatio <= LFHF_BALANCED_MAX) {
                byLfHf = Tier.RECOVERED; // balanced
            } else {
                byLfHf = Tier.MODERATE;  // mildly skewed
            }
        }

        // Combine (simple rule-based consensus)
        Tier finalTier = combine(byRmssd, byLfHf);

        // Copy for UI (short, actionable)
        switch (finalTier) {
            case RECOVERED:
                return new Advice(finalTier, "Recovered",
                        "You’re good to go. Train normally or add intensity if planned.");
            case MODERATE:
                return new Advice(finalTier, "Train Easy-Moderate",
                        "Keep it steady today. Prioritize technique and avoid maximal work.");
            case STRAINED:
                return new Advice(finalTier, "Prioritize Recovery",
                        "Go light or rest. Focus on mobility, sleep, and hydration.");
            default:
                return new Advice(finalTier, "Insufficient Data",
                        "Re-measure seated for 60–90s (stable breathing).");
        }
    }

    private static Tier combine(Tier a, Tier b) {
        if (a == null) return (b != null) ? b : Tier.INVALID;
        if (b == null) return a;

        // If any says STRAINED, bias conservative.
        if (a == Tier.STRAINED || b == Tier.STRAINED) return Tier.STRAINED;

        // If both RECOVERED → RECOVERED
        if (a == Tier.RECOVERED && b == Tier.RECOVERED) return Tier.RECOVERED;

        // Any mix with MODERATE → MODERATE
        return Tier.MODERATE;
    }
}
