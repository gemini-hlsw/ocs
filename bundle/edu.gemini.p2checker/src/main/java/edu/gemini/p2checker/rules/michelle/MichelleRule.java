//$Id$

package edu.gemini.p2checker.rules.michelle;

import edu.gemini.p2checker.api.*;
import edu.gemini.p2checker.rules.altair.AltairRule;
import edu.gemini.p2checker.util.AbstractConfigRule;
import edu.gemini.p2checker.util.SequenceRule;
import edu.gemini.pot.sp.ISPProgramNode;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.gemini.michelle.InstMichelle;
import edu.gemini.spModel.gemini.michelle.MichelleParams;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.guide.GuideProbeUtil;
import edu.gemini.spModel.obs.ObsClassService;
import edu.gemini.spModel.obsclass.ObsClass;
import edu.gemini.spModel.target.env.GuideGroup;
import edu.gemini.spModel.target.env.GuideProbeTargets;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe;
import edu.gemini.spModel.target.obsComp.TargetObsComp;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


/**
 * Michelle Rule Set
 */
public class MichelleRule implements IRule {
    private static final String PREFIX = "MichelleRule_";
    private static Collection<IConfigRule> MICHELLE_RULES = new ArrayList<IConfigRule>();

    /**
     * Rules involving the WFS options available for Michelle
     */
    private static IRule WFS_RULE = new IRule() {

        private static final String NO_P2_MESSAGE = "MICHELLE normally uses PWFS2";
        private static final String OI_PRESENT_MESSAGE = "MICHELLE has no OIWFS";

        public IP2Problems check(ObservationElements elements)  {
            P2Problems prob = new P2Problems();
            for (TargetObsComp obsCommp : elements.getTargetObsComp()) {

                TargetEnvironment env = obsCommp.getTargetEnvironment();

                Option<GuideProbeTargets> gtOpt = env.getPrimaryGuideProbeTargets(PwfsGuideProbe.pwfs2);

                // TODO: GuideProbeTargets.isEnabled
                boolean activeP2 = elements.getObsContext().exists(c -> GuideProbeUtil.instance.isAvailable(c, PwfsGuideProbe.pwfs2));
                boolean hasP2 = !gtOpt.isEmpty() && activeP2 && (gtOpt.getValue().getOptions().size() > 0);

                if (!hasP2) {
                    prob.addWarning(PREFIX + "NO_P2_MESSAGE", NO_P2_MESSAGE, elements.getTargetObsComponentNode().getValue());
                }
                if (hasOI(env)) {
                    prob.addError(PREFIX + "OI_PRESENT_MESSAGE", OI_PRESENT_MESSAGE, elements.getTargetObsComponentNode().getValue());
                }
            }
            return prob;
        }

        private boolean hasOI(TargetEnvironment env) {
            GuideGroup grp = env.getOrCreatePrimaryGuideGroup();

            ImList<GuideProbeTargets> col = grp.getAllMatching(GuideProbe.Type.OIWFS);
            if ((col == null) || (col.size() == 0)) return false;

            for (GuideProbeTargets gt : col) {
                if (gt.getOptions().size() > 0) return true;
            }
            return false;
        }
    };

    /**
     * Rules for the Chop throw value
     */
    private static IConfigRule CHOP_THROW_RULE = new AbstractConfigRule() {
        private static final String MESSAGE_OVER = "MICHELLE chop throw limited to <= 15 arc-seconds";
        private static final String MESSAGE_UNDER = "MICHELLE chop/nod throw must be > 5.0 arc-seconds";

        public Problem check(Config config, int step, ObservationElements elems, Object state) {

            Double chopThrow =
                    (Double) SequenceRule.getInstrumentItem(config, InstMichelle.CHOP_THROW_PROP);

            MichelleParams.Disperser disperser =
                    (MichelleParams.Disperser) SequenceRule.getInstrumentItem(config, InstMichelle.DISPERSER_PROP);

            if ((disperser.getMode() == MichelleParams.DisperserMode.CHOP) && (chopThrow > 15.0)) {
                return new Problem(ERROR, PREFIX+"CHOP_THROW_RULE_MESSAGE_OVER", MESSAGE_OVER,
                        SequenceRule.getInstrumentOrSequenceNode(step, elems));
            } else if (chopThrow < 5.0) {
                return new Problem(ERROR, PREFIX+"CHOP_THROW_RULE_MESSAGE_UNDER", MESSAGE_UNDER,
                        SequenceRule.getInstrumentOrSequenceNode(step, elems));
            }
            return null;
        }
    };

