package org.anthony.wolvercotebus.oxontime;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class OxontimeDecoderTest {
//    private static String TAG = "WolvercoteBus Tests";

    private final static String TEST_PATH="src/test/java/org/anthony/wolvercotebus/testdata/";


    private final static String TEST_SAMPLE_WOLVERCOTE = TEST_PATH + "sample_wolvercote.json";
    private final static String TEST_SAMPLE_OXFORD = TEST_PATH + "sample_oxford.json";
    private final static String TEST_NOSERVICES = TEST_PATH + "sample_noservices.json";

    private final static List<String> routes = Collections.singletonList("6");

    @Test
    public void testWolvercote() {
        OxontimeDecoder oxontimeDecoder = new OxontimeDecoder(routes);
        OxontimeResponse response = oxontimeDecoder.parseFile(TEST_SAMPLE_WOLVERCOTE);

        assertNotNull(response);

        assertEquals("340001008SOU", response.getAtcoCode());
        assertEquals("oxfadtgp", response.getNaptanCode());

        assertEquals(2, response.getServices().size());

        BusDeparture s1 = response.getServices().get(0);
        assertEquals("6", s1.getRouteCode());
        assertEquals("19 min", s1.getDisplayTime());
        assertEquals("George Street", s1.getDestination());
        assertTrue(s1.isRealtime());
    }

    @Test
    public void testOxford() {
        OxontimeDecoder oxontimeDecoder = new OxontimeDecoder(routes);
        OxontimeResponse response = oxontimeDecoder.parseFile(TEST_SAMPLE_OXFORD);

        assertNotNull(response);

        assertEquals("340000005C3", response.getAtcoCode());
        assertEquals("oxfamjpa", response.getNaptanCode());

        assertEquals(3, response.getServices().size());

        BusDeparture s1 = response.getServices().get(0);
        assertEquals("6", s1.getRouteCode());
        assertEquals("22 min", s1.getDisplayTime());
        assertEquals("Clifford Place", s1.getDestination());
        assertTrue(s1.isRealtime());

        BusDeparture s2 = response.getServices().get(1);
        assertEquals("6", s2.getRouteCode());
        assertEquals("47 min", s2.getDisplayTime());
        assertEquals("Clifford Place", s2.getDestination());
        assertTrue(s2.isRealtime());

        BusDeparture s3 = response.getServices().get(2);
        assertEquals("6", s3.getRouteCode());
        assertEquals("22:35", s3.getDisplayTime());
        assertEquals("Clifford Place", s3.getDestination());
        assertFalse(s3.isRealtime());
    }

    @Test
    public void testNoServices() {
        OxontimeResponse response =
                new OxontimeDecoder(routes).parseFile(TEST_NOSERVICES);
        assertNotNull(response);
        assertEquals("340001008SOU", response.getAtcoCode());
        assertEquals("oxfadtgp", response.getNaptanCode());
        assertEquals(0, response.getServices().size());
    }

}
