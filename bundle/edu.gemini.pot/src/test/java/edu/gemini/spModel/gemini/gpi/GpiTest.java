package edu.gemini.spModel.gemini.gpi;

import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ConfigSequence;
import edu.gemini.spModel.config2.DefaultConfig;
import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.gemini.calunit.calibration.CalDictionary;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.xml.PioXmlFactory;
import junit.framework.TestCase;

import java.util.Arrays;

/**
 * Tests the GPI instrument class
 */
public class GpiTest  extends TestCase {
    static final private double _ERROR = .00001;

    public GpiTest(String message) {
        super(message);
    }

    // Setup some test objects.
    public void testInitial() {

        Gpi inst = new Gpi();
        assertEquals(inst.isAstrometricField(), Gpi.DEFAULT_ASTROMETRIC_FIELD);
        assertEquals(inst.getAdc(), Gpi.Adc.DEFAULT);
        assertEquals(inst.getObservingMode(), None.instance());
        assertEquals(inst.getDisperser(), Gpi.Disperser.DEFAULT);
        assertEquals(inst.getHalfWavePlateAngle(), 0.0);
        assertEquals(inst.getDetectorReadoutArea(), Gpi.DetectorReadoutArea.DEFAULT);
        assertEquals(inst.getReadoutArea(), None.instance());
        assertEquals(inst.getEntranceShutter(), Gpi.Shutter.DEFAULT);
        assertEquals(inst.getScienceArmShutter(), Gpi.Shutter.DEFAULT);
        assertEquals(inst.getCalEntranceShutter(), Gpi.Shutter.DEFAULT);
        assertEquals(inst.getReferenceArmShutter(), Gpi.Shutter.DEFAULT);
        assertEquals(inst.getApodizer(), Gpi.Apodizer.DEFAULT);
        assertEquals(inst.getLyot(), Gpi.Lyot.DEFAULT);
        assertEquals(inst.getIrLaserLampEnum(), Gpi.ArtificialSource.DEFAULT);
        assertEquals(inst.getVisibleLaserLampEnum(), Gpi.ArtificialSource.DEFAULT);
        assertEquals(inst.getSuperContinuumLampEnum(), Gpi.ArtificialSource.DEFAULT);
        assertEquals(inst.getArtificialSourceAttenuation(), Gpi.DEFAULT_ARTIFICIAL_SOURCE_ATTENUATION);
        assertEquals(inst.getPupilCamera(), Gpi.PupilCamera.DEFAULT);
        assertEquals(inst.getFpm(), Gpi.FPM.DEFAULT);
        assertEquals(inst.getDetectorSamplingMode(), Gpi.DetectorSamplingMode.DEFAULT);
        assertEquals(inst.getMcdsCount(), None.instance());
        assertEquals(inst.getCoadds(), InstConstants.DEF_COADDS);
        assertEquals(inst.getPosAngleDegrees(), InstConstants.DEF_POS_ANGLE, _ERROR);
        assertEquals(inst.getTotalExposureTime(), Gpi.DEFAULT_EXPOSURE_TIME, _ERROR);
    }

