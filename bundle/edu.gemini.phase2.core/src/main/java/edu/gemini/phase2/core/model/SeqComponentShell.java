package edu.gemini.phase2.core.model;

import edu.gemini.pot.sp.*;
import edu.gemini.spModel.data.*;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class SeqComponentShell extends ComponentShell {
    public static final String PARAM_SET_NAME = "seqComponentShell";

    public final List<SeqComponentShell> children;

    public SeqComponentShell(ISPDataObject dataObject) {
        super(dataObject);
        this.children   = Collections.emptyList();
    }

    public SeqComponentShell(ISPDataObject dataObject, Collection<SeqComponentShell> children) {
        super(dataObject);
        if (children == null) throw new IllegalArgumentException("children is null");
        this.children   = Collections.unmodifiableList(new ArrayList<SeqComponentShell>(children));
    }

    public SeqComponentShell(ParamSet pset) {
        super(pset);

        List<SeqComponentShell> children = new ArrayList<SeqComponentShell>();
        for (ParamSet childPset : pset.getParamSets(PARAM_SET_NAME)) {
            children.add(new SeqComponentShell(childPset));
        }
        this.children = Collections.unmodifiableList(children);
    }

    public SeqComponentShell(ISPSeqComponent sp)  {
        super(sp.getDataObject());
        this.children = Collections.unmodifiableList(
                shellList(sp.getSeqComponents())
        );
    }

    public String paramSetName() { return PARAM_SET_NAME; }

    @Override public ParamSet toParamSet(PioFactory factory) {
        ParamSet pset = super.toParamSet(factory);

        for (SeqComponentShell shell : children) {
            pset.addParamSet(shell.toParamSet(factory));
        }

        return pset;
    }

    public ISPSeqComponent toSp(ISPFactory factory, ISPProgram prog)  {
        try {
            ISPSeqComponent sp = factory.createSeqComponent(prog, dataObject.getType(), null);
            sp.setDataObject(dataObject);
            sp.setSeqComponents(spList(factory, prog, children));
            return sp;
        } catch (SPException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static List<ISPSeqComponent> spList(ISPFactory factory, ISPProgram prog, List<SeqComponentShell> shells)  {
        List<ISPSeqComponent> spList = new ArrayList<ISPSeqComponent>();
        for (SeqComponentShell shell : shells) spList.add(shell.toSp(factory, prog));
        return spList;
    }

    public static List<SeqComponentShell> shellList(List<ISPSeqComponent> spList)  {
        List<SeqComponentShell> shells = new ArrayList<SeqComponentShell>();
        for (ISPSeqComponent sp : spList) shells.add(new SeqComponentShell(sp));
        return shells;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        SeqComponentShell that = (SeqComponentShell) o;

        if (!children.equals(that.children)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + children.hashCode();
        return result;
    }
}