    /**
     * WARN if nod_orientation == "Orthogonal to chop", "Normal usage is to nod in the chop direction"
     * Nod orientation is available on-site only, and it's not iterable. So, we need to
     * check only the static component
     */
    private static IRule NOD_ORIENTATION_RULE = new IRule() {
        private static final String MESSAGE = "Normal usage is to nod in the chop direction";

        public IP2Problems check(ObservationElements elements)  {
            InstMichelle inst = (InstMichelle)elements.getInstrument();
            if (inst == null) return null; // can't check
            if (inst.getNodOrientation() == MichelleParams.NodOrientation.ORTHOGONAL) {
                IP2Problems problems = new P2Problems();
                problems.addWarning(PREFIX+"NOD_ORIENTATION_RULE", MESSAGE, elements.getInstrumentNode());
                return problems;
            }
            return null;
        }
    };

    /**
     * (2) Imaging and spectroscopy mode are distinguished by the disperser.  For imaging one has
     * <param name="disperser" value="MIRROR"/>
     * while anything else implies spectroscopy.
     */
   private static IConfigMatcher IMAGING_MATCHER = new IConfigMatcher() {
        public boolean matches(Config config, int step, ObservationElements elems) {
            MichelleParams.Disperser disperser =
                    (MichelleParams.Disperser) SequenceRule.getInstrumentItem(config, InstMichelle.DISPERSER_PROP);
            return disperser != null && disperser == MichelleParams.Disperser.MIRROR;
        }
    };

    private static IConfigMatcher SPECTROSCOPY_MATCHER = new IConfigMatcher() {
        public boolean matches(Config config, int step, ObservationElements elems) {
            MichelleParams.Disperser disperser =
                    (MichelleParams.Disperser) SequenceRule.getInstrumentItem(config, InstMichelle.DISPERSER_PROP);
            return disperser != null && disperser != MichelleParams.Disperser.MIRROR;
        }
    };

    private static IConfigMatcher ACQUISITION_MATCHER = new IConfigMatcher() {
        public boolean matches(Config config, int step, ObservationElements elems) {
            ObsClass obsClass = SequenceRule.getObsClass(config);
            return obsClass == ObsClass.ACQ || obsClass == ObsClass.ACQ_CAL;
        }
    };


    private static class ObsModeRule implements IConfigRule {
        IConfigMatcher _validator;
        IChecker _delegate;
        Problem.Type _type;

        /**
         * An IChecker performs a check on a given step (similar as
         * the IConfigRule, but returns true if the given configuration
         * has a problem. The ObsModeRule will issue an Problem.Type with the message gotten from
         * getMessage()
         */
        interface IChecker {
            boolean check(Config config, int steps, ObservationElements elems);

            String getMessage();
        }

        public ObsModeRule(IChecker rule, IConfigMatcher validator) {
            this(rule, validator, ERROR);
        }

        public ObsModeRule(IChecker rule, IConfigMatcher validator, Problem.Type type) {
            _validator = validator;
            _delegate = rule;
            _type = type;
        }


        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            if (_delegate.check(config, step, elems)) {
                return new Problem(_type, PREFIX+"ObsModeRule", _delegate.getMessage(),
                        SequenceRule.getInstrumentOrSequenceNode(step, elems));
            }
            return null;
        }


        public IConfigMatcher getMatcher() {
            return _validator;
        }
    }

    /**
     * ERROR if focal_plane_mask != "Imaging" for Imaging Science
     */
    private static ObsModeRule FOCAL_PLANE_MASK_IMAGING_RULE = new ObsModeRule(
            new ObsModeRule.IChecker() {
                private static final String MESSAGE = "Cannot do MICHELLE imaging with a slit in place";

                public boolean check(Config config, int step, ObservationElements elems) {
                    MichelleParams.Mask mask =
                            (MichelleParams.Mask) SequenceRule.getInstrumentItem(config, InstMichelle.MASK_PROP);
                    return mask != MichelleParams.Mask.MASK_IMAGING;
                }

                public String getMessage() {
                    return MESSAGE;
                }
            },
            IMAGING_MATCHER
    );

    /**
     * ERROR if disperser != "Mirror" for Imaging Science
     * This will never match since that's the definition for an IMAGING_MATCHER
     */
