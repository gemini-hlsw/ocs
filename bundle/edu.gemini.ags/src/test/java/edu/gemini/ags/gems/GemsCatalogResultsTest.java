package edu.gemini.ags.gems;

import edu.gemini.ags.gems.mascot.Strehl;
import edu.gemini.ags.gems.mascot.MascotProgress;
import edu.gemini.skycalc.Angle;
import edu.gemini.skycalc.Coordinates;
import edu.gemini.skycalc.Offset;
import edu.gemini.shared.skyobject.Magnitude;
import edu.gemini.shared.skyobject.coords.HmsDegCoordinates;
import edu.gemini.shared.skyobject.coords.SkyCoordinates;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.core.*;
import edu.gemini.spModel.gemini.flamingos2.Flamingos2;
import edu.gemini.spModel.gemini.gems.Canopus;
import edu.gemini.spModel.gemini.gems.Gems;
import edu.gemini.spModel.gemini.gems.GemsInstrument;
import edu.gemini.spModel.gemini.gsaoi.Gsaoi;
import edu.gemini.spModel.gemini.gsaoi.GsaoiOdgw;
import edu.gemini.spModel.gemini.obscomp.SPSiteQuality;
import edu.gemini.spModel.gems.GemsTipTiltMode;
import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.obs.context.ObsContext;
import edu.gemini.spModel.obscomp.SPInstObsComp;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.env.GuideGroup;
import edu.gemini.spModel.target.env.TargetEnvironment;
import jsky.catalog.skycat.SkycatConfigFile;
import jsky.coords.WorldCoords;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;
import edu.gemini.shared.util.immutable.*;

import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

/**
 * See OT-27
 */
@Ignore
public class GemsCatalogResultsTest  implements MascotProgress {
    @Before
    public void init() {
        URL url = getClass().getResource("test.skycat.cfg");
        assert(url != null);
        SkycatConfigFile.setConfigFile(url);
    }

