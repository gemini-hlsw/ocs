package jsky.catalog.skycat;

//
// $
//

import edu.gemini.catalog.skycat.CatalogException;
import edu.gemini.catalog.skycat.binding.skyobj.FactorySupport;
import edu.gemini.catalog.skycat.table.CatalogHeader;
import edu.gemini.catalog.skycat.table.CatalogRow;
import edu.gemini.catalog.skycat.table.SkyObjectFactory;
import edu.gemini.shared.skyobject.Magnitude;
import edu.gemini.shared.skyobject.SkyObject;
import jsky.catalog.QueryArgs;
import jsky.catalog.TableQueryResult;
import edu.gemini.catalog.skycat.table.CatalogValueExtractor.MagnitudeDescriptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;


/**
 * A {@link edu.gemini.catalog.skycat.table.SkyObjectFactory} used for user catalogs
 * (local catalog files that include the necessary colums).
 * Assumes a standard first three colums: id, ra, dec (with any names),
 * optional pmRA and pmDEC, and any number of *mag columns (where * is a wildcard for the
 * magnitude band). Upper and lower case are not important for the column names.
 */
public class UserCatalogSkyObjectFactory implements SkyObjectFactory {

    // The local user catalog on which this object is based
    private final TableQueryResult _table;

    private final FactorySupport _sup;

    private final Set<Magnitude.Band> _bands = new TreeSet<Magnitude.Band>();

    public UserCatalogSkyObjectFactory(TableQueryResult table) {
        _table = table;
        _sup = _factorySupport();
        _adjustTableSearchColums();
    }

    // Returns the name of the given column, if found, otherwise the one at the default index, or
    // null if the default is not a valid index.
    private static String _findColumnName(List<String> colums, String name, int defaultIndex) {
        for (String columnName : colums) {
            if (columnName.equalsIgnoreCase(name)) return columnName;
        }
        if (defaultIndex >= 0)
            return colums.get(defaultIndex);
        return null;
    }

    private FactorySupport _factorySupport() {
        List<String> colums = _table.getColumnIdentifiers();
        if (colums == null || colums.size() < 3) {
            throw new RuntimeException("Expected a catalog table with at least the columns id, ra, dec.");
        }
        String idCol = _findColumnName(colums, "id", 0);
        String raCol = _findColumnName(colums, "RA(J2000)", 1);
        String decCol = _findColumnName(colums, "Dec(J2000)", 2);
        String pmRaCol = _findColumnName(colums, "pmRA", -1);
        String pmDecCol = _findColumnName(colums, "pmDec", -1);

        // find *mag colums
        List<MagnitudeDescriptor> magColums = new ArrayList<MagnitudeDescriptor>();
        for(String columnName : colums) {
            if (columnName.toLowerCase().endsWith("mag")) {
                try {
                    String bandName = columnName.substring(0,columnName.length()-3);
                    if (bandName.endsWith("1")) { // r1mag == Rmag...
                        bandName = bandName.substring(0, bandName.length()-1);
                    }
                    Magnitude.Band magBand = Magnitude.Band.valueOf(bandName);
                    _bands.add(magBand);
                    magColums.add(new MagnitudeDescriptor(magBand, columnName));
                } catch (IllegalArgumentException ignore) {
                    // ignore if not a known mag band
                }
            }
        }

        FactorySupport.Builder builder = new FactorySupport.Builder(idCol, raCol, decCol);
        if (pmRaCol != null && pmDecCol != null) {
            builder = builder.pmRa(pmRaCol).pmDec(pmDecCol);
        }
        for(MagnitudeDescriptor mag : magColums) {
            builder = builder.add(mag);
        }
        return builder.build();
    }

    private void _adjustTableSearchColums() {
        _table.getCatalog();
    }

    @Override
    public Set<Magnitude.Band> bands() { return _bands; }

    @Override
    public String getMagColumn(Magnitude.Band band) {
        return _sup.getMagColumn(band);
    }

    @Override
    public SkyObject create(CatalogHeader header, CatalogRow row) throws CatalogException {
        return _sup.create(header, row);
    }
}
