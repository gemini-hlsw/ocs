package edu.gemini.spModel.obs.plannedtime;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.config.ConfigBridge;
import edu.gemini.spModel.config.map.ConfigValMapInstances;
import edu.gemini.spModel.config2.Config;
import edu.gemini.spModel.config2.ConfigSequence;
import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.dataset.DatasetLabel;
import edu.gemini.spModel.obs.ObsClassService;
import edu.gemini.spModel.obs.plannedtime.PlannedTime.*;
import edu.gemini.spModel.obsclass.ObsClass;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.obsrecord.ObsExecRecord;
import edu.gemini.spModel.time.ChargeClass;
import edu.gemini.spModel.util.SPTreeUtil;


import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Calculates detailed planned time accounting information.
 */
public enum PlannedTimeCalculator {
    instance;

    private static final ItemKey OBS_KEY      = new ItemKey("observe");
    private static final ItemKey OBS_CLASS_KEY    = new ItemKey(OBS_KEY, InstConstants.OBS_CLASS_PROP);
    private static final ItemKey OBS_TYPE_KEY     = new ItemKey(OBS_KEY, InstConstants.OBSERVE_TYPE_PROP);

    private static final Logger LOG = Logger.getLogger(PlannedTimeCalculator.class.getName());

    public PlannedTime calc(ISPObservation obs)  {
        ObsExecRecord obsExecRecord = SPTreeUtil.getObsRecord(obs);
        ChargeClass obsChargeClass = chargeClass(obs);

        // add the setup time for the instrument
        double setupTime = 15 * 60; // default setup time
        double reacqTime = 0;       // default reacquisition time
        ISPObsComponent instNode = SPTreeUtil.findInstrument(obs);
        if (instNode != null) {
            SPInstObsComp inst = (SPInstObsComp) instNode.getDataObject();
            if (inst != null) setupTime = inst.getSetupTime(obs);
            if (inst != null) reacqTime = inst.getReacquisitionTime(obs);
        }
        Setup setup = Setup.fromSeconds(setupTime, reacqTime, obsChargeClass);

        // Calculate the overhead time
        Option<Config> prev = None.instance();
        List<PlannedTime.Step> steps = new ArrayList<PlannedTime.Step>();
        ConfigSequence cs = ConfigBridge.extractSequence(obs, null, ConfigValMapInstances.IDENTITY_MAP, false);
        for (Config c : cs.getAllSteps()) {
            ChargeClass stepChargeClass = stepChargeClass(obsChargeClass, c);
            boolean executed            = isExecuted(obsExecRecord, c);
            String obsType              = getObsType(c);
            CategorizedTimeGroup gtc    = calculator(instNode).calc(c, prev);
            prev = new Some<Config>(c);

            steps.add(Step.apply(gtc, stepChargeClass, executed, obsType));
        }

        return PlannedTime.apply(setup, steps, cs);
    }

    /**
     * For ITC.
     * @deprecated config is a key-object collection and is thus not type-safe. It is meant for ITC only.
     */
    @Deprecated
    public PlannedTime calc(Config[] conf, ItcOverheadProvider instr)  {
        ChargeClass obsChargeClass = ChargeClass.PROGRAM;

        // add the setup time for the instrument
        double setupTime = 15 * 60; // default setup time
        double reacqTime = 0;       // default reacquisition time
        if (instr != null) {
            setupTime = instr.getSetupTime(conf[0]);
            reacqTime = instr.getReacquisitionTime();
        }
        Setup setup = Setup.fromSeconds(setupTime, reacqTime, obsChargeClass);

        // Calculate the overhead time
        Option<Config> prev = None.instance();
        List<PlannedTime.Step> steps = new ArrayList<>();
        ConfigSequence cs = new ConfigSequence(conf);
        for (Config c : cs.getAllSteps()) {
            CategorizedTimeGroup gtc    = instr.calc(c, prev);
            prev = new Some<>(c);

            steps.add(Step.apply(gtc));
        }

        return PlannedTime.apply(setup, steps, cs);
    }

    private StepCalculator calculator(ISPObsComponent inst)  {
        if (inst == null) return DefaultStepCalculator.instance;
        SPInstObsComp dobj = (SPInstObsComp) inst.getDataObject();
        return (dobj instanceof StepCalculator) ? (StepCalculator) dobj : DefaultStepCalculator.instance;
    }

    private static ChargeClass chargeClass(ISPObservation obs)  {
        ObsClass obsClass = ObsClassService.lookupObsClass(obs);
        if (obsClass == null) return ChargeClass.NONCHARGED;

        // Why don't we respect the associated charge class here. Shouldn't we
        // just update the charge class of ACQ and ACQ_CAL?
        if ((obsClass == ObsClass.ACQ) || (obsClass == ObsClass.ACQ_CAL)) {
            return ChargeClass.NONCHARGED;
        }

        return obsClass.getDefaultChargeClass();
    }

    private static ChargeClass chargeClass(Config c) {
        ObsClass obsClass = getObsClass(c);

        if ((ObsClass.ACQ == obsClass) || (ObsClass.ACQ_CAL == obsClass)) {
            return ChargeClass.NONCHARGED;
        }

        return obsClass.getDefaultChargeClass();
    }

    // Figure out the charge class for the step.  An individual step can't have
    // a more costly charge class than the observation as a whole.  For example,
    // if the observation as a whole is non-charged, then an individual step
    // can't be charged to the partner or program.
    private static ChargeClass stepChargeClass(ChargeClass obsChargeClass, Config stepConfig) {
        ChargeClass nominal = chargeClass(stepConfig);
        return (nominal.compareTo(obsChargeClass) < 0) ? nominal : obsChargeClass;
    }

    /**
     * The key that identifies the data label item of a
     * {@link Config}.
     */
    public static final ItemKey DATALABEL_KEY = new ItemKey("observe:dataLabel");

    private static boolean isExecuted(ObsExecRecord obsExecRecord, Config c) {
        // Determine whether or not this step has been executed. This is retarded.
        boolean executed = false;
        if (obsExecRecord != null) {
            Object obj = c.getItemValue(DATALABEL_KEY);
            if (obj != null) {
                DatasetLabel lab = null;
                if (obj instanceof DatasetLabel) {
                    lab = (DatasetLabel) obj;
                } else {
                    try {
                        lab = new DatasetLabel(obj.toString());
                    } catch (ParseException pe) {
                        LOG.log(Level.FINE, "Could not parse label: " + obj.toString(), pe);
                    }
                }
                executed = (lab != null) && (obsExecRecord.getDatasetExecRecord(lab) != null);
            } else {
                // is this possible? seems like there should always be a label
                LOG.warning("Config has no DATA_LABEL_PROP ...");
            }
        }
        return executed;
    }


    private static String /* yes, a string, sigh */ getObsType(Config c) {
        Object val = c.getItemValue(OBS_TYPE_KEY);
        return (val == null) ? InstConstants.SCIENCE_OBSERVE_TYPE : val.toString();
    }

    private static ObsClass getObsClass(Config c) {
        Object val = c.getItemValue(OBS_CLASS_KEY);
        if (val == null) return ObsClass.SCIENCE;
        if (val instanceof ObsClass) return (ObsClass) val;

        ObsClass res = ObsClass.parseType(val.toString());
        if (res == null) return ObsClass.SCIENCE;
        return res;
    }
}
