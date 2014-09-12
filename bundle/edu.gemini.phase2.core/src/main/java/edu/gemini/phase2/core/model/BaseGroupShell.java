package edu.gemini.phase2.core.model;

import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class BaseGroupShell implements Serializable {

    public final List<ObsComponentShell> obsComponents;
    public final List<ObservationShell> observations;

    public BaseGroupShell() {
        this.obsComponents = Collections.emptyList();
        this.observations  = Collections.emptyList();
    }

    public BaseGroupShell(Collection<ObsComponentShell> obsComponents, Collection<ObservationShell> observations) {
        this.obsComponents = Collections.unmodifiableList(new ArrayList<ObsComponentShell>(obsComponents));
        this.observations  = Collections.unmodifiableList(new ArrayList<ObservationShell>(observations));
    }

    public BaseGroupShell(ParamSet pset) {
        List<ObsComponentShell> obsComponents = new ArrayList<ObsComponentShell>();
        for (ParamSet ps : pset.getParamSets(ObsComponentShell.PARAM_SET_NAME)) {
            obsComponents.add(new ObsComponentShell(ps));
        }
        this.obsComponents = Collections.unmodifiableList(obsComponents);

        List<ObservationShell> observations = new ArrayList<ObservationShell>();
        for (ParamSet ps : pset.getParamSets(ObservationShell.PARAM_SET_NAME)) {
            observations.add(new ObservationShell(ps));
        }
        this.observations = Collections.unmodifiableList(observations);
    }

    public abstract String paramSetName();

    public ParamSet toParamSet(PioFactory factory) {
        ParamSet pset = factory.createParamSet(paramSetName());

        for (ObsComponentShell obsComp : obsComponents) pset.addParamSet(obsComp.toParamSet(factory));
        for (ObservationShell obs : observations) pset.addParamSet(obs.toParamSet(factory));

        return pset;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BaseGroupShell that = (BaseGroupShell) o;

        if (!obsComponents.equals(that.obsComponents)) return false;
        if (!observations.equals(that.observations)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = obsComponents.hashCode();
        result = 31 * result + observations.hashCode();
        return result;
    }
}