    @Test
    public void testGsaoiSearch() throws Exception {
        // Astrometric fields: NGC 6352 C
        // result #1: GemsGuideStars{pa=0.000000 deg, tiptilt=CWFS, avg Strehl=88.5318548480311,
        // guiders=cwfs1[17:25:20.346,-48:28:49.52] cwfs2[17:25:15.606,-48:28:06.57] cwfs3[17:25:19.100,-48:27:26.02]
        // odgw2[17:25:15.676,-48:28:41.96] }

        WorldCoords base = new WorldCoords("17:25:17.633", "-48:28:01.47");
        Gsaoi inst = new Gsaoi();
        List<GemsGuideStars> results = search(inst, base.getRA().toString(), base.getDec().toString(),
                GemsTipTiltMode.canopus);

        assertTrue(!results.isEmpty());
        GemsGuideStars result = results.get(0);
        assertEquals(0.0, result.getPa().toDegrees(), 0.0001);
        assertEquals("CWFS", result.getTiptiltGroup().getKey());

        ObsContext obsContext = ObsContext.create(null, inst, None.<Site>instance(), null, null, null);
        assertEquals(10.589586438901101* GemsCatalogResults.getStrehlFactor(new Some<>(obsContext)),
                result.getStrehl().getAvg(), 0.001);

        GuideGroup group = result.getGuideGroup();
        SortedSet<GuideProbe> set = group.getReferencedGuiders();

        assertTrue(set.contains(Canopus.Wfs.cwfs1));
        assertTrue(set.contains(Canopus.Wfs.cwfs2));
        assertTrue(set.contains(Canopus.Wfs.cwfs3));

        assertTrue(set.contains(GsaoiOdgw.odgw1));
        assertFalse(set.contains(GsaoiOdgw.odgw2));
        assertFalse(set.contains(GsaoiOdgw.odgw3));
        assertFalse(set.contains(GsaoiOdgw.odgw4));

        Coordinates cwfs1 = group.get(Canopus.Wfs.cwfs1).getValue().getPrimary().getValue().getSkycalcCoordinates();
        Coordinates cwfs2 = group.get(Canopus.Wfs.cwfs2).getValue().getPrimary().getValue().getSkycalcCoordinates();
        Coordinates cwfs3 = group.get(Canopus.Wfs.cwfs3).getValue().getPrimary().getValue().getSkycalcCoordinates();
        Coordinates odgw1 = group.get(GsaoiOdgw.odgw1).getValue().getPrimary().getValue().getSkycalcCoordinates();

        Coordinates cwfs1x = Coordinates.create("17:25:20.057", "-48:27:39.99");
        Coordinates cwfs2x = Coordinates.create("17:25:20.321", "-48:28:47.20");
        Coordinates cwfs3x = Coordinates.create("17:25:16.940", "-48:27:29.76");
        Coordinates odgw1x = Coordinates.create("17:25:20.321", "-48:28:47.20");

        assertEquals(cwfs1x.getRaDeg(), cwfs1.getRaDeg(), 0.001);
        assertEquals(cwfs1x.getDecDeg(), cwfs1.getDecDeg(), 0.001);

        assertEquals(cwfs2x.getRaDeg(), cwfs2.getRaDeg(), 0.001);
        assertEquals(cwfs2x.getDecDeg(), cwfs2.getDecDeg(), 0.001);

        assertEquals(cwfs3x.getRaDeg(), cwfs3.getRaDeg(), 0.001);
        assertEquals(cwfs3x.getDecDeg(), cwfs3.getDecDeg(), 0.001);

        assertEquals(odgw1x.getRaDeg(), odgw1.getRaDeg(), 0.001);
        assertEquals(odgw1x.getDecDeg(), odgw1.getDecDeg(), 0.001);

        double cwfs1Mag = group.get(Canopus.Wfs.cwfs1).getValue().getPrimary().getValue().getMagnitude(Magnitude.Band.R).getValue().getBrightness();
        double cwfs2Mag = group.get(Canopus.Wfs.cwfs2).getValue().getPrimary().getValue().getMagnitude(Magnitude.Band.R).getValue().getBrightness();
        double cwfs3Mag = group.get(Canopus.Wfs.cwfs3).getValue().getPrimary().getValue().getMagnitude(Magnitude.Band.R).getValue().getBrightness();
        assertTrue(cwfs3Mag < cwfs1Mag && cwfs3Mag < cwfs2Mag); // cwfs3 is brightest

    }


// XXX TODO: Flamingos not supported yet
//    @Test
//    public void testFlamingosSearch() throws Exception {
//        // Astrometric fields: NGC 6352 C
//        // result #1: GemsGuideStars{pa=0.000000 deg, tiptilt=CWFS, avg Strehl=88.5318548480311,
//        // guiders=cwfs1[17:25:20.346,-48:28:49.52] cwfs2[17:25:15.606,-48:28:06.57] cwfs3[17:25:19.100,-48:27:26.02]
//        // FII OIWFS[17:25:23.959,-48:26:13.18] }
//
//        Flamingos2 inst = new Flamingos2();
//        List<GemsGuideStars> results = search(inst, "17:25:17.633", "-48:28:01.47", GemsTipTiltMode.both);
//        assertTrue(!results.isEmpty());
//        GemsGuideStars result = results.get(0);
//        assertEquals(0.0, result.getPa().toDegrees().getMagnitude(), 0.0001);
//        assertEquals("CWFS", result.getTiptiltGroup().getKey());
//        assertEquals(88.5318548480311*GemsCatalogResults.getStrehlFactor(inst), result.getStrehl().getAvg()*100, 0.0001);
//
//        GuideGroup group = result.getGuideGroup();
//        SortedSet<GuideProbe> set = group.getReferencedGuiders();
//
//        assertTrue(set.contains(Canopus.Wfs.cwfs1));
//        assertTrue(set.contains(Canopus.Wfs.cwfs2));
//        assertTrue(set.contains(Canopus.Wfs.cwfs3));
//        assertTrue(set.contains(Flamingos2OiwfsGuideProbe.instance));
//
//        Coordinates cwfs1 = group.get(Canopus.Wfs.cwfs1).getValue().getPrimary().getValue().getSkycalcCoordinates();
//        Coordinates cwfs2 = group.get(Canopus.Wfs.cwfs2).getValue().getPrimary().getValue().getSkycalcCoordinates();
//        Coordinates cwfs3 = group.get(Canopus.Wfs.cwfs3).getValue().getPrimary().getValue().getSkycalcCoordinates();
//        Coordinates oiwfs = group.get(Flamingos2OiwfsGuideProbe.instance).getValue().getPrimary().getValue().getSkycalcCoordinates();
//
//        Coordinates cwfs1x = Coordinates.create("17:25:20.346", "-48:28:49.52");
//        Coordinates cwfs2x = Coordinates.create("17:25:15.606", "-48:28:06.57");
//        Coordinates cwfs3x = Coordinates.create("17:25:19.100", "-48:27:26.02");
//        Coordinates oiwfsx = Coordinates.create("17:25:23.959", "-48:26:13.18");
//
//        assertEquals(cwfs1x.getRaDeg(), cwfs1.getRaDeg(), 0.001);
//        assertEquals(cwfs1x.getDecDeg(), cwfs1.getDecDeg(), 0.001);
//
//        assertEquals(cwfs2x.getRaDeg(), cwfs2.getRaDeg(), 0.001);
//        assertEquals(cwfs2x.getDecDeg(), cwfs2.getDecDeg(), 0.001);
//
//        assertEquals(cwfs3x.getRaDeg(), cwfs3.getRaDeg(), 0.001);
//        assertEquals(cwfs3x.getDecDeg(), cwfs3.getDecDeg(), 0.001);
//
//        assertEquals(oiwfsx.getRaDeg(), oiwfs.getRaDeg(), 0.001);
//        assertEquals(oiwfsx.getDecDeg(), oiwfs.getDecDeg(), 0.001);
//
//        double cwfs1Mag = group.get(Canopus.Wfs.cwfs1).getValue().getPrimary().getValue().getMagnitude(Magnitude.Band.R).getValue().getBrightness();
//        double cwfs2Mag = group.get(Canopus.Wfs.cwfs2).getValue().getPrimary().getValue().getMagnitude(Magnitude.Band.R).getValue().getBrightness();
//        double cwfs3Mag = group.get(Canopus.Wfs.cwfs3).getValue().getPrimary().getValue().getMagnitude(Magnitude.Band.R).getValue().getBrightness();
//        assertTrue(cwfs3Mag < cwfs1Mag && cwfs3Mag < cwfs2Mag); // cwfs3 is brightest
//    }

