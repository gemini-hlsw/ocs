package edu.gemini.phase2.core.model;

import edu.gemini.pot.sp.*;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.template.TemplateGroup;
import edu.gemini.spModel.template.TemplateParameters;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class TemplateGroupShell extends BaseGroupShell {
    public static final String PARAM_SET_NAME = "templateGroupShell";

    public final TemplateGroup templateGroup;
    public final List<TemplateParameters> argList;

    public TemplateGroupShell(TemplateGroup templateGroup, Collection<TemplateParameters> argList) {
        super();
        if (templateGroup == null) throw new IllegalArgumentException("templateGroup is null");
        this.templateGroup = templateGroup;
        this.argList       = Collections.unmodifiableList(new ArrayList<TemplateParameters>(argList));
    }

    public TemplateGroupShell(TemplateGroup templateGroup, Collection<TemplateParameters> argList, Collection<ObsComponentShell> obsComponents, Collection<ObservationShell> observations) {
        super(obsComponents, observations);
        if (templateGroup == null) throw new IllegalArgumentException("templateGroup is null");
        this.templateGroup = templateGroup;
        this.argList       = Collections.unmodifiableList(new ArrayList<TemplateParameters>(argList));
    }

    public TemplateGroupShell(ParamSet pset) {
        super(pset);
        this.templateGroup = new TemplateGroup();
        templateGroup.setParamSet(pset.getParamSet(TemplateGroup.SP_TYPE.readableStr));

        final List<TemplateParameters> argList = new ArrayList<TemplateParameters>();
        for (ParamSet ps : pset.getParamSets(TemplateParameters.SP_TYPE.readableStr)) {
            argList.add(new TemplateParameters(ps));
        }
        this.argList = Collections.unmodifiableList(argList);
    }

    public TemplateGroupShell(ISPTemplateGroup sp)  {
        super(ObsComponentShell.shellList(sp.getObsComponents()), ObservationShell.shellList(sp.getObservations()));
        this.templateGroup = (TemplateGroup) sp.getDataObject();

        final List<TemplateParameters> argList = new ArrayList<TemplateParameters>();
        for (ISPTemplateParameters tp : sp.getTemplateParameters()) {
            argList.add((TemplateParameters) tp.getDataObject());
        }
        this.argList = Collections.unmodifiableList(argList);
    }

    public String paramSetName() { return PARAM_SET_NAME; }

    public ParamSet toParamSet(PioFactory factory) {
        ParamSet pset = super.toParamSet(factory);
        pset.addParamSet(templateGroup.getParamSet(factory));
        for (TemplateParameters args : argList) pset.addParamSet(args.getParamSet(factory));
        return pset;
    }

    public ISPTemplateGroup toSp(ISPFactory factory, ISPProgram prog)  {
        try {
            final ISPTemplateGroup sp = factory.createTemplateGroup(prog, null);
            sp.setDataObject(templateGroup);
            for (TemplateParameters args : argList) {
                ISPTemplateParameters tp = factory.createTemplateParameters(prog, null);
                tp.setDataObject(args);
                sp.addTemplateParameters(tp);
            }
            sp.setObsComponents(ObsComponentShell.spList(factory, prog, obsComponents));
            sp.setObservations(ObservationShell.spList(factory, prog, observations));
            return sp;
        } catch (SPException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        final TemplateGroupShell that = (TemplateGroupShell) o;
        if (!argList.equals(that.argList)) return false;
        return templateGroup.equals(that.templateGroup);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + templateGroup.hashCode();
        result = 31 * result + argList.hashCode();
        return result;
    }
}
