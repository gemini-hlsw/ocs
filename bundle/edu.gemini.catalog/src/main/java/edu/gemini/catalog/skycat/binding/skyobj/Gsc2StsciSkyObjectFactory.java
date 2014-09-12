//
// $
//

package edu.gemini.catalog.skycat.binding.skyobj;

import edu.gemini.catalog.skycat.CatalogException;
import edu.gemini.catalog.skycat.table.CatalogHeader;
import edu.gemini.catalog.skycat.table.CatalogRow;
import edu.gemini.catalog.skycat.table.CatalogValueExtractor.MagnitudeDescriptor;
import edu.gemini.catalog.skycat.table.SkyObjectFactory;
import static edu.gemini.shared.skyobject.Magnitude.Band.*;
import edu.gemini.shared.skyobject.SkyObject;
import edu.gemini.shared.skyobject.Magnitude;
import edu.gemini.shared.util.immutable.PredicateOp;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A {@link edu.gemini.catalog.skycat.table.SkyObjectFactory} used with the
 * GSC2@STScI catalog.
 */
public enum Gsc2StsciSkyObjectFactory implements SkyObjectFactory {
    instance;

    public static final String ID_COL    = "HSTGSID";
    public static final String RA_COL    = "Ra";
    public static final String DEC_COL   = "Dec";
    public static final String EPOCH_COL = "Epoch";

    public static final MagnitudeDescriptor U_MAG, B_MAG, V_MAG, R_MAG, I_MAG;
    static {
        U_MAG = new MagnitudeDescriptor(U, "Umag");
        B_MAG = new MagnitudeDescriptor(B, "Bmag");
        V_MAG = new MagnitudeDescriptor(V, "Vmag");
        R_MAG = new MagnitudeDescriptor(R, "Rmag");
        I_MAG = new MagnitudeDescriptor(I, "Imag");
    }

    private static final FactorySupport sup =
            new FactorySupport.Builder(ID_COL, RA_COL, DEC_COL).add(U_MAG, B_MAG, V_MAG, R_MAG, I_MAG).build();

    // Only accept magnitudes less than 99.  This catalog uses 99.9 to flag
    // unknown magnitude values.
    private static final PredicateOp<Magnitude> MAG_FILTER =
            new BrightnessFilter(BrightnessFilter.Op.lt, 99);

    public static final Set<Magnitude.Band> BANDS = Collections.unmodifiableSet(
            new HashSet<Magnitude.Band>(Arrays.asList(U, B, V, R, I)));

    @Override
    public Set<Magnitude.Band> bands() { return BANDS; }

    @Override
    public String getMagColumn(Magnitude.Band band) {
        return sup.getMagColumn(band);
    }

    @Override
    public SkyObject create(CatalogHeader header, CatalogRow row) throws CatalogException {
        SkyObject res = sup.create(header, row);
        return res.withMagnitudes(res.getMagnitudes().filter(MAG_FILTER));
    }
}
