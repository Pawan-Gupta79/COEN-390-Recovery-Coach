package com.team11.smartgym.data;

import org.junit.Test;
import static org.junit.Assert.*;

public class AdviceEngineTest {

    @Test
    public void recovered_whenHighRmssd() {
        AdviceRubric.Advice a = AdviceEngine.advise(70.0, null);
        assertEquals(AdviceRubric.Tier.RECOVERED, a.tier);
    }

    @Test
    public void strained_whenLowRmssd_orHighLfHf() {
        assertEquals(AdviceRubric.Tier.STRAINED, AdviceEngine.advise(20.0, null).tier);
        assertEquals(AdviceRubric.Tier.STRAINED, AdviceEngine.advise(null, 3.5).tier);
    }

    @Test
    public void invalid_whenNoMetrics() {
        assertEquals(AdviceRubric.Tier.INVALID, AdviceEngine.advise(null, null).tier);
    }
}
