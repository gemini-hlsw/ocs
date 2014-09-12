package edu.gemini.phase2.core.model;

import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class TemplateFolderExpansion implements Serializable {
    public static final String PARAM_SET_NAME = "templateFolderExpansion";

    public final List<TemplateGroupShell> templateGroups;
    public final List<GroupShell> baselineCalibrations;

    public TemplateFolderExpansion(Collection<TemplateGroupShell> templateGroups, Collection<GroupShell> baselineCalibrations) {
        this.templateGroups       = Collections.unmodifiableList(new ArrayList<TemplateGroupShell>(templateGroups));
        this.baselineCalibrations = Collections.unmodifiableList(new ArrayList<GroupShell>(baselineCalibrations));
    }

    public TemplateFolderExpansion(ParamSet pset) {
        List<TemplateGroupShell> groups = new ArrayList<TemplateGroupShell>();
        for (ParamSet ps : pset.getParamSets(TemplateGroupShell.PARAM_SET_NAME)) {
            groups.add(new TemplateGroupShell(ps));
        }
        this.templateGroups = Collections.unmodifiableList(groups);

        List<GroupShell> baselines = new ArrayList<GroupShell>();
        for (ParamSet ps : pset.getParamSets(GroupShell.PARAM_SET_NAME)) {
            baselines.add(new GroupShell(ps));
        }
        this.baselineCalibrations = Collections.unmodifiableList(baselines);
    }

    public ParamSet toParamSet(PioFactory factory) {
        ParamSet pset = factory.createParamSet(PARAM_SET_NAME);

        for (TemplateGroupShell group : templateGroups) pset.addParamSet(group.toParamSet(factory));
        for (GroupShell group : baselineCalibrations) pset.addParamSet(group.toParamSet(factory));

        return pset;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TemplateFolderExpansion that = (TemplateFolderExpansion) o;

        if (!baselineCalibrations.equals(that.baselineCalibrations)) return false;
        if (!templateGroups.equals(that.templateGroups)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = templateGroups.hashCode();
        result = 31 * result + baselineCalibrations.hashCode();
        return result;
    }
}
