//
// $
//

package edu.gemini.catalog.skycat.table;

import edu.gemini.catalog.skycat.CatalogException;
import edu.gemini.shared.skyobject.SkyObject;
import edu.gemini.shared.util.immutable.Option;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * A singleton that simplifies extracting a collection of {@link SkyObject}
 * given a {@link CatalogReader} and a {@link SkyObjectFactory factory} for
 * converting the {@link CatalogRow}s it produces.
 */
public enum CatalogTableUtil {
    instance;

    /**
     * Creates a collection of {@link SkyObject} by reading the provided
     * {@link CatalogReader}s output and using the provided
     * {@link SkyObjectFactory factory}.
     *
     * @param reader catalog reader that will generate the catalog data
     * @param factory factory used to turn each {@link CatalogRow} generated
     * by the reader into a {@link SkyObject}
     *
     * @return collection of {@link SkyObject} parsed from the catalog reader
     * output
     *
     * @throws IOException if there is a connection problem reading the catalog
     * @throws CatalogException if there is a problem parsing the catalog output
     */
    public Collection<SkyObject> readSkyObjects(CatalogReader reader, SkyObjectFactory factory) throws IOException, CatalogException {
        Collection<SkyObject> res;

        reader.open();
        try {
            Option<CatalogHeader> headerOpt = reader.getHeader();
            if (headerOpt.isEmpty()) return Collections.emptyList();
            CatalogHeader header = headerOpt.getValue();

            res = new ArrayList<SkyObject>();
            while (reader.hasNext()) {
                CatalogRow row = reader.next();
                res.add(factory.create(header, row));
            }
        } finally {
            reader.close();
        }

        return res;
    }
}
