package edu.gemini.itc.shared;

import edu.gemini.spModel.type.DisplayableSpType;

public enum BrightnessUnit implements DisplayableSpType {

    // ==== INTEGRATED BRIGHTNESS

    // Magnitudes
    MAG                 ("Vega"),
    ABMAG               ("AB"),

    // Spectral Flux Density / Spectral Irradiance [W/m²/m]
    JY                  ("Jy"),             // 10e-26 W/m²/Hz
    WATTS               ("W/m²/µm"),
    ERGS_WAVELENGTH     ("erg/s/cm²/Å"),
    ERGS_FREQUENCY      ("erg/s/cm²/Hz"),

    // ==== UNIFORM SURFACE BRIGHTNESS

    // Magnitudes per area
    MAG_PSA             ("Vega/arcsec²"),
    ABMAG_PSA           ("AB/arcsec²"),

    // Spectral Flux Density / Spectral Irradiance per area
    JY_PSA              ("Jy/arcsec²"),
    WATTS_PSA           ("W/m²/µm/arcsec²"),
    ERGS_WAVELENGTH_PSA ("erg/s/cm²/Å/arcsec²"),
    ERGS_FREQUENCY_PSA  ("erg/s/cm²/Hz/arcsec²")
    ;
    private final String displayValue;
    private BrightnessUnit(final String displayName) { this.displayValue = displayName; }
    public String displayValue() {return displayValue;}
}
