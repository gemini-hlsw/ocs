//
// $
//

package edu.gemini.catalog.skycat.binding.skyobj;

import edu.gemini.catalog.skycat.CatalogException;
import edu.gemini.catalog.skycat.table.CatalogHeader;
import edu.gemini.catalog.skycat.table.CatalogRow;
import edu.gemini.catalog.skycat.table.CatalogValueExtractor.MagnitudeDescriptor;
import edu.gemini.catalog.skycat.table.SkyObjectFactory;
import static edu.gemini.shared.skyobject.Magnitude.Band.J;
import static edu.gemini.shared.skyobject.Magnitude.Band.K;
import static edu.gemini.shared.skyobject.Magnitude.Band.I;
import static edu.gemini.shared.skyobject.Magnitude.Band.H;
import static edu.gemini.shared.skyobject.Magnitude.Band.B;
import static edu.gemini.shared.skyobject.Magnitude.Band.R;

import edu.gemini.shared.skyobject.Magnitude;
import edu.gemini.shared.skyobject.SkyObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A {@link SkyObjectFactory} used with the PPMXL catalog.
 */
@Deprecated
public enum PpmxlSkyObjectFactory implements SkyObjectFactory {
    instance;

    public static final String ID_COL  = "PPMXL";
    public static final String RA_COL  = "RAJ2000";
    public static final String DEC_COL = "DEJ2000";

    public static final String PM_RA   = "pmRA";
    public static final String PM_DEC  = "pmDE";

    public static final MagnitudeDescriptor H_MAG, I_MAG, J_MAG, K_MAG, B_MAG, R_MAG;
    static {
        J_MAG = new MagnitudeDescriptor(J, "Jmag");
        I_MAG = new MagnitudeDescriptor(I, "imag");
        H_MAG = new MagnitudeDescriptor(H, "Hmag");
        K_MAG = new MagnitudeDescriptor(K, "Kmag");
        B_MAG = new MagnitudeDescriptor(B, "b1mag");
        R_MAG = new MagnitudeDescriptor(R, "r1mag");
    }

    private static final FactorySupport sup =
            new FactorySupport.Builder(ID_COL, RA_COL, DEC_COL).
                    pmRa(PM_RA).pmDec(PM_DEC).add(I_MAG, J_MAG, H_MAG, K_MAG, B_MAG, R_MAG).build();

    public static final Set<Magnitude.Band> BANDS = Collections.unmodifiableSet(
            new HashSet<Magnitude.Band>(Arrays.asList(J, I, H, K, B, R)));

    @Override
    public Set<Magnitude.Band> bands() { return BANDS; }

    @Override
    public String getMagColumn(Magnitude.Band band) {
        return sup.getMagColumn(band);
    }

    @Override
    public SkyObject create(CatalogHeader header, CatalogRow row) throws CatalogException {
        return sup.create(header, row);
    }
}
