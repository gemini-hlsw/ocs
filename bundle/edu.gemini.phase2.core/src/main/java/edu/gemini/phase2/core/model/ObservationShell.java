package edu.gemini.phase2.core.model;

import edu.gemini.pot.sp.*;
import edu.gemini.spModel.obs.SPObservation;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;

import edu.gemini.shared.util.immutable.None;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class ObservationShell implements Serializable {
    public static final String PARAM_SET_NAME = "observationShell";

    public final SPObservation observation;
    public final List<ObsComponentShell> obsComponents;
    public final SeqComponentShell seqShell;

    public ObservationShell(SPObservation observation, Collection<ObsComponentShell> obsComponents, SeqComponentShell seqShell) {
        if (observation == null) throw new IllegalArgumentException("observation is null");
        if (obsComponents == null) throw new IllegalArgumentException("obsComponents is null");
        if (seqShell == null) throw new IllegalArgumentException("seqShell is null");
        this.observation   = observation;
        this.obsComponents = Collections.unmodifiableList(new ArrayList<ObsComponentShell>(obsComponents));
        this.seqShell      = seqShell;
    }

    public ObservationShell(ParamSet pset) {
        this.observation = new SPObservation();
        this.observation.setParamSet(pset.getParamSet(SPObservation.SP_TYPE.readableStr));

        List<ObsComponentShell> children = new ArrayList<ObsComponentShell>();
        for (ParamSet ocPset : pset.getParamSets(ObsComponentShell.PARAM_SET_NAME)) {
            children.add(new ObsComponentShell(ocPset));
        }
        this.obsComponents = Collections.unmodifiableList(children);

        ParamSet scPset = pset.getParamSet(SeqComponentShell.PARAM_SET_NAME);
        this.seqShell = new SeqComponentShell(scPset);
    }

    public ObservationShell(ISPObservation sp)  {
        this.observation = (SPObservation) sp.getDataObject();
        this.obsComponents = Collections.unmodifiableList(
          ObsComponentShell.shellList(sp.getObsComponents())
        );
        this.seqShell    = new SeqComponentShell(sp.getSeqComponent());
    }

    public ParamSet toParamSet(PioFactory factory) {
        ParamSet pset = factory.createParamSet(PARAM_SET_NAME);

        pset.addParamSet(observation.getParamSet(factory));
        for (ObsComponentShell ocs : obsComponents) {
            pset.addParamSet(ocs.toParamSet(factory));
        }
        pset.addParamSet(seqShell.toParamSet(factory));

        return pset;
    }

    public ISPObservation toSp(ISPFactory factory, ISPProgram prog)  {
        try {
            ISPObservation sp = factory.createObservation(prog, None.instance(), null);

            // Set the observation's data object, but don't clobber the
            // TooType. Leave it at the default for the program TooType.
            final SPObservation dobj = (SPObservation) observation.clone();
            dobj.setOverrideRapidToo(false);
            sp.setDataObject(dobj);

            sp.setObsComponents(ObsComponentShell.spList(factory, prog, obsComponents));
            sp.setSeqComponent(seqShell.toSp(factory, prog));
            return sp;
        } catch (SPException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static List<ISPObservation> spList(ISPFactory factory, ISPProgram prog, List<ObservationShell> shells)  {
        List<ISPObservation> spList = new ArrayList<ISPObservation>();
        for (ObservationShell shell : shells) spList.add(shell.toSp(factory, prog));
        return spList;
    }

    public static List<ObservationShell> shellList(List<ISPObservation> spList)  {
        List<ObservationShell> shells = new ArrayList<ObservationShell>();
        for (ISPObservation sp : spList) shells.add(new ObservationShell(sp));
        return shells;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ObservationShell that = (ObservationShell) o;

        if (!obsComponents.equals(that.obsComponents)) return false;
        if (!observation.equals(that.observation)) return false;
        if (!seqShell.equals(that.seqShell)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = observation.hashCode();
        result = 31 * result + obsComponents.hashCode();
        result = 31 * result + seqShell.hashCode();
        return result;
    }
}
