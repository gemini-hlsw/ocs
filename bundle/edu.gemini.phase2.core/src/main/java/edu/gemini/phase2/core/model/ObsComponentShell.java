package edu.gemini.phase2.core.model;

import edu.gemini.pot.sp.*;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;


import java.util.ArrayList;
import java.util.List;

public final class ObsComponentShell extends ComponentShell {
    public static final String PARAM_SET_NAME = "obsComponentShell";

    public ObsComponentShell(ISPDataObject dataObject) {
        super(dataObject);
    }

    public ObsComponentShell(ParamSet pset) {
        super(pset);
    }

    public ObsComponentShell(ISPObsComponent sp)  {
        this(sp.getDataObject());
    }

    public String paramSetName() { return PARAM_SET_NAME; }

    @Override public ParamSet toParamSet(PioFactory factory) {
        return super.toParamSet(factory);
    }

    public ISPObsComponent toSp(ISPFactory factory, ISPProgram prog)  {
        try {
            ISPObsComponent sp = factory.createObsComponent(prog, dataObject.getType(), null);
            sp.setDataObject(dataObject);
            return sp;
        } catch (SPUnknownIDException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static List<ISPObsComponent> spList(ISPFactory factory, ISPProgram prog, List<ObsComponentShell> shells)  {
        List<ISPObsComponent> spList = new ArrayList<ISPObsComponent>();
        for (ObsComponentShell shell : shells) spList.add(shell.toSp(factory, prog));
        return spList;
    }

    public static List<ObsComponentShell> shellList(List<ISPObsComponent> spList)  {
        List<ObsComponentShell> shells = new ArrayList<ObsComponentShell>();
        for (ISPObsComponent sp : spList) shells.add(new ObsComponentShell(sp));
        return shells;
    }
}
