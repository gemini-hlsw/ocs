package edu.gemini.phase2.core.model;

import edu.gemini.pot.sp.*;
import edu.gemini.spModel.obscomp.SPGroup;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.template.TemplateFolder;
import edu.gemini.spModel.template.TemplateGroup;


import java.util.Collection;

public final class GroupShell extends BaseGroupShell {
    public static final String PARAM_SET_NAME = "groupShell";

    public final SPGroup group;

    public GroupShell(SPGroup group) {
        super();
        if (group == null) throw new IllegalArgumentException("group is null");
        this.group = group;
    }

    public GroupShell(SPGroup group, Collection<ObsComponentShell> obsComponents, Collection<ObservationShell> observations) {
        super(obsComponents, observations);
        if (group == null) throw new IllegalArgumentException("group is null");
        this.group = group;
    }

    public GroupShell(ParamSet pset) {
        super(pset);
        this.group = new SPGroup("");
        this.group.setParamSet(pset.getParamSet(SPGroup.SP_TYPE.readableStr));
    }

    public GroupShell(ISPGroup sp)  {
        super(ObsComponentShell.shellList(sp.getObsComponents()), ObservationShell.shellList(sp.getObservations()));
        this.group = (SPGroup) sp.getDataObject();
    }

    public String paramSetName() { return PARAM_SET_NAME; }

    public ParamSet toParamSet(PioFactory factory) {
        ParamSet pset = super.toParamSet(factory);
        pset.addParamSet(group.getParamSet(factory));
        return pset;
    }

    public ISPGroup toSp(ISPFactory factory, ISPProgram prog)  {
        try {
            ISPGroup sp = factory.createGroup(prog, null);
            sp.setDataObject(group);
            sp.setObsComponents(ObsComponentShell.spList(factory, prog, obsComponents));
            sp.setObservations(ObservationShell.spList(factory, prog, observations));
            return sp;
        } catch (SPException ex) {
            throw new RuntimeException(ex);
        }
    }

    public TemplateGroupShell toTemplateGroupShell(TemplateFolder.Phase1Group pig) {
        return toTemplateGroupShell(pig.blueprintId, pig.argsList);
    }

    public TemplateGroupShell toTemplateGroupShell(String blueprintId, Collection<TemplateGroup.Args> argList) {
        TemplateGroup dataObj = new TemplateGroup();
        dataObj.setBlueprintId(blueprintId);
        dataObj.setTitle(group.getTitle());
        return new TemplateGroupShell(dataObj, argList, obsComponents, observations);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        GroupShell that = (GroupShell) o;

        if (!group.equals(that.group)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + group.hashCode();
        return result;
    }
}
