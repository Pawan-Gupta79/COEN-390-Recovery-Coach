package com.team11.smartgym.ble;

import org.junit.Test;
import static org.junit.Assert.*;

public class HeartRateStreamTest {

    @Test
    public void testStreamBasicFlow() {
        HeartRateStream s = new HeartRateStream();

        byte[] packet = new byte[] { 0x00, 0x50 }; // 80 bpm
        long t = 1000;

        Integer bpm = s.onPacket(packet, t);
        assertNotNull(bpm);
        assertEquals(Integer.valueOf(80), bpm);
    }

    @Test
    public void testStreamInvalidPackets() {
        HeartRateStream s = new HeartRateStream();

        long t = 1000;

        // Invalid packets produce no new BPM
        assertNull(s.onPacket(null, t));

        // Now send a valid packet
        byte[] ok = new byte[]{ 0x00, 0x4B }; // 75
        assertEquals(Integer.valueOf(75), s.onPacket(ok, t + 10));
    }

    @Test
    public void testStaleDataBecomesNull() throws Exception {
        HeartRateStream s = new HeartRateStream();

        byte[] ok = new byte[] { 0x00, 0x4B }; // 75 bpm
        long t = 1000;

        assertEquals(Integer.valueOf(75), s.onPacket(ok, t));

        // Wait > stale window (2000 ms)
        assertNull(s.getCurrent(t + 2500));
    }
}
