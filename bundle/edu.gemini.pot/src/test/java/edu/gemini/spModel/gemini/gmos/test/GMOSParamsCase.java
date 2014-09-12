/* Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 * See the file COPYRIGHT for complete details.
 *
 * $Id: GMOSParamsCase.java 44395 2012-04-11 12:58:58Z nbarriga $
 */
package edu.gemini.spModel.gemini.gmos.test;

import edu.gemini.spModel.gemini.gmos.GmosCommonType;
import edu.gemini.spModel.gemini.gmos.GmosNorthType;
import static edu.gemini.spModel.test.TestFile.ser;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Tests GMOSParams.
 */
public final class GMOSParamsCase {

    // Test the fpunit convenience methods
    @Test
    public void testFPU() {
        GmosCommonType.FPUnit fpu = GmosNorthType.FPUnitNorth.FPU_NONE;
        assertTrue(fpu.isImaging());
        assertTrue(!fpu.isSpectroscopic());
        assertTrue(!fpu.isIFU());

        fpu = GmosNorthType.FPUnitNorth.LONGSLIT_2;
        assertTrue(!fpu.isImaging());
        assertTrue(fpu.isSpectroscopic());
        assertTrue(!fpu.isIFU());

        fpu = GmosNorthType.FPUnitNorth.IFU_2;
        assertTrue(!fpu.isImaging());
        assertTrue(!fpu.isSpectroscopic());
        assertTrue(fpu.isIFU());

    }

    // Test the ROIDescription and special ROI class
    @Test
    public void testROIDescription() {
        GmosCommonType.BuiltinROI roi1 = GmosCommonType.BuiltinROI.FULL_FRAME;
        GmosCommonType.ROIDescription roid = roi1.getROIDescription().getOrNull();
        assertEquals("xstart", roid.getXStart(), 1);
        assertEquals("ystart", roid.getYStart(), 1);
        assertEquals("xsize", roid.getXSize(), 6144);
        assertEquals("ysize", roid.getYSize(), 4608);
        // Binning 2
        assertEquals("xstart", roid.getXStart(), 1);
        assertEquals("ystart", roid.getYStart(), 1);
        assertEquals("xsize", roid.getXSize(GmosCommonType.Binning.TWO), 3072);
        assertEquals("ysize", roid.getYSize(GmosCommonType.Binning.TWO), 2304);
        // Binning 4
        assertEquals("xstart", roid.getXStart(), 1);
        assertEquals("ystart", roid.getYStart(), 1);
        assertEquals("xsize", roid.getXSize(GmosCommonType.Binning.FOUR), 1536);
        assertEquals("ysize", roid.getYSize(GmosCommonType.Binning.FOUR), 1152);
        roi1 = GmosCommonType.BuiltinROI.BOTTOM_SPECTRUM;
        roid = roi1.getROIDescription().getOrNull();
        assertEquals("xstart", roid.getXStart(), 1);
        assertEquals("ystart", roid.getYStart(), 256);
        assertEquals("xsize", roid.getXSize(), 6144);
        assertEquals("ysize", roid.getYSize(), 1024);
        // Binning 2
        assertEquals("xstart", roid.getXStart(), 1);
        assertEquals("ystart", roid.getYStart(), 256);
        assertEquals("xsize", roid.getXSize(GmosCommonType.Binning.TWO), 3072);
        assertEquals("ysize", roid.getYSize(GmosCommonType.Binning.TWO), 512);
        // Binning 4
        assertEquals("xstart", roid.getXStart(), 1);
        assertEquals("ystart", roid.getYStart(), 256);
        assertEquals("xsize", roid.getXSize(GmosCommonType.Binning.FOUR), 1536);
        assertEquals("ysize", roid.getYSize(GmosCommonType.Binning.FOUR), 256);
    }

    // Test ROI Description changes
    @Test
    public void testROIDescriptionChanges() {
        GmosCommonType.BuiltinROI roi1 = GmosCommonType.BuiltinROI.BOTTOM_SPECTRUM;
        GmosCommonType.ROIDescription roid1 = roi1.getROIDescription().getOrNull();
        assertEquals("xstart", roid1.getXStart(), 1);
        assertEquals("ystart", roid1.getYStart(), 256);
        assertEquals("xsize", roid1.getXSize(), 6144);
        assertEquals("ysize", roid1.getYSize(), 1024);

        GmosCommonType.BuiltinROI roi2 = GmosCommonType.BuiltinROI.TOP_SPECTRUM;
        GmosCommonType.ROIDescription roid2 = roi2.getROIDescription().getOrNull();
        assertEquals("xstart", roid2.getXStart(), 1);
        assertEquals("ystart", roid2.getYStart(), 3328);
        assertEquals("xsize", roid2.getXSize(), 6144);
        assertEquals("ysize", roid2.getYSize(), 1024);

        // Try to exchange them
        GmosCommonType.ROIDescription roidswap = roid2;
        roid2 = roid1;
        roid1 = roidswap;

        GmosCommonType.ROIDescription roid4 = roi1.getROIDescription().getOrNull();
        assertEquals("xstart", roid4.getXStart(), 1);
        assertEquals("ystart", roid4.getYStart(), 256);
        assertEquals("xsize", roid4.getXSize(), 6144);
        assertEquals("ysize", roid4.getYSize(), 1024);
        assertTrue(!(roid4 == roid1));

        GmosCommonType.ROIDescription roid5 = roi2.getROIDescription().getOrNull();
        assertEquals("xstart", roid5.getXStart(), 1);
        assertEquals("ystart", roid5.getYStart(), 3328);
        assertEquals("xsize", roid5.getXSize(), 6144);
        assertEquals("ysize", roid5.getYSize(), 1024);
        assertTrue(!(roid5 == roid2));
    }

    @Test
    public void testBuiltinROISerialization() throws Exception {
        final GmosCommonType.BuiltinROI outObject = GmosCommonType.BuiltinROI.BOTTOM_SPECTRUM;
        final GmosCommonType.BuiltinROI inObject = ser(outObject);
        assertSame("BuiltinROI", GmosCommonType.BuiltinROI.BOTTOM_SPECTRUM, inObject);
    }
}
