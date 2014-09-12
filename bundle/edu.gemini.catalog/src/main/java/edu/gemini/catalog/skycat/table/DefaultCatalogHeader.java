//
// $
//

package edu.gemini.catalog.skycat.table;

import edu.gemini.shared.util.immutable.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Default, immutable implementation of {@link CatalogHeader} backed with an
 * immutable list.
 */
public class DefaultCatalogHeader implements CatalogHeader {
    private final ImList<Tuple2<String, Class>> columns;
    private final Map<String, Integer> map;

    public DefaultCatalogHeader(ImList<Tuple2<String, Class>> columns) {
        this.columns = columns;

        Map<String, Integer> tmp = new HashMap<String, Integer>();
        int i = 0;
        for (Tuple2<String, Class> tup : columns) tmp.put(tup._1(), i++);
        this.map = Collections.unmodifiableMap(tmp);
    }

    @Override
    public int getColumnCount() {
        return columns.size();
    }

    @Override
    public Option<Integer> columnIndex(String name) {
        Integer res = map.get(name);
        if (res == null) return None.instance();
        return new Some<Integer>(res);
    }

    @Override
    public String getName(int columnIndex) {
        return columns.get(columnIndex)._1();
    }

    @Override
    public Class getClass(int columnIndex) {
        return columns.get(columnIndex)._2();
    }

    @Override
    public String toString() { return columns.mkString("Column Header: [", ",", "]"); }
}
