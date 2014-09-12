/* Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 * See the file COPYRIGHT for complete details.
 *
 * $Id: SPSiteQualityCase.java 34347 2011-05-05 15:32:10Z swalker $
 */
package edu.gemini.spModel.gemini.obscomp.test;

import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import static edu.gemini.spModel.test.TestFile.ser;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;


/**
 * Class SPSiteQualityTest tests the SPSiteQuality obscomp
 */
public final class SPSiteQualityCase {
    private SPSiteQuality _t1;

    @Before
    public void setUp() throws Exception {
        _t1 = new SPSiteQuality();
    }

    // Setup some test objects.
    @Test
    public void testInitial() {
        assertTrue(_t1.getImageQuality() == SPSiteQuality.ImageQuality.ANY);
        assertTrue(_t1.getSkyBackground() == SPSiteQuality.SkyBackground.ANY);
        assertTrue(_t1.getCloudCover() == SPSiteQuality.CloudCover.ANY);
        assertTrue(_t1.getWaterVapor() == SPSiteQuality.WaterVapor.ANY);
    }

    // Test title
    @Test
    public void testTitle() {
        // Name should be type
        assertFalse(_t1.isTitleChanged());
        assertEquals(_t1.getType().readableStr, _t1.getTitle());

        // set the title
        String name = "Test Site Quality";
        // Now set the name
        _t1.setTitle(name);
        assertEquals(name, _t1.getTitle());
    }

    /**
     * Test cloneable
     */
    @Test
    public void testCloneable() {
        String title1 = "Initial Test SPSiteQuality";
        // Give the data object a title
        SPSiteQuality sq1 = _t1;
        assertNotNull(sq1);
        sq1.setTitle(title1);

        // Create change
        sq1.setCloudCover(SPSiteQuality.CloudCover.PERCENT_50);
        sq1.setSkyBackground(SPSiteQuality.SkyBackground.PERCENT_80);

        assertTrue(sq1.getImageQuality() == SPSiteQuality.ImageQuality.ANY);
        assertTrue(sq1.getSkyBackground() == SPSiteQuality.SkyBackground.PERCENT_80);
        assertTrue(sq1.getCloudCover() == SPSiteQuality.CloudCover.PERCENT_50);
        assertTrue(sq1.getWaterVapor() == SPSiteQuality.WaterVapor.ANY);

        SPSiteQuality sq2 = sq1.clone();
        assertNotNull(sq2);
        sq2.setCloudCover(SPSiteQuality.CloudCover.PERCENT_80);
        sq2.setWaterVapor(SPSiteQuality.WaterVapor.PERCENT_80);

        assertTrue(sq2.getImageQuality() == SPSiteQuality.ImageQuality.ANY);
        assertTrue(sq2.getSkyBackground() == SPSiteQuality.SkyBackground.PERCENT_80);
        assertTrue(sq2.getCloudCover() == SPSiteQuality.CloudCover.PERCENT_80);
        assertTrue(sq2.getWaterVapor() == SPSiteQuality.WaterVapor.PERCENT_80);

        assertTrue(sq1.getImageQuality() == SPSiteQuality.ImageQuality.ANY);
        assertTrue(sq1.getSkyBackground() == SPSiteQuality.SkyBackground.PERCENT_80);
        assertTrue(sq1.getCloudCover() == SPSiteQuality.CloudCover.PERCENT_50);
        assertTrue(sq1.getWaterVapor() == SPSiteQuality.WaterVapor.ANY);

    }

    @Test
    public void testSerialization() throws Exception {
        final SPSiteQuality outObject = new SPSiteQuality();

        // Create change
        outObject.setCloudCover(SPSiteQuality.CloudCover.PERCENT_50);
        outObject.setSkyBackground(SPSiteQuality.SkyBackground.PERCENT_80);

        final SPSiteQuality inObject = ser(outObject);

        assertSame("Image Quality", SPSiteQuality.ImageQuality.ANY, inObject.getImageQuality());
        assertSame("Sky Background", SPSiteQuality.SkyBackground.PERCENT_80, inObject.getSkyBackground());
        assertSame("Cloud Cover", SPSiteQuality.CloudCover.PERCENT_50, inObject.getCloudCover());
        assertSame("Water Vapor", SPSiteQuality.WaterVapor.ANY, inObject.getWaterVapor());

        assertEquals("Image Quality", SPSiteQuality.ImageQuality.ANY, inObject.getImageQuality());
        assertEquals("Sky Background", SPSiteQuality.SkyBackground.PERCENT_80, inObject.getSkyBackground());
        assertEquals("Cloud Cover", SPSiteQuality.CloudCover.PERCENT_50, inObject.getCloudCover());
        assertEquals("Water Vapor", SPSiteQuality.WaterVapor.ANY, inObject.getWaterVapor());
    }
}
