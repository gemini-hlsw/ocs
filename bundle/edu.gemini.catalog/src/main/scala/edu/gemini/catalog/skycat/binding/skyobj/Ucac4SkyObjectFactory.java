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
 * A {@link SkyObjectFactory} used with the
 * UCAC4 VoTable catalog.
 */
@Deprecated
public enum Ucac4SkyObjectFactory implements SkyObjectFactory {
    instance;

    public static final String ID_COL  = "4UC";
    public static final String RA_COL  = "RA";
    public static final String DEC_COL = "DEC";

    public static final String PM_RA   = "pmRA";
    public static final String PM_DEC  = "pmDEC";

    public static final MagnitudeDescriptor UC_MAG, B_MAG, V_MAG, g_MAG, r_MAG, J_MAG, K_MAG, H_MAG, i_MAG;
    static {
        UC_MAG = new MagnitudeDescriptor(UC,  "UC");
        B_MAG  = new MagnitudeDescriptor(B,  "B");
        V_MAG  = new MagnitudeDescriptor(V,  "V");
        g_MAG  = new MagnitudeDescriptor(g,  "g");
        r_MAG  = new MagnitudeDescriptor(r,  "r");
        i_MAG  = new MagnitudeDescriptor(i,  "i");
        J_MAG  = new MagnitudeDescriptor(J,  "J");
        H_MAG  = new MagnitudeDescriptor(H,  "H");
        K_MAG  = new MagnitudeDescriptor(K,  "K");
    }

    private static final FactorySupport sup =
            new FactorySupport.Builder(ID_COL, RA_COL, DEC_COL).
                    pmRa(PM_RA).pmDec(PM_DEC).add(UC_MAG, B_MAG, V_MAG, g_MAG, r_MAG, i_MAG, J_MAG, H_MAG, K_MAG).build();

    public static final Set<Magnitude.Band> BANDS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(UC, B, V, g, r, i, J, H, K)));

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
