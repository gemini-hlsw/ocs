package edu.gemini.catalog.api;

import edu.gemini.catalog.impl.*;
import edu.gemini.shared.util.immutable.DefaultImList;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.shared.util.immutable.MapOp;

/**
 * A default AgsCatalogServer configuration.
 */
public final class CatalogServerInstances {
    private CatalogServerInstances() {}

    public static final String UCAC3_CADC  = "UCAC3@CADC";
    public static final String UCAC3_CDS   = "UCAC3@CDS";

    public static final String PPMXL_CADC  = "PPMXL@CADC";
    public static final String PPMXL_CDS   = "PPMXL@CDS";

    public static final String NOMAD1_CADC = "NOMAD1@CADC";
    public static final String NOMAD1_CDS  = "NOMAD1@CDS";

    public static final ImList<String> UCAC3_SERVER_IDS  = DefaultImList.create(UCAC3_CADC, UCAC3_CDS);
    public static final ImList<String> PPMXL_SERVER_IDS  = DefaultImList.create(PPMXL_CADC, PPMXL_CDS);
    public static final ImList<String> NOMAD1_SERVER_IDS = DefaultImList.create(NOMAD1_CADC, NOMAD1_CDS);

    // Maps a catalog id to an AgsCatalogServer.
    private static final MapOp<String, CatalogServer> TO_SERVER = new MapOp<String, CatalogServer>() {
        @Override public CatalogServer apply(String id) {
            return new CachingCatalogServer(20, new SkycatCatalogServer(id));
        }
    };

    public static final CatalogServer UCAC3_PARALLEL  = new ParallelCatalogServer(UCAC3_SERVER_IDS.map(TO_SERVER));
    public static final CatalogServer PPMXL_PARALLEL  = new ParallelCatalogServer(PPMXL_SERVER_IDS.map(TO_SERVER));
    public static final CatalogServer NOMAD1_PARALLEL = new ParallelCatalogServer(NOMAD1_SERVER_IDS.map(TO_SERVER));

    /**
     * The normally used AGS Catalog Server.
     */
    public static final CatalogServer STANDARD =
            new SequentialCatalogServer(
                    DefaultImList.create(UCAC3_PARALLEL, PPMXL_PARALLEL)
            );

    public static final CatalogServer NOMAD1 =
            new SequentialCatalogServer(
                    DefaultImList.create(NOMAD1_PARALLEL)
            );

    // See REL-604
    public static final CatalogServer UCAC3 =
            new SequentialCatalogServer(
                    DefaultImList.create(UCAC3_PARALLEL)
            );
}
