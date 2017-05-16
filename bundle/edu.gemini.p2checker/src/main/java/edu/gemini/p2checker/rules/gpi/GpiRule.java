package edu.gemini.p2checker.rules.gpi;

import edu.gemini.p2checker.api.*;
import edu.gemini.p2checker.util.SequenceRule;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ConfigSequence;
import edu.gemini.spModel.core.Magnitude;
import edu.gemini.spModel.core.MagnitudeBand;
import edu.gemini.spModel.gemini.gpi.Gpi;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.TargetEnvironment;

import java.util.*;

/**
 * Gpi rules.
 */
public class GpiRule implements IRule {
    private static final String PREFIX = "GpiRule_";
    private static final Collection<IConfigRule> GPI_RULES = new ArrayList<>();

    private static final IConfigMatcher ANY_MATCHER = (config, step, elems) -> true;

    // OT-106: Filter iteration only allowed in Observation Modes direct and NRM
    private static IConfigRule FILTER_ITER_RULE = new IConfigRule() {

        private static final String MESSAGE = "Filter iteration only allowed in Observation Modes direct and NRM.";

        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            Object[] values = elems.getSequence().getDistinctItemValues(Gpi.Filter.KEY);
            if (values.length > 1) {
                // iterated!
                Gpi inst = (Gpi) elems.getInstrument();
                if (inst != null) {
                    if (inst.getObservingMode().isEmpty() || !inst.getObservingMode().getValue().isFilterIterable()) {
                        return new Problem(ERROR, PREFIX + "FILTER_ITER_RULE",
                                MESSAGE, SequenceRule.getInstrumentOrSequenceNode(step, elems, config));
                    }
                }
            }

            return null;
        }

