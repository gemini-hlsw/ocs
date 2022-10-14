package edu.gemini.p2checker.rules.gnirs;

import edu.gemini.p2checker.api.*;
import edu.gemini.p2checker.rules.altair.AltairRule;
import edu.gemini.p2checker.util.AbstractConfigRule;
import edu.gemini.p2checker.util.NoPOffsetWithSlitRule;
import edu.gemini.p2checker.util.SequenceRule;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.gemini.gnirs.GNIRSParams;
import edu.gemini.spModel.gemini.gnirs.GNIRSParams.*;
import edu.gemini.spModel.gemini.gnirs.InstGNIRS;
import scala.runtime.AbstractFunction2;

import java.util.*;

public class GnirsRule implements IRule {
    private static final Collection<IConfigRule> GNIRS_RULES = new ArrayList<>();
    private static final String PREFIX = "GnirsRule_";

    // ERROR if Camera ?= *red (or equivalent of pixel scale+wavelength) && Cross-dispersed != No, "The red cameras cannot be used in cross-dispersed mode."
    private static IConfigRule RED_CAMERA_XD_RULE = new IConfigRule() {
        private static final String MESSAGE = "The red cameras cannot be used in cross-dispersed mode.";

        @Override
        public Problem check(final Config config, final int step, final ObservationElements elems, final Object state) {
            final Camera camera = (Camera) SequenceRule.getInstrumentItem(config, InstGNIRS.CAMERA_PROP);
            if (camera == null) return null;
            switch (camera) {
                case SHORT_BLUE:
                case LONG_BLUE:
                    return null;
            }

            final CrossDispersed xd = (CrossDispersed) SequenceRule.getInstrumentItem(config, InstGNIRS.CROSS_DISPERSED_PROP);
            if ((xd == null) || (xd == CrossDispersed.NO)) return null;

            return new Problem(ERROR, PREFIX+"RED_CAMERA_XD_RULE", MESSAGE, elems.getSeqComponentNode());
        }

        @Override
        public IConfigMatcher getMatcher() {
            return IConfigMatcher.ALWAYS;
        }
    };

    private static final IConfigRule SHORT_RED_CAMERA_RULE = new IConfigRule() {
        private static final String MESSAGE = "The short red camera is currently unavailable.";

        @Override
        public Problem check(final Config config, final int step, final ObservationElements elems, final Object state) {
            final Camera camera = (Camera) SequenceRule.getInstrumentItem(config, InstGNIRS.CAMERA_PROP);
            if (camera == null) return null;
            boolean isShortRed = camera == Camera.SHORT_RED;

            final PixelScale ps = (PixelScale) SequenceRule.getInstrumentItem(config, InstGNIRS.PIXEL_SCALE_PROP);
            if (!isShortRed && ps != PixelScale.PS_015) return null;
            final Wavelength l = (Wavelength) SequenceRule.getInstrumentItem(config, InstGNIRS.CENTRAL_WAVELENGTH_PROP);
            if (!isShortRed && ((l == null) || l.doubleValue() <= 2.5)) return null;

            return new Problem(ERROR, PREFIX + "SHORT_RED_CAMERA_RULE", MESSAGE, elems.getSeqComponentNode());
        }

        @Override
        public IConfigMatcher getMatcher() {
            return IConfigMatcher.ALWAYS;
        }
    };

    // ERROR if Wavelength > 2.5 um && Cross-dispered != No, "Cross-dispersed mode is not available in the L or M bands."
    private static IConfigRule WAVELENGTH_XD_RULE = new IConfigRule() {
        private static final String MESSAGE = "Cross-dispersed mode is not available in the L or M bands.";

        @Override
        public Problem check(final Config config, final int step, final ObservationElements elems, final Object state) {
            final CrossDispersed xd = (CrossDispersed) SequenceRule.getInstrumentItem(config, InstGNIRS.CROSS_DISPERSED_PROP);
            if ((xd == null) || (xd == CrossDispersed.NO)) return null;

            final Wavelength l = (Wavelength) SequenceRule.getInstrumentItem(config, InstGNIRS.CENTRAL_WAVELENGTH_PROP);
            if (l.doubleValue() <= 2.5) return null;

            return new Problem(ERROR, PREFIX+"WAVELENGTH_XD_RULE", MESSAGE, SequenceRule.getInstrumentOrSequenceNode(step, elems, config));
        }

        @Override
        public IConfigMatcher getMatcher() {
            return IConfigMatcher.ALWAYS;
        }
    };

