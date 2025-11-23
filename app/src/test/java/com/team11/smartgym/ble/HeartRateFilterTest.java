package com.team11.smartgym.ble;

import org.junit.Test;
import static org.junit.Assert.*;

public class HeartRateFilterTest {

    @Test
    public void testRejectImpossibleValues() {
        HeartRateFilter f = new HeartRateFilter();

        // Too low (<30)
        assertNull(f.addSample(10));

        // Too high (>230)
        assertNull(f.addSample(250));
    }

    @Test
    public void testJumpFiltering() {
        HeartRateFilter f = new HeartRateFilter();

        // Start stable
        assertEquals(Integer.valueOf(80), f.addSample(80));

        // Huge jump → ignore
        assertEquals(Integer.valueOf(80), f.addSample(150));
    }

    @Test
    public void testRollingAverage() {
        HeartRateFilter f = new HeartRateFilter();

        f.addSample(80);
        f.addSample(82);
        f.addSample(84);
        f.addSample(86);
        f.addSample(88);

        // Avg = (80+82+84+86+88)/5 = 84
        assertEquals(Integer.valueOf(84), f.getLastStable());
    }
}
