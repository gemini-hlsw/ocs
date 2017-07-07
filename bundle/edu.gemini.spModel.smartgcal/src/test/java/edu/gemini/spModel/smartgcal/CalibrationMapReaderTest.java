// Copyright 2011 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id$

package edu.gemini.spModel.smartgcal;

import edu.gemini.shared.util.immutable.ImCollections;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.spModel.gemini.calunit.CalUnitParams;
import edu.gemini.spModel.gemini.calunit.smartgcal.Calibration;
import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationMap;
import edu.gemini.spModel.gemini.calunit.smartgcal.ConfigurationKey;
import edu.gemini.spModel.gemini.calunit.smartgcal.Version;
import edu.gemini.spModel.gemini.calunit.smartgcal.maps.GNIRSCalibrationMap;
import edu.gemini.spModel.type.SpTypeUtil;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.stream.Stream;

public class CalibrationMapReaderTest {

    private static String calibrationHeader = ",CALIBRATION1,CALIBRATION2";
    private static String calibrationValues = ",Value1,Value2";

    @Test
    public void canLookupByApproximateDisplayName() {
        // UX-638: check if lookup works when special characters / whitespaces are not precisely correct
        Assert.assertEquals(SpTypeUtil.displayValueToEnum(CalUnitParams.Lamp.class, "IR grey body - high"), CalUnitParams.Lamp.IR_GREY_BODY_HIGH);  // exact lookup ("normal" dash)
        Assert.assertEquals(SpTypeUtil.displayValueToEnum(CalUnitParams.Lamp.class, "IR grey body \u2013 high"), CalUnitParams.Lamp.IR_GREY_BODY_HIGH);  // en-dash
        Assert.assertEquals(SpTypeUtil.displayValueToEnum(CalUnitParams.Lamp.class, "IR grey body \u2014 high"), CalUnitParams.Lamp.IR_GREY_BODY_HIGH);  // em-dash
        Assert.assertEquals(SpTypeUtil.displayValueToEnum(CalUnitParams.Lamp.class, "IR  grey   body -high"), CalUnitParams.Lamp.IR_GREY_BODY_HIGH);  // additional and omitted blanks
        Assert.assertEquals(SpTypeUtil.displayValueToEnum(CalUnitParams.Lamp.class, "irgreybodyhigh"), CalUnitParams.Lamp.IR_GREY_BODY_HIGH); // all special chars removed
        Assert.assertEquals(SpTypeUtil.displayValueToEnum(CalUnitParams.Lamp.class, "IRGreyBodyHigh"), CalUnitParams.Lamp.IR_GREY_BODY_HIGH);
        Assert.assertEquals(SpTypeUtil.displayValueToEnum(CalUnitParams.Lamp.class, "IRGREYBODYHIGH"), CalUnitParams.Lamp.IR_GREY_BODY_HIGH);
    }

    @Test
    public void acceptsCorrectHeader() {
        CalibrationMapReader reader  = new CalibrationMapReader(new FakeMap());
        reader.read(("HEADER1,HEADER2" + calibrationHeader).getBytes());
        Assert.assertFalse(reader.hasErrors());
        Assert.assertEquals(0, reader.getErrors().size());
    }

    @Test
    public void detectsMissingKeyHeaderColumn() {
        CalibrationMapReader reader  = new CalibrationMapReader(new FakeMap());
        reader.read(("HEADER2"+calibrationHeader).getBytes());
        Assert.assertTrue(reader.hasErrors());
        Assert.assertEquals(2, reader.getErrors().size());
    }

    @Test
    public void detectsMissingCalibrationHeaderColumn() {
        CalibrationMapReader reader  = new CalibrationMapReader(new FakeMap());
        reader.read(("HEADER1,HEADER2,CALIBRATION2").getBytes());
        Assert.assertTrue(reader.hasErrors());
        Assert.assertEquals(2, reader.getErrors().size());
    }


