package edu.gemini.catalog.impl;

import edu.gemini.catalog.api.*;
import edu.gemini.catalog.api.MagnitudeLimits;
import edu.gemini.catalog.api.MagnitudeLimits.FaintnessLimit;
import edu.gemini.catalog.api.MagnitudeLimits.SaturationLimit;
import edu.gemini.shared.util.immutable.MapOp;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.skycalc.Angle;

import java.io.IOException;

import static edu.gemini.skycalc.Angle.Unit.ARCMINS;

/**
 * A catalog server that delegates to another catalog server and maintains a
 * limited-size cache of past results.
 */
public class CachingCatalogServer implements CatalogServer {

    private final Cache cache;
    private final CatalogServer delegate;

    public CachingCatalogServer(int size, CatalogServer delegate) {
        if (size < 0) throw new IllegalArgumentException("size is " + size);
        if (delegate == null) throw new IllegalArgumentException("delegate is null");

        this.cache    = new Cache(size);
        this.delegate = delegate;
    }

    public CatalogServer getDelegate() { return delegate; }

    @Override public CatalogResult query(QueryConstraint p) throws IOException {

        // First check the cache.
        Option<CatalogResult> r = cache.search(p);
        if (!r.isEmpty()) return r.getValue().filter(p.base, p.radiusLimits, p.magnitudeLimits);

        // Not there, so broaden the limits and search.  The rules for how much
        // to embiggen aren't clear but are based on the BEST and WORST
        // SPSiteQuality conditions adjustments.  We're just embiggening the
        // results in the cache, not the actual return value.
        QueryConstraint big = p.copy(embiggen(p.radiusLimits)).copy(embiggen(p.magnitudeLimits));
        CatalogResult   res = delegate.query(big);
        cache.record(big, res);

        return res.filter(p);
    }

    private RadiusLimits embiggen(RadiusLimits requested) {
        double max = requested.getMaxLimit().toArcmins().getMagnitude() * 1.5;
        if (max < 10.0) max = 10;
        return new RadiusLimits(new Angle(max, ARCMINS));
    }

    private MagnitudeLimits embiggen(MagnitudeLimits requested) {
        // We may be adjusting already adjusted magnitude limits, which could
        // result in a broader search than we'd ever actually need.  That's
        // okay.
        final FaintnessLimit fainter   = new FaintnessLimit(requested.getFaintnessLimit().getBrightness() + 0.5);
        final Option<SaturationLimit> brighter = requested.getSaturationLimit().map(new MapOp<SaturationLimit, SaturationLimit>() {
            @Override public SaturationLimit apply(SaturationLimit saturationLimit) {
                return new SaturationLimit(saturationLimit.getBrightness() - 5.0);
            }
        });

        return new MagnitudeLimits(requested.getBand(), fainter, brighter);
    }
}
