package edu.gemini.ags.gems;

import edu.gemini.catalog.api.CatalogName;
import edu.gemini.catalog.api.CatalogName.GaiaEsa$;
import edu.gemini.catalog.api.CatalogName.GaiaGemini$;
import edu.gemini.catalog.api.CatalogName.PPMXL$;
import edu.gemini.catalog.api.CatalogName.UCAC4$;

public enum GemsCatalogChoice {
    GAIA_ESA(GaiaEsa$.MODULE$, "Gaia at ESA"),
    GAIA_Gemini(GaiaGemini$.MODULE$, "Gaia at Gemini"),
    PPMXL_GEMINI(PPMXL$.MODULE$, "PPMXL at Gemini"),
    UCAC4_GEMINI(UCAC4$.MODULE$, "UCAC4 at Gemini"),
    ;

    public static final GemsCatalogChoice DEFAULT = GAIA_Gemini;

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
