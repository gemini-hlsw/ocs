package edu.gemini.phase2.core.model;

import edu.gemini.pot.sp.*;
import edu.gemini.spModel.obscomp.SPGroup;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.target.env.AsterismType;
import edu.gemini.spModel.template.Phase1Group;
import edu.gemini.spModel.template.TemplateGroup;
import edu.gemini.spModel.template.TemplateParameters;


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
            final ISPGroup sp = factory.createGroup(prog, null);
            sp.setDataObject(group);
            sp.setObsComponents(ObsComponentShell.spList(factory, prog, obsComponents));
            sp.setObservations(ObservationShell.spList(factory, prog, observations));
            return sp;
        } catch (SPException ex) {
            throw new RuntimeException(ex);
        }
    }

    public TemplateGroupShell toTemplateGroupShell(Phase1Group pig, AsterismType astType) {
        return toTemplateGroupShell(pig.blueprintId, pig.argsList, astType);
    }

    public TemplateGroupShell toTemplateGroupShell(
        String blueprintId,
        Collection<TemplateParameters> argList,
        AsterismType astType
    ) {
        final TemplateGroup dataObj = new TemplateGroup();
        dataObj.setBlueprintId(blueprintId);
        dataObj.setTitle(group.getTitle());
        dataObj.setGroupType(group.getGroupType());
        dataObj.setAsterismType(astType);
        return new TemplateGroupShell(dataObj, argList, obsComponents, observations);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        final GroupShell that = (GroupShell) o;
        return group.equals(that.group);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + group.hashCode();
        return result;
    }
}
