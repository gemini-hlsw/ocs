package edu.gemini.spModel.gemini.gnirs.blueprint;

import edu.gemini.spModel.gemini.altair.blueprint.SpAltair;
import edu.gemini.spModel.gemini.gnirs.GNIRSParams.CrossDispersed;
import edu.gemini.spModel.gemini.gnirs.GNIRSParams.Disperser;
import edu.gemini.spModel.gemini.gnirs.GNIRSParams.SlitWidth;
import edu.gemini.spModel.gemini.gnirs.GNIRSParams.PixelScale;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;

public final class SpGnirsBlueprintSpectroscopy extends SpGnirsBlueprintBase {
    public static final String PARAM_SET_NAME             = "gnirsBlueprintSpectroscopy";
    public static final String DISPERSER_PARAM_NAME       = "disperser";
    public static final String CROSS_DISPERSER_PARAM_NAME = "crossDisperser";
    public static final String FPU_PARAM_NAME             = "fpu";
    public static final String CENTRAL_WAVELENGTH_PARAM_NAME = "centralWavelengthGe2_5";

    public final Disperser disperser;
    public final CrossDispersed crossDisperser;
    public final SlitWidth fpu;
    public final boolean wavelengthGe2_5;

    public SpGnirsBlueprintSpectroscopy(SpAltair altair, PixelScale pixelScale, Disperser disperser, CrossDispersed crossDisperser, SlitWidth fpu, boolean wavelengthGe2_5) {
        super(altair, pixelScale);
        this.disperser       = disperser;
        this.crossDisperser  = crossDisperser;
        this.fpu             = fpu;
        this.wavelengthGe2_5 = wavelengthGe2_5;
    }

    public SpGnirsBlueprintSpectroscopy(ParamSet paramSet) {
        super(paramSet);
        this.disperser       = Pio.getEnumValue(paramSet, DISPERSER_PARAM_NAME, Disperser.DEFAULT);
        this.crossDisperser  = Pio.getEnumValue(paramSet, CROSS_DISPERSER_PARAM_NAME, CrossDispersed.DEFAULT);
        this.fpu             = Pio.getEnumValue(paramSet, FPU_PARAM_NAME, SlitWidth.DEFAULT);
        this.wavelengthGe2_5 = Pio.getBooleanValue(paramSet, CENTRAL_WAVELENGTH_PARAM_NAME, false);
    }

    public String paramSetName() { return PARAM_SET_NAME; }

    @Override
    public String toString() {
        return String.format("GNIRS Spectroscopy %s %s %s %s %s %s",
                altair.shortName(),
                pixelScale.displayValue(),
                disperser.displayValue(),
                crossDisperser.displayValue(),
                fpu.displayValue(),
                "wavelength " + (wavelengthGe2_5 ? ">=2.5" : "<2.5"));
    }

    public ParamSet toParamSet(PioFactory factory) {
        ParamSet paramSet = super.toParamSet(factory);
        Pio.addEnumParam(factory, paramSet, DISPERSER_PARAM_NAME, disperser);
        Pio.addEnumParam(factory, paramSet, CROSS_DISPERSER_PARAM_NAME, crossDisperser);
        Pio.addEnumParam(factory, paramSet, FPU_PARAM_NAME, fpu);
        Pio.addBooleanParam(factory, paramSet, CENTRAL_WAVELENGTH_PARAM_NAME, wavelengthGe2_5);
        return paramSet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        SpGnirsBlueprintSpectroscopy that = (SpGnirsBlueprintSpectroscopy) o;

        if (crossDisperser != that.crossDisperser) return false;
        if (disperser != that.disperser) return false;
        if (fpu != that.fpu) return false;
        if (wavelengthGe2_5 != that.wavelengthGe2_5) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + disperser.hashCode();
        result = 31 * result + crossDisperser.hashCode();
        result = 31 * result + fpu.hashCode();
        result = 31 * result + Boolean.valueOf(wavelengthGe2_5).hashCode();
        return result;
    }
}
