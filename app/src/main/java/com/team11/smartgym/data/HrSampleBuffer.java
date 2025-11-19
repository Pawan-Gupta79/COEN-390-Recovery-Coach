package com.team11.smartgym.data;

import java.util.ArrayList;
import java.util.List;

/**
 * DS-06.2 - HR sample buffer that batches Reading inserts for a single session.
 *
 * Usage:
 *   SessionRepository repo = new SessionRepository(AppDb.get(context).sessionDao());
 *   long sessionId = repo.createSession(System.currentTimeMillis());
 *   HrSampleBuffer buffer = new HrSampleBuffer(repo, sessionId);
 *
 *   // On each HR sample:
 *   buffer.addSample(bpm, System.currentTimeMillis());
 *
 *   // On Stop:
 *   buffer.flush();
 *
 * Thread-safety: simple synchronized buffer; intended to be used from a single
 * thread (e.g. main thread) in this sprint.
 */
public class HrSampleBuffer {

    private final SessionRepository repo;
    private final long sessionId;
    private final int batchSize;

    // In-memory buffer of pending readings
    private final List<Reading> buffer = new ArrayList<>();

    /**
     * Default constructor with a reasonable batch size (20 samples).
     */
    public HrSampleBuffer(SessionRepository repo, long sessionId) {
        this(repo, sessionId, 20);
    }

    /**
     * Constructor with explicit batch size.
     *
     * @param repo      session repository wrapping SessionDao
     * @param sessionId session ID to attach to every Reading
     * @param batchSize number of samples per DB batch insert
     */
    public HrSampleBuffer(SessionRepository repo, long sessionId, int batchSize) {
        this.repo = repo;
        this.sessionId = sessionId;
        this.batchSize = Math.max(1, batchSize);
    }

    /**
     * Add a new HR sample to the buffer.
     * When the buffer reaches batchSize, it is flushed to the DB.
     *
     * @param bpm          heart rate in beats per minute
     * @param timestampMs  epoch timestamp in milliseconds
     */
    public synchronized void addSample(int bpm, long timestampMs) {
        Reading r = new Reading();
        r.sessionId = sessionId;
        r.timestamp = timestampMs;
        r.bpm = bpm;

        buffer.add(r);

        if (buffer.size() >= batchSize) {
            flushInternal();
        }
    }

    /**
     * Flush any remaining buffered samples to the DB.
     * Safe to call multiple times; no-op if buffer is empty.
     */
    public synchronized void flush() {
        flushInternal();
    }

    /**
     * For debugging / tests: how many samples are currently buffered in memory.
     */
    public synchronized int getBufferedCount() {
        return buffer.size();
    }

    // ---- Internal helpers ----

    private void flushInternal() {
        if (buffer.isEmpty()) {
            return;
        }

        // Copy current buffer so we don't hold reference while inserting
        List<Reading> toInsert = new ArrayList<>(buffer);
        buffer.clear();

        // DS-06.2: batch insert with attached sessionId + timestamp.
        // insertReadings(...) is synchronous; DB access is allowed on main
        // thread in this sprint via allowMainThreadQueries() in AppDb.
        repo.insertReadings(toInsert);
    }
}