    // ERROR if ((Camera == "Short blue") || (pixel scale == 0.15)) && Cross-dispersed==LXD, "The short blue camera must be used with the SXD prism."
    private static IConfigRule SHORT_BLUE_XD_RULE = new IConfigRule() {
        private static final String BLUE_MESSAGE = "The short blue camera cannot be used with the LXD prism";
        private static final String SCALE_MESSAGE = "The 0.15\"/pix scale cannot be used with the LXD prism";

        @Override
        public Problem check(final Config config, final int step, final ObservationElements elems, final Object state) {
            // Only apply this rule if using LXD.
            final CrossDispersed xd = (CrossDispersed) SequenceRule.getInstrumentItem(config, InstGNIRS.CROSS_DISPERSED_PROP);
            if (xd != CrossDispersed.LXD) return null;

            final PixelScale scale = (PixelScale) SequenceRule.getInstrumentItem(config, InstGNIRS.PIXEL_SCALE_PROP);
            if (scale == PixelScale.PS_015) {
                return new Problem(ERROR, PREFIX+"SCALE_MESSAGE", SCALE_MESSAGE, SequenceRule.getInstrumentOrSequenceNode(step, elems, config));
            }

            final Camera camera = (Camera) SequenceRule.getInstrumentItem(config, InstGNIRS.CAMERA_PROP);
            if (camera == Camera.SHORT_BLUE) {
                return new Problem(ERROR, PREFIX+"BLUE_MESSAGE", BLUE_MESSAGE, elems.getSeqComponentNode());
            }
            return null;
        }

        @Override
        public IConfigMatcher getMatcher() {
            return IConfigMatcher.ALWAYS;
        }
    };


    /*
  ERROR if inst=GNIRS && Read Mode == Very Bright Objects && exptime < 0.2, 'Exposure times for Very Bright Object
      mode must be >= 0.2 sec.'
  WARNING if inst=GNIRS && Read Mode == Very Bright Objects && exptime > 1.0, 'Exposure times for Very Bright Object
      mode are normally less than 1.0 sec; consider using a lower read noise mode instead.'

  ERROR if inst=GNIRS && Read Mode == Bright Objects && exptime < 0.6, 'Exposure times for Bright Object mode must
      be >= 0.6 sec'
  WARNING if inst=GNIRS && Read Mode == Bright Objects && exptime > 20.0, 'Exposure times for Bright Object mode are
      normally less than 20.0 sec; consider using a lower read noise mode instead.'

  ERROR if inst=GNIRS && Read Mode == Faint Objects && exptime < 9.0, 'Exposure times for Faint Object mode must
      be >= 9.0 sec.'
  WARNING if inst=GNIRS && Read Mode == Faint Objects && exptime > 60.0, 'Exposure times for Faint Object mode are
      normally less than 60.0 sec. consider using a lower read noise mode instead.'

  ERROR if inst=GNIRS && Read Mode == Very Faint Objects && exptime < 18, 'Exposure times for Very Faint Object mode
      must be greater than 18 sec.'
    */
    private enum ExpTimeComparison {
        tooShort {
            @Override
            public boolean check(final double expTime, final double limit) {
                return expTime < limit;
            }

            @Override
            public Problem.Type getProblemType(final ReadMode rm, final double expTime) {
                if (check(expTime, limits.get(rm)[0])) {
                    return ERROR;
                }
                return Problem.Type.NONE;
            }
        },
        tooLong {
            @Override
            public boolean check(final double expTime, final double limit) {
                return expTime > limit;
            }

            @Override
            public Problem.Type getProblemType(final ReadMode rm, final double expTime) {
                if (check(expTime, limits.get(rm)[1])) {
                    return WARNING;
                }
                return Problem.Type.NONE;
            }
        };
        private static final Map<ReadMode, Double[]> limits = new HashMap<>();
        static {
            limits.put(ReadMode.VERY_BRIGHT, new Double[]{0.2, 1.0});
            limits.put(ReadMode.BRIGHT, new Double[]{0.6, 20.0});
            limits.put(ReadMode.FAINT, new Double[]{9.0, 60.0});
            limits.put(ReadMode.VERY_FAINT, new Double[]{18.0, Double.MAX_VALUE});
        }

