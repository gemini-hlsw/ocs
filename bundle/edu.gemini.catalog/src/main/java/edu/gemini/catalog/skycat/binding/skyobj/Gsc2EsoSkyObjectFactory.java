//
// $
//

package edu.gemini.catalog.skycat.binding.skyobj;

import edu.gemini.catalog.skycat.CatalogException;
import edu.gemini.catalog.skycat.table.CatalogHeader;
import edu.gemini.catalog.skycat.table.CatalogRow;
import edu.gemini.catalog.skycat.table.CatalogValueExtractor.MagnitudeDescriptor;
import edu.gemini.catalog.skycat.table.SkyObjectFactory;
import static edu.gemini.shared.skyobject.Magnitude.Band.B;
import static edu.gemini.shared.skyobject.Magnitude.Band.I;
import static edu.gemini.shared.skyobject.Magnitude.Band.R;
import static edu.gemini.shared.skyobject.Magnitude.Band.V;
import edu.gemini.shared.skyobject.SkyObject;
import edu.gemini.shared.skyobject.Magnitude;
import edu.gemini.shared.util.immutable.PredicateOp;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A {@link edu.gemini.catalog.skycat.table.SkyObjectFactory} used with the
 * GSC2@ESO catalog.
 */
@Deprecated
public enum Gsc2EsoSkyObjectFactory implements SkyObjectFactory {
    instance;

    public static final String ID_COL  = "GSC2ID";
    public static final String RA_COL  = "Ra";
    public static final String DEC_COL = "Dec";

    public static final MagnitudeDescriptor B_MAG, I_MAG, R_MAG, V_MAG;
    static{
        B_MAG = new MagnitudeDescriptor(B, "Jmag");
        I_MAG = new MagnitudeDescriptor(I, "Nmag");
        R_MAG = new MagnitudeDescriptor(R, "Fmag");
        V_MAG = new MagnitudeDescriptor(V, "Vmag");
    }

    private static final FactorySupport sup =
            new FactorySupport.Builder(ID_COL, RA_COL, DEC_COL).add(B_MAG, I_MAG, R_MAG, V_MAG).build();

    // Only accept magnitudes greater than -99.  This catalog uses -99.9 to
    // flag unknown magnitude values.
    private static final PredicateOp<Magnitude> MAG_FILTER =
            new BrightnessFilter(BrightnessFilter.Op.gt, -99);


    public static final Set<Magnitude.Band> BANDS = Collections.unmodifiableSet(
            new HashSet<Magnitude.Band>(Arrays.asList(B, I, R, V)));

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
