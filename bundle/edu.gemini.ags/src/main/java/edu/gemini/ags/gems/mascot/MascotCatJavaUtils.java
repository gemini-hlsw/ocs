package edu.gemini.ags.gems.mascot;

import edu.gemini.catalog.skycat.table.CatalogHeader;
import edu.gemini.catalog.skycat.table.CatalogRow;
import edu.gemini.catalog.skycat.table.DefaultCatalogHeader;
import edu.gemini.catalog.skycat.table.DefaultCatalogRow;
import edu.gemini.shared.util.immutable.DefaultImList;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.shared.util.immutable.Pair;
import edu.gemini.shared.util.immutable.Tuple2;

import java.util.Collection;

/**
 * Local utility methods that are implemented in Java
 */
class MascotCatJavaUtils {

    /**
     * Creates a CatalogHeader and CatalogRow from the corresponding Collections.
     * (Doing this in Java, since the types are hard to get right from Scala)
     */
    @SuppressWarnings("varargs")
    static Tuple2<CatalogHeader, CatalogRow> wrap(Collection<String> header, Collection<Object> row) {
        ImList<Tuple2<String, Class<?>>> colLst = DefaultImList.create();
        for (String col : header) {
            colLst = colLst.append(new Pair<String, Class<?>>(col, String.class));
        }
        return new Pair<CatalogHeader, CatalogRow>(
                new DefaultCatalogHeader(colLst),
                new DefaultCatalogRow(DefaultImList.create(row)));
    }
}
