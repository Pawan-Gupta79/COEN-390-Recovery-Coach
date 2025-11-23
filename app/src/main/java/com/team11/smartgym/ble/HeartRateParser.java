package com.team11.smartgym.ble;

import androidx.annotation.Nullable;

/**
 * Parser for BLE Heart Rate Measurement (UUID 0x2A37).
 *
 * Supports:
 *  - 8-bit and 16-bit heart rate values
 *  - Returns null for malformed / too-short frames
 */
public final class HeartRateParser {

    private HeartRateParser() {
        // Utility class; no instances.
    }

    /**
     * Parse heart rate from the given BLE value.
     *
     * @param value raw characteristic bytes
     * @return BPM, or null if malformed / not parseable
     */
    @Nullable
    public static Integer parse(@Nullable byte[] value) {
        if (value == null || value.length == 0) return null;

        int flags = value[0] & 0xFF;
        boolean is16Bit = (flags & 0x01) != 0;

        if (is16Bit) {
            // Need at least 3 bytes: [flags][LSB][MSB]
            if (value.length < 3) return null;
            int hr = (value[1] & 0xFF) | ((value[2] & 0xFF) << 8);
            return hr;
        } else {
            // Need at least 2 bytes: [flags][HR]
            if (value.length < 2) return null;
            return value[1] & 0xFF;
        }
    }
}