//    private static ObsModeRule DISPERSER_IMAGING_RULE = new ObsModeRule(
//            new ObsModeRule.IChecker() {
//                private static final String MESSAGE = "Cannot do MICHELLE imaging with a grating in place";
//
//                public boolean check(Config config, int step, ObservationElements elems) {
//                    MichelleParams.Disperser disperser =
//                            (MichelleParams.Disperser) SequenceRule.getInstrumentItem(config, InstMichelle.DISPERSER_PROP);
//                    return disperser != MichelleParams.Disperser.MIRROR;
//                }
//
//                public String getMessage() {
//                    return MESSAGE;
//                }
//            },
//            IMAGING_MATCHER
//    );

    /**
     * ERROR if filter == "None" for MICHELLE imaging
     */
    private static ObsModeRule FILTER_IMAGING_RULE = new ObsModeRule(
            new ObsModeRule.IChecker() {
                private static final String MESSAGE = "A filter must be used for MICHELLE imaging";

                public boolean check(Config config, int step, ObservationElements elems) {
                    MichelleParams.Filter filter =
                            (MichelleParams.Filter) SequenceRule.getInstrumentItem(config, InstMichelle.FILTER_PROP);
                    return filter == MichelleParams.Filter.NONE;
                }

                public String getMessage() {
                    return MESSAGE;
                }
            },
            IMAGING_MATCHER
    );

    /**
     * slit position angle must be between 0 and 180 degrees
     */
    private static ObsModeRule POS_ANGLE_SPECTROSCOPY_RULE = new ObsModeRule(
            new ObsModeRule.IChecker() {
                private static final String MESSAGE = "MICHELLE slit orientation has to be from 0 to 180 degrees";

                public boolean check(Config config, int step, ObservationElements elems) {
                    Double posAngle =
                            (Double) SequenceRule.getInstrumentItem(config, InstMichelle.POS_ANGLE_PROP);
                    return posAngle != null && (posAngle < 0 || posAngle > 180);
                }

                public String getMessage() {
                    return MESSAGE;
                }
            },
            SPECTROSCOPY_MATCHER
    );


    /**
     * Checker class for the DISPERSER_WAVELENGTH_SPECTROSCOPY_RULE rule.
     * Checks for
     * ERROR if disperser == "Low Res 10 Grating (lowN)" && wavelength != 10.5
     * ERROR if disperser == "Low Res 20 Grating (lowQ)" && wavelength != 20.5
     *
     * SCT-255: For lowres20 spectroscopy with Michelle, the normal central wavelength
     * should be 19.8 microns... we would like to have 20.5 changed to 19.8
     *
     * SCT-379: Default Michelle lowN central wavelength needs to be changed to
     * 9.5 (from 10.5)
     *
     */
    private static class DisperserImagingChecker implements ObsModeRule.IChecker {
        private static final String MESSAGE_N = "MICHELLE lowN spectroscopy central wavelength must be 9.5 microns";
        private static final String MESSAGE_Q = "MICHELLE lowQ spectroscopy central wavelength must be 19.8 microns";

        private String _message = MESSAGE_N;

        private static DisperserImagingChecker _instance = new DisperserImagingChecker();

        public static DisperserImagingChecker getInstance() {
            return _instance;
        }

        public String getMessage() {
            return _message;
        }

        public boolean check(Config config, int steps, ObservationElements elems) {
            MichelleParams.Disperser disperser =
                    (MichelleParams.Disperser) SequenceRule.getInstrumentItem(config, InstMichelle.DISPERSER_PROP);
            if (disperser == null) return false; //can't check

            switch (disperser) {
                case LOW_RES_10:
                    _message = MESSAGE_N;
                    break;
                case LOW_RES_20:
                    _message = MESSAGE_Q;
                    break;
                default:
                    return false; //not a problem, disperser is set to something else
            }

            Double wavelength =
                    (Double) SequenceRule.getInstrumentItem(config, InstMichelle.DISPERSER_LAMBDA_PROP);
            if (wavelength == null) return false;

            Double lamda = disperser.getLamda();
            if (lamda == null) return false;

            return (lamda.compareTo(wavelength) != 0);
        }
    }

    /**
     * Checks for
     * ERROR if disperser == "Low Res 10 Grating (lowN)" && wavelength != 10.5
     * ERROR if disperser == "Low Res 20 Grating (lowQ)" && wavelength != 20.5
     * Delegates all the job to the DisperserImagingChecker class
     */
    private static ObsModeRule DISPERSER_WAVELENGTH_SPECTROSCOPY_RULE = new ObsModeRule(
            DisperserImagingChecker.getInstance(),
            SPECTROSCOPY_MATCHER
    );


    /**
     * make sure a slit is in
     * ERROR if focal_plane_mask == "Imaging"
     */
    private static ObsModeRule FOCAL_PLANE_SPECTROSCOPY_RULE = new ObsModeRule(
            new ObsModeRule.IChecker() {
                private static final String MESSAGE = "A slit is needed for spectroscopy";

                public boolean check(Config config, int step, ObservationElements elems) {
                    MichelleParams.Mask mask =
                            (MichelleParams.Mask) SequenceRule.getInstrumentItem(config, InstMichelle.MASK_PROP);
                    return mask == MichelleParams.Mask.MASK_IMAGING;
                }

                public String getMessage() {
                    return MESSAGE;
                }
            },
            SPECTROSCOPY_MATCHER
    );

    /**
     * check higher resolution modes, filter is chosen automaticly
     * if disperser == "Low Res 10 Grating (lowN)" ||
     * disperser == "Low Res 20 Grating (lowQ)" ||
     * disperser == "Med Res Grating (medN1)" {
     * WARN if filter != None, "Filters are not normally specified for MICHELLE lowN/medN1 spectroscopy"
     */
    private static ObsModeRule AUTOMATIC_FILTER_FOR_DISPERSER_SPECTROSCOPY_RULE = new ObsModeRule(
            new ObsModeRule.IChecker() {
                private static final String MESSAGE = "Filters are not normally specified for MICHELLE lowN/medN1/medN2 spectroscopy";

                public boolean check(Config config, int step, ObservationElements elems) {

                    MichelleParams.Filter filter =
                            (MichelleParams.Filter) SequenceRule.getInstrumentItem(config, InstMichelle.FILTER_PROP);
                    if (filter == MichelleParams.Filter.NONE) return false; //no error if the filter is set to NONE

                    MichelleParams.Disperser disperser =
                            (MichelleParams.Disperser) SequenceRule.getInstrumentItem(config, InstMichelle.DISPERSER_PROP);
                    if (disperser == null) return false; //can't check

                    switch (disperser) {
                        case LOW_RES_10:
                        case LOW_RES_20:
                        case MED_RES:
                        case HIGH_RES:
                            return true;   //these disperser with some filter issue a warning
                        default:
                            return false;
                    }
                }

                public String getMessage() {
                    return MESSAGE;
                }
            },
            SPECTROSCOPY_MATCHER,
            WARNING
    );

    /**
     * make sure the right filter is chosen for the higher resolution modes
     * if disperser == "Echelle 15 km/s" || disperser == "Med Res Grating (medN2)" {
     * ERROR if filter == "None", "A filter must be chosen for MICHELLE medN2/echelle spectroscopy"
     * ERROR if filter == "Si-1 7.7um"  && wavelength < 7.35, "Wrong filter for MICHELLE medN2/echelle spectroscopy"
     * ERROR if filter == "Si-1 7.7um"  && wavelength > 8.0, "Wrong filter for MICHELLE medN2/echelle spectroscopy"
     * ERROR if filter == "Si-2 8.7um"  && wavelength < 8.0, "Wrong filter for MICHELLE medN2/echelle spectroscopy"
     * ERROR if filter == "Si-2 8.7um"  && wavelength > 9.2, "Wrong filter for MICHELLE medN2/echelle spectroscopy"
     * ERROR if filter == "Si-3 9.7um"  && wavelength < 9.2, "Wrong filter for MICHELLE medN2/echelle spectroscopy"
     * ERROR if filter == "Si-3 9.7um"  && wavelength > 10.1, "Wrong filter for MICHELLE medN2/echelle spectroscopy"
     * ERROR if filter == "Si-4 10.4um" && wavelength < 10.1, "Wrong filter for MICHELLE medN2/echelle spectroscopy"
     * ERROR if filter == "Si-4 10.4um" && wavelength > 10.85, "Wrong filter for MICHELLE medN2/echelle spectroscopy"
     * ERROR if filter == "Si-5 11.7um" && wavelength < 11.2, "Wrong filter for MICHELLE medN2/echelle spectroscopy"
     * ERROR if filter == "Si-5 11.7um" && wavelength > 12.15, "Wrong filter for MICHELLE medN2/echelle spectroscopy"
     * ERROR if filter == "Si-6 12.3um" && wavelength < 12.15, "Wrong filter for MICHELLE medN2/echelle spectroscopy"
     * ERROR if filter == "Si-6 12.3um" && wavelength > 13.1, "Wrong filter for MICHELLE medN2/echelle spectroscopy"
     * ERROR if filter == "N' 11.2um"   && wavelength < 10.85, "Wrong filter for MICHELLE medN2/echelle spectroscopy"
     * ERROR if filter == "N' 11.2um"   && wavelength > 11.30, "Wrong filter for MICHELLE medN2/echelle spectroscopy"
     * ERROR if filter == "Qa 18.5um"   && wavelength < 17.30, "Wrong filter for MICHELLE medN2/echelle spectroscopy"
     * ERROR if filter == "Qa 18.5um"   && wavelength > 18.50, "Wrong filter for MICHELLE medN2/echelle spectroscopy"
     * ERROR if filter == "Q' 19.8um"   && wavelength < 17.30, "Wrong filter for MICHELLE medN2/echelle spectroscopy"
     * ERROR if filter == "Q' 19.8um"   && wavelength > 22.50, "Wrong filter for MICHELLE medN2/echelle spectroscopy"
     * }
     */
    private static class DisperserFilterWavelengthChecker implements ObsModeRule.IChecker {
        private static final String MESSAGE = "Wrong filter for MICHELLE echelle spectroscopy";
        private static final String NO_FILTER_MESSAGE = "A filter must be chosen for MICHELLE echelle spectroscopy";

        private static class Limits {
            public Limits(Double min, Double max) {
                this.min = min;
                this.max = max;
            }

            Double min;
            Double max;
        }

        private static Map<MichelleParams.Filter, Limits> WAVELENGTH_LIMITS_TABLE =
                new HashMap<MichelleParams.Filter, Limits>();

        static {
            WAVELENGTH_LIMITS_TABLE.put(MichelleParams.Filter.SI_1, new Limits(7.35, 8.0));
            WAVELENGTH_LIMITS_TABLE.put(MichelleParams.Filter.SI_2, new Limits(8.0, 9.2));
            WAVELENGTH_LIMITS_TABLE.put(MichelleParams.Filter.SI_3, new Limits(9.2, 10.1));
            WAVELENGTH_LIMITS_TABLE.put(MichelleParams.Filter.SI_4, new Limits(10.1, 10.85));
            WAVELENGTH_LIMITS_TABLE.put(MichelleParams.Filter.SI_5, new Limits(11.2, 12.15));
            WAVELENGTH_LIMITS_TABLE.put(MichelleParams.Filter.SI_6, new Limits(12.15, 13.1));
            WAVELENGTH_LIMITS_TABLE.put(MichelleParams.Filter.N_PRIME, new Limits(10.85, 11.3));
            WAVELENGTH_LIMITS_TABLE.put(MichelleParams.Filter.QA, new Limits(17.3, 18.5));
            WAVELENGTH_LIMITS_TABLE.put(MichelleParams.Filter.Q19, new Limits(17.3, 22.5));
        }

        private static DisperserFilterWavelengthChecker _instance = new DisperserFilterWavelengthChecker();

        public static DisperserFilterWavelengthChecker getInstance() {
            return _instance;
        }

        public String _message = MESSAGE;

        public String getMessage() {
            return _message;
        }

        public boolean check(Config config, int steps, ObservationElements elems) {
            MichelleParams.Disperser disperser =
                    (MichelleParams.Disperser) SequenceRule.getInstrumentItem(config, InstMichelle.DISPERSER_PROP);
            if (disperser == null) return false; //can't check
            //problem happens  if disperser == "Echelle 15 km/s" || disperser == "Med Res Grating (medN2)"

            if (disperser != MichelleParams.Disperser.ECHELLE) {
                return false; // only applies to ECHELLE
            }
//            switch (disperser) {
//                case ECHELLE:
//                case HIGH_RES:
//                    break;
//                default:
//                    return false; //all the other disperser don't generate problems
//            }

            MichelleParams.Filter filter =
                    (MichelleParams.Filter) SequenceRule.getInstrumentItem(config, InstMichelle.FILTER_PROP);

            if (filter == null) return false;

            if (filter == MichelleParams.Filter.NONE) {
                _message = NO_FILTER_MESSAGE;
                return true;
            }

            _message = MESSAGE;

            Limits limits = WAVELENGTH_LIMITS_TABLE.get(filter);

            Double wavelength =
                    (Double) SequenceRule.getInstrumentItem(config, InstMichelle.DISPERSER_LAMBDA_PROP);

            return limits != null && wavelength != null &&
                    (wavelength < limits.min || wavelength > limits.max);

        }
    }

    /**
     * Make sure the right filter is chose for hte higher resolution modes.
     * The checker is handling all the validation, see #DisperserFilterWavelengthChecker
     */
    private static ObsModeRule DISPERSER_FILTER_WAVELENGTH_SPECTROSCOPY_RULE = new ObsModeRule(
            DisperserFilterWavelengthChecker.getInstance(),
            SPECTROSCOPY_MATCHER
    );


    /**
     * Warning for 1 pixel slit (may not work...)
     */
    private static ObsModeRule ONE_PIXEL_SLIT_SPECTROSCOPY_RULE = new ObsModeRule(
            new ObsModeRule.IChecker() {
                private static final String MESSAGE = "The MICHELLE 1-pixel slit is not normally used";

                public boolean check(Config config, int step, ObservationElements elems) {
                    MichelleParams.Mask mask =
                            (MichelleParams.Mask) SequenceRule.getInstrumentItem(config, InstMichelle.MASK_PROP);
                    return mask == MichelleParams.Mask.MASK_1;
                }

                public String getMessage() {
                    return MESSAGE;
                }
            },
            SPECTROSCOPY_MATCHER,
            WARNING
    );

    /**
     * Flag exposure times that are too short
     * on-source times of < 30 seconds go to acquisition mode
     * ERROR if exposure < 1.0, "MICHELLE exposure time is too short"
     * ERROR if exposure == 1.0 && !acquisition, "MICHELLE exposure time is too short for normal observations"
     * ERROR if exposure < 30.0 && science, "MICHELLE exposure time is too short for normal observations"
     * allow for faint source acq, where an exposure time of 30 seconds should be used
     * WARN  if exposure != 1.0 && acquisition, "Unusual MICHELLE acquisition exposure time: is the target faint?"
     * Long observations should probably be split, hence the following warning
     * WARN  if exposure > 1000.0, "Long MICHELLE observations are best split into several steps"
     *
     * SCT-255:
     *
     * Change to rule: WARN if exposure != 1.0 && acquisition, "Unusual Michelle acquisition exposure time: is the target faint?";
     * Should be WARN if exposure >= 30.0 && acquisition, "Unusual Michelle acquisition on-source time: is the target faint?"
     * The following just clarify the error messages so are less critical:
     * 1) ERROR if exposure == 1.0 && !acquisition,
     * "Michelle exposure time is too short for normal observations";
     * Error message should read "On-source times < 30 sec produce single nods and should only be used for acquisitions"
     * 2) ERROR if exposure <30.0 && science, "Michelle exposure time is too short for normal observations";
     * Error message should read "On-source times < 30 sec produce single nods and should only be used for acquisitions"
     * 3) ERROR if exposure < 1.0, "Michelle exposure time is too short";
     * Error message should read "Michelle on-source time is too short; must be >= 1 sec"
     * Note that I've changed "exposure time" to "on-source time" for consistency with OT notation
     * (seems trivial but the Michelle iterator does contain an "exposure time" item which doesn't
     * actually do anything and has caught people out in the past).
     *
     * SCT-255: After discussion, rules 1) and 2) were merged into one:
     * WRROR if exp < 30 sec && !acquisition,
     * "On-source times < 30 sec produce single nods and should only be used for acquisitions"
     *
     * SCT-263: Replace rule :
     *     WARN  if exposure > 1000.0, "Long MICHELLE observations are best split into several steps"
     * by these 2 rules:
     * WARN if disperser == lowN, lowQ or Mirror && time on-source >> 600 sec
     * "It is recommended that the time on-source not exceed 600 sec for this observing mode, so that a file is written to disk reasonably often"
     * WARN if disperser == medN1, medN2 or echelle && time on-source >> 1800 sec
     * "It is recommended that the time on-source not exceed 1800 sec for this observing mode so that a file is written to disk reasonably often"
     *
     */
    private static IConfigRule EXPTIME_RULE = new AbstractConfigRule() {
        private static final String EXP_LESS_ONE = "Michelle on-source time is too short; must be >= 1 sec";
        private static final String EXP_TOO_SHORT = "On-source times < 30 sec produce single nods and should only be used for acquisitions";
        private static final String EXP_UNUSUAL = "Unusual Michelle acquisition on-source time: is the target faint?";
        private static final String EXP_TOO_LONG = "It is recommended that the time on-source not exceed %d sec for " +
                "this observation mode, so that a file is written to disk reasonably often";
        public Problem check(Config config, int step, ObservationElements elems, Object state) {

            Double expTime = (Double)SequenceRule.getInstrumentItem(config, InstMichelle.TOTAL_ON_SOURCE_TIME_PROP);
            if (expTime == null) return null; //can't check

            ISPProgramNode node = SequenceRule.getInstrumentOrSequenceNode(step, elems);

            if (expTime < 1) return new Problem(ERROR, PREFIX+"EXPTIME_RULE_EXP_LESS_ONE", EXP_LESS_ONE, node);

            ObsClass obsClass = SequenceRule.getObsClass(config);

            if (expTime < 30.0 && obsClass != ObsClass.ACQ && obsClass != ObsClass.ACQ_CAL) {
                return new Problem(ERROR, PREFIX+"EXPTIME_RULE_EXP_TOO_SHORT", EXP_TOO_SHORT, node);
            }

            if (expTime >= 30.0 && (obsClass == ObsClass.ACQ || obsClass == ObsClass.ACQ_CAL)) {
                return new Problem(WARNING, PREFIX+"EXPTIME_RULE_EXP_UNUSUAL", EXP_UNUSUAL, node);
            }

            MichelleParams.Disperser disperser =
                    (MichelleParams.Disperser) SequenceRule.getInstrumentItem(config, InstMichelle.DISPERSER_PROP);

            if (disperser == null) return null; //can't check

            switch (disperser) {

                case ECHELLE:
                case HIGH_RES:
                case MED_RES:
                    if (expTime > 1800) {
                        return new Problem(WARNING, PREFIX+"EXPTIME_RULE_EXP_TOO_LONG", String.format(EXP_TOO_LONG, 1800), node);
                    }
                    break;

                case LOW_RES_10:
                case LOW_RES_20:
                case MIRROR:
                    if (expTime > 600) {
                        return new Problem(WARNING, PREFIX+"EXPTIME_RULE_EXP_TOO_LONG", String.format(EXP_TOO_LONG, 600), node);
                    }
                    break;
            }
            return null;  //no problem
        }
    };

    /**
     * New rule from SCT-255
     * ERROR if filter !Si-5 && acquisition, "All Michelle acquisitions must be caried out using the Si-5 filter".
     * Michelle will automatically use Si-5 anyway but the PI should be aware of this
     */

    private static IConfigRule SI_5_ACQ_RULE = new AbstractConfigRule() {
        private static final String MESSAGE = "All Michelle acquisitions must be carried out using the Si-5 filter";
        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            ObsClass obsClass = SequenceRule.getObsClass(config);
            if (obsClass != ObsClass.ACQ && obsClass != ObsClass.ACQ_CAL) return null;
            MichelleParams.Filter filter =
                    (MichelleParams.Filter) SequenceRule.getInstrumentItem(config, InstMichelle.FILTER_PROP);

            if (filter != MichelleParams.Filter.SI_5) {
                return new Problem(ERROR, PREFIX+"SI_5_ACQ_RULE", MESSAGE, SequenceRule.getInstrumentOrSequenceNode(step, elems));
            }
            return null;
        }
    };

    /**
     * ERROR if disperser !mirror && acquisition
     *  ==> "Disperser must be set to 'mirror' for acquisition observations"
     *
     * ERROR if focal plane mask !imaging && acquisition
     *
     * ==> "Focal plane mask must be set to 'imaging' for acquisition observations"
     */

    private static IConfigRule DISPERSER_ACQ_RULE  = new AbstractConfigRule() {
        private static final String MESSAGE_DISPERSER = "Disperser must be set to 'mirror' for acquisition observations";

        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            MichelleParams.Disperser disperser =
                               (MichelleParams.Disperser) SequenceRule.getInstrumentItem(config, InstMichelle.DISPERSER_PROP);
            if (disperser != MichelleParams.Disperser.MIRROR) {
                return new Problem(ERROR, PREFIX+"DISPERSER_ACQ_RULE", MESSAGE_DISPERSER,
                        SequenceRule.getInstrumentOrSequenceNode(step, elems));
            }
            return null;
        }

        public IConfigMatcher getMatcher() {
            return ACQUISITION_MATCHER;
        }

    };
    /**
    * ERROR if focal plane mask !imaging && acquisition
    *
    * ==> "Focal plane mask must be set to 'imaging' for acquisition observations"
    */

   private static IConfigRule FPU_ACQ_RULE  = new AbstractConfigRule() {
       private static final String MESSAGE_FPU = "Focal plane mask must be set to 'imaging' for acquisition observations";

       public Problem check(Config config, int step, ObservationElements elems, Object state) {
           MichelleParams.Mask mask =
                              (MichelleParams.Mask) SequenceRule.getInstrumentItem(config, InstMichelle.MASK_PROP);
           if (mask != MichelleParams.Mask.MASK_IMAGING) {
               return new Problem(ERROR, PREFIX+"FPU_ACQ_RULE", MESSAGE_FPU,
                       SequenceRule.getInstrumentOrSequenceNode(step, elems));
           }
           return null;
       }

       public IConfigMatcher getMatcher() {
           return ACQUISITION_MATCHER;
       }

   };


    private static IRule CC_RULE = new IRule() {
        private static final String MESSAGE = "Most Michelle modes will not produce useful data in CC=80/ANY conditions";
        public IP2Problems check(ObservationElements elements)  {
            if (elements == null || elements.getSiteQualityNode().isEmpty()) return null; // can't check

            //check wheter this is a science obs. Otherwise, won't apply phase 1 rules
            ObsClass obsClass = ObsClassService.lookupObsClass(elements.getObservationNode());
            if (obsClass != ObsClass.SCIENCE) return null;

            P2Problems problems = new P2Problems();
            for (SPSiteQuality sq : elements.getSiteQuality()) {
                SPSiteQuality.CloudCover cc = sq.getCloudCover();
                if ((cc != null) && (cc.getPercentage() >= 80)) {
                    problems.addWarning(PREFIX + "CC_RULE", MESSAGE, elements.getSiteQualityNode().getValue());
                }
            }
            return problems;
        }
    };

    /**
     * Rule for Sky Background and Michelle
     */
    private static IRule SKY_BG_RULE = new IRule() {
        private static final String SKY_BG_MESSAGE =
                "MID-IR observations are not affected by the moon. " +
                "Only in the case of a very faint guide star might sky background " +
                "constraint be necessary";


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


    static {
//        MICHELLE_RULES.add(SequenceRule.DUMP_CONFIG_RULE);
        MICHELLE_RULES.add(CHOP_THROW_RULE);
        MICHELLE_RULES.add(FOCAL_PLANE_MASK_IMAGING_RULE);
        MICHELLE_RULES.add(FILTER_IMAGING_RULE);
        MICHELLE_RULES.add(POS_ANGLE_SPECTROSCOPY_RULE);
        MICHELLE_RULES.add(DISPERSER_WAVELENGTH_SPECTROSCOPY_RULE);
        MICHELLE_RULES.add(FOCAL_PLANE_SPECTROSCOPY_RULE);
        MICHELLE_RULES.add(AUTOMATIC_FILTER_FOR_DISPERSER_SPECTROSCOPY_RULE);
        MICHELLE_RULES.add(DISPERSER_FILTER_WAVELENGTH_SPECTROSCOPY_RULE);
        MICHELLE_RULES.add(ONE_PIXEL_SLIT_SPECTROSCOPY_RULE);
        MICHELLE_RULES.add(EXPTIME_RULE);
        MICHELLE_RULES.add(SI_5_ACQ_RULE);
        MICHELLE_RULES.add(DISPERSER_ACQ_RULE);
        MICHELLE_RULES.add(FPU_ACQ_RULE);
    }


    public IP2Problems check(ObservationElements elems)  {
        IP2Problems probs = new SequenceRule(MICHELLE_RULES, null).check(elems);
        probs.append(WFS_RULE.check(elems));
        probs.append(NOD_ORIENTATION_RULE.check(elems));
        probs.append(CC_RULE.check(elems));
        probs.append(SKY_BG_RULE.check(elems));

        // Altair checks (See REL-386)
        probs.append(AltairRule.INSTANCE.check(elems));

        return probs;

    }
}
