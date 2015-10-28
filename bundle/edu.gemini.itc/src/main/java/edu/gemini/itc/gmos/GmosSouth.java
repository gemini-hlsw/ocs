package edu.gemini.itc.gmos;

import edu.gemini.itc.base.GainLimit;
import edu.gemini.itc.base.SaturationLimit;
import edu.gemini.itc.base.WarningLimit;
import edu.gemini.itc.shared.GmosParameters;
import edu.gemini.itc.shared.ObservationDetails;
import edu.gemini.spModel.gemini.gmos.InstGmosSouth;

import java.util.ArrayList;
import java.util.List;

/**
 * Gmos specification class
 */
public final class GmosSouth extends Gmos {

    private static final double WELL_DEPTH = 106000.0;

    /**
     * /** Related files will start with this prefix
     */
    public static final String INSTR_PREFIX = "gmos_s_";

    // Instrument reads its configuration from here.
    private static final String FILENAME = "gmos_s" + getSuffix();

    // Detector data files (see REL-478)
    private static final String[] DETECTOR_CCD_FILES = {"ccd_hamamatsu_bb", "ccd_hamamatsu_hsc", "ccd_hamamatsu_sc"};

    // Detector display names corresponding to the detectorCcdIndex
    private static final String[] DETECTOR_CCD_NAMES = {"BB", "HSC", "SC"};


    public GmosSouth(final GmosParameters gp, final ObservationDetails odp, final int detectorCcdIndex) {
        super(gp, odp, FILENAME, detectorCcdIndex);
    }

    protected Gmos[] createCcdArray() {
        return new Gmos[]{this, new GmosSouth(gp, odp, 1), new GmosSouth(gp, odp, 2)};
    }

    protected String getPrefix() {
        return INSTR_PREFIX;
    }

    protected String[] getCcdFiles() {
        return DETECTOR_CCD_FILES;
    }

    protected String[] getCcdNames() {
        return DETECTOR_CCD_NAMES;
    }

    @Override public List<WarningLimit> warnings() {
        return new ArrayList<WarningLimit>() {{
            add(new SaturationLimit(WELL_DEPTH * getSpatialBinning() * getSpectralBinning(), 0.95));
            add(new GainLimit(getADSaturation() * InstGmosSouth.getMeanGain(gp.ampGain(), gp.ampReadMode(), gp.ccdType()), 0.95));
        }};
    }


}
