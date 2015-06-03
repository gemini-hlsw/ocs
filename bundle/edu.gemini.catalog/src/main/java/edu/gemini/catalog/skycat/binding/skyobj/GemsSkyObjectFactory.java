package edu.gemini.catalog.skycat.binding.skyobj;

import edu.gemini.catalog.skycat.CatalogException;
import edu.gemini.catalog.skycat.table.CatalogHeader;
import edu.gemini.catalog.skycat.table.CatalogRow;
import edu.gemini.catalog.skycat.table.CatalogValueExtractor;
import edu.gemini.catalog.skycat.table.SkyObjectFactory;

import edu.gemini.shared.skyobject.Magnitude;
import edu.gemini.shared.skyobject.SkyObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static edu.gemini.shared.skyobject.Magnitude.Band.*;

/**
 * A {@link edu.gemini.catalog.skycat.table.SkyObjectFactory} used with the GemsGuideStarSearchDialog class.
 */
@Deprecated
public enum GemsSkyObjectFactory implements SkyObjectFactory {
    instance;

//    [x] ID R H RA Dec
    public static final String ID_COL  = "Id";
    public static final String RA_COL  = "RA";
    public static final String DEC_COL = "Dec";

    public static final CatalogValueExtractor.MagnitudeDescriptor UC_MAG, J_MAG, H_MAG, K_MAG, R_MAG, r_MAG;
    static {
        UC_MAG = new CatalogValueExtractor.MagnitudeDescriptor(UC, "UC");
        J_MAG = new CatalogValueExtractor.MagnitudeDescriptor(J, "J");
        H_MAG = new CatalogValueExtractor.MagnitudeDescriptor(H, "H");
        K_MAG = new CatalogValueExtractor.MagnitudeDescriptor(K, "K");
        R_MAG = new CatalogValueExtractor.MagnitudeDescriptor(R, "R");
        r_MAG = new CatalogValueExtractor.MagnitudeDescriptor(r, "r'");
    }

    private static final FactorySupport sup =
            new FactorySupport.Builder(ID_COL, RA_COL, DEC_COL).add(UC_MAG, J_MAG, H_MAG, K_MAG, R_MAG, r_MAG).build();

    public static final Set<Magnitude.Band> BANDS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(UC, J, H, K, R, r)));

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