    @Test
    public void detectsDuplicateHeaderColumn() {
        CalibrationMapReader reader  = new CalibrationMapReader(new FakeMap());
        reader.read(("HEADER1,HEADER2,HEADER1"+calibrationHeader).getBytes());
        Assert.assertTrue(reader.hasErrors());
        Assert.assertEquals(2, reader.getErrors().size());
    }

    @Test
    public void canHandleWhiteSpaces() {
        String data =
            "   \n" +
            "  HEADER1   ,  HEADER2  , CALIBRATION1, CALIBRATION2\n\n" +
            "# something something \n" +
            "value1,value2" + calibrationValues + "\n" +
            "value3,value4" + calibrationValues + "\n" +
            "# something else\n";

        CalibrationMapReader reader  = new CalibrationMapReader(new FakeMap());
        reader.read(data.getBytes());
        Assert.assertFalse(reader.hasErrors());
        Assert.assertEquals(0, reader.getErrors().size());
    }

    @Test
    public void detectsMultipleErrors() {
        String data =
            "HEADER1,HEADER2" + calibrationHeader + "\n" +
            "value1,value2" + calibrationValues + "\n" +
            "ERROR,value3" + calibrationValues + "\n" +     // this will cause an error in key constructor
            "value4,value5" + calibrationValues + "\n" +
            "ERROR,value6" + calibrationValues + "\n";      // this will cause an error in key constructor

        CalibrationMapReader reader  = new CalibrationMapReader(new FakeMap());
        reader.read(data.getBytes());
        Assert.assertTrue(reader.hasErrors());
        Assert.assertEquals(2, reader.getErrors().size());
        Assert.assertTrue(reader.getErrors().get(0).startsWith("line 3"));
        Assert.assertTrue(reader.getErrors().get(1).startsWith("line 5"));
    }

    @Test
    public void detectsGNIRSErrors() {
        String data =
            "Mode,Pixel Scale,Disperser,Cross Dispersed,Central Wavelength,Focal Plane Unit,Well Depth,Calibration Lamps,Calibration Shutter,Calibration Filter,Calibration Diffuser,Calibration Observe,Calibration Exposure Time,Calibration Coadds,Calibration Basecal\n" +
            /*error*/"Spectroscopy,\"0.05\"\"/pix\",BROKEN,LXD,0.9 - 2.56,0.10 arcsec,Shallow,Ar arc,Closed,none,IR,2,5.0,1,\n" +
            /* ok  */"Spectroscopy,\"0.05\"\"/pix\",10 l/mm grating,LXD,0.9 - 2.56,0.10 arcsec,Shallow,Ar arc,Closed,none,IR,2,5.0,1,\n" +
            /*error*/"Spectroscopy,\"0.05\"\"/pix\",10 l/mm grating,LXD,0.9 - 2.56,0.45 arcsec,Shallow,Ar arc,Closed,BROKEN,IR,2,3.0,1,\n" +
            /* ok  */"Spectroscopy,\"0.05\"\"/pix\",10 l/mm grating,LXD,0.9 - 2.56,0.45 arcsec,Shallow,Ar arc,Closed,none,IR,2,3.0,1,";

        CalibrationMapReader reader  = new CalibrationMapReader(new GNIRSCalibrationMap(new Version(1, new Date())));
        reader.read(data.getBytes());
        Assert.assertTrue(reader.hasErrors());
        Assert.assertEquals(2, reader.getErrors().size());
        Assert.assertTrue(reader.getErrors().get(0).startsWith("line 2"));
        Assert.assertTrue(reader.getErrors().get(1).startsWith("line 4"));
    }

