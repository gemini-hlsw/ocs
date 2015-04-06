package edu.gemini.itc.shared;

import edu.gemini.spModel.type.DisplayableSpType;

public enum BrightnessUnit implements DisplayableSpType {
    // TODO: The "displayable" units are pretty ugly, but we have to keep them for
    // TODO: now in order to be backwards compatible for regression testing.
    MAG                 ("mag"),
    ABMAG               ("ABmag"),
    JY                  ("Jy"),
    WATTS               ("watts_fd_wavelength"),
    ERGS_WAVELENGTH     ("ergs_fd_wavelength"),
    ERGS_FREQUENCY      ("ergs_fd_frequency"),
    // -- TODO: same in blue but per area, can we unify those two sets of values?
    MAG_PSA             ("mag_per_sq_arcsec"),
    ABMAG_PSA           ("ABmag_per_sq_arcsec"),
    JY_PSA              ("jy_per_sq_arcsec"),
    WATTS_PSA           ("watts_fd_wavelength_per_sq_arcsec"),
    ERGS_WAVELENGTH_PSA ("ergs_fd_wavelength_per_sq_arcsec"),
    ERGS_FREQUENCY_PSA  ("ergs_fd_frequency_per_sq_arcsec")
    ;
    private final String displayValue;
    private BrightnessUnit(final String displayName) { this.displayValue = displayName; }
    public String displayValue() {return displayValue;}
}