        public IConfigMatcher getMatcher() {
            return ANY_MATCHER;
        }
    };

    private static IConfigRule HALF_WAVE_PLATE_ANGLE_RULE = new IConfigRule() {

        private static final String MESSAGE = "The half wave plane angle parameter can only be iterated if "
        + "the disperser is set to Wollaston .";

        // OT-107: P2 check for half wave plate angle iterator
        // The half wave plane angle parameter can only be iterated if the disperser is set to Wollaston
        private Double getHalfWavePlateAngle(Config config) {
            return (Double) SequenceRule.getItem(config, Double.class, Gpi.HALFWAVE_PLATE_ANGLE_KEY);
        }

        private Gpi.Disperser getDisperser(Config config) {
            return (Gpi.Disperser) SequenceRule.getItem(config, Gpi.Disperser.class, Gpi.Disperser.KEY);
        }

        public Problem check(Config config, int step, ObservationElements elems, Object state) {
            Gpi inst = (Gpi) elems.getInstrument();
            Gpi.Disperser disperser = getDisperser(config);
            if (disperser != Gpi.Disperser.WOLLASTON) {
                Double a = getHalfWavePlateAngle(config);
                if (a != null && a != inst.getHalfWavePlateAngle()) {
                    return new Problem(ERROR, PREFIX+"HALF_WAVE_PLATE_ANGLE_RULE",
                            MESSAGE, SequenceRule.getInstrumentOrSequenceNode(step, elems, config));
                }
            }
            return null;
        }

        public IConfigMatcher getMatcher() {
            return ANY_MATCHER;
        }
    };

    private static IRule STATIC_EXPOSURE_TIME_RULE = elements -> {
        if (elements.getInstrument() instanceof Gpi) {
            Gpi inst = (Gpi)elements.getInstrument();
            double expTime = inst.getExposureTime();
            int coadds = inst.getCoadds();
            int maxVal = inst.getMaximumExposureTimeSecs();
            double minVal = inst.getMinimumExposureTimeSecs();
            P2Problems problems = new P2Problems();
            String txt = null;
            String id = null;
            if (expTime > maxVal) {
                txt = "Exposure times of less than " + maxVal + " seconds give optimum performance.";
                id = "STATIC_EXPOSURE_TIME_RULE_1";
            } else if (expTime * coadds > maxVal) {
                float c = 0.1F; // See OT-78
                int n = Math.round((float) (maxVal / (expTime + c)));
                txt = "Exposures longer than " + maxVal + " seconds may lead to smearing. "
                        + n + " coadds are recommended based on the exposure time";
                id = "STATIC_EXPOSURE_TIME_RULE_2";
            } else if (expTime < minVal) {
                txt = String.format("Below recommendation (" + "%.2f" + " sec).", minVal);
                id = "STATIC_EXPOSURE_TIME_RULE_3";
            }
            if (txt != null) {
                problems.addError(PREFIX+id, txt, elements.getInstrumentNode());
            }
            return problems;
        }
        return null;
    };

    // See OT-79
    private static IRule TOTAL_EXPOSURE_TIME_RULE = new IRule() {
        private static final String MESSAGE =
                "It is recommended that single observations be less than one hour in duration since "
                        + "the GPI system performance has not been evaluated for times longer than one hour.";

        @Override
        public IP2Problems check(ObservationElements elements) {
            P2Problems probs = new P2Problems();

            ConfigSequence seq = elements.getSequence();
            double totalExpTime = 0;
            int step = 0;
            Config config = null;
            for (Iterator<Config> it = seq.iterator(); it.hasNext(); ++step) {
                config = it.next();
                Double expTime = SequenceRule.getExposureTime(config);
                Integer coadds = SequenceRule.getCoadds(config);
                Integer repeatCount = SequenceRule.getStepCount(config);
                if (expTime != null && coadds != null && repeatCount != null) {
                    totalExpTime += (expTime * coadds * repeatCount);
                }
            }

            if (totalExpTime > 3600) {
                probs.addError(PREFIX + "TOTAL_EXPOSURE_TIME_RULE",
                        MESSAGE, SequenceRule.getInstrumentOrSequenceNode(step - 1, elements, config));
            }

            return probs;
        }
    };


    private static IRule MAGNITUDE_RULE = new IRule() {

        private static final String MAG_BAND_MESSAGE = "For GPI you must provide the target magnitude in the ";
        private static final String TOO_BRIGHT_MESSAGE = "Target is too bright for the AO system";
        private static final String MAG_BRIGHT_MESSAGE = "Target is too bright, it will saturate";
        // OT-127 and OT-128
        private static final String MAG_BRIGHT_LOFS_MESSAGE = "Target is too bright, it will saturate the LOWFS";
        private static final String MAG_FAINT_LOFS_MESSAGE = "Target is too faint for proper CAL (LOWFS) operation and thus mask centering on the coronograph will be severely affected.";
        private static final double MAG_BRIGHT_LOFS_LIMIT = 2.0;
        private static final double MAG_FAINT_LOFS_LIMIT = 9.5;

        @Override
        public IP2Problems check(ObservationElements elements)  {
            return elements.getTargetObsComp().map((obsComp) -> {

                P2Problems problems = new P2Problems();
                TargetEnvironment env = obsComp.getTargetEnvironment();

                // We only consider the first science target because a multi-target asterism is
                // a configuration error that will raise a further P2 warning.
                SPTarget base = env.getAsterism().allSpTargets().head();

                scala.Option<Magnitude> imag = base.getMagnitude(MagnitudeBand.I$.MODULE$);
                scala.Option<Magnitude> hmag = base.getMagnitude(MagnitudeBand.H$.MODULE$);
                // OT-74
                if (imag.isEmpty()) {
                    problems.addError(PREFIX + "MAG_BAND_MESSAGE", MAG_BAND_MESSAGE + "I-band.", elements.getTargetObsComponentNode().getValue());
                }
                // OT-75
                if (hmag.isEmpty()) {
                    problems.addError(PREFIX + "MAG_BAND_MESSAGE", MAG_BAND_MESSAGE + "H-band.", elements.getTargetObsComponentNode().getValue());
                }

                if (elements.getInstrument() instanceof Gpi) {
                    Gpi inst = (Gpi) elements.getInstrument();
                    if (!inst.getObservingMode().isEmpty()) {
                        Gpi.ObservingMode obsMode = inst.getObservingMode().getValue();
                        MagnitudeBand band = inst.getFilter().getBand(); // OT-102: obsMode could be NONSTANDARD
                        scala.Option<Magnitude> mag = base.getMagnitude(band);
                        if (mag.isEmpty()) {
                            // OT-99
                            problems.addError(PREFIX + "MAG_BAND_MESSAGE", MAG_BAND_MESSAGE + band + "-band",
                                    elements.getTargetObsComponentNode().getValue());
                        } else if (obsMode.getBrightLimit(inst.getDisperser()).exists(a -> mag.get().value() < a)) {
                            // OT-76
                            problems.addWarning(PREFIX + "MAG_BRIGHT_MESSAGE", MAG_BRIGHT_MESSAGE, elements.getTargetObsComponentNode().getValue());
                        }
                        if (!hmag.isEmpty() && obsMode.logValue().startsWith("Coronograph")) {
                            // OT-127
                            if (hmag.get().value() < MAG_BRIGHT_LOFS_LIMIT) {
                                problems.addError(PREFIX + "MAG_BRIGHT_LOFS_MESSAGE", MAG_BRIGHT_LOFS_MESSAGE, elements.getTargetObsComponentNode().getValue());
                            }
                            // OT-128
                            if (hmag.get().value() > MAG_FAINT_LOFS_LIMIT) {
                                problems.addError(PREFIX + "MAG_FAINT_LOFS_MESSAGE", MAG_FAINT_LOFS_MESSAGE, elements.getTargetObsComponentNode().getValue());
                            }
                        }
                    }
                }
                // OT-77
                if (!imag.isEmpty()) {
                    double brightness = imag.get().value();
                    if (brightness != -99 && brightness < 0.5   ) {
                        problems.addWarning(PREFIX + "TOO_BRIGHT_MESSAGE", TOO_BRIGHT_MESSAGE, elements.getTargetObsComponentNode().getValue());
                    }
                }

                return problems;
            }).getOrNull();
        }
    };


    /**
     * Register all the Gpi rules to apply.
     */
    static {
//        GPI_RULES.add(OFFSET_RULE);
        GPI_RULES.add(FILTER_ITER_RULE);
        GPI_RULES.add(HALF_WAVE_PLATE_ANGLE_RULE);
    }


    public IP2Problems check(ObservationElements elements)  {
        IP2Problems seqProblems = (new SequenceRule(GPI_RULES, null)).check(elements);

        // Check the sky background.
        seqProblems.append(MAGNITUDE_RULE.check(elements));
        seqProblems.append(STATIC_EXPOSURE_TIME_RULE.check(elements));
        seqProblems.append(TOTAL_EXPOSURE_TIME_RULE.check(elements));

        return seqProblems;
    }
}