    @Test
    // test behavior of CentralWavelengthMap using the GNIRS implementation
    public void detectsOverlappingGNIRSKeys() {
        String data =
            "Mode,Pixel Scale,Disperser,Cross Dispersed,Central Wavelength,Focal Plane Unit,Well Depth,Calibration Lamps,Calibration Shutter,Calibration Filter,Calibration Diffuser,Calibration Observe,Calibration Exposure Time,Calibration Coadds,Calibration Basecal\n" +
            "Spectroscopy,\"0.05\"\"/pix\",10 l/mm grating,LXD,0.9 - 2.56,0.10 arcsec,Shallow,Ar arc,Closed,none,IR,2,5.0,1,Night\n" +
            "Spectroscopy,\"0.05\"\"/pix\",10 l/mm grating,LXD,2.00 - 2.56,0.10 arcsec,Shallow,Ar arc,Closed,none,IR,2,5.0,1,Day\n";

        CalibrationMapReader reader  = new CalibrationMapReader(new GNIRSCalibrationMap(new Version(1, new Date())));
        reader.read(data.getBytes());
        Assert.assertTrue(reader.hasErrors());
        Assert.assertEquals(1, reader.getErrors().size());
        Assert.assertTrue(reader.getErrors().get(0).contains("overlap"));
    }

    // -------- fake objects for testing

    public static class FakeMap implements CalibrationMap {

        @Override
        public Set<ConfigurationKey> createConfig(Properties properties) {
            Set<ConfigurationKey> keys = new HashSet<ConfigurationKey>();
            keys.add(new FakeKey(properties));
            return keys;
        }

        @Override
        public ConfigurationKey.Values[] getKeyValueNames() {
            return FakeKey.Headers.values();
        }

        @Override
        public Calibration createCalibration(Properties properties) {
            return new FakeCalibration();
        }

        @Override
        public ConfigurationKey.Values[] getCalibrationValueNames() {
            return FakeCalibration.Values.values();
        }

        @Override
        public Calibration put(ConfigurationKey key, Properties properties, Calibration calibration) {
            return null;
        }

        @Override
        public List<Calibration> get(ConfigurationKey key) {
            return null;
        }

        @Override
        public List<Calibration> get(ConfigurationKey key, Double centralWavelength) {
            return null;
        }

        @Override
        public Version getVersion() {
            return new Version(1, new Date());
        }

        @Override
        public Stream<ImList<String>> export() { return Stream.empty(); }
    }

    public static class FakeCalibration implements Calibration {

        public enum Values implements ConfigurationKey.Values {
            CALIBRATION1,
            CALIBRATION2
        }

        @Override
        public Boolean isFlat() {
            return null;
        }

        @Override
        public Boolean isArc() {
            return null;
        }

        @Override
        public Boolean isBasecalDay() {
            return false;
        }

        @Override
        public Boolean isBasecalNight() {
            return false;
        }

        @Override
        public Set<CalUnitParams.Lamp> getLamps() {
            return null;
        }

        @Override
        public CalUnitParams.Shutter getShutter() {
            return null;
        }

        @Override
        public CalUnitParams.Filter getFilter() {
            return null;
        }

        @Override
        public CalUnitParams.Diffuser getDiffuser() {
            return null;
        }

        @Override
        public Integer getObserve() {
            return null;
        }

        @Override
        public Double getExposureTime() {
            return null;
        }

        @Override
        public Integer getCoadds() {
            return null;
        }

        @Override
        public ImList<String> export() { return ImCollections.emptyList(); }
    }

    public static class FakeKey implements ConfigurationKey {

        private Properties properties;

        public enum Headers implements Values {
            HEADER1,
            HEADER2
        }

        public FakeKey(Properties properties) {
            this.properties = properties;
            if (properties.getProperty("HEADER1").equals("ERROR")) {
                throw new RuntimeException("error  happened");
            }
        }

        @Override
        public int hashCode() {
            return properties.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return properties.equals(o);
        }

        @Override
        public String getInstrumentName() {
            return "FAKE";
        }

        @Override
        public ImList<String> export() { return ImCollections.emptyList(); }
    }

}
