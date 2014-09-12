package edu.gemini.spModel.gemini.altair.blueprint;

import edu.gemini.spModel.gemini.altair.AltairParams.FieldLens;
import edu.gemini.spModel.gemini.altair.AltairParams.GuideStarType;
import edu.gemini.spModel.gemini.altair.AltairParams.Mode;

public abstract class SpAltairAo extends SpAltair {
    public abstract FieldLens fieldLens();
    public abstract GuideStarType guideStarType();
    public abstract boolean usePwfs1();

    public abstract Mode mode();
}

