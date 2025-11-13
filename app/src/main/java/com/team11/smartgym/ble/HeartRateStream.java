package com.team11.smartgym.ble;

import androidx.annotation.Nullable;

import java.util.concurrent.TimeUnit;

/**
 * Maintains a clean BPM output stream:
 *  - Parses raw BLE packets via HeartRateParser
 *  - Filters spikes / smooths via HeartRateFilter
 *  - Tracks last valid BPM + timestamp
 *  - Returns null when data is stale (no packets for a while)
 */
public class HeartRateStream {

    // How long we keep a BPM before treating it as "stale"
    private static final long STALE_WINDOW_MS = TimeUnit.SECONDS.toMillis(2);

    private final HeartRateFilter filter = new HeartRateFilter();

    @Nullable
    private Integer currentBpm = null;
    private long lastUpdateMs = 0L;

    /**
     * Process a new BLE HR packet.
     *
     * @param packet   raw BLE payload (may be null or malformed)
     * @param nowMs    current time in ms (System.currentTimeMillis in production, test-controlled in unit tests)
     * @return latest stable BPM after this packet, or null if still none.
     */
    @Nullable
    public synchronized Integer onPacket(@Nullable byte[] packet, long nowMs) {
        if (packet == null || packet.length == 0) {
            log("Ignoring empty/null packet");
            return currentBpm;
        }

        Integer parsed = HeartRateParser.parse(packet);
        if (parsed == null) {
            log("Malformed HR packet, ignoring");
            return currentBpm;
        }

        Integer filtered = filter.addSample(parsed);
        if (filtered != null) {
            currentBpm = filtered;
            lastUpdateMs = nowMs;
            log("Updated BPM=" + filtered + " at " + nowMs);
        } else {
            log("Filter did not produce a value");
        }

        return currentBpm;
    }

    /**
     * Get the current BPM, or null if data is stale or never seen.
     */
    @Nullable
    public synchronized Integer getCurrent(long nowMs) {
        if (currentBpm == null) return null;
        if (nowMs - lastUpdateMs > STALE_WINDOW_MS) {
            log("BPM stale, returning null");
            return null;
        }
        return currentBpm;
    }

    private void log(String msg) {
        // Use System.out so it works in local unit tests (android.util.Log is not available there).
        System.out.println("HeartRateStream: " + msg);
    }
}