    // test get/set paramset
    public void testParamSetIO() {
        Gpi inst = new Gpi();
        inst.setAstrometricField(true);
        inst.setAdc(Gpi.Adc.OUT);
        inst.setObservingMode(new Some<Gpi.ObservingMode>(Gpi.ObservingMode.CORON_Y_BAND));
        inst.setDisperser(Gpi.Disperser.WOLLASTON);
        inst.setDetectorReadoutArea(Gpi.DetectorReadoutArea.CENTRAL_256);
        inst.setEntranceShutter(Gpi.Shutter.CLOSE);
        inst.setApodizer(Gpi.Apodizer.APOD_J);
        inst.setLyot(Gpi.Lyot.LYOT_080m12_03);
        inst.setIrLaserLamp(Gpi.ArtificialSource.ON.toBoolean());
        inst.setPupilCamera(Gpi.PupilCamera.IN);
        inst.setFpm(Gpi.FPM.FPM_H);
        inst.setDetectorSamplingMode(Gpi.DetectorSamplingMode.MULTIPLE_CDS);
        inst.setExposureTime(300.);
        inst.setCoadds(2);
        inst.setPosAngleDegrees(90.);

        ParamSet p = inst.getParamSet(new PioXmlFactory());

        Gpi copy = new Gpi();
        copy.setParamSet(p);

        assertEquals(true, copy.isAstrometricField());
        assertEquals(inst.isAstrometricField(), copy.isAstrometricField());

        assertEquals(Gpi.Adc.OUT, copy.getAdc());
        assertEquals(inst.getAdc(), copy.getAdc());

        assertEquals(new Some(Gpi.ObservingMode.NONSTANDARD), copy.getObservingMode()); // was overridden (OT-101)

        assertEquals(Gpi.Disperser.WOLLASTON, copy.getDisperser());
        assertEquals(inst.getDisperser(), copy.getDisperser());

        assertEquals(inst.getHalfWavePlateAngle(), copy.getHalfWavePlateAngle());
        assertEquals(0.0, copy.getHalfWavePlateAngle());

        assertEquals(Gpi.DetectorReadoutArea.CENTRAL_256, copy.getDetectorReadoutArea());
        assertEquals(inst.getDetectorReadoutArea(), copy.getDetectorReadoutArea());

        assertEquals(Gpi.Shutter.CLOSE, copy.getEntranceShutter());
        assertEquals(inst.getEntranceShutter(), copy.getEntranceShutter());

        assertEquals(Gpi.Apodizer.APOD_J, copy.getApodizer());
        assertEquals(inst.getApodizer(), copy.getApodizer());

        assertEquals(Gpi.Lyot.LYOT_080m12_03, copy.getLyot());
        assertEquals(inst.getLyot(), copy.getLyot());

        assertEquals(Gpi.ArtificialSource.ON, copy.getIrLaserLampEnum());
        assertEquals(inst.getIrLaserLampEnum(), copy.getIrLaserLampEnum());
        assertEquals(Gpi.DEFAULT_ARTIFICIAL_SOURCE_ATTENUATION, inst.getArtificialSourceAttenuation());

        assertEquals(Gpi.PupilCamera.IN, copy.getPupilCamera());
        assertEquals(inst.getPupilCamera(), copy.getPupilCamera());

        assertEquals(Gpi.FPM.FPM_H, copy.getFpm());
        assertEquals(inst.getFpm(), copy.getFpm());

        assertEquals(Gpi.DetectorSamplingMode.MULTIPLE_CDS, copy.getDetectorSamplingMode());
        assertEquals(inst.getDetectorSamplingMode(), copy.getDetectorSamplingMode());
        assertEquals(Gpi.DEFAULT_MCDS_COUNT, inst.getMcdsCount().getValue().intValue());

        assertEquals(300., copy.getExposureTime(), _ERROR);
        assertEquals(2, copy.getCoadds(), _ERROR);
        assertEquals(90., copy.getPosAngleDegrees(), _ERROR);
    }

    public void testResetASUAttenuation() {
        Gpi inst = new Gpi();
        inst.setIrLaserLamp(true);
        inst.setVisibleLaserLamp(true);
        inst.setSuperContinuumLamp(true);
        inst.setArtificialSourceAttenuation(25.5);
        assertEquals(25.5, inst.getArtificialSourceAttenuation(), 0);

        inst.setIrLaserLamp(false);
        assertEquals(25.5, inst.getArtificialSourceAttenuation(), 0);
        inst.setVisibleLaserLamp(false);
        assertEquals(25.5, inst.getArtificialSourceAttenuation(), 0);
        inst.setSuperContinuumLamp(false);
        assertEquals(Gpi.DEFAULT_ARTIFICIAL_SOURCE_ATTENUATION, inst.getArtificialSourceAttenuation(), 0);
    }

    public void testDirectModesSetCALandAO() {
        Gpi inst = new Gpi();
        assertTrue(inst.isUseAo());
        assertTrue(inst.isUseCal());
        inst.setObservingMode(new Some<Gpi.ObservingMode>(Gpi.ObservingMode.DIRECT_H_BAND));
        assertTrue(inst.isUseAo());
        assertFalse(inst.isUseCal());
    }

    public void testNRMModesSetCALandAO() {
        Gpi inst = new Gpi();
        assertTrue(inst.isUseAo());
        assertTrue(inst.isUseCal());
        inst.setObservingMode(new Some<Gpi.ObservingMode>(Gpi.ObservingMode.NRM_H));
        assertTrue(inst.isUseAo());
        assertFalse(inst.isUseCal());
    }

