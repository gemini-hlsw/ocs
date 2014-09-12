package edu.gemini.spModel.gemini.altair.blueprint;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.gemini.altair.AltairParams.FieldLens;
import edu.gemini.spModel.gemini.altair.AltairParams.GuideStarType;
import edu.gemini.spModel.gemini.altair.AltairParams.Mode;
import edu.gemini.spModel.gemini.altair.InstAltair;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;

public final class SpAltairLgs extends SpAltairAo {
    public static final String PARAM_SET_NAME      = "altairLgs";
    public static final String LGS_MODE_PARAM_NAME = "mode";
    public static final String PWFS1_PARAM_NAME    = "pwfs1";

    public enum LgsMode {
        LGS(Mode.LGS, null),
        LGS_P1(Mode.LGS_P1, "PWFS1"),
        LGS_OI(Mode.LGS_OI, "OIWFS"),
        ;
        public final Mode altairMode;
        public final String shortName;
        public final String display;

        private LgsMode(Mode m, String suffix) {
            this.altairMode = m;
            this.shortName  = "LGS" + ((suffix == null) ? "" : "/" + suffix);
            this.display    = "Altair Laser Guidestar" + ((suffix == null) ? "" : " w/ " + suffix);
        }

        @Override public String toString() { return display; }
    }

    public final LgsMode lgsMode;

    public SpAltairLgs(LgsMode lgsMode) {
        this.lgsMode = lgsMode;
    }

    public SpAltairLgs(ParamSet paramSet) {
        LgsMode tmp = Pio.getEnumValue(paramSet, LGS_MODE_PARAM_NAME, LgsMode.LGS);
        if (tmp == LgsMode.LGS) {
            // handle migration from old boolean pwfs1 option
            final boolean oldPwfs1 = Pio.getBooleanValue(paramSet, PWFS1_PARAM_NAME, false);
            if (oldPwfs1) tmp = LgsMode.LGS_P1;
        }
        lgsMode = tmp;
    }

    public FieldLens fieldLens() { return FieldLens.IN; }
    public GuideStarType guideStarType() { return GuideStarType.LGS; }
    public boolean usePwfs1() { return lgsMode == LgsMode.LGS_P1; }

    public SPComponentType instrumentType() { return InstAltair.SP_TYPE; }
    public boolean useAo() { return true; }

    public String shortName() { return lgsMode.shortName; }

    public Mode mode() { return lgsMode.altairMode; }

    @Override
    public String toString() { return lgsMode.display; }

    public String paramSetName() { return PARAM_SET_NAME; }

    public ParamSet toParamSet(PioFactory factory) {
        final ParamSet paramSet = factory.createParamSet(PARAM_SET_NAME);
        Pio.addEnumParam(factory, paramSet, LGS_MODE_PARAM_NAME, lgsMode);
        return paramSet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final SpAltairLgs that = (SpAltairLgs) o;
        return lgsMode == that.lgsMode;
    }

    @Override
    public int hashCode() {
        return lgsMode.hashCode();
    }
}
