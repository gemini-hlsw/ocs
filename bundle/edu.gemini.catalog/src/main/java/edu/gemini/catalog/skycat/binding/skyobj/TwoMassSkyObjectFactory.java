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

import edu.gemini.shared.skyobject.Magnitude;
import edu.gemini.shared.skyobject.SkyObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A {@link SkyObjectFactory} used with the 2MASS catalog.
 */
@Deprecated
public enum TwoMassSkyObjectFactory implements SkyObjectFactory {
    instance;

    public static final String ID_COL  = "2MASS";
    public static final String RA_COL  = "RAJ2000";
    public static final String DEC_COL = "DEJ2000";

    public static final MagnitudeDescriptor J_MAG, H_MAG, K_MAG;
    static {
        J_MAG = new MagnitudeDescriptor(J, "Jmag", "e_Jmag");
        H_MAG = new MagnitudeDescriptor(H, "Hmag", "e_Hmag");
        K_MAG = new MagnitudeDescriptor(K, "Kmag", "e_Kmag");
    }

    private static final FactorySupport sup =
            new FactorySupport.Builder(ID_COL, RA_COL, DEC_COL).add(J_MAG, H_MAG, K_MAG).build();

    public static final Set<Magnitude.Band> BANDS = Collections.unmodifiableSet(
            new HashSet<Magnitude.Band>(Arrays.asList(J, H, K)));

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
