//$Id: NiriRule.java 46768 2012-07-16 18:58:53Z rnorris $

package edu.gemini.p2checker.rules.niri;

import edu.gemini.p2checker.api.*;
import edu.gemini.p2checker.rules.altair.AltairRule;
import edu.gemini.p2checker.util.AbstractConfigRule;
import edu.gemini.p2checker.util.SequenceRule;
import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.skycalc.Offset;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ConfigSequence;
import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.gemini.altair.AltairAowfsGuider;
import edu.gemini.spModel.gemini.niri.InstNIRI;
import edu.gemini.spModel.gemini.niri.Niri;
import edu.gemini.spModel.gemini.niri.NiriOiwfsGuideProbe;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.obs.ObsClassService;
import edu.gemini.spModel.obsclass.ObsClass;
import edu.gemini.spModel.target.env.GuideProbeTargets;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe;
import edu.gemini.spModel.target.obsComp.TargetObsComp;

import java.beans.PropertyDescriptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


/**
 * NIRI Rule set
 */
public final class NiriRule implements IRule {
    private static final String PREFIX = "NiriRule_";
    private static Collection<IConfigRule> NIRI_RULES = new ArrayList<IConfigRule>();


    /**
     * NIRI Imaging ==> Disperser == "none"
     */
    private static IConfigMatcher IMAGING_MATCHER = new IConfigMatcher() {

        public boolean matches(Config config, int step, ObservationElements elems) {
            Niri.Disperser disp = (Niri.Disperser) SequenceRule.getInstrumentItem(config, InstNIRI.DISPERSER_PROP);
            return disp == Niri.Disperser.NONE;
        }
    };

    private static IConfigMatcher NIRI_SCIENCE_MATCHER = new IConfigMatcher() {

        public boolean matches(Config config, int step, ObservationElements elems) {
            ObsClass obsClass = SequenceRule.getObsClass(config);
            return obsClass == ObsClass.SCIENCE;
        }
    };

    /**
     * NIRI + Altair
     */
    private static IConfigMatcher ALTAIR_MATCHER = new IConfigMatcher() {

        public boolean matches(Config config, int step, ObservationElements elems) {
            return elems.hasAltair();
        }
    };

    /**
     * Rule for Sky Background and NIRI
     */
    private static IRule SKY_BG_RULE = new IRule() {
        private static final String SKY_BG_MESSAGE =
                "NIR observations are not affected by the moon. " +
                        "Only in the case of a very faint guide star should moon constraints " +
                        "be necessary.";

        public P2Problems check(ObservationElements elements)  {
            if (elements == null || elements.getSiteQualityNode().isEmpty()) return null; // can't check
            P2Problems problems = new P2Problems();
            for (SPSiteQuality sq : elements.getSiteQuality()) {
                SPSiteQuality.SkyBackground bg = sq.getSkyBackground();
                if (bg != SPSiteQuality.SkyBackground.ANY) {
                    problems.addWarning(PREFIX + "SKY_BG_RULE", SKY_BG_MESSAGE, elements.getSiteQualityNode().getValue());
                }
            }
            return problems;
        }
    };

    /**
     * Rule for water vapor and wavelength. Doesn't apply for Day Cals
     * <p/>
     * SCT-255: The NIRI water vapor check should only be applied to science and
     * nighttime calibrations; not to acquisitions (since the wavelength of the
     * science and acquisition may be different).
     * <p/>
     * NIRI observations which include any observations at > 3 microns (L or M)
     * should not trigger the warning about the water vapor constraint. Note
     * that there may be a NIRI sequence which iterates through several
     * filters, and if any of them are L or M, then it is okay to have water
     * vapor observing conditions < ANY.
     */
    private static IConfigRule WV_AND_WAVELENGTH_RULE = new AbstractConfigRule() {


        public Problem check(Config config, int step, ObservationElements elems, Object state) {

            NiriState niristate = (NiriState) state;
            ObsClass obsclass = SequenceRule.getObsClass(config);
            if (obsclass == null) return null;
            //skip everything with a priority lower than a partner nighttime cal
            if (obsclass.getPriority() > ObsClass.PARTNER_CAL.getPriority()) return null;

            for (SPSiteQuality sq : elems.getSiteQuality()) {

                if (sq.getWaterVapor() == SPSiteQuality.WaterVapor.ANY) return null; //if water vapor is any, no issues

                //so WV is not ANY. check the wavelength

                niristate.waterVaporRuleState.checkWavelength(config);

                //and check the filter, to see whether any of the L or M filters are in use
                niristate.waterVaporRuleState.checkFilters(config);

                // no problems
            }
            return null;
        }
    };

