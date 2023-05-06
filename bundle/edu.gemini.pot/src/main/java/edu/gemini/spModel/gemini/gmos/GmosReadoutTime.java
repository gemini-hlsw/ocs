/**
 * $Id: GmosReadoutTime.java 38186 2011-10-24 13:21:33Z swalker $
 */

package edu.gemini.spModel.gemini.gmos;

import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.gemini.gmos.GmosCommonType.AmpCount;
import edu.gemini.spModel.gemini.gmos.GmosCommonType.AmpGain;
import edu.gemini.spModel.gemini.gmos.GmosCommonType.AmpReadMode;
import edu.gemini.spModel.gemini.gmos.GmosCommonType.Binning;
import edu.gemini.spModel.gemini.gmos.GmosCommonType.BuiltinROI;
import edu.gemini.spModel.gemini.gmos.GmosCommonType.DetectorManufacturer;

import java.util.Comparator;
import java.util.Map;
import java.util.HashMap;

/** Maps GMOS instrument parameters to readout times */
public class GmosReadoutTime {

    //Maps the GmosReadoutKey to the overhead. Allows faster searchs.
    private static final Map<GmosReadoutKey, Double> map = new HashMap<GmosReadoutKey, Double>(500);

    //        ampCount          ampSpeed         ROI                          Xbin   Ybin  ampGain      detectorManufacturer, overhead
    //        ---------------------------------------------------------------------
        static {
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 1, 1, AmpGain.LOW, DetectorManufacturer.E2V, 123.874);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 1, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 123.875);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 1, 2, AmpGain.LOW, DetectorManufacturer.E2V, 63.1579);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 1, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 63.1574);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 1, 4, AmpGain.LOW, DetectorManufacturer.E2V, 32.7995);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 1, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 32.7995);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 2, 1, AmpGain.LOW, DetectorManufacturer.E2V, 71.3920);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 2, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 71.3915);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 2, 2, AmpGain.LOW, DetectorManufacturer.E2V, 36.9166);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 2, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 36.9166);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 2, 4, AmpGain.LOW, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 2, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 4, 1, AmpGain.LOW, DetectorManufacturer.E2V, 45.4162);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 4, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 45.4162);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 4, 2, AmpGain.LOW, DetectorManufacturer.E2V, 23.9286);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 4, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 23.9285);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 4, 4, AmpGain.LOW, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 4, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CCD2, 1, 1, AmpGain.LOW, DetectorManufacturer.E2V, 123.874);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CCD2, 1, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 123.875);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CCD2, 1, 2, AmpGain.LOW, DetectorManufacturer.E2V, 63.1574);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CCD2, 1, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 63.1578);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CCD2, 1, 4, AmpGain.LOW, DetectorManufacturer.E2V, 32.7991);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CCD2, 1, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 32.7995);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CCD2, 2, 1, AmpGain.LOW, DetectorManufacturer.E2V, 71.3920);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CCD2, 2, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 71.3920);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CCD2, 2, 2, AmpGain.LOW, DetectorManufacturer.E2V, 36.9166);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CCD2, 2, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 36.9165);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CCD2, 2, 4, AmpGain.LOW, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CCD2, 2, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CCD2, 4, 1, AmpGain.LOW, DetectorManufacturer.E2V, 45.4162);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CCD2, 4, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 45.4162);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CCD2, 4, 2, AmpGain.LOW, DetectorManufacturer.E2V, 23.9287);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CCD2, 4, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 23.9282);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CCD2, 4, 4, AmpGain.LOW, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CCD2, 4, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 1, 1, AmpGain.LOW, DetectorManufacturer.E2V, 28.4802);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 1, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 28.4805);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 1, 2, AmpGain.LOW, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 1, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 1, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 1, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 2, 1, AmpGain.LOW, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 2, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 2, 2, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 2, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 2, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 2, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 4, 1, AmpGain.LOW, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 4, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 4, 2, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 4, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 4, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 4, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 1, 1, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 1, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 1, 2, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 1, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 1, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 1, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 2, 1, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 2, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 2, 2, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 2, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 2, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 2, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 4, 1, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 4, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 4, 2, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 4, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 4, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 4, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 1, 1, AmpGain.LOW, DetectorManufacturer.E2V, 29.2526);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 1, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 29.2521);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 1, 2, AmpGain.LOW, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 1, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 1, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 1, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 2, 1, AmpGain.LOW, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 2, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 2, 2, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 2, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 2, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 2, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 4, 1, AmpGain.LOW, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 4, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 4, 2, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 4, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 4, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 4, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 1, 1, AmpGain.LOW, DetectorManufacturer.E2V, 27.7087);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 1, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 27.7083);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 1, 2, AmpGain.LOW, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 1, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 1, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 1, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 2, 1, AmpGain.LOW, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 2, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 2, 2, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 2, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 2, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 2, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 4, 1, AmpGain.LOW, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 4, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 4, 2, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 4, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 4, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 4, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 1, 1, AmpGain.LOW, DetectorManufacturer.E2V, 44.5518);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 1, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 44.5519);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 1, 2, AmpGain.LOW, DetectorManufacturer.E2V, 23.4877);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 1, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 23.4879);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 1, 4, AmpGain.LOW, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 1, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 2, 1, AmpGain.LOW, DetectorManufacturer.E2V, 30.1952);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 2, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 30.1952);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 2, 2, AmpGain.LOW, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 2, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 2, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 2, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 4, 1, AmpGain.LOW, DetectorManufacturer.E2V, 23.2824);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 4, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 23.2826);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 4, 2, AmpGain.LOW, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 4, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 4, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 4, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CCD2, 1, 1, AmpGain.LOW, DetectorManufacturer.E2V, 44.5515);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CCD2, 1, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 44.5517);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CCD2, 1, 2, AmpGain.LOW, DetectorManufacturer.E2V, 23.4875);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CCD2, 1, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 23.4875);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CCD2, 1, 4, AmpGain.LOW, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CCD2, 1, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CCD2, 2, 1, AmpGain.LOW, DetectorManufacturer.E2V, 30.1953);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CCD2, 2, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 30.1951);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CCD2, 2, 2, AmpGain.LOW, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CCD2, 2, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CCD2, 2, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CCD2, 2, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CCD2, 4, 1, AmpGain.LOW, DetectorManufacturer.E2V, 23.2825);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CCD2, 4, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 23.2824);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CCD2, 4, 2, AmpGain.LOW, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CCD2, 4, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CCD2, 4, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CCD2, 4, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 1, 1, AmpGain.LOW, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 1, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 1, 2, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 1, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 1, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 1, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 2, 1, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 2, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 2, 2, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 2, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 2, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 2, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 4, 1, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 4, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 4, 2, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 4, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 4, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 4, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 1, 1, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 1, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 1, 2, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 1, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 1, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 1, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 2, 1, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 2, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 2, 2, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 2, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 2, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 2, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 4, 1, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 4, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 4, 2, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 4, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 4, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 4, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 1, 1, AmpGain.LOW, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 1, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 1, 2, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 1, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 1, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 1, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 2, 1, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 2, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 2, 2, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 2, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 2, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 2, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 4, 1, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 4, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 4, 2, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 4, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 4, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 4, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 1, 1, AmpGain.LOW, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 1, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 1, 2, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 1, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 1, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 1, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 2, 1, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 2, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 2, 2, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 2, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 2, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 2, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 4, 1, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 4, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 4, 2, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 4, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 4, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.THREE, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 4, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 1, 1, AmpGain.LOW, DetectorManufacturer.E2V, 65.8587);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 1, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 65.8587);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 1, 2, AmpGain.LOW, DetectorManufacturer.E2V, 34.1499);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 1, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 34.1497);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 1, 4, AmpGain.LOW, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 1, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 2, 1, AmpGain.LOW, DetectorManufacturer.E2V, 39.5642);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 2, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 39.5644);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 2, 2, AmpGain.LOW, DetectorManufacturer.E2V, 21.0029);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 2, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 21.0027);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 2, 4, AmpGain.LOW, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 2, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 4, 1, AmpGain.LOW, DetectorManufacturer.E2V, 26.6825);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 4, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 26.6824);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 4, 2, AmpGain.LOW, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 4, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 4, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 4, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CCD2, 1, 1, AmpGain.LOW, DetectorManufacturer.E2V, 65.8586);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CCD2, 1, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 65.8585);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CCD2, 1, 2, AmpGain.LOW, DetectorManufacturer.E2V, 34.1500);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CCD2, 1, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 34.1499);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CCD2, 1, 4, AmpGain.LOW, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CCD2, 1, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CCD2, 2, 1, AmpGain.LOW, DetectorManufacturer.E2V, 39.5642);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CCD2, 2, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 39.5642);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CCD2, 2, 2, AmpGain.LOW, DetectorManufacturer.E2V, 21.0027);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CCD2, 2, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 21.0029);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CCD2, 2, 4, AmpGain.LOW, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CCD2, 2, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CCD2, 4, 1, AmpGain.LOW, DetectorManufacturer.E2V, 26.6825);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CCD2, 4, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 26.6823);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CCD2, 4, 2, AmpGain.LOW, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CCD2, 4, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CCD2, 4, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CCD2, 4, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 1, 1, AmpGain.LOW, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 1, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 1, 2, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 1, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 1, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 1, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 2, 1, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 2, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 2, 2, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 2, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 2, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 2, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 4, 1, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 4, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 4, 2, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 4, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 4, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 4, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 1, 1, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 1, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 1, 2, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 1, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 1, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 1, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 2, 1, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 2, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 2, 2, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 2, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 2, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 2, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 4, 1, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 4, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 4, 2, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 4, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 4, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 4, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 1, 1, AmpGain.LOW, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 1, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 1, 2, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 1, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 1, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 1, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 2, 1, AmpGain.LOW, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 2, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 2, 2, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 2, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 2, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 2, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 4, 1, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 4, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 4, 2, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 4, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 4, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 4, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 1, 1, AmpGain.LOW, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 1, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 1, 2, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 1, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 1, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 1, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 2, 1, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 2, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 2, 2, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 2, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 2, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 2, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 4, 1, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 4, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 4, 2, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 4, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 4, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 4, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 1, 1, AmpGain.LOW, DetectorManufacturer.E2V, 24.6618);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 1, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 24.6622);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 1, 2, AmpGain.LOW, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 1, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 1, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 1, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 2, 1, AmpGain.LOW, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 2, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 2, 2, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 2, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 2, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 2, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 4, 1, AmpGain.LOW, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 4, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 4, 2, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 4, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 4, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 4, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CCD2, 1, 1, AmpGain.LOW, DetectorManufacturer.E2V, 24.6619);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CCD2, 1, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 24.6620);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CCD2, 1, 2, AmpGain.LOW, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CCD2, 1, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CCD2, 1, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CCD2, 1, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CCD2, 2, 1, AmpGain.LOW, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CCD2, 2, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CCD2, 2, 2, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CCD2, 2, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CCD2, 2, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CCD2, 2, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CCD2, 4, 1, AmpGain.LOW, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CCD2, 4, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CCD2, 4, 2, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CCD2, 4, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CCD2, 4, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CCD2, 4, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 1, 1, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 1, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 1, 2, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 1, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 1, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 1, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 2, 1, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 2, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 2, 2, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 2, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 2, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 2, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 4, 1, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 4, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 4, 2, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 4, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 4, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 4, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 1, 1, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 1, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 1, 2, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 1, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 1, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 1, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 2, 1, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 2, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 2, 2, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 2, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 2, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 2, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 4, 1, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 4, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 4, 2, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 4, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 4, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 4, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 1, 1, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 1, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 1, 2, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 1, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 1, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 1, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 2, 1, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 2, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 2, 2, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 2, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 2, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 2, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 4, 1, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 4, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 4, 2, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 4, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 4, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 4, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 1, 1, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 1, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 1, 2, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 1, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 1, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 1, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 2, 1, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 2, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 2, 2, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 2, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 2, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 2, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 4, 1, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 4, 1, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 4, 2, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 4, 2, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 4, 4, AmpGain.LOW, DetectorManufacturer.E2V, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 4, 4, AmpGain.HIGH, DetectorManufacturer.E2V, 10.0000);


                    // Gmos HAMAMATSU
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 1, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 165.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 1, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 30.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 1, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 82.2000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 1, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 30.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 1, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 40.2000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 1, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 30.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 2, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 100.6000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 2, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 30.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 2, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 48.6000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 2, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 30.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 2, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 24.2000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 2, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 4, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 66.8000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 4, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 30.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 4, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 31.8000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 4, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 23.9285);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 4, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 15.2000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 4, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CCD2, 1, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 158.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CCD2, 1, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 30.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CCD2, 1, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 79.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CCD2, 1, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 30.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CCD2, 1, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 39.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CCD2, 1, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 30.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CCD2, 2, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 93.4000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CCD2, 2, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 30.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CCD2, 2, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 47.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CCD2, 2, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 30.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CCD2, 2, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 21.4000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CCD2, 2, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CCD2, 4, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 61.4000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CCD2, 4, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 30.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CCD2, 4, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 29.6000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CCD2, 4, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 23.9282);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CCD2, 4, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 12.6000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CCD2, 4, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 1, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 40.2000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 1, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 28.4805);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 1, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 21.4000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 1, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 1, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 13.2000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 1, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 2, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 30.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 2, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 2, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 17.2000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 2, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 2, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 9.2000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 2, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 4, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 21.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 4, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 4, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 13.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 4, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 4, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 8.8000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 4, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 1, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 14.4000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 1, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 1, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 8.6000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 1, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 1, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 6.4000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 1, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 2, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.8000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 2, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 2, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 8.6000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 2, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 2, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 7.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 2, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 4, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.6000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 4, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 4, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 8.6000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 4, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 4, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 4.6000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 4, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 1, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 29.2526);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 1, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 29.2521);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 1, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 1, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 1, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 1, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 2, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 2, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 2, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 2, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 2, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 2, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 4, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 4, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 4, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 4, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 4, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 4, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 1, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 27.7087);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 1, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 27.7083);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 1, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 1, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 1, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 1, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 2, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 2, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 2, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 2, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 2, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 2, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 4, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 4, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 4, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 4, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 4, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 4, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 1, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 70.8000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 1, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 70.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 1, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 36.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 1, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 35.8000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 1, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 16.2000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 1, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 16.6000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 2, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 48.8000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 2, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 49.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 2, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 22.6000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 2, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 22.6000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 2, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 9.8000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 2, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 9.8000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 4, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 43.6000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 4, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 43.2000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 4, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 19.6000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 4, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 19.6000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 4, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 9.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 4, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 9.2000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CCD2, 1, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 55.8000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CCD2, 1, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 56.6000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CCD2, 1, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 33.2000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CCD2, 1, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 33.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CCD2, 1, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 15.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CCD2, 1, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 14.8000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CCD2, 2, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 41.2000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CCD2, 2, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 41.2000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CCD2, 2, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 19.4000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CCD2, 2, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 19.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CCD2, 2, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 9.4000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CCD2, 2, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 9.4000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CCD2, 4, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 39.4000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CCD2, 4, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 39.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CCD2, 4, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 19.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CCD2, 4, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 18.8000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CCD2, 4, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 7.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CCD2, 4, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 7.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 1, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 22.4000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 1, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 22.2000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 1, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 13.4000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 1, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 13.4000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 1, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 7.2000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 1, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 7.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 2, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 17.6000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 2, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 17.8000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 2, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 9.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 2, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 9.2000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 2, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 7.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 2, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 7.2000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 4, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 13.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 4, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 13.6000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 4, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 9.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 4, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 8.6000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 4, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 6.6000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 4, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 6.8000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 1, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 8.4000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 1, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 8.6000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 1, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 6.4000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 1, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 6.6000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 1, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 6.8000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 1, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 6.6000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 2, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 6.4000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 2, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 6.6000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 2, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 7.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 2, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 7.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 2, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 7.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 2, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 7.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 4, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 6.6000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 4, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 6.4000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 4, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 6.6000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 4, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 6.6000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 4, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 4.4000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 4, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 5.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 1, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 1, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 1, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 1, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 1, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 1, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 2, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 2, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 2, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 2, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 2, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 2, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 4, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 4, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 4, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 4, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 4, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 4, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 1, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 1, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 20.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 1, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 1, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 1, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 1, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 2, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 2, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 2, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 2, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 2, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 2, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 4, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 4, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 4, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 4, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 4, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.SIX, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 4, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 1, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 82.5000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 1, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 30.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 1, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 41.1000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 1, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 30.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 1, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 20.1000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 1, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 20.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 2, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 50.3000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 2, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 30.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 2, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 24.3000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 2, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 21.0027);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 2, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 12.1000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 2, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 20.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 4, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 33.4000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 4, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 26.6824);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 4, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 15.9000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 4, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 20.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 4, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 7.6000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.FULL_FRAME, 4, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CCD2, 1, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 79.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CCD2, 1, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 30.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CCD2, 1, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 39.5000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CCD2, 1, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 30.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CCD2, 1, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 19.5000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CCD2, 1, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 20.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CCD2, 2, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 46.7000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CCD2, 2, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 30.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CCD2, 2, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 23.5000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CCD2, 2, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 21.0029);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CCD2, 2, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.7000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CCD2, 2, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 20.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CCD2, 4, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 30.7000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CCD2, 4, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 26.6823);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CCD2, 4, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 14.8000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CCD2, 4, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 20.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CCD2, 4, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 6.3000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CCD2, 4, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 1, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 20.1000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 1, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 20.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 1, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.7000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 1, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 1, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 6.6000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 1, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 2, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 15.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 2, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 2, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 8.6000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 2, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 2, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 4.6000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 2, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 4, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.5000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 4, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 4, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 6.5000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 4, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 4, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 4.4000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_SPECTRUM, 4, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 1, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 7.2000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 1, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 1, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 4.3000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 1, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 1, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 3.2000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 1, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 2, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 5.4000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 2, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 2, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 4.3000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 2, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 2, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 3.5000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 2, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 4, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 5.3000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 4, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 4, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 4.3000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 4, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 4, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 2.3000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.CENTRAL_STAMP, 4, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 1, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 20.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 1, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 20.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 1, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 1, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 1, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 1, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 2, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 20.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 2, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 20.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 2, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 2, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 2, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 2, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 4, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 4, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 4, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 4, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 4, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.TOP_SPECTRUM, 4, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 1, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 20.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 1, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 20.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 1, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 1, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 1, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 1, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 2, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 2, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 2, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 2, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 2, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 2, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 4, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 4, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 4, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 4, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 4, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.SLOW, BuiltinROI.BOTTOM_SPECTRUM, 4, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 1, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 35.4000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 1, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 35.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 1, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 18.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 1, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 17.9000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 1, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 8.1000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 1, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 8.3000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 2, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 24.4000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 2, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 24.5000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 2, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 11.3000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 2, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 11.3000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 2, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 4.9000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 2, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 4.9000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 4, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 21.8000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 4, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 21.6000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 4, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 9.8000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 4, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 9.8000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 4, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 4.5000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.FULL_FRAME, 4, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 4.6000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CCD2, 1, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 27.9000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CCD2, 1, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 28.3000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CCD2, 1, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 16.6000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CCD2, 1, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 16.5000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CCD2, 1, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 7.5000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CCD2, 1, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 7.4000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CCD2, 2, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 20.6000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CCD2, 2, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 20.6000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CCD2, 2, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 9.7000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CCD2, 2, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 9.5000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CCD2, 2, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 4.7000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CCD2, 2, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 4.7000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CCD2, 4, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 19.7000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CCD2, 4, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 19.5000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CCD2, 4, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 9.5000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CCD2, 4, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 9.4000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CCD2, 4, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 3.5000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CCD2, 4, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 3.5000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 1, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 11.2000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 1, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 11.1000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 1, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 6.7000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 1, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 6.7000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 1, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 3.6000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 1, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 3.5000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 2, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 8.8000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 2, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 8.9000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 2, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 4.5000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 2, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 4.6000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 2, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 3.5000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 2, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 3.6000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 4, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 6.5000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 4, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 6.8000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 4, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 4.5000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 4, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 4.3000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 4, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 3.3000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CENTRAL_SPECTRUM, 4, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 3.4000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 1, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 4.2000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 1, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 4.3000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 1, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 3.2000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 1, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 3.3000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 1, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 3.4000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 1, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 3.3000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 2, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 3.2000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 2, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 3.3000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 2, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 3.5000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 2, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 3.5000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 2, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 3.5000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 2, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 3.5000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 4, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 3.3000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 4, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 3.2000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 4, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 3.3000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 4, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 3.3000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 4, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 2.2000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.CENTRAL_STAMP, 4, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 2.5000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 1, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 1, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 1, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 1, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 1, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 1, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 2, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 2, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 2, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 2, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 2, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 2, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 4, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 4, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 4, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 4, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 4, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.TOP_SPECTRUM, 4, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 1, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 1, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 1, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 1, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 1, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 1, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 2, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 2, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 2, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 2, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 2, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 2, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 4, 1, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 4, 1, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 4, 2, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 4, 2, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 4, 4, AmpGain.LOW, DetectorManufacturer.HAMAMATSU, 10.0000);
            addEntry( GmosCommonType.AmpCount.TWELVE, AmpReadMode.FAST, BuiltinROI.BOTTOM_SPECTRUM, 4, 4, AmpGain.HIGH, DetectorManufacturer.HAMAMATSU, 10.0000);
        }

    /**
     * This class represent a key composed of those GMOS parameters that uniquely defines a particular
     * overhead. For every set of possible configurations declared in the lookup table
     * there is one overhead value associated. The GmosReadoutKey groups those parameters that
     * are associated with one specific overhead (a Double value)
     */
    private static final class GmosReadoutKey implements Comparable<GmosReadoutKey> {
        private final AmpCount _ampCount;
        private final AmpReadMode _ampSpeed;
        private final BuiltinROI _builtinROI;
        private final int _xBin;
        private final int _yBin;
        private final AmpGain _ampGain;
        private final DetectorManufacturer _detectorManufacturer;

        private GmosReadoutKey(AmpCount ampCount, AmpReadMode ampSpeed,
                               BuiltinROI builtinROI, int xBin, int yBin,
                               AmpGain ampGain, DetectorManufacturer detectorManufacturer) {
            _ampCount = ampCount;
            _ampSpeed = ampSpeed;
            _builtinROI = builtinROI;
            _xBin = xBin;
            _yBin = yBin;
            _ampGain = ampGain;
            _detectorManufacturer = detectorManufacturer;
        }

        public String toString() {
            StringBuilder buf = new StringBuilder();
            buf.append("AmpCount:");
            buf.append(_ampCount);
            buf.append("\t");
            buf.append("AmpReadMode:");
            buf.append(_ampSpeed);
            buf.append("\t");
            buf.append("AmpGain:");
            buf.append(_ampGain);
            buf.append("\t");
            buf.append("builtinROI:");
            buf.append(_builtinROI);
            buf.append("\t");
            buf.append("xBin:");
            buf.append(_xBin);
            buf.append("\t");
            buf.append("yBin:");
            buf.append(_yBin);
            buf.append("\t");
            buf.append("DetectorManufacturer:");
            buf.append(_detectorManufacturer);
            buf.append("\t");
            return buf.toString();
        }

        public int hashCode() {
            int res = _ampCount.hashCode();
            res = 37*res + _ampSpeed.hashCode();
            res = 37*res + _ampGain.hashCode();
            res = 37*res + _builtinROI.hashCode();
            res = 37*res + _xBin;
            res = 37*res + _yBin;
            res = 37*res + _detectorManufacturer.hashCode();
            return res;
        }

        public boolean equals(Object other) {
            if (other == null) return false;
            if (other.getClass() != this.getClass()) return false;
            GmosReadoutKey that = (GmosReadoutKey)other;
            if (!this._ampCount.equals(that._ampCount)) return false;
            if (!this._ampSpeed.equals(that._ampSpeed)) return false;
            if (!this._builtinROI.equals(that._builtinROI)) return false;
            if (this._xBin != that._xBin) return false;
            if (this._yBin != that._yBin) return false;
            if (!this._detectorManufacturer.equals(that._detectorManufacturer)) return false;
            return this._ampGain.equals(that._ampGain);
        }

        @Override
        public int compareTo(GmosReadoutKey o) {
            return COMPARATOR.compare(this, o);
        }

        public static final Comparator<GmosReadoutKey> COMPARATOR =
          Comparator
              .<GmosReadoutKey, AmpCount>comparing(k -> k._ampCount)
              .thenComparing(k -> k._ampSpeed)
              .thenComparing(k -> k._ampGain)
              .thenComparing(k -> k._builtinROI)
              .thenComparing(k -> k._xBin)
              .thenComparing(k -> k._yBin)
              .thenComparing(k -> k._detectorManufacturer);
    }


    private static void addEntry(AmpCount ampCount, AmpReadMode ampSpeed,
                            BuiltinROI builtinROI, int xBin, int yBin,
                            AmpGain ampGain, DetectorManufacturer detectorManufacturer,
                            double readoutTime) {
        GmosReadoutKey _readoutKey = new GmosReadoutKey(ampCount, ampSpeed, builtinROI, xBin, yBin, ampGain, detectorManufacturer);
        map.put(_readoutKey, readoutTime);
    }

    /**
     * Return the amount of time it takes in seconds to readout an image, based on the
     * configuration in the sequence and any custom ROI settings.
     *
     * @param config the current configuration
     * @param customRoiList non-empty if custom ROIS are defined for the instrument
     */
    public static double getReadoutOverhead(Config config, GmosCommonType.CustomROIList customRoiList) {
        final AmpCount             ampCount    = (AmpCount) config.getItemValue(AmpCount.KEY);
        final AmpGain              ampGain     = (AmpGain) config.getItemValue(AmpGain.KEY);
        final AmpReadMode          ampReadMode = (AmpReadMode) config.getItemValue(AmpReadMode.KEY);
        final BuiltinROI           builtinROI  = (BuiltinROI) config.getItemValue(BuiltinROI.KEY);
        final DetectorManufacturer detMan      = (DetectorManufacturer) config.getItemValue(DetectorManufacturer.KEY);
        final Binning              xBin        = (Binning) config.getItemValue(InstGmosCommon.X_BIN_KEY);
        final Binning              yBin        = (Binning) config.getItemValue(InstGmosCommon.Y_BIN_KEY);

        final BuiltinROI roiKey = (builtinROI == BuiltinROI.CUSTOM) ? BuiltinROI.FULL_FRAME : builtinROI;

        final GmosReadoutKey key = new GmosReadoutKey(ampCount, ampReadMode, roiKey, xBin.getValue(), yBin.getValue(), ampGain, detMan);
        final Double d = map.get(key);

        double overhead = (d == null) ? 0 : d;
        if (builtinROI == BuiltinROI.CUSTOM) {
            // REL-1385
            final int rows = customRoiList.totalUnbinnedRows();
            if (rows > 0) {
                overhead = 1 + overhead * rows / detMan.getYsize();
            }
        }
        return overhead;
    }

    private static String capitalize(String input) {
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    private static String roi(GmosCommonType.BuiltinROI roi) {
        switch (roi) {
            case FULL_FRAME:       return "FullFrame";
            case CCD2:             return "Ccd2";
            case CENTRAL_SPECTRUM: return "CentralSpectrum";
            case CENTRAL_STAMP:    return "CentralStamp";
            case TOP_SPECTRUM:     return "TopSpectrum";
            case BOTTOM_SPECTRUM:  return "BottomSpectrum";
            case CUSTOM:           return "Custom";
            default: throw new RuntimeException("Unexpected GMOS ROI: " + roi);
        }
    }

    public static void main(String[] args) {
        final Comparator<Map.Entry<GmosReadoutKey, Double>> order = (o1, o2) -> {
            int k = o1.getKey().compareTo(o2.getKey());
            if (k == 0) return o1.getValue().compareTo(o2.getValue());
            else return k;
        };

        GmosReadoutTime
                .map
                .entrySet()
                .stream()
                .filter(entry -> entry.getKey()._detectorManufacturer.equals(DetectorManufacturer.HAMAMATSU))
                .sorted(order)
                .forEach(entry -> {
            GmosReadoutKey key = entry.getKey();
            Double value       = entry.getValue();
            System.out.printf(
                "%s\t%s\t%s\t%s\t%s\t%s\t%1.4f seconds%n",
                key._ampCount.displayValue(),
                key._ampSpeed.displayValue(),
                key._ampGain.displayValue(),
                roi(key._builtinROI),
                capitalize(GmosCommonType.Binning.getBinningByValue(key._xBin).name().toLowerCase()),
                capitalize(GmosCommonType.Binning.getBinningByValue(key._yBin).name().toLowerCase()),
                value
            );
        });
    }
}
