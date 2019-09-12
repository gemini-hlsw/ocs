package edu.gemini.ags.gems;

import edu.gemini.ags.gems.GemsMagnitudeTable.CanopusWfsCalculator;
import edu.gemini.catalog.api.CatalogName;
import edu.gemini.catalog.api.CatalogName.Gaia$;
import edu.gemini.catalog.api.CatalogName.PPMXL$;
import edu.gemini.catalog.api.CatalogName.UCAC4$;
import edu.gemini.catalog.api.MagnitudeConstraints;
import edu.gemini.spModel.core.Angle;
import edu.gemini.spModel.core.MagnitudeBand;
import edu.gemini.spModel.gemini.gems.CanopusWfs;
import edu.gemini.spModel.gems.GemsGuideProbeGroup;
import static edu.gemini.spModel.gems.GemsGuideStarType.tiptilt;
import edu.gemini.spModel.gems.GemsTipTiltMode;
import edu.gemini.spModel.obs.context.ObsContext;


import java.util.*;

/**
 * An immutable class specifying the Gems guide star search options.
 * An instance of this class will be created by the UI or other client
 * and used to control the search process.
 *
 * See OT-25
 */
public final class GemsGuideStarSearchOptions {

    public enum CatalogChoice {
        GAIA_ESA(    Gaia$.MODULE$,  "Gaia at ESA"),
        PPMXL_GEMINI(PPMXL$.MODULE$, "PPMXL at Gemini"),
        UCAC4_GEMINI(UCAC4$.MODULE$, "UCAC4 at Gemini"),
        ;

        public static final CatalogChoice DEFAULT = GAIA_ESA;

        private final CatalogName _catalogName;
        private final String      _displayValue;

        CatalogChoice(CatalogName catalogName, String displayValue) {
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

    public static final CatalogChoice DEFAULT = CatalogChoice.DEFAULT;

}