    /**
     * Check for dispersers on acquisition observations
     */
    private static IConfigRule ACQUISITION_WITH_DISPERSER_RULE = new AbstractConfigRule() {
        private static final String MESSAGE = "There is a disperser in the acquisition observation";

        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            //Apply only to Acquisition or Acquisition Calibrations  observations. Ignore all the others
            ObsClass obsClass = SequenceRule.getObsClass(config);
            if (obsClass != ObsClass.ACQ && obsClass != ObsClass.ACQ_CAL) return null;

            Niri.Disperser disperser = (Niri.Disperser) SequenceRule.getInstrumentItem(config, InstNIRI.DISPERSER_PROP);

            if (disperser != Niri.Disperser.NONE) {
                return new Problem(WARNING, PREFIX+"ACQUISITION_WITH_DISPERSER_RULE", MESSAGE,
                        SequenceRule.getInstrumentOrSequenceNode(step, elems));
            }
            return null;
        }
    };

    /**
     * Checks if Imaging masks are used with dispersers
     */
    private static IConfigRule IMAGING_AND_DISPERSER_RULE = new AbstractConfigRule() {
        private static final String MESSAGE = "There is a disperser in the beam with no slit.";

        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            Niri.Mask mask = (Niri.Mask) SequenceRule.getInstrumentItem(config, InstNIRI.MASK_PROP);
            if (mask == Niri.Mask.MASK_IMAGING) {
                Niri.Disperser disperser = (Niri.Disperser) SequenceRule.getInstrumentItem(config, InstNIRI.DISPERSER_PROP);
                if (disperser != Niri.Disperser.NONE) {
                    return new Problem(WARNING, PREFIX+"IMAGING_AND_DISPERSER_RULE", MESSAGE,
                            SequenceRule.getInstrumentOrSequenceNode(step, elems));
                }
            }
            return null;
        }
    };


    private static IConfigRule SCIENCE_WITH_SLIT_NO_DISPERSER_RULE = new IConfigRule() {

        private static final String MESSAGE = "There is a science observation with a slit but no disperser";

        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            Niri.Disperser disperser = (Niri.Disperser) SequenceRule.getInstrumentItem(config, InstNIRI.DISPERSER_PROP);
            if (disperser == Niri.Disperser.NONE) {
                Niri.Mask mask = (Niri.Mask) SequenceRule.getInstrumentItem(config, InstNIRI.MASK_PROP);
                if (mask != Niri.Mask.MASK_IMAGING) {
                    return new Problem(WARNING, PREFIX+"SCIENCE_WITH_SLIT_NO_DISPERSER_RULE", MESSAGE,
                            SequenceRule.getInstrumentOrSequenceNode(step, elems));
                }
            }
            return null;
        }

        public IConfigMatcher getMatcher() {
            return NIRI_SCIENCE_MATCHER;
        }
    };


    private static class EngineeringRule extends AbstractConfigRule {

        private PropertyDescriptor _deviceProperty;
        private Enum<?> _expectedValue;
        private String _message;
        private Problem.Type _problemType;

        public EngineeringRule(PropertyDescriptor deviceProp, Enum<?> expectedValue, String message, Problem.Type probType) {
            _deviceProperty = deviceProp;
            _expectedValue = expectedValue;
            _message = message;
            _problemType = probType;
        }

        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            Enum<?> device = (Enum<?>) SequenceRule.getInstrumentItem(config, _deviceProperty);
            if (device == _expectedValue) {
                return new Problem(_problemType, PREFIX+"EngineeringRule", _message,
                        SequenceRule.getInstrumentOrSequenceNode(step, elems));
            }
            return null;
        }
    }

    private static IConfigRule ENGINEERING_CAMERA_RULE = new EngineeringRule(
            InstNIRI.CAMERA_PROP,
            Niri.Camera.F32_PV,
            "The pupil viewer is for engineering purposes only",
            ERROR
    );

    private static IConfigRule ENGINEERING_MASK_RULE = new EngineeringRule(
            InstNIRI.MASK_PROP,
            Niri.Mask.PINHOLE_MASK,
            "The pinhole mask is for engineering purposes only",
            WARNING
    );

    /**
     * Implements rules:
     * ERROR if MASK == "Polarimetry 1-2.5um" || MASK == "Polarimetry 3-5um", \
     * "Polarimitery with NIRI is currently unavailable."
     * <p/>
     * ERROR if DISPERSER == "Wollaston", \
     * "Polarimitery with NIRI is currently unavailable."
     */
    private static IConfigRule POLARIMETRY_RULE = new AbstractConfigRule() {

        private static final String MESSAGE = "Polarimetry with NIRI is currently unavailable ";

        public Problem check(Config config, int step, ObservationElements elems, Object state) {

            Niri.Mask mask = (Niri.Mask) SequenceRule.getInstrumentItem(config, InstNIRI.MASK_PROP);
            boolean polarimetryInUse = false;
            if (mask == Niri.Mask.MASK_7 || mask == Niri.Mask.MASK_8) { //polarimetry masks
                polarimetryInUse = true;
            } else {
                Niri.Disperser disperser =
                        (Niri.Disperser) SequenceRule.getInstrumentItem(config, InstNIRI.DISPERSER_PROP);
                if (disperser == Niri.Disperser.WOLLASTON) {
                    polarimetryInUse = true;
                }
            }
            if (polarimetryInUse) {
                return new Problem(ERROR, PREFIX+"POLARIMETRY_RULE", MESSAGE,
                        SequenceRule.getInstrumentOrSequenceNode(step, elems));
            }
            return null;
        }
    };

    /**
     * Rules related to the type of observation and the exposure time.
     * WARN if CLASS == "Acquisition" && (EXPTIME * COADDS) > 60s,
     * "This will result in a very long acquisition which may exceed \
     * the time estimated by the OT; please double-check that you \
     * really need this long of an exposure time and this many coadds."
     * <p/>
     * WARN if CLASS == "Science" && (EXPTIME * COADDS) > 300s, \
     * "Very long exposures are not recommended in the NIR due to the \
     * variability of the sky background.  Try to keep the product \
     * of exposure time and number of coadds to less than 300 \
     * seconds."
     * <p/>
     * WARN if CLASS == "Science" && EXPTIME >= 60s && COADDS > 1, \
     * "For longer exposures there is little benefit to using \
     * multiple coadds, and it is usually better to take separate \
     * images."
     */
    private static IConfigRule ACQUISITION_EXPOSURE_RULE = new AbstractConfigRule() {
        private static final String MESSAGE_ACQ = "This will result in a very long acquisition which may exceed " +
                "the time estimated by the OT; please double-check that you really need this long of an " +
                "exposure time and this many coadds.";

        private static final String MESSAGE_SCIENCE_1 = "Very long exposures are not recommended in the NIR due to the " +
                "variability of the sky background. Try to keep the product of exposure time and number of coadds to " +
                "less than 300 seconds";
        private static final String MESSAGE_SCIENCE_2 = "For long exposures there is little benefit to using " +
                "multiple coadds, and its usually better to take separate images";

        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            //Apply only to Acquisition observations. Ignore all the others
            ObsClass obsClass = SequenceRule.getObsClass(config);
            Double expTime = SequenceRule.getExposureTime(config);

            if (expTime == null) return null;

            Integer coadds = SequenceRule.getCoadds(config);

            if (coadds == null) return null;

            if (obsClass == ObsClass.ACQ || obsClass == ObsClass.ACQ_CAL) {
                if (expTime * coadds > 60) {
                    return new Problem(WARNING, PREFIX+"ACQUISITION_EXPOSURE_RULE_MESSAGE_ACQ", MESSAGE_ACQ, SequenceRule.getInstrumentOrSequenceNode(step, elems, config));
                }
            } else if (obsClass == ObsClass.SCIENCE) {

                if (expTime * coadds > 300) {
                    return new Problem(WARNING, PREFIX+"ACQUISITION_EXPOSURE_RULE_MESSAGE_SCIENCE_1", MESSAGE_SCIENCE_1,
                            SequenceRule.getInstrumentOrSequenceNode(step, elems, config));
                } else if (expTime > 60 && coadds > 1) {
                    return new Problem(WARNING, PREFIX+"ACQUISITION_EXPOSURE_RULE_MESSAGE_SCIENCE_2", MESSAGE_SCIENCE_2,
                            SequenceRule.getInstrumentOrSequenceNode(step, elems, config));
                }
            }
            return null;

        }
    };


    private static IConfigRule WAVELENGTH_AND_WELL_DEPTH_RULE = new IConfigRule() {


        public Problem check(Config config, int step, ObservationElements elems, Object state) {

            Niri.WellDepth wellDepth = (Niri.WellDepth) SequenceRule.getInstrumentItem(config, InstNIRI.WELL_DEPTH_PROP);
            if (wellDepth != Niri.WellDepth.SHALLOW) {
                double wavelength = getWavelength(config);
                NiriState niriState = (NiriState) state;
                niriState.wavelengthState.recordShallowState(wavelength, step);
            }
            return null;
        }

        public IConfigMatcher getMatcher() {
            return NIRI_SCIENCE_MATCHER;
        }
    };


    private static IConfigRule WAVELENGTH_AND_READ_MODE_RULE = new IConfigRule() {


        public Problem check(Config config, int step, ObservationElements elems, Object state) {

            Niri.ReadMode readMode = (Niri.ReadMode) SequenceRule.getInstrumentItem(config, InstNIRI.READ_MODE_PROP);
            if (readMode == Niri.ReadMode.IMAG_SPEC_3TO5) {
                double wavelength = getWavelength(config);
                NiriState niriState = (NiriState) state;
                niriState.wavelengthState.recordHighBckgdState(wavelength, step);
            }
            return null;
        }

        public IConfigMatcher getMatcher() {
            return NIRI_SCIENCE_MATCHER;
        }
    };


    private static IConfigRule FOCUS_RULE = new AbstractConfigRule() {
        private static final String MESSAGE = "All observations should use the best focus";

        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            Niri.Focus focus = (Niri.Focus) SequenceRule.getInstrumentItem(config, InstNIRI.FOCUS_PROP);
            if (!Niri.FocusSuggestion.BEST_FOCUS.displayValue().equals(focus.getStringValue())) {
                return new Problem(ERROR, PREFIX+"FOCUS_RULE", MESSAGE, SequenceRule.getInstrumentOrSequenceNode(step, elems));
            }

            return null;
        }
    };


    /**
     * Rule:
     * ERROR if Beam-splitter != 'same as camera', \
     * "All observations should have the beam splitter the same as \
     * the camera."
     * <p/>
     * From SCT-255:
     * The check that the NIRI camera and beam splitter are the same only works
     * if the beam splitter is "same as camera". This rule should allow one to
     * select the same camera by name:
     * <p/>
     * Error if Camera = "f/6 (0.12 arcsec/pix) && \
     * (Beam Splitter != "f/6" && Beam Splitter != "same as camera"), \
     * All observations should have the beam splitter the same as the camera.
     * <p/>
     * Error if Camera = "f/14 (0.05 arcsec/pix) && \
     * (Beam Splitter != "f/14" && Beam Splitter != "same as camera"), \
     * All observations should have the beam splitter the same as the camera.
     * <p/>
     * Error if Camera = "f/32 (0.02 arcsec/pix) && \
     * (Beam Splitter != "f/32" && Beam Splitter != "same as camera"), \
     * All observations should have the beam splitter the same as the camera.
     */

    private static class BeamSplitterRule extends AbstractConfigRule {
        private static final String MESSAGE = "All observations should have the beam splitter the same as the camera";

        private static final Map<Niri.Camera, Niri.BeamSplitter> configs =
                new HashMap<Niri.Camera, Niri.BeamSplitter>();

        static {
            configs.put(Niri.Camera.F6, Niri.BeamSplitter.f6);
            configs.put(Niri.Camera.F14, Niri.BeamSplitter.f14);
            configs.put(Niri.Camera.F32, Niri.BeamSplitter.f32);
        }

        private static final BeamSplitterRule INSTANCE = new BeamSplitterRule();

        public Problem check(Config config, int step, ObservationElements elems, Object state) {

            Niri.BeamSplitter beamSplitter = (Niri.BeamSplitter)
                    SequenceRule.getInstrumentItem(config, InstNIRI.BEAM_SPLITTER_PROP);


            //no error if the beam splitter is unknown
            if (beamSplitter == null) return null;

            //Let's see if it matches with
            //the camera configuration.
            Niri.Camera camera = (Niri.Camera) SequenceRule.getInstrumentItem(config, InstNIRI.CAMERA_PROP);

            Niri.BeamSplitter configBeamSplitter = configs.get(camera);


            if (configBeamSplitter == null) return null; //no error if we don't know about this config.

            //The NIRI beamsplitter is stuck at the f/6 position so the OT should only allow this configuration.
            if ((beamSplitter != Niri.BeamSplitter.f6) &&
                    (beamSplitter != Niri.BeamSplitter.same_as_camera || configBeamSplitter != Niri.BeamSplitter.f6)) {
                return new Problem(ERROR, PREFIX+"BeamSplitterRule",
                        "The NIRI beamsplitter must be in the f/6 position", SequenceRule.getInstrumentOrSequenceNode(step, elems));
            }

            //Removed for 2012A: REL-103, REL-99
//            if (configBeamSplitter != beamSplitter && beamSplitter != Niri.BeamSplitter.same_as_camera) {
//                return new Problem(ERROR, MESSAGE, SequenceRule.getInstrumentOrSequenceNode(step, elems));
//            }
            return null;
        }
    }

    /**
     * Altair only works with the f/32 and f/14 cameras in NIRI (and not with the f/6 camera).
     */
    private static class ALTAIR_CAMERA_RULE extends AbstractConfigRule {
        private static final String MESSAGE = "Altair can only be used with NIRI's f/32 or f/14 cameras.";
        private static final ALTAIR_CAMERA_RULE INSTANCE = new ALTAIR_CAMERA_RULE();

        private boolean hasAO(TargetEnvironment env) {
            return hasPrimary(env, AltairAowfsGuider.instance);
        }

        private boolean hasPrimary(TargetEnvironment env, GuideProbe guider) {
            // TODO: GuideProbeTargets.isEnabled
            if (!env.isActive(guider)) return false;
            Option<GuideProbeTargets> gtOpt = env.getPrimaryGuideProbeTargets(guider);
            return (!gtOpt.isEmpty()) && !gtOpt.getValue().getPrimary().isEmpty();
        }

        @Override
        public Problem check(Config config, int step, ObservationElements elements, Object state) {
            boolean hasAOComp = elements.hasAltair();

             Niri.Camera camera = (Niri.Camera) SequenceRule.getInstrumentItem(config, InstNIRI.CAMERA_PROP);

            if (hasAOComp && (camera == Niri.Camera.F6)) {
                return new Problem(ERROR, PREFIX+"ALTAIR_CAMERA_RULE", MESSAGE,
                        SequenceRule.getInstrumentOrSequenceNode(step, elements));
            }
            return null;
        }
    }

    /**
     * Implement these rules
     * ERROR if OIWFS != "park", \
     * "The NIRI OIWFS is currently unavailable."
     * <p/>
     * WARN if PWFS2-1 == INDEF && AOWFS-1 == INDEF, \
     * "Please define a guide star using either PWFWS2 or AOWFS."
     * (Except for day cals)
     * <p/>
     * ERROR if Altair && AOWFS == INDEF, \
     * "If using Altair you must define an AOWFS target."
     * <p/>
     * ERROR if AOWFS && Altair == INDEF, \
     * "You have an AOWFS target defined, but do not have an Altair \
     * component in your observation."
     */
    private static IRule WFS_RULE = new IRule() {
        //private static final String MESSAGE = "Please define a guide star using either PWFS2 or AOWFS";
        private static final String OI_MESSAGE = "The NIRI OIWFS is currently unavailable";
        private static final String AO_NOALTAIR_MSG = "You have an AOWFS target defined, but do not " +
                "have an Altair component in your observation";

        //private static final String ALTAIR_NOAO_MESSAGE = "If using Altair you must define an AOWFS target";
        public P2Problems check(ObservationElements elements)  {
            for (TargetObsComp obsComp : elements.getTargetObsComp()) {

                TargetEnvironment env = obsComp.getTargetEnvironment();

                P2Problems problems = new P2Problems();
                boolean hasP2 = hasP2(env);

                boolean hasAOTarget = hasAO(env);
                boolean hasAOComp = elements.hasAltair();


                if (hasAOTarget && !hasAOComp) {
                    problems.addError(PREFIX + "AO_NOALTAIR_MSG", AO_NOALTAIR_MSG, elements.getTargetObsComponentNode().getValue());
                }
//            else if (!hasAOTarget && hasAOComp) {
//                problems.addError(ALTAIR_NOAO_MESSAGE, elements.getTargetObsComponentNode());
//            }

                if (hasOI(env)) {
                    problems.addError(PREFIX + "OI_MESSAGE", OI_MESSAGE, elements.getTargetObsComponentNode().getValue());
                }

                //Removed for SCI11-301: PWFS1 + LGS phase 2 checks
//            if (!(hasP2 || hasAOTarget)) {
//                if (ObsClassService.lookupObsClass(elements.getObservationNode()) != ObsClass.DAY_CAL) {
//                    problems.addWarning(MESSAGE, elements.getTargetObsComponentNode());
//                }
//            }
                return problems;
            }
            return null;
        }

        private boolean hasP2(TargetEnvironment env) {
            return hasPrimary(env, PwfsGuideProbe.pwfs2);
        }

        private boolean hasOI(TargetEnvironment env) {
            return hasPrimary(env, NiriOiwfsGuideProbe.instance);
        }

        private boolean hasAO(TargetEnvironment env) {
            return hasPrimary(env, AltairAowfsGuider.instance);
        }

        private boolean hasPrimary(TargetEnvironment env, GuideProbe guider) {
            // TODO: GuideProbeTargets.isEnabled
            if (!env.isActive(guider)) return false;
            Option<GuideProbeTargets> gtOpt = env.getPrimaryGuideProbeTargets(guider);
            return (!gtOpt.isEmpty()) && !gtOpt.getValue().getPrimary().isEmpty();
        }
    };


    /**
     * Implement these rules
     * ERROR if read_mode=="Low Background" && ARRAY==1024 && EXPTIME<8.762,\
     * "Exposure time is shorter than the minimum."
     * ERROR if read_mode=="Low Background" && ARRAY==768  && EXPTIME<4.980,\
     * "Exposure time is shorter than the minimum."
     * ERROR if read_mode=="Low Background" && ARRAY==512  && EXPTIME<2.276,\
     * "Exposure time is shorter than the minimum."
     * ERROR if read_mode=="Low Background" && ARRAY==256  && EXPTIME<0.654,\
     * "Exposure time is shorter than the minimum."
     * ERROR if read_mode=="Medium Background" && ARRAY==1024 && EXPTIME<0.548,\
     * "Exposure time is shorter than the minimum."
     * ERROR if read_mode=="Medium Background" && ARRAY==768  && EXPTIME<0.313,\
     * "Exposure time is shorter than the minimum."
     * ERROR if read_mode=="Medium Background" && ARRAY==512  && EXPTIME<0.144,\
     * "Exposure time is shorter than the minimum."
     * ERROR if read_mode=="Medium Background" && ARRAY==256  && EXPTIME<0.043,\
     * "Exposure time is shorter than the minimum."
     * ERROR if read_mode=="High Background" && ARRAY==1024 && EXPTIME<0.179,\
     * "Exposure time is shorter than the minimum."
     * ERROR if read_mode=="High Background" && ARRAY==768  && EXPTIME<0.106,\
     * "Exposure time is shorter than the minimum."
     * ERROR if read_mode=="High Background" && ARRAY==512  && EXPTIME<0.052,\
     * "Exposure time is shorter than the minimum."
     * ERROR if read_mode=="High Background" && ARRAY==256  && EXPTIME<0.020,\
     * "Exposure time is shorter than the minimum."
     */
    private static class ReadModeRule extends AbstractConfigRule {

        private static final ReadModeRule INSTANCE = new ReadModeRule();

        private static final String MESSAGE_TEMPLATE = "Exposure time is shorter than the minimum (%.3f sec)";

        private static final Map<Niri.BuiltinROI, Double> LOW_BACKGROUND_LIMITS =
                new HashMap<Niri.BuiltinROI, Double>();

        static {
            LOW_BACKGROUND_LIMITS.put(Niri.BuiltinROI.FULL_FRAME, 8.762);
            LOW_BACKGROUND_LIMITS.put(Niri.BuiltinROI.CENTRAL_768, 4.980);
            LOW_BACKGROUND_LIMITS.put(Niri.BuiltinROI.CENTRAL_512, 2.276);
            LOW_BACKGROUND_LIMITS.put(Niri.BuiltinROI.CENTRAL_256, 0.654);
        }

        private static final Map<Niri.BuiltinROI, Double> MEDIUM_BACKGROUND_LIMITS =
                new HashMap<Niri.BuiltinROI, Double>();

        static {
            MEDIUM_BACKGROUND_LIMITS.put(Niri.BuiltinROI.FULL_FRAME, 0.548);
            MEDIUM_BACKGROUND_LIMITS.put(Niri.BuiltinROI.CENTRAL_768, 0.313);
            MEDIUM_BACKGROUND_LIMITS.put(Niri.BuiltinROI.CENTRAL_512, 0.144);
            MEDIUM_BACKGROUND_LIMITS.put(Niri.BuiltinROI.CENTRAL_256, 0.043);
        }

        private static final Map<Niri.BuiltinROI, Double> HIGH_BACKGROUND_LIMITS =
                new HashMap<Niri.BuiltinROI, Double>();

        static {
            HIGH_BACKGROUND_LIMITS.put(Niri.BuiltinROI.FULL_FRAME, 0.179);
            HIGH_BACKGROUND_LIMITS.put(Niri.BuiltinROI.CENTRAL_768, 0.106);
            HIGH_BACKGROUND_LIMITS.put(Niri.BuiltinROI.CENTRAL_512, 0.052);
            HIGH_BACKGROUND_LIMITS.put(Niri.BuiltinROI.CENTRAL_256, 0.020);
        }

        private static final Map<Niri.ReadMode, Map<Niri.BuiltinROI, Double>> READ_MODE_TABLES =
                new HashMap<Niri.ReadMode, Map<Niri.BuiltinROI, Double>>();

        static {
            READ_MODE_TABLES.put(Niri.ReadMode.IMAG_SPEC_NB, LOW_BACKGROUND_LIMITS);
            READ_MODE_TABLES.put(Niri.ReadMode.IMAG_1TO25, MEDIUM_BACKGROUND_LIMITS);
            READ_MODE_TABLES.put(Niri.ReadMode.IMAG_SPEC_3TO5, HIGH_BACKGROUND_LIMITS);
        }

        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            Niri.BuiltinROI array = (Niri.BuiltinROI) SequenceRule.getInstrumentItem(config, InstNIRI.BUILTIN_ROI_PROP);
            if (array == null) return null; //can't check
            if (array == Niri.BuiltinROI.SPEC_1024_512) return null; // Spectroscopy 1024x512 doesn't generate problems

            Niri.ReadMode readMode = (Niri.ReadMode) SequenceRule.getInstrumentItem(config, InstNIRI.READ_MODE_PROP);
            if (readMode == null) return null; // can't check


            Double expTime = SequenceRule.getExposureTime(config);

            if (expTime == null) return null; //can't check

            Map<Niri.BuiltinROI, Double> map = READ_MODE_TABLES.get(readMode);

            Double limit = map.get(array);

            if (expTime < limit) {
                return new Problem(ERROR, PREFIX+"ReadModeRule", String.format(MESSAGE_TEMPLATE, limit),
                        SequenceRule.getInstrumentOrSequenceNode(step, elems, config));
            }
            return null;

        }
    }

    /**
     * Implements the rules:
     * WARN if CLASS=="Science" && Offsets==INDEF, \
     * "Most NIR observations should use dithering to help remove \
     * detector cosmetic defects."
     * <p/>
     * WARN if CLASS=="Science" && CAMERA==f/6 && Offsets<120, \
     * "Small on-source dithers may be inadequate to construct a \
     * median sky frame if the field is crowded or has extended \
     * objects."
     * <p/>
     * WARN if CLASS=="Science" && CAMERA==f/14 && Offsets<51, \
     * "Small on-source dithers may be inadequate to construct a \
     * median sky frame if the field is crowded or has extended \
     * objects."
     * <p/>
     * WARN if CLASS=="Science" && CAMERA==f/32 && Offsets<22, \
     * "Small on-source dithers may be inadequate to construct a \
     * median sky frame if the field is crowded or has extended \
     * objects."
     * <p/>
     * These rules only apply for NIRI imaging, i.e, when the
     * disperser == "none"
     */
    private static class CameraOffsetRule implements IConfigRule {

        private static CameraOffsetRule INSTANCE = new CameraOffsetRule();

        private CameraOffsetRule() {
        }


        private static final String NO_OFFSET_MESSAGE = "Most NIR observations should use dithering to help " +
                "remove detector cosmetic defects";
        /**
         * Maps a given Camera to the minimum offset that that camera supports
         */
        private static Map<Niri.Camera, Integer> CAMERA_OFFSET_MAP = new HashMap<Niri.Camera, Integer>();


        static {
            CAMERA_OFFSET_MAP.put(Niri.Camera.F6, 120);
            CAMERA_OFFSET_MAP.put(Niri.Camera.F14, 51);
            CAMERA_OFFSET_MAP.put(Niri.Camera.F32, 22);
        }

        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            //Apply only to Science observations. Ignore all the others
            ObsClass obsClass = SequenceRule.getObsClass(config);
            if (obsClass != ObsClass.SCIENCE) return null;

            Niri.Camera camera = (Niri.Camera) SequenceRule.getInstrumentItem(config, InstNIRI.CAMERA_PROP);

            if (camera == null) return null; //can't check without camera

            final scala.Option<Double> p = SequenceRule.getPOffset(config);
            final scala.Option<Double> q = SequenceRule.getQOffset(config);

            if (p.isEmpty() || q.isEmpty()) {
                //warn always in the sequence node
                //check whether there are more than one observe
                Integer repeatCount = SequenceRule.getStepCount(config);
                if (repeatCount == null) return null; //can't check
                if (repeatCount <= 1) return null; // doesn't apply to single exposure
                return new Problem(WARNING, PREFIX+"CameraOffsetRule", NO_OFFSET_MESSAGE, elems.getSeqComponentNode());
            } else {
                Integer minOffset = CAMERA_OFFSET_MAP.get(camera);
                if (minOffset == null) return null; // no entry in the map for this camera

                NiriState s = (NiriState) state;
                s.cameraOffsetState.recordState(p.get(), q.get(), minOffset);
            }
            return null;
        }

        public IConfigMatcher getMatcher() {
            return IMAGING_MATCHER;
        }
    }


    /**
     * Implements the rule:
     * WARN if DISPERSER != NONE && p-offsets != 0, \
     * "P-offsets in spectroscopy will move the target out of the \
     * slit.
     */
    private static IConfigRule SPECTROSCOPY_P_OFFSET_RULE = new AbstractConfigRule() {

        private static final String MESSAGE = "P-offsets in spectroscopy will move the target out of the slit";

        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            Niri.Disperser disperser = (Niri.Disperser) SequenceRule.getInstrumentItem(config, InstNIRI.DISPERSER_PROP);

            //If disperser != none and p-offsets != 0
            if (disperser != Niri.Disperser.NONE) {
                scala.Option<Double> p = SequenceRule.getPOffset(config);
                if (p.isDefined() && !Offset.isZero(p.get())) {
                    return new Problem(WARNING, PREFIX+"SPECTROSCOPY_P_OFFSET_RULE", MESSAGE, elems.getSeqComponentNode());
                }
            }

            return null;
        }
    };

    /**
     * Implements this set of rules:
     * WARN if DISPERSER == "none" && FILTER == "Order Sorting*", \
     * "The order sorting filters are meant for spectroscopy, \
     * and will result in a high background if used in imaging \
     * mode."
     * WARN if DISPERSER == "J-grism f/6" && FILTER != "J-ordersort", \
     * "The J-grism is normally used with the J-ordersort filter."
     * <p/>
     * WARN if DISPERSER == "H-grism f/6" && FILTER != "H-ordersort", \
     * "The H-grism is normally used with the H-ordersort filter."
     * <p/>
     * WARN if DISPERSER == "K-grism f/6" && FILTER != "K-ordersort", \
     * "The K-grism is normally used with the K-ordersort filter."
     * <p/>
     * WARN if DISPERSER == "L-grism f/6" && FILTER != "L-ordersort", \
     * "The L-grism is normally used with the L-ordersort filter."
     * <p/>
     * WARN if DISPERSER == "M-grism f/6" && FILTER != "M-ordersort", \
     * "The M-grism is normally used with the M-ordersort filter."
     * <p/>
     * WARN if DISPERSER == "J-grism f/32" && FILTER != "J-ordersort", \
     * "The J-grism is normally used with the J-ordersort filter."
     * <p/>
     * WARN if DISPERSER == "H-grism f/32" && FILTER != "H", \
     * "The f/32 H-grism is normally used with the broad-band H \
     * filter since the image quality seen when using  the \
     * H-ordersort filter at f/32 is poor.  Only use the H-ordersort \
     * filter if the extra wavelength coverage is required."
     * <p/>
     * WARN if DISPERSER == "K-grism f/32" && FILTER != "K-ordersort", \
     * "The K-grism is normally used with the K-ordersort filter."
     */
    private static class DisperserFilterRule extends AbstractConfigRule {

        private static DisperserFilterRule INSTANCE = new DisperserFilterRule();

        private DisperserFilterRule() {
        }

        private static final String ORDER_SORTING_MESSAGE = "The order sorting filters " +
                "are meant for spectroscopy, and will result in a high background if used " +
                "in imaging mode";

        /**
         * Maps a given Disperser to the FilterAndMessage class that contains the
         * filter that can NOT be used with that filter, and the warning message
         * to issue if that happens.
         */
        private static Map<Niri.Disperser, FilterAndMessage> DISPERSER_FILTER_TABLE =
                new HashMap<Niri.Disperser, FilterAndMessage>();

        private static class FilterAndMessage {
            private Niri.Filter _filter;
            private String _message;

            public FilterAndMessage(Niri.Filter filter, String message) {
                _filter = filter;
                _message = message;
            }

            public Niri.Filter getFilter() {
                return _filter;
            }

            public String getMessage() {
                return _message;
            }

        }

        static {
            DISPERSER_FILTER_TABLE.put(Niri.Disperser.J, new FilterAndMessage(Niri.Filter.BBF_J_ORDER_SORT,
                    "The J-grism is normally used with the J-ordersort filter"));
            DISPERSER_FILTER_TABLE.put(Niri.Disperser.H, new FilterAndMessage(Niri.Filter.BBF_H_ORDER_SORT,
                    "The H-grism is normally used with the H-ordersort filter"));
            DISPERSER_FILTER_TABLE.put(Niri.Disperser.K, new FilterAndMessage(Niri.Filter.BBF_K_ORDER_SORT,
                    "The K-grism is normally used with the K-ordersort filter"));
            DISPERSER_FILTER_TABLE.put(Niri.Disperser.L, new FilterAndMessage(Niri.Filter.BBF_L_ORDER_SORT,
                    "The L-grism is normally used with the L-ordersort filter"));
            DISPERSER_FILTER_TABLE.put(Niri.Disperser.M, new FilterAndMessage(Niri.Filter.BBF_M_ORDER_SORT,
                    "The M-grism is normally used with the M-ordersort filter"));
            DISPERSER_FILTER_TABLE.put(Niri.Disperser.J_F32, new FilterAndMessage(Niri.Filter.BBF_J_ORDER_SORT,
                    "The J-grism is normally used with the J-ordersort filter"));
            DISPERSER_FILTER_TABLE.put(Niri.Disperser.H_F32, new FilterAndMessage(Niri.Filter.BBF_H,
                    "The f/32 H-grism is normally used with the broad band H filter " +
                            "since the image quality seen when using the " +
                            "H-ordersort filter at f/32 is poor. Only use the " +
                            "H-ordersort filter if the extra wavelength coverage " +
                            "is required"));
        }


        public Problem check(Config config, int step, ObservationElements elems, Object state) {

            Niri.Disperser disperser = (Niri.Disperser) SequenceRule.getInstrumentItem(config, InstNIRI.DISPERSER_PROP);

            Niri.Filter filter = (Niri.Filter) SequenceRule.getInstrumentItem(config, InstNIRI.FILTER_PROP);

            if (filter == null) return null; //can't check without a filter.

            if (disperser == Niri.Disperser.NONE) {
                switch (filter) {
                    case BBF_H_ORDER_SORT:
                    case BBF_J_ORDER_SORT:
                    case BBF_K_ORDER_SORT:
                    case BBF_L_ORDER_SORT:
                    case BBF_M_ORDER_SORT:
                        return new Problem(WARNING, PREFIX+"DisperserFilterRule_ORDER_SORTING_MESSAGE", ORDER_SORTING_MESSAGE,
                                SequenceRule.getInstrumentOrSequenceNode(step, elems));
                }
            } else {

                FilterAndMessage fltMsg = DISPERSER_FILTER_TABLE.get(disperser);

                if (fltMsg == null) return null; //not an entry in the table

                if (fltMsg.getFilter() != filter) {
                    return new Problem(WARNING, PREFIX+"DisperserFilterRule_filterMessage", fltMsg.getMessage(),
                            SequenceRule.getInstrumentOrSequenceNode(step, elems));
                }
            }
            return null;
        }
    }

    /**
     * Implements the rule:
     * WARN if EXPTIME > 1s && ARRAY != Full Frame Readout, \
     * "Subarrays are typically only used for very short exposures."
     */
    private static IConfigRule EXPTIME_ARRAY_RULE = new AbstractConfigRule() {
        private static final String MESSAGE = "Subarrays are typically only used for very short exposures.";

        public Problem check(Config config, int step, ObservationElements elems, Object state) {

            Double expTime = SequenceRule.getExposureTime(config);

            if (expTime == null) return null; //can't check

            if (expTime > 1) {
                Niri.BuiltinROI array = (Niri.BuiltinROI) SequenceRule.getInstrumentItem(config, InstNIRI.BUILTIN_ROI_PROP);
                if (array != Niri.BuiltinROI.FULL_FRAME) {
                    return new Problem(WARNING, PREFIX+"EXPTIME_ARRAY_RULE",
                            MESSAGE, SequenceRule.getInstrumentOrSequenceNode(step, elems, config));
                }
            }

            return null;
        }
    };

    /**
     * State used to check the OFFSET_RULE. If a sequence contains 2 observes with
     * the conditions defined on the OFFSET_RULES at the same position, a
     * warning is generated suggesting to dither instead.
     */
    private static final class NiriOffsetState {

        Double lastP;
        Double lastQ;

        /**
         * Reset the state in case we need to start over
         */
        void clearState() {
            lastP = null;
            lastQ = null;
        }

        /**
         * Return true if the new p and q are the same as the one
         * stored in the state.
         */
        boolean checkPQ(Double p, Double q) {
            if (lastP != null && lastQ != null && Offset.areEqual(lastP, p) && Offset.areEqual(lastQ, q)) {
                return true;
            }
            lastP = p;
            lastQ = q;
            return false;
        }
    }

    /**
     * State used to check the WV_AND_WAVELENGTH_RULE. If a sequence uses the L or M filters
     * then the WV_AND_WAVELENGTH rule should not trigger warnings.
     */
    private static final class WaterVaporRuleState {

        private static final String WV_MESSAGE = "NIR observations are rarely affected by water vapor. " +
                "Only in the case of spectroscopy of features between the JHK bands should " +
                "stricter water vapor constraints be necessary";


        boolean LMused = false;
        boolean waterVaporWarning = false;

        boolean foundProblem() {
            return !LMused && waterVaporWarning;
        }

        void checkWavelength(Config config) {
            if (waterVaporWarning) return;
            double wavelength = getWavelength(config);
            if (wavelength > 0 && wavelength < 3.0) {
                waterVaporWarning = true;
            }
        }

        void checkFilters(Config config) {
            if (LMused) return;
            Niri.Filter filter = (Niri.Filter) SequenceRule.getInstrumentItem(config, InstNIRI.FILTER_PROP);
            if (filter == null) {
                LMused = false;
                return;
            }
            switch (filter) {
                case BBF_LPRIME:
                case BBF_MPRIME:
                case BBF_L_ORDER_SORT:
                case BBF_M_ORDER_SORT:
                    LMused = true;
                    break;
                default:
                    break;
            }
        }

        void appendProblem(IP2Problems problems, ObservationElements elems) {
            if (foundProblem()) {
                for(ISPObsComponent sqn:elems.getSiteQualityNode()){
                    problems.append(new Problem(WARNING, PREFIX+"WaterVaporRuleState", WV_MESSAGE, sqn));
                }
            }
        }

    }


    private static enum State {
        UNDEFINED,
        PROBLEM,
        NOPROBLEM
    }

    /**
     * State to be used in CameraOffsetRule
     * <p/>
     * OT-680
     * The "small on-source dithers" warning should go away when there are sky
     * dithers, so we need to check that ALL the offsets are < FOV, rather than
     * give the warning if ANY of the offsets are < FOV:
     * <p/>
     * WARN if CLASS=="Science" && CAMERA==f/6 && ALL Offsets<120, \
     * "Small on-source dithers may be inadequate to construct a \
     * median sky frame if the field is crowded or has extended \
     * objects."
     * <p/>
     * WARN if CLASS=="Science" && CAMERA==f/14 && ALL Offsets<51, \
     * "Small on-source dithers may be inadequate to construct a \
     * median sky frame if the field is crowded or has extended \
     * objects."
     * <p/>
     * WARN if CLASS=="Science" && CAMERA==f/32 && ALL Offsets<22, \
     * "Small on-source dithers may be inadequate to construct a \
     * median sky frame if the field is crowded or has extended \
     * objects."
     */
    private static final class CameraOffsetState {

        private State problemState = State.UNDEFINED;
        private double _minOffset = 0;
        private static final String MESSAGE = "On-source dithers smaller than the FOV (%.0f arcsecs) may be " +
                "inadequate to construct a median sky frame if the field is crowded or has extended objects";

        public void recordState(double p, double q, double min) {
            if (problemState == State.NOPROBLEM) return; // no need to continue, this sequence is cleared off.
            p = Math.abs(p);
            q = Math.abs(q);
            if ((p + q > 0) && p < min && q < min) {
                if (problemState == State.UNDEFINED) {
                    //return new Problem(WARNING, MESSAGE, elems.getSeqComponentNode());
                    problemState = State.PROBLEM;
                    _minOffset = min;
                }
            } else if ((p + q) > 0) {
                problemState = State.NOPROBLEM;
            }
        }


        public void appendProblem(IP2Problems problems, ObservationElements elems) {
            if (problemState == State.PROBLEM) {
                problems.append(new Problem(WARNING, PREFIX+"CameraOffsetState", String.format(MESSAGE, _minOffset), elems.getSeqComponentNode()));
            }
        }
    }

    /**
     * State to be used on rules:
     * WAVELENGTH_AND_WELL_DEPTH_RULE and WAVELENGTH_AND_READ_MODE_RULE
     * <p/>
     * Form OT-680:
     * NIRI observations which include ANY observations at > 3 microns (L or M,
     * including inside a NIRI sequence iterator) should not trigger the
     * warning "High-background read-mode is typically only used for
     * observations in the thermal infrared..." or the warning "We recommend
     * shallow-well mode when saturation is not an issue..."
     */
    private static final class WavelengthState {
        private static final String SHALLOW_MESSAGE = "We recommend shallow-well mode when saturation is not an " +
                "issue. Using deep-well mode will significantly increase the number of hot pixels";

        private static final String HBCKG_MESSAGE = "High-background read-mode is typically only used for " +
                "observations in the thermal infrared due to the significantly higher read noise";


        private State highBckgdReadModeState = State.UNDEFINED;
        private int stepHighBckgd = 0;
        private State shallowWellModeState = State.UNDEFINED;
        private int stepShallowWeelMode = 0;


        public void recordShallowState(double wavelength, int step) {

            if (shallowWellModeState == State.NOPROBLEM) return; //we already know there is no problem

            if (wavelength < 3) {
                if (shallowWellModeState == State.UNDEFINED) {
                    shallowWellModeState = State.PROBLEM;
                    stepShallowWeelMode = step;
                }
            } else {
                shallowWellModeState = State.NOPROBLEM;
            }
        }


        public void recordHighBckgdState(Double wavelength, int step) {

            if (highBckgdReadModeState == State.NOPROBLEM) return; //we already know there is no problem

            if (wavelength < 3) {
                if (highBckgdReadModeState == State.UNDEFINED) {
                    highBckgdReadModeState = State.PROBLEM;
                    stepHighBckgd = step;
                }
            } else {
                highBckgdReadModeState = State.NOPROBLEM;
            }
        }


        public void appendProblem(IP2Problems problems, ObservationElements elems) {

            if (highBckgdReadModeState == State.PROBLEM) {
                problems.addWarning(PREFIX+"HBCKG_MESSAGE", HBCKG_MESSAGE, SequenceRule.getInstrumentOrSequenceNode(stepHighBckgd, elems));
            }

            if (shallowWellModeState == State.PROBLEM) {
                problems.addWarning(PREFIX+"SHALLOW_MESSAGE", SHALLOW_MESSAGE, SequenceRule.getInstrumentOrSequenceNode(stepShallowWeelMode, elems));
            }


        }

    }


    private static final class NiriState {
        CameraOffsetState cameraOffsetState = new CameraOffsetState();
        NiriOffsetState offsetState = new NiriOffsetState();
        WaterVaporRuleState waterVaporRuleState = new WaterVaporRuleState();
        WavelengthState wavelengthState = new WavelengthState();
    }


    /**
     * Check the rule   WARN if CLASS == "Science" && EXPTIME >=30s && observe>1
     * <p/>
     * observe > 1 could be generated because multiple observes or because
     * the useage of an offset iterator at the same position more than once
     */
    private static IConfigRule OFFSET_RULE = new IConfigRule() {
        private static final String MESSAGE = "It is usually a good idea to dither rather than take multiple " +
                "exposures at the same position";
        private static final double MAX_EXP_TIME = 30.0;

        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            NiriState niristate = (NiriState) state;

            Double expTime = SequenceRule.getExposureTime(config);
            if (expTime == null || expTime < MAX_EXP_TIME) {
                niristate.offsetState.clearState();
                return null; //can't check
            }

            //check for observe > 1
            Integer repeatCount = SequenceRule.getStepCount(config);
            if (repeatCount != null && repeatCount > 1) {
                return new Problem(WARNING, PREFIX+"OFFSET_RULE", MESSAGE, elems.getSeqComponentNode());
            }
            //check the offset. If they are the same as the previous known state,
            //issue a problem
            final scala.Option<Double> pOpt = SequenceRule.getPOffset(config);
            final scala.Option<Double> qOpt = SequenceRule.getQOffset(config);
            final double p = pOpt.isDefined() ? pOpt.get() : 0.0;
            final double q = qOpt.isDefined() ? qOpt.get() : 0.0;
            if (niristate.offsetState.checkPQ(p, q)) {
                return new Problem(WARNING, PREFIX+"OFFSET_RULE", MESSAGE, elems.getSeqComponentNode());
            }
            return null;
        }

        public IConfigMatcher getMatcher() {
            return NIRI_SCIENCE_MATCHER;
        }
    };

    /**
     * From SCT-255:
     * This is to check that the sequence does not try to iterate through
     * different slits, which will not work without a reacquisition. In theory
     * they could take spectra with a slit then change to imaging mode
     * (mask="none"), but if it is hard to do this check then we can just force
     * the users to create a separate imaging sequence.
     * <p/>
     * ERROR if CLASS == "Science" && 'multiple slit masks defined in a NIRI
     * sequence', "The slit cannot be changed without a re-acquisition.
     */
    private static IRule SLIT_CHANGES_RULE = new IRule() {
        private static final String MESSAGE = "The slit cannot be changed without a re-acquisition";
        private final ItemKey MASK_KEY = new ItemKey("instrument:mask");

        public IP2Problems check(ObservationElements elements)  {

            //check wheter this is a science obs. Otherwise, won't apply this rule
            ObsClass obsClass = ObsClassService.lookupObsClass(elements.getObservationNode());
            if (obsClass != ObsClass.SCIENCE) return null;

            ConfigSequence confSeq = elements.getSequence();

            if (confSeq == null) return null;

            Object[] o = confSeq.getItemValueAtEachStep(MASK_KEY);
            if (o.length > 0) {
                Niri.Mask currentMask = (Niri.Mask) o[0];

                for (int i = 1; i < o.length; i++) {

                    Niri.Mask newMask = (Niri.Mask) o[i];

                    if (!isValidTranslation(currentMask, newMask)) {
                        P2Problems probs = new P2Problems();
                        probs.addError(PREFIX+"SLIT_CHANGES_RULE", MESSAGE, elements.getSeqComponentNode());
                        return probs;
                    }
                    currentMask = newMask;
                }
            }
            return null;
        }

        /**
         * The only valid change in the mask is using one slit and the change to
         * imaging mode.
         */
        private boolean isValidTranslation(Niri.Mask currentMask, Niri.Mask newMask) {
            return currentMask == newMask || currentMask != Niri.Mask.MASK_IMAGING && newMask == Niri.Mask.MASK_IMAGING;
        }
    };


    /**
     * Rules for NIRI + Altair.
     * See REL-386.
     */
    private static IConfigRule ALTAIR_RULE = new AbstractConfigRule() {

        private static final String NIRI_L_PRIME = "NIRI L' observations with Altair must use exposure times < 0.12 sec";
        private static final String NIRI_L_PRIME2 = "NIRI L' observations with Altair can not use the f/14 or f/6 cameras";
        private static final String STREHLS = "Altair typically provides Strehls of 5% or less for wavelengths shorter than 1.3 microns";
        private static final String M_BAND = "The throughput and thermal emission from Altair prevent its use in the M-band";

        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            Niri.Filter filter = (Niri.Filter) SequenceRule.getInstrumentItem(config, InstNIRI.FILTER_PROP);
            Niri.Camera camera = (Niri.Camera)SequenceRule.getInstrumentItem(config, InstNIRI.CAMERA_PROP);
            boolean isF32 = camera == Niri.Camera.F32 || camera == Niri.Camera.F32_PV;
            Niri.Disperser disperser = (Niri.Disperser) SequenceRule.getInstrumentItem(config, InstNIRI.DISPERSER_PROP);
            Double expTime = SequenceRule.getExposureTime(config);
            if (expTime == null) {
                expTime = SequenceRule.getInstrumentExposureTime(config);
            }
            if (filter == Niri.Filter.BBF_LPRIME && isF32 && expTime != null && expTime >= 0.12) {
                return new Problem(WARNING, PREFIX + "NIRI_L_PRIME", NIRI_L_PRIME, SequenceRule.getInstrumentOrSequenceNode(step,elems));
            } else if (filter == Niri.Filter.BBF_LPRIME && !isF32) {
                return new Problem(WARNING, PREFIX + "NIRI_L_PRIME2", NIRI_L_PRIME2, SequenceRule.getInstrumentOrSequenceNode(step,elems));
            } else if (filter != null && filter.getWavelength() < 1.3) { // See comments in REL-386
                return new Problem(WARNING, PREFIX + "STREHLS", STREHLS, SequenceRule.getInstrumentOrSequenceNode(step,elems));
            } else if (filter == Niri.Filter.BBF_MPRIME || filter == Niri.Filter.BBF_M_ORDER_SORT || disperser == Niri.Disperser.M) {
                return new Problem(ERROR, PREFIX + "M_BAND", M_BAND, SequenceRule.getInstrumentOrSequenceNode(step,elems));
            }
            return null;
        }

        public IConfigMatcher getMatcher() {
            return ALTAIR_MATCHER;
        }
    };


    /**
     * Register all the NIRI rules to apply
     */
    static {
//        NIRI_RULES.add(SequenceRule.DUMP_CONFIG_RULE);
        NIRI_RULES.add(WV_AND_WAVELENGTH_RULE);
        NIRI_RULES.add(ACQUISITION_WITH_DISPERSER_RULE);
        NIRI_RULES.add(IMAGING_AND_DISPERSER_RULE);
        NIRI_RULES.add(SCIENCE_WITH_SLIT_NO_DISPERSER_RULE);
        NIRI_RULES.add(ENGINEERING_CAMERA_RULE);
        NIRI_RULES.add(ENGINEERING_MASK_RULE);
        NIRI_RULES.add(POLARIMETRY_RULE);
        NIRI_RULES.add(ACQUISITION_EXPOSURE_RULE);
        NIRI_RULES.add(WAVELENGTH_AND_WELL_DEPTH_RULE);
        NIRI_RULES.add(WAVELENGTH_AND_READ_MODE_RULE);
        NIRI_RULES.add(FOCUS_RULE);
        NIRI_RULES.add(BeamSplitterRule.INSTANCE);
        NIRI_RULES.add(ALTAIR_CAMERA_RULE.INSTANCE);
        NIRI_RULES.add(ReadModeRule.INSTANCE);
        NIRI_RULES.add(CameraOffsetRule.INSTANCE);
        NIRI_RULES.add(SPECTROSCOPY_P_OFFSET_RULE);
        NIRI_RULES.add(DisperserFilterRule.INSTANCE);
        NIRI_RULES.add(EXPTIME_ARRAY_RULE);
        NIRI_RULES.add(OFFSET_RULE);
        NIRI_RULES.add(ALTAIR_RULE);
    }

    public IP2Problems check(ObservationElements elems)  {

        NiriState state = new NiriState();

        IP2Problems seqProblems = (new SequenceRule(NIRI_RULES, state)).check(elems);

        // Check the sky background.
        seqProblems.append(SKY_BG_RULE.check(elems));
        // Check the WFS
        seqProblems.append(WFS_RULE.check(elems));

        //append the possible problems found regarding the Water Vapor
        state.waterVaporRuleState.appendProblem(seqProblems, elems);

        //append the possible problems found regarding wavelenght
        state.wavelengthState.appendProblem(seqProblems, elems);

        //append the possible problems found on the offset iterator associated to the camera in use
        state.cameraOffsetState.appendProblem(seqProblems, elems);

        //Check for invalid changes in the slit
        seqProblems.append(SLIT_CHANGES_RULE.check(elems));

        // Altair checks (See REL-386)
        seqProblems.append(AltairRule.INSTANCE.check(elems));

        return seqProblems;
    }


    private static double getWavelength(Config config) {
        Niri.Disperser disperser = (Niri.Disperser) SequenceRule.getInstrumentItem(config, InstNIRI.DISPERSER_PROP);

        if (disperser == null) return -1; //can't get wvlength

        double wavelength = -1.0;
        if (disperser == Niri.Disperser.NONE) {
            Niri.Filter filter = (Niri.Filter) SequenceRule.getInstrumentItem(config, InstNIRI.FILTER_PROP);
            if (filter != null) {
                wavelength = filter.getWavelength();
            }
        } else {
            //get the wavelength from the Disperser
            wavelength = disperser.getCentralWavelength();
        }

        return wavelength;

    }
}
