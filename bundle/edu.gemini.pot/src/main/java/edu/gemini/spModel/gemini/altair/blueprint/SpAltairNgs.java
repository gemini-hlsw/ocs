package edu.gemini.spModel.gemini.altair.blueprint;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.gemini.altair.AltairParams;
import edu.gemini.spModel.gemini.altair.AltairParams.FieldLens;
import edu.gemini.spModel.gemini.altair.AltairParams.GuideStarType;
import edu.gemini.spModel.gemini.altair.InstAltair;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;

public final class SpAltairNgs extends SpAltairAo {
    public static final String PARAM_SET_NAME        = "altairNgs";
    public static final String FIELD_LENS_PARAM_NAME = "fieldLens";

    public final FieldLens fieldLens;

    public SpAltairNgs(FieldLens fieldLens) {
        this.fieldLens = fieldLens;
    }

    public SpAltairNgs(ParamSet paramSet) {
        this.fieldLens     = Pio.getEnumValue(paramSet, FIELD_LENS_PARAM_NAME, FieldLens.DEFAULT);
    }

    public FieldLens fieldLens() { return fieldLens; }
    public GuideStarType guideStarType() { return GuideStarType.NGS; }
    public boolean usePwfs1() { return false; }

    public SPComponentType instrumentType() { return InstAltair.SP_TYPE; }
    public boolean useAo() { return true; }

    public String shortName() {
        return (fieldLens == FieldLens.IN) ? "NGS/FL" : "NGS";
    }

    public AltairParams.Mode mode() {
        switch (fieldLens) {
            case IN: return AltairParams.Mode.NGS_FL;
            default: return AltairParams.Mode.NGS;
        }
    }

    @Override
    public String toString() {
        return String.format("Altair Natural Guidestar%s", (fieldLens == FieldLens.IN) ? " w/ Field Lens": "");
    }

    public String paramSetName() { return PARAM_SET_NAME; }

    public ParamSet toParamSet(PioFactory factory) {
        ParamSet paramSet = factory.createParamSet(PARAM_SET_NAME);
        Pio.addEnumParam(factory, paramSet, FIELD_LENS_PARAM_NAME, fieldLens);
        return paramSet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SpAltairNgs that = (SpAltairNgs) o;

        if (fieldLens != that.fieldLens) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return fieldLens.hashCode();
    }
}
