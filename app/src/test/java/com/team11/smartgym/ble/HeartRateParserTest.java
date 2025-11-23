package com.team11.smartgym.ble;

import org.junit.Test;
import static org.junit.Assert.*;

public class HeartRateParserTest {

    @Test
    public void testParse8bit() {
        byte[] packet = new byte[] {
                0x00,      // FLAGS: no 16-bit
                0x4B       // HR = 75 BPM
        };
        assertEquals(Integer.valueOf(75), HeartRateParser.parse(packet));
    }

    @Test
    public void testParse16bit() {
        byte[] packet = new byte[] {
                0x01,               // FLAGS: 16-bit format
                (byte)0xE1, 0x00    // 225 in little-endian -> 225 bpm
        };
        assertEquals(Integer.valueOf(225), HeartRateParser.parse(packet));
    }

    @Test
    public void testMalformedPackets() {
        assertNull(HeartRateParser.parse(null));
        assertNull(HeartRateParser.parse(new byte[]{}));
        assertNull(HeartRateParser.parse(new byte[]{0x00}));
    }
}
