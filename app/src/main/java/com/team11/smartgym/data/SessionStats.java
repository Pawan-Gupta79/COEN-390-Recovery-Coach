package com.team11.smartgym.data;

import java.io.Serializable;

/**
 * Basic statistics computed at session end.
 */
public final class SessionStats implements Serializable {

    public final int averageBpm;
    public final int maxBpm;
    public final boolean invalid;

    public SessionStats(int averageBpm, int maxBpm, boolean invalid) {
        this.averageBpm = Math.max(0, averageBpm);
        this.maxBpm = Math.max(0, maxBpm);
        this.invalid = invalid;
    }

    @Override
    public String toString() {
        return "SessionStats{" +
                "averageBpm=" + averageBpm +
                ", maxBpm=" + maxBpm +
                ", invalid=" + invalid +
                '}';
    }
}