        /**
         * Checks if the exposure time is over/under the limit
         *
         * @param expTime the exposure time to check
         * @param limit   against which to check
         * @return true if we are "outside" the limit (i.e. if there is a problem)
         */
        public abstract boolean check(final double expTime, final double limit);

        /**
         * Get a warning, error or none for the given exposure time and ReadMode
         *
         * @param rm      ReadMode for this step
         * @param expTime the exposure time to check
         * @return Problem.Type.WARNING, Problem.Type.ERROR or Problem.Type.NONE
         */
        public abstract Problem.Type getProblemType(final ReadMode rm, final double expTime);

        /**
         * The entry point for using this class. Checks if there is a problem.
         *
         * @param config complete configuration to be sent to the control system
         * @param step   the step count of the sequence to which the configuration
         *               applies
         * @param elems  collection of nodes and data objects associated with the
         *               observation
         * @return a Problem if there is one, otherwise null
         */
        public Problem getProblem(final Config config, final int step, final ObservationElements elems) {
            final ReadMode rm = (ReadMode) SequenceRule.getInstrumentItem(config, InstGNIRS.READ_MODE_PROP);
            final Double expTime = SequenceRule.getExposureTime(config); // getInstrumentItem(config,InstGNIRS.EXPOSURE_TIME_PROP);
            if (expTime == null) return null;

            final Problem.Type type = getProblemType(rm, expTime);
            switch (type) {
                case ERROR:
                    return new Problem(type, PREFIX+"ExpTimeComparisonError_"+rm.name(),
                            "Exposure times for "
                                    + rm.name().toLowerCase().replace('_', ' ')
                                    + " mode must be >= " + limits.get(rm)[0]
                                    + " sec.",
                            SequenceRule.getInstrumentOrSequenceNode(step, elems, config));
                case WARNING:
                    return new Problem(type, PREFIX+"ExpTimeComparisonWarning_"+rm.name(),
                            "Exposure times for "
                                    + rm.name().toLowerCase().replace('_', ' ')
                                    + " mode are normally less than "
                                    + limits.get(rm)[1]
                                    + " sec; consider using a lower read noise mode instead.",
                            SequenceRule.getInstrumentOrSequenceNode(step, elems, config));
                case NONE:
                    return null;
            }
            return null;
        }
    }


    /**
     * This class wraps an ExpTimeComparison and uses it according to the selected ReadMode
     */
    private static class ReadModeRule implements IConfigRule {
        private final ReadMode mode;
        private final ExpTimeComparison comp;

        private ReadModeRule(ReadMode mode, ExpTimeComparison comp) {
            this.mode = mode;
            this.comp = comp;
        }

        @Override
        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            ReadMode rm = (ReadMode) SequenceRule.getInstrumentItem(config, InstGNIRS.READ_MODE_PROP);
            if (rm == mode) {
                return comp.getProblem(config, step, elems);
            }
            return null;
        }

