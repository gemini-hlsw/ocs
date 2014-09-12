package edu.gemini.catalog.impl;

import edu.gemini.catalog.api.*;
import edu.gemini.catalog.skycat.table.*;
import edu.gemini.catalog.api.MagnitudeLimits;
import edu.gemini.shared.skyobject.SkyObject;
import edu.gemini.shared.util.immutable.*;
import edu.gemini.skycalc.Angle;
import edu.gemini.skycalc.Coordinates;
import jsky.catalog.*;
import jsky.catalog.skycat.SkyObjectFactoryRegistrar;
import jsky.catalog.skycat.SkycatCatalog;
import jsky.catalog.skycat.SkycatConfigFile;
import jsky.catalog.skycat.SkycatTable;
import jsky.coords.CoordinateRadius;
import jsky.coords.WorldCoords;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Performs an skycat catalog server query on a single server.
 */
public final class SkycatCatalogServer implements CatalogServer {
    private final SkycatCatalog cat;
    private final SkyObjectFactory factory;

    public SkycatCatalogServer(String id) {
        cat     = (SkycatCatalog) SkycatConfigFile.getConfigFile().getCatalog(id);
        factory = SkyObjectFactoryRegistrar.instance.lookup(id).getValue();
    }

    @Override public CatalogResult query(QueryConstraint cons) throws IOException {
        QueryArgs    args = mkQueryArgs(cons);
        SkycatTable table;
        try {
            table = (SkycatTable) cat.noPopupCatalogQuery(args);
            // XXX TODO: use version below (allan)
//            table = (SkycatTable) cat.query(args, false);
        } catch (CatalogException ex) {
            throw new IOException(ex);
        }

        return new CatalogResult(cons, toSkyObjects(table, factory));
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
            } catch (edu.gemini.catalog.skycat.CatalogException ex) {
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


    private QueryArgs mkQueryArgs(QueryConstraint cons) {
        BasicQueryArgs res = new BasicQueryArgs(cat);
        res.setRegion(getRegion(cons.base, cons.radiusLimits));
        setMagnitudeQueryArgs(res, cons.magnitudeLimits);
        res.setMaxRows(10000);
        return res;
    }

    private CoordinateRadius getRegion(Coordinates coords, RadiusLimits limits) {
        Angle ra  = coords.getRa();
        Angle dec = coords.getDec();
        WorldCoords pos = new WorldCoords(ra.toDegrees().getMagnitude(), dec.toDegrees().getMagnitude(), 2000.0);

        double min = limits.getMinLimit().toArcmins().getMagnitude();
        double max = limits.getMaxLimit().toArcmins().getMagnitude();
        return new CoordinateRadius(pos, min, max);
    }


    private void setMagnitudeQueryArgs(final BasicQueryArgs queryArgs, MagnitudeLimits magLimits) {
        String magColumn = factory.getMagColumn(magLimits.getBand());

        int numParams = cat.getNumParams();
        for (int i = 0; i < numParams; i++) {
            final int index = i;
            FieldDesc fieldDesc = cat.getParamDesc(i);
            String id = fieldDesc.getId();
            if (magColumn.equals(id)) {
                if (fieldDesc.isMax()) {
                    queryArgs.setParamValue(i, magLimits.getFaintnessLimit());
                } else if (fieldDesc.isMin()) {
                    magLimits.getSaturationLimit().map(new MapOp<MagnitudeLimits.SaturationLimit, Double>() {
                        public Double apply(MagnitudeLimits.SaturationLimit sl) {
                            return sl.getBrightness();
                        }
                    }).foreach(new ApplyOp<Double>() {
                        @Override public void apply(Double limit) {
                            queryArgs.setParamValue(index, limit);
                        }
                    });
                }
            }
        }
    }

}
