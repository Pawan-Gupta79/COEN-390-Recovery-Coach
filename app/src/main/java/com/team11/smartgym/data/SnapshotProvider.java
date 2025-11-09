package com.team11.smartgym.data;

import android.content.Context;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Helper to load and pretty-print last or history of TempSessionSnapshot(s). */
public final class SnapshotProvider {

    private final TempSessionStore store;
    private final DateFormat dateTime;

    public SnapshotProvider(Context context) {
        Context app = context.getApplicationContext();
        this.store = new TempSessionStore(app);
        this.dateTime = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, Locale.getDefault());
    }

    public TempSessionSnapshot load() { return store.load(); }

    public String loadFormatted() {
        TempSessionSnapshot s = store.load();
        if (s == null) return "No snapshot saved.";
        return formatOne(s);
    }

    public List<TempSessionSnapshot> loadAll() { return store.loadAll(); }

    public String loadAllFormatted() {
        List<TempSessionSnapshot> list = store.loadAll();
        if (list.isEmpty()) return "No sessions yet.";
        StringBuilder sb = new StringBuilder();
        sb.append("Total: ").append(list.size()).append('\n');
        for (int i = 0; i < list.size(); i++) {
            TempSessionSnapshot s = list.get(i);
            sb.append('\n').append("#").append(i + 1).append('\n');
            sb.append(formatOne(s));
            if (i < list.size() - 1) sb.append("\n");
        }
        return sb.toString();
    }

    public void clear() { store.clear(); }

    // --------- formatting ---------
    private String formatOne(TempSessionSnapshot s) {
        String idShort = (s.tempId == null) ? "-" :
                (s.tempId.length() > 8 ? s.tempId.substring(0, 8) : s.tempId);
        String startStr = (s.startMs > 0) ? dateTime.format(new Date(s.startMs)) : "-";
        String endStr   = (s.endMs   > 0) ? dateTime.format(new Date(s.endMs))   : "-";
        String stats = (s.stats == null) ? "-" :
                String.format(Locale.getDefault(),
                        "avg=%d, max=%d, invalid=%s",
                        s.stats.averageBpm, s.stats.maxBpm, s.stats.invalid);
        return "ID " + idShort +
                "\nStart:  " + startStr +
                "\nEnd:    " + endStr +
                "\nSamples:" + s.samplesCount +
                "\nStats:  " + stats;
    }
}
