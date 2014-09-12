package edu.gemini.phase2.core.model;

import edu.gemini.pot.sp.*;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.template.TemplateFolder;
import edu.gemini.spModel.template.TemplateFolder.Phase1Group;
import edu.gemini.spModel.template.TemplateGroup;
import edu.gemini.spModel.template.TemplateGroup.Args;
import edu.gemini.spModel.template.TemplateParameters;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class TemplateGroupShell extends BaseGroupShell {
    public static final String PARAM_SET_NAME = "templateGroupShell";

    public final TemplateGroup templateGroup;
    public final List<Args> argList;

    public TemplateGroupShell(TemplateGroup templateGroup, Collection<Args> argList) {
        super();
        if (templateGroup == null) throw new IllegalArgumentException("templateGroup is null");
        this.templateGroup = templateGroup;
        this.argList       = Collections.unmodifiableList(new ArrayList<Args>(argList));
    }

    public TemplateGroupShell(TemplateGroup templateGroup, Collection<TemplateGroup.Args> argList, Collection<ObsComponentShell> obsComponents, Collection<ObservationShell> observations) {
        super(obsComponents, observations);
        if (templateGroup == null) throw new IllegalArgumentException("templateGroup is null");
        this.templateGroup = templateGroup;
        this.argList       = Collections.unmodifiableList(new ArrayList<Args>(argList));
    }

    public TemplateGroupShell(ParamSet pset) {
        super(pset);
        this.templateGroup = new TemplateGroup();
        templateGroup.setParamSet(pset.getParamSet(TemplateGroup.SP_TYPE.readableStr));

        List<Args> argList = new ArrayList<Args>();
        for (ParamSet ps : pset.getParamSets(Args.PARAM_SET_NAME)) {
            argList.add(new Args(ps));
        }
        this.argList = Collections.unmodifiableList(argList);
    }

    public TemplateGroupShell(ISPTemplateGroup sp)  {
        super(ObsComponentShell.shellList(sp.getObsComponents()), ObservationShell.shellList(sp.getObservations()));
        this.templateGroup = (TemplateGroup) sp.getDataObject();

        List<Args> argList = new ArrayList<Args>();
        for (ISPTemplateParameters tp : sp.getTemplateParameters()) {
            TemplateParameters dobj = (TemplateParameters) tp.getDataObject();
            Args args = new Args(dobj.getTargetId(), dobj.getSiteQualityId(), dobj.getTime());
            argList.add(args);
        }
        this.argList = Collections.unmodifiableList(argList);
    }

    public String paramSetName() { return PARAM_SET_NAME; }

    /**
     * Creates empty TemplateGroupShells (in that they have no template
     * observations) corresponding to the groups in the given folder.
     */
    public static List<TemplateGroupShell> emptyShells(TemplateFolder folder) {
        List<Phase1Group> pigs = folder.getGroups();
        List<TemplateGroupShell> res = new ArrayList<TemplateGroupShell>(pigs.size());

        for (Phase1Group pig : pigs) {
            TemplateGroup tg = new TemplateGroup();
            tg.setTitle(folder.getBlueprints().get(pig.blueprintId).toString());
            tg.setBlueprintId(pig.blueprintId);
            res.add(new TemplateGroupShell(tg, pig.argsList));
        }

        return res;
    }

    public ParamSet toParamSet(PioFactory factory) {
        ParamSet pset = super.toParamSet(factory);
        pset.addParamSet(templateGroup.getParamSet(factory));
        for (Args args : argList) pset.addParamSet(args.getParamSet(factory));
        return pset;
    }

    public ISPTemplateGroup toSp(ISPFactory factory, ISPProgram prog)  {
        try {
            ISPTemplateGroup sp = factory.createTemplateGroup(prog, null);
            sp.setDataObject(templateGroup);
            for (Args args : argList) {
                ISPTemplateParameters tp = factory.createTemplateParameters(prog, null);
                tp.setDataObject(new TemplateParameters(args));
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

        TemplateGroupShell that = (TemplateGroupShell) o;

        if (!argList.equals(that.argList)) return false;
        if (!templateGroup.equals(that.templateGroup)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + templateGroup.hashCode();
        result = 31 * result + argList.hashCode();
        return result;
    }
}