    private List<GemsGuideStars> search(SPInstObsComp inst, String raStr, String decStr, GemsTipTiltMode tipTiltMode)
            throws Exception {
        WorldCoords coords = new WorldCoords(raStr, decStr);
        SPTarget baseTarget = new SPTarget(coords.getRaDeg(), coords.getDecDeg());
        TargetEnvironment env = TargetEnvironment.create(baseTarget);
        Set<Offset> offsets = new HashSet<Offset>();
        ObsContext obsContext = ObsContext.create(env, inst, None.<Site>instance(), SPSiteQuality.Conditions.BEST, offsets, new Gems());

        Angle baseRA = new Angle(coords.getRaDeg(), Angle.Unit.DEGREES);
        Angle baseDec = new Angle(coords.getDecDeg(), Angle.Unit.DEGREES);
        SkyCoordinates base = new HmsDegCoordinates.Builder(baseRA, baseDec).build();

        String opticalCatalog = GemsGuideStarSearchOptions.DEFAULT_CATALOG;
        String nirCatalog = GemsGuideStarSearchOptions.DEFAULT_CATALOG;
        GemsInstrument instrument = inst instanceof Flamingos2 ? GemsInstrument.flamingos2 : GemsInstrument.gsaoi;

        Set<Angle> posAngles = new HashSet<>();
        posAngles.add(obsContext.getPositionAngle());
        posAngles.add(new Angle(0., Angle.Unit.DEGREES));
//        posAngles.add(new Angle(90., Angle.Unit.DEGREES));
//        posAngles.add(new Angle(180., Angle.Unit.DEGREES));
//        posAngles.add(new Angle(270., Angle.Unit.DEGREES));

        GemsGuideStarSearchOptions options = new GemsGuideStarSearchOptions(opticalCatalog, nirCatalog,
                instrument, tipTiltMode, null);

        List<GemsCatalogSearchResults> results = new GemsCatalog().search(obsContext, base, options, None.<Magnitude.Band>instance(), null);
        if (options.getTipTiltMode() == GemsTipTiltMode.both) {
            assertEquals(4, results.size());
        } else {
            assertEquals(2, results.size());
        }
        int i = 0;
        for(GemsCatalogSearchResults result : results) {
            i++;
            System.out.println("Result #" + i);
            System.out.println(" Criteria:" + result.criterion());
            System.out.println(" Results size:" + result.results().size());
        }

        List<GemsGuideStars> gemsResults = new GemsCatalogResults().analyze(obsContext, posAngles, results, null);
        System.out.println("gems results: size = " + gemsResults.size());

        return gemsResults;
    }

    @Override
    public boolean progress(Strehl s, int count, int total, boolean usable) {
        return true;
    }

    @Override
    public void setProgressTitle(String s) {
        System.out.println(s);
    }
}
