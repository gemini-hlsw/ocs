package edu.gemini.spModel.template;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.shared.util.TimeValue;
import edu.gemini.spModel.data.AbstractDataObject;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;

/**
 * Data object representing template parameters.
 *
 * Note that this class is effectivley immutable. Either use the non-empty ctor or call setParamSet() immediately on
 * construction. setParamSet() can be invoked only once.
 */
public final class TemplateParameters extends AbstractDataObject {


    public static final SPComponentType SP_TYPE = SPComponentType.TEMPLATE_PARAMETERS;
    public static final String VERSION = "2012B-1";

    private TemplateGroup.Args args;

    public TemplateParameters() {
        setTitle("Template Parameters");
        setType(SP_TYPE);
        setVersion(VERSION);
    }

    public TemplateParameters(TemplateGroup.Args args) {
        this();
        if (args == null) throw new IllegalArgumentException("args is null");
        this.args = args;
    }

    public String getTargetId() {
        return getArgs().getTargetId();
    }

    public String getSiteQualityId() {
        return getArgs().getSiteQualityId();
    }

    public TimeValue getTime() {
        return getArgs().getTime();
    }

    public ParamSet getParamSet(PioFactory factory) {
        final ParamSet ps = super.getParamSet(factory);
        ps.addParamSet(getArgs().getParamSet(factory));
        return ps;
    }

    public void setParamSet(ParamSet paramSet) {
        super.setParamSet(paramSet);
        final ParamSet argsPS = paramSet.getParamSet(TemplateGroup.Args.PARAM_SET_NAME);
        final TemplateGroup.Args args = new TemplateGroup.Args(argsPS);
        initArgs(args);
    }

    public int hashCode() {
        return getArgs().hashCode();
    }

    public boolean equals(Object obj) {
        if (obj instanceof TemplateParameters) {
            final TemplateParameters tp = (TemplateParameters) obj;
            return args == null ? tp.args == null : args.equals(tp.args);
//            return getArgs().equals(tp.getArgs());
        }
        return false;
    }

    private TemplateGroup.Args getArgs() {
        if (args == null)
            throw new IllegalStateException("Not initialized.");
        return args;
    }

    private void initArgs(TemplateGroup.Args args) {
        if (this.args != null)
            throw new IllegalStateException("Already initialized.");
        this.args = args;
    }

}
