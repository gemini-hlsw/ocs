package edu.gemini.catalog.api;

import edu.gemini.shared.skyobject.SkyObject;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.skycalc.Coordinates;

/**
 * The result of a successful AGS search.
 */
public final class CatalogResult {

    public final QueryConstraint constraint;
    public final ImList<SkyObject> candidates;

    public CatalogResult(QueryConstraint constraint, ImList<SkyObject> candidates) {
        this.constraint = constraint;
        this.candidates = candidates;
    }

/*
    public static CatalogResult apply(QueryConstraint constraint, SkycatTable table, SkyObjectFactory factory) {
        return new CatalogResult(constraint, table, toSkyObjects(table, factory));
    }

    private static ImList<SkyObject> toSkyObjects(SkycatTable table, SkyObjectFactory factory) {
        CatalogHeader header = getHeader(table);
        int rows = table.getRowCount();
        Vector<Vector<Object>> dataVector = table.getDataVector();
        final List<SkyObject> res = new ArrayList<SkyObject>();
        for (int i=0; i<rows; ++i) {
            CatalogRow row = new DefaultCatalogRow(DefaultImList.create(dataVector.get(i)));
            try {
                res.add(factory.create(header, row));
            } catch (edu.gemini.shared.catalog.CatalogException ex) {
                ex.printStackTrace();
            }
        }
        return DefaultImList.create(res);
    }

    private static CatalogHeader getHeader(TableQueryResult table) {
        ImList<String> headerList = DefaultImList.create(table.getColumnIdentifiers());
        ImList<Tuple2<String,Class>> headerPairList = headerList.map(new MapOp<String, Tuple2<String, Class>>() {
            @Override
            public Tuple2<String, Class> apply(String s) {
                return new Pair<String, Class>(s, String.class);
            }
        });
        return new DefaultCatalogHeader(headerPairList);
    }
*/

    public CatalogResult filter(PredicateOp<SkyObject> op) {
        // Filter the skyobjects.
        ImList<SkyObject> filteredList = candidates.filter(op);
        if (filteredList.size() == candidates.size()) return this;
        return new CatalogResult(constraint, filteredList);
    }

    public CatalogResult filter(MagnitudeLimits magLimits) {
        return filter(magLimits.skyObjectFilter());
    }

    public CatalogResult filter(Coordinates base, RadiusLimits radLimits) {
        return filter(radLimits.skyObjectFilter(base));
    }

    public CatalogResult filter(Coordinates base, RadiusLimits radLimits, MagnitudeLimits magLimits) {
        final PredicateOp<SkyObject> r = radLimits.skyObjectFilter(base);
        final PredicateOp<SkyObject> m = magLimits.skyObjectFilter();
        return filter(new PredicateOp<SkyObject>() {
            @Override public Boolean apply(SkyObject skyObject) {
                return m.apply(skyObject) && r.apply(skyObject);
            }
        });
    }

    public CatalogResult filter(QueryConstraint cons) {
        return filter(cons.base, cons.radiusLimits, cons.magnitudeLimits);
    }
}
