package edu.gemini.spModel.inst;

import edu.gemini.spModel.obscomp.SPInstObsComp;

public interface VignettableScienceAreaInstrument<I extends SPInstObsComp> {
    public ScienceAreaGeometry<I> getVignettableScienceArea();
}
