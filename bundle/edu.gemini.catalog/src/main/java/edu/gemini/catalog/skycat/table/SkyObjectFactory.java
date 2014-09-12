//
// $
//

package edu.gemini.catalog.skycat.table;

import edu.gemini.catalog.skycat.CatalogException;
import edu.gemini.shared.skyobject.SkyObject;
import edu.gemini.shared.skyobject.Magnitude.Band;

import java.util.Map;
import java.util.Set;

/**
 * A factory for converting a {@link CatalogRow} into a {@link SkyObject}.
 * Implementations of this class contain the knowledge of exactly what data is
 * provided by particular catalogs and how it is provided.  They use this
 * information to extract data to construct a matching {@link SkyObject}.
 *
 * <p>The {@link CatalogHeader} may be used to simply the process by providing
 * a lookup from a header item name to its index in the row.  The
 * {@link CatalogValueExtractor} provides convenience methods to simplify the
 * process.
 */
public interface SkyObjectFactory {
    /**
     * Creates a {@link SkyObject} from the given {@link CatalogRow},
     * potentially using the {@link CatalogHeader} to simplify the process.
     *
     * @param header header for the catalog
     * @param row row containing data used to build the {@link SkyObject}
     *
     * @return new {@link SkyObject} created from the information in the
     * {@link CatalogRow}
     *
     * @throws CatalogException if there is a problem accessing the
     * data in the row
     */
    SkyObject create(CatalogHeader header, CatalogRow row) throws CatalogException;

    /**
     * @return the magnitude bands provided by the catalog query results
     */
    Set<Band> bands();

    /**
     * Needed for formulating a query from a list of magnitude limits.
     * @param band the magnitude band
     * @return the name of the column containing the given magnitude band, or null if not valid
     */
    String getMagColumn(Band band);
}
