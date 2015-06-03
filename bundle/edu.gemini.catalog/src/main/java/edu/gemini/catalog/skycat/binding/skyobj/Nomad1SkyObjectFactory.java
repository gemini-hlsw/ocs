//
// $
//

package edu.gemini.catalog.skycat.binding.skyobj;

import edu.gemini.catalog.skycat.CatalogException;
import edu.gemini.catalog.skycat.table.CatalogHeader;
import edu.gemini.catalog.skycat.table.CatalogRow;
import edu.gemini.catalog.skycat.table.CatalogValueExtractor.MagnitudeDescriptor;
import edu.gemini.catalog.skycat.table.SkyObjectFactory;
import edu.gemini.shared.skyobject.Magnitude;
import edu.gemini.shared.skyobject.SkyObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static edu.gemini.shared.skyobject.Magnitude.Band.*;

/**
 * A {@link SkyObjectFactory} used with the NOMAD1 catalog.
 */
@Deprecated
public enum Nomad1SkyObjectFactory implements SkyObjectFactory {
    instance;

//    Bmag	r_Bmag	Vmag	r_Vmag	Rmag	r_Rmag	Jmag	Hmag	Kmag	R
    public static final String ID_COL  = "NOMAD1";
    public static final String RA_COL  = "RAJ2000";
    public static final String DEC_COL = "DEJ2000";

    public static final String PM_RA   = "pmRA";
    public static final String PM_DEC  = "pmDE";

    public static final MagnitudeDescriptor J_MAG, H_MAG, K_MAG, B_MAG, R_MAG, V_MAG;
    static {
        J_MAG = new MagnitudeDescriptor(J, "Jmag");
        H_MAG = new MagnitudeDescriptor(H, "Hmag");
        K_MAG = new MagnitudeDescriptor(K, "Kmag");
        B_MAG = new MagnitudeDescriptor(B, "Bmag");
        R_MAG = new MagnitudeDescriptor(R, "Rmag");
        V_MAG = new MagnitudeDescriptor(V, "Vmag");
    }

    private static final FactorySupport sup =
            new FactorySupport.Builder(ID_COL, RA_COL, DEC_COL).
                    pmRa(PM_RA).pmDec(PM_DEC).add(J_MAG, H_MAG, K_MAG, B_MAG, R_MAG, V_MAG).build();

    public static final Set<Magnitude.Band> BANDS = Collections.unmodifiableSet(
            new HashSet<Magnitude.Band>(Arrays.asList(J, H, K, B, R, V)));

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
