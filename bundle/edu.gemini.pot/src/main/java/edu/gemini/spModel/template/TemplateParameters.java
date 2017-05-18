package edu.gemini.spModel.template;

import edu.gemini.pot.sp.ISPTemplateFolder;
import edu.gemini.pot.sp.ISPTemplateGroup;
import edu.gemini.pot.sp.ISPTemplateParameters;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.shared.util.TimeValue;
import edu.gemini.shared.util.immutable.ApplyOp;
import edu.gemini.spModel.data.AbstractDataObject;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.SPTargetPio;
import edu.gemini.spModel.target.env.Asterism;
import edu.gemini.spModel.target.env.Asterism$;

/**
 * Data object representing template parameters.
 *
 * Note that this class is effectively immutable. Either use the non-empty
 * constructor or call setParamSet() immediately on construction. setParamSet()
 * can be invoked only once.  Sorry this is so awkward.  Everything should be
 * truly immutable.
 */
public final class TemplateParameters extends AbstractDataObject {
    public static final SPComponentType SP_TYPE = SPComponentType.TEMPLATE_PARAMETERS;
    public static final String VERSION = "2015A-1";
    public static final String PARAM_TIME = "time";

    public static TemplateParameters newEmpty() {
        return new TemplateParameters(new SPTarget(), new SPSiteQuality(), TimeValue.ZERO_HOURS);
    }

    public static TemplateParameters newInstance(SPTarget target, SPSiteQuality conditions, TimeValue timeValue) {
        return new TemplateParameters(
                target.clone(),
                conditions.clone(),
                timeValue
        );
    }

    private SPTarget target;
    private SPSiteQuality conditions;
    private TimeValue time;

    public TemplateParameters() {
        setTitle("Template Parameters");
        setType(SP_TYPE);
        setVersion(VERSION);
    }

    public TemplateParameters(ParamSet paramSet) {
        this();
        setParamSet(paramSet);
    }

    // The private constructor is used to prevent making clones during calls
    // to copy() when not necessary.
    private TemplateParameters(SPTarget target, SPSiteQuality conditions, TimeValue timeValue) {
        this();
        this.target     = target;
        this.conditions = conditions;
        this.time       = timeValue;
    }

    private void checkRef(Object o) {
        if (o == null) throw new IllegalStateException("Not initialized.");
    }
    private void checkRefs() {
        checkRef(target);
        checkRef(conditions);
        checkRef(time);
    }

    public SPTarget getTarget() {
        checkRef(target);
        return target.clone();
    }

    public Asterism getAsterism() {
      return new Asterism.Single(getTarget());
    }

    public TemplateParameters copy(SPTarget target) {
        return new TemplateParameters(target.clone(), conditions, time);
    }

    public SPSiteQuality getSiteQuality() {
        checkRef(conditions);
        return conditions.clone();
    }

    public TemplateParameters copy(SPSiteQuality sq) {
        return new TemplateParameters(target, sq.clone(), time);
    }

    public TimeValue getTime() {
        checkRef(time);
        return time;  // actually immutable
    }

    public TemplateParameters copy(TimeValue time) {
        return new TemplateParameters(target, conditions, time);
    }

    public ParamSet getParamSet(PioFactory factory) {
        checkRefs();
        final ParamSet ps = super.getParamSet(factory);

        ps.addParamSet(target.getParamSet(factory));
        ps.addParamSet(conditions.getParamSet(factory));
        Pio.addLongParam(factory, ps, PARAM_TIME, time.getMilliseconds());

        return ps;
    }

    public void setParamSet(ParamSet paramSet) {
        if ((target != null) || (conditions != null) || (time != null)) {
            throw new IllegalStateException("Already initialized.");
        }

        super.setParamSet(paramSet);

        final ParamSet targetPs = paramSet.getParamSet(SPTargetPio.PARAM_SET_NAME);
        target = new SPTarget();
        target.setParamSet(targetPs);

        final ParamSet conditionsPs = paramSet.getParamSet(SPSiteQuality.SP_TYPE.readableStr);
        conditions = new SPSiteQuality();
        conditions.setParamSet(conditionsPs);

        time = TimeValue.millisecondsToTimeValue(Pio.getLongValue(paramSet, PARAM_TIME, 0L), TimeValue.Units.hours);
    }

    @Override
    public boolean equals(Object o) {
        checkRefs();
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final TemplateParameters that = (TemplateParameters) o;
        if (!conditions.equals(that.conditions)) return false;
        if (!target.equals(that.target)) return false;
        return time.equals(that.time);
    }

    @Override
    public int hashCode() {
        checkRefs();
        int result = target.hashCode();
        result = 31 * result + conditions.hashCode();
        result = 31 * result + time.hashCode();
        return result;
    }

    public static void foreach(ISPTemplateFolder folder, ApplyOp<TemplateParameters> op) {
        if (folder != null) {
            for (ISPTemplateGroup g : folder.getTemplateGroups()) {
                for (ISPTemplateParameters p : g.getTemplateParameters()) {
                    op.apply((TemplateParameters) p.getDataObject());
                }
            }
        }
    }
}
