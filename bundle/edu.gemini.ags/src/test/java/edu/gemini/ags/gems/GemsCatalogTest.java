package edu.gemini.ags.gems;

import edu.gemini.skycalc.Angle;
import edu.gemini.shared.skyobject.Magnitude;
import edu.gemini.shared.skyobject.coords.HmsDegCoordinates;
import edu.gemini.shared.skyobject.coords.SkyCoordinates;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.gemini.gems.GemsInstrument;
import edu.gemini.spModel.gemini.gsaoi.Gsaoi;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.gems.GemsTipTiltMode;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.TargetEnvironment;
import edu.gemini.spModel.telescope.IssPort;
import jsky.catalog.skycat.SkycatConfigFile;
import jsky.coords.WorldCoords;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 */
@Ignore
public class GemsCatalogTest {

    @Before
    public void init() {
        URL url = getClass().getResource("test.skycat.cfg");
        assert(url != null);
        SkycatConfigFile.setConfigFile(url);
    }

    @Test
    public void testSearch() throws Exception {


        WorldCoords coords = new WorldCoords("03:19:48.2341", "+41:30:42.078");
        SPTarget target = new SPTarget(coords.getRaDeg(), coords.getDecDeg());
        TargetEnvironment env = TargetEnvironment.create(target);
        Gsaoi inst = new Gsaoi();
        inst.setPosAngle(0.);
        inst.setIssPort(IssPort.SIDE_LOOKING);
        ObsContext ctx = ObsContext.create(env, inst, None.<Site>instance(), SPSiteQuality.Conditions.BEST, null, null);
        SkyCoordinates base =
                (new HmsDegCoordinates.Builder(
                        new Angle(coords.getRaDeg(), Angle.Unit.DEGREES),
                        new Angle(coords.getDecDeg(), Angle.Unit.DEGREES)).build());

//        String name = "test";
//        MagnitudeLimits magLimits = new MagnitudeLimits(Magnitude.Band.J, 18., 2.);
//        RadiusLimits radiusLimits = new RadiusLimits(new Angle(10., Angle.Unit.ARCMINS), new Angle(2., Angle.Unit.ARCMINS));
//        Option<Offset> offset = None.instance();
//        Option<Angle> posAngle = None.instance();
//        CatalogSearchCriterion crit = new CatalogSearchCriterion("test", magLimits, radiusLimits,
//                offset, posAngle);

        String opticalCatalog = GemsGuideStarSearchOptions.DEFAULT_CATALOG;
        String nirCatalog = GemsGuideStarSearchOptions.DEFAULT_CATALOG;
        GemsInstrument instrument = GemsInstrument.gsaoi;

        GemsTipTiltMode tipTiltMode = GemsTipTiltMode.instrument;

//        Option<MagnitudeLimits> nirMagLimits = new Some<MagnitudeLimits>(magLimits);
        Set<Angle> posAngles = new HashSet<Angle>();
        GemsGuideStarSearchOptions options = new GemsGuideStarSearchOptions(opticalCatalog, nirCatalog,
                instrument, tipTiltMode, posAngles);

        List<GemsCatalogSearchResults> results = new GemsCatalog().search(ctx, base, options, None.<Magnitude.Band>instance(), null);
        System.out.println("XXX results: size = " + results.size());
    }
}