        @Override
        public IConfigMatcher getMatcher() {
            // REL-233: this is only an issue for science observations.
            // REL-1608: We now want this to apply to all observation classes for min exposure time.
            return comp == ExpTimeComparison.tooShort ? IConfigMatcher.ALWAYS : SequenceRule.SCIENCE_MATCHER;
        }

    }


    /*
    ERROR If Acquisition Mirror == In && wavelength < 2.5 microns && filter == (L or M or PAH): "Acquisitions with
        the blue cameras must be done in the X, J, H, H2 or K band filters"

    ERROR If Acquisition Mirror == In && filter = XD blocker: "The XD blocking filter cannot be used for acquisitions"
        [I think this is correct]

    ERROR If Acquisition Mirror == In && wavelength > 2.5 microns && filter == (X or J or H): "Acquisitions with the
        red cameras must be done in the PAH, K or H2 filters"

    ERROR If Acquisition Mirror == In && wavelength > 2.5 microns && filter == (L or M): "Acquisitions with the red
        cameras must be done in the PAH, K or H2 filters. The broad L and M filters saturate on the sky background."

    REL-375: 05/11/2012
    CONTEXT: The GNIRS OT currently contains a phase 2 check for the red cameras being used for acquisitions with
    filters other than PAH, H2 and K. I'm afraid I can't find the original task which lays out the rules, but it
    must be something like "If wavelength > 2.5 um and acq mirror=in, filter != PAH, K or H2 --> error, acquisitions
    with the red cameras must be done with the PAH, K or H2 filters". We would like to add the H band filter to the
    list of permitted filters.

    REQUIREMENT: The GNIRS acquisition filter phase-2 check must be:
    If [wavelength > 2.5 um && acq mirror=in && filter != PAH, H, K or H2] then ERROR "Acquisitions with the red cameras
    must be done with the PAH, H, K or H2 filter
     */
    private static IConfigRule ACQUISITION_FILTER_RULE = new IConfigRule() {

        @Override
        public Problem check(final Config config, final int step, final ObservationElements elems, final Object state) {
            final AcquisitionMirror mirror = (AcquisitionMirror) SequenceRule.getInstrumentItem(config, InstGNIRS.ACQUISITION_MIRROR_PROP);
            if (mirror == AcquisitionMirror.IN) {
                final Wavelength l = (Wavelength) SequenceRule.getInstrumentItem(config, InstGNIRS.CENTRAL_WAVELENGTH_PROP);
                final Filter f = (Filter) SequenceRule.getInstrumentItem(config, InstGNIRS.FILTER_PROP);
                if (f == null) return null;
                if (f == Filter.X_DISPERSED) {
                    return new Problem(ERROR, PREFIX+"ACQUISITION_FILTER_RULE_1", "The XD blocking filter cannot be used for acquisitions",
                            SequenceRule.getInstrumentOrSequenceNode(step, elems, config));
                }
                final double wl;
                try {
                    wl = l.doubleValue();
                } catch (NumberFormatException ex) {
                    return new Problem(ERROR, PREFIX+"ACQUISITION_FILTER_RULE_2", "Central Wavelength must be defined",
                            SequenceRule.getInstrumentOrSequenceNode(step, elems, config));
                }
                if (wl < 2.5) {
                    switch (f) {
                        case ORDER_2://L
                        case ORDER_1://M
                        case PAH:
                        case Y:
                            return new Problem(ERROR, PREFIX+"ACQUISITION_FILTER_RULE_3",
                                    "Acquisitions with the blue cameras must be done in the X, J, H, H2 or K band filters",
                                    SequenceRule.getInstrumentOrSequenceNode(step, elems, config));
                    }
                }
                if (wl > 2.5) {
                    switch (f) {
                        case ORDER_2://L
                        case ORDER_1://M
                            return new Problem(ERROR, PREFIX+"ACQUISITION_FILTER_RULE_4",
                                    "Acquisitions with the red cameras must be done in the PAH, H, K or H2 filters. The broad L and M filters saturate on the sky background.",
                                    SequenceRule.getInstrumentOrSequenceNode(step, elems, config));
                        case ORDER_5://J
                        case ORDER_6://X
                        case Y:
                        case J:
                            return new Problem(ERROR, PREFIX+"ACQUISITION_FILTER_RULE_5",
                                    "Acquisitions with the red cameras must be done in the PAH, H, K or H2 filters.",
                                    SequenceRule.getInstrumentOrSequenceNode(step, elems, config));
                    }
                }
            }
            return null;
        }

        @Override
        public IConfigMatcher getMatcher() {
            return IConfigMatcher.ALWAYS;
        }
    };

    
    /**
     * Rules for GNIRS + Altair.
     * See REL-386.
     */
    private static IConfigRule ALTAIR_RULE = new AbstractConfigRule() {

        private static final String STREHLS = "Altair typically provides Strehls of 5% or less for wavelengths shorter than 1.3 microns";
        private static final String M_BAND = "The throughput and thermal emission from Altair prevent its use in the M-band";

        public Problem check(final Config config, final int step, final ObservationElements elems, final Object state) {
            final Filter filter = (Filter) SequenceRule.getInstrumentItem(config, InstGNIRS.FILTER_PROP);
            final Wavelength wavelength = (Wavelength) SequenceRule.getInstrumentItem(config,
                    InstGNIRS.CENTRAL_WAVELENGTH_PROP);
            final AcquisitionMirror acqMirror = (AcquisitionMirror) SequenceRule.getInstrumentItem(config,
                    InstGNIRS.ACQUISITION_MIRROR_PROP);
            final CrossDispersed xd = (CrossDispersed) SequenceRule.getInstrumentItem(config, InstGNIRS.CROSS_DISPERSED_PROP);

            // Altair use for GNIRS (X,J)
            // From JIRA:
            //In order to cover both GNIRS spectroscopy and imaging, we may need to consider both the filter and the
            // central wavelength.  Could we warn if:
            //Acquisition Mirror == "In" AND Filter == (X or J)
            //OR
            //Acquisition Mirror == "Out" AND XD == "No" AND Wavelength < 1.3
            //
            //I don't think we want to warn about X-dispersed since most of the data are at wavelengths > 1.3 microns.
            if ((acqMirror == AcquisitionMirror.IN
                    && (filter == Filter.J || filter == Filter.ORDER_5 || filter == Filter.ORDER_6))
                    || ((acqMirror == AcquisitionMirror.OUT && xd == CrossDispersed.NO &&
                    wavelength != null && wavelength.doubleValue() < 1.3))) {
                return new Problem(WARNING, PREFIX + "STREHLS", STREHLS, elems.getSeqComponentNode());
            }

            // Altair use for M-band observations
            if (wavelength != null && wavelength.doubleValue() >= 4.3) {
                return new Problem(ERROR, PREFIX + "M_BAND", M_BAND, SequenceRule.getInstrumentOrSequenceNode(step,elems));
            }

            return null;
        }

        public IConfigMatcher getMatcher() {
            return (config, step, elems) -> elems.hasAltair();
        }
    };

    /**
     * REL-1811: Warn if there are P-offsets for a slit spectroscopy observation.
     * Warn if FPU = *arcsec.
     */
    private static IConfigRule NO_P_OFFSETS_WITH_SLIT_SPECTROSCOPY_RULE = new NoPOffsetWithSlitRule(
        PREFIX,
        new AbstractFunction2<Config, ObservationElements, Boolean>() {
            public Boolean apply(final Config config, final ObservationElements elems){
                return
                    ((GNIRSParams.SlitWidth) SequenceRule.getInstrumentItem(config, InstGNIRS.SLIT_WIDTH_PROP)).
                    isSlitSpectroscopy();
            }
        }
    );

    /**
     * REL-2167: Warn if acq mirror is out and decker is acq.
     */
    private static IConfigRule DECKER_ACQ_MIRROR_OUT_RULE = new IConfigRule() {
        private final static String NAME = "M_DECKER_ACQ_MIRROR_OUT";
        private final static String M_DECKER_ACQ_MIRROR_OUT = "The Decker is set to acquisition but the Acquisition Mirror is out.";

        @Override
        public Problem check(final Config config, final int step, final ObservationElements elems, final Object state) {
            final GNIRSParams.Decker decker = ((GNIRSParams.Decker) SequenceRule.getInstrumentItem(config, InstGNIRS.DECKER_PROP));
            final GNIRSParams.AcquisitionMirror mirror =
                    ((GNIRSParams.AcquisitionMirror) SequenceRule.getInstrumentItem(config, InstGNIRS.ACQUISITION_MIRROR_PROP));
            if (mirror == AcquisitionMirror.OUT && decker == Decker.ACQUISITION)
                return new Problem(WARNING, PREFIX + NAME, M_DECKER_ACQ_MIRROR_OUT, elems.getSeqComponentNode());
            return null;
        }

        @Override
        public IConfigMatcher getMatcher() {
            return IConfigMatcher.ALWAYS;
        }
    };


    static {
        GNIRS_RULES.add(RED_CAMERA_XD_RULE);
        GNIRS_RULES.add(SHORT_RED_CAMERA_RULE);
        GNIRS_RULES.add(WAVELENGTH_XD_RULE);
        GNIRS_RULES.add(SHORT_BLUE_XD_RULE);

        for (final ReadMode rm : ReadMode.values()) {
            if (rm == ReadMode.VERY_FAINT) {
                GNIRS_RULES.add(new ReadModeRule(rm, ExpTimeComparison.tooShort));
            } else {
                for (final ExpTimeComparison comp : ExpTimeComparison.values()) {
                    GNIRS_RULES.add(new ReadModeRule(rm, comp));
                }
            }
        }

        GNIRS_RULES.add(ACQUISITION_FILTER_RULE);
        GNIRS_RULES.add(ALTAIR_RULE);
        GNIRS_RULES.add(NO_P_OFFSETS_WITH_SLIT_SPECTROSCOPY_RULE);
        GNIRS_RULES.add(DECKER_ACQ_MIRROR_OUT_RULE);
    }

    public IP2Problems check(final ObservationElements elements)  {
        final IP2Problems prob = new SequenceRule(GNIRS_RULES, null).check(elements);

        // Altair checks (See REL-386)
        prob.append(AltairRule.INSTANCE.check(elements));

        return prob;
    }
}
