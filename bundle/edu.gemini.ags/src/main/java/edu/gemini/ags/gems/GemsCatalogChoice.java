package edu.gemini.ags.gems;

import edu.gemini.catalog.api.CatalogName;
import edu.gemini.catalog.api.CatalogName.Gaia$;
import edu.gemini.catalog.api.CatalogName.PPMXL$;
import edu.gemini.catalog.api.CatalogName.UCAC4$;

public enum GemsCatalogChoice {
    GAIA_ESA(    Gaia$.MODULE$,  "Gaia at ESA"),
    PPMXL_GEMINI(PPMXL$.MODULE$, "PPMXL at Gemini"),
    UCAC4_GEMINI(UCAC4$.MODULE$, "UCAC4 at Gemini"),
    ;

    public static final GemsCatalogChoice DEFAULT = GAIA_ESA;

    private final CatalogName _catalogName;
    private final String      _displayValue;

    GemsCatalogChoice(CatalogName catalogName, String displayValue) {
        _catalogName  = catalogName;
        _displayValue = displayValue;
    }

    public String displayValue() {
        return _displayValue;
    }

    public CatalogName catalog() {
        return _catalogName;
    }

    public String toString() {
        return displayValue();
    }

}