    public void testDeployASUOnAcquisitionSequences() {
        Gpi inst = new Gpi();
        ItemKey obsTypeKey = new ItemKey(CalDictionary.OBS_KEY, InstConstants.OBS_CLASS_PROP);
        Config conf = new DefaultConfig();
        conf.putItem(obsTypeKey, "acqCal");
        ConfigSequence in = new ConfigSequence(new Config[] {conf});
        ConfigSequence resultSequence = inst.postProcessSequence(in);
        ItemKey asuKey = new ItemKey(new ItemKey(InstConstants.INSTRUMENT_NAME_PROP), Gpi.ARTIFICIAL_SOURCE_ATTENUATION_PROP.getName());
        ItemKey scKey = new ItemKey(new ItemKey(InstConstants.INSTRUMENT_NAME_PROP), Gpi.SUPER_CONTINUUM_LAMP_PROP.getName());
        assertEquals(resultSequence.getItemValue(0, asuKey), Gpi.CALIBRATION_ARTIFICIAL_SOURCE_ATTENUATION);
        assertEquals(resultSequence.getItemValue(0, scKey), Gpi.ArtificialSource.ON);
    }

    public void testUseAoAndUseCalOnDark() {
        Gpi inst = new Gpi();
        ItemKey obsTypeKey = new ItemKey(CalDictionary.OBS_KEY, InstConstants.OBSERVE_TYPE_PROP);
        Config conf = new DefaultConfig();
        conf.putItem(obsTypeKey, "DARK");
        ConfigSequence in = new ConfigSequence(new Config[] {conf});
        ConfigSequence resultSequence = inst.postProcessSequence(in);
        ItemKey useAoKey = new ItemKey(new ItemKey(InstConstants.INSTRUMENT_NAME_PROP), Gpi.USE_AO_PROP.getName());
        ItemKey useCarKey = new ItemKey(new ItemKey(InstConstants.INSTRUMENT_NAME_PROP), Gpi.USE_CAL_PROP.getName());
        assertEquals(resultSequence.getItemValue(0, useAoKey), Boolean.FALSE);
        assertEquals(resultSequence.getItemValue(0, useCarKey), Boolean.FALSE);
    }

    public void testUseAoAndUseCalOnFlat() {
        Gpi inst = new Gpi();
        ItemKey obsTypeKey = new ItemKey(CalDictionary.OBS_KEY, InstConstants.OBSERVE_TYPE_PROP);
        Config conf = new DefaultConfig();
        conf.putItem(obsTypeKey, "FLAT");
        ConfigSequence in = new ConfigSequence(new Config[] {conf});
        ConfigSequence resultSequence = inst.postProcessSequence(in);
        ItemKey useAoKey = new ItemKey(new ItemKey(InstConstants.INSTRUMENT_NAME_PROP), Gpi.USE_AO_PROP.getName());
        ItemKey useCarKey = new ItemKey(new ItemKey(InstConstants.INSTRUMENT_NAME_PROP), Gpi.USE_CAL_PROP.getName());
        assertEquals(resultSequence.getItemValue(0, useAoKey), Boolean.FALSE);
        assertEquals(resultSequence.getItemValue(0, useCarKey), Boolean.FALSE);
    }

    public void testUseAoAndUseCalOnArc() {
        Gpi inst = new Gpi();
        ItemKey obsTypeKey = new ItemKey(CalDictionary.OBS_KEY, InstConstants.OBSERVE_TYPE_PROP);
        Config conf = new DefaultConfig();
        conf.putItem(obsTypeKey, "ARC");
        ConfigSequence in = new ConfigSequence(new Config[] {conf});
        ConfigSequence resultSequence = inst.postProcessSequence(in);
        ItemKey useAoKey = new ItemKey(new ItemKey(InstConstants.INSTRUMENT_NAME_PROP), Gpi.USE_AO_PROP.getName());
        ItemKey useCarKey = new ItemKey(new ItemKey(InstConstants.INSTRUMENT_NAME_PROP), Gpi.USE_CAL_PROP.getName());
        assertEquals(resultSequence.getItemValue(0, useAoKey), Boolean.FALSE);
        assertEquals(resultSequence.getItemValue(0, useCarKey), Boolean.FALSE);
    }
}
