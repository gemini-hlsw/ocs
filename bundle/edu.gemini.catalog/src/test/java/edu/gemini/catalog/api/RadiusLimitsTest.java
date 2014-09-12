package edu.gemini.catalog.api;


import edu.gemini.skycalc.Angle;
import static edu.gemini.skycalc.Angle.Unit.ARCMINS;
import edu.gemini.skycalc.Coordinates;
import edu.gemini.shared.skyobject.SkyObject;
import edu.gemini.shared.skyobject.coords.HmsDegCoordinates;
import edu.gemini.shared.skyobject.coords.SkyCoordinates;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import edu.gemini.shared.util.immutable.*;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class RadiusLimitsTest {
    public static final Angle min = new Angle(10.0, ARCMINS);
    public static final Angle max = new Angle(20.0, ARCMINS);

    private static final MapOp<SkyObject, String> TO_NAME = new MapOp<SkyObject, String>() {
        @Override public String apply(SkyObject skyObject) {
            return skyObject.getName();
        }
    };

    @Test public void testFilter0_0() {
        Coordinates base = new Coordinates(0, 0);
        RadiusLimits rad = new RadiusLimits(max, min);

        Angle outMaxNeg = new Angle(-20.1, ARCMINS);
        Angle outMaxPos = new Angle( 20.1, ARCMINS);

        Angle brdMaxNeg = new Angle(-19.99999, ARCMINS);
        Angle brdMaxPos = new Angle( 19.99999, ARCMINS);

        Angle inMaxNeg  = new Angle(-19.9, ARCMINS);
        Angle inMaxPos  = new Angle( 19.9, ARCMINS);


        Angle outMinNeg = new Angle( -9.9, ARCMINS);
        Angle outMinPos = new Angle(  9.9, ARCMINS);

        Angle brdMinNeg = new Angle(-10.0001, ARCMINS);
        Angle brdMinPos = new Angle( 10.0001, ARCMINS);

        Angle inMinNeg  = new Angle(-10.1, ARCMINS);
        Angle inMinPos  = new Angle( 10.1, ARCMINS);

        ImList<Angle> out = DefaultImList.create(outMaxNeg, outMaxPos, outMinNeg, outMinPos);
        ImList<Angle>  in = DefaultImList.create(brdMaxNeg, brdMaxPos, inMaxNeg, inMaxPos, brdMinNeg, brdMinPos, inMinNeg, inMinPos);
        PredicateOp<SkyObject> f = rad.skyObjectFilter(base);

//        System.out.println(ras(in, Angle.ANGLE_0DEGREES).filter(f).map(TO_NAME).mkString("", "\n", ""));

        // cardinal positions near the borders
        assertEquals(0, ras(out, Angle.ANGLE_0DEGREES).filter(f).size());
        assertEquals(0, decs(Angle.ANGLE_0DEGREES, out).filter(f).size());
        assertEquals(8, ras(in, Angle.ANGLE_0DEGREES).filter(f).size());
        assertEquals(8, decs(Angle.ANGLE_0DEGREES, in).filter(f).size());

        // in the middle of the limits along the diagonals
        ImList<Angle> mins = DefaultImList.create(outMinNeg, outMinPos, brdMinNeg, brdMinPos, inMinNeg, inMinPos);
        assertEquals(36, perms(mins).filter(f).size());

        // out of the max limits along the diagonals
        ImList<Angle> maxs = DefaultImList.create(outMaxNeg, outMaxPos, brdMaxNeg, brdMaxPos, inMaxNeg, inMaxPos);
        assertEquals(0,  perms(maxs).filter(f).size());
    }

    @Test public void testFilter0_90() {
        Coordinates base = new Coordinates(0, 90);
        RadiusLimits rad = new RadiusLimits(max, min);

        PredicateOp<SkyObject> f = rad.skyObjectFilter(base);

        // Since the base is at the pole, the RA is really irrelevant.
        Angle   ra = new Angle(180, Angle.Unit.DEGREES);

        Angle deg90 = new Angle(90, Angle.Unit.DEGREES);
        Angle dec9  = deg90.add( -9, ARCMINS);
        Angle dec11 = deg90.add(-11, ARCMINS);
        Angle dec19 = deg90.add(-19, ARCMINS);
        Angle dec21 = deg90.add(-21, ARCMINS);

        assertFalse(f.apply(mkSkyObject(ra, dec9)));
        assertTrue(f.apply(mkSkyObject(ra, dec11)));
        assertTrue(f.apply(mkSkyObject(ra, dec19)));
        assertFalse(f.apply(mkSkyObject(ra, dec21)));
    }

    private ImList<SkyObject> ras(ImList<Angle> ras, Angle dec) {
        List<SkyObject> res = new ArrayList<SkyObject>();
        for (Angle ra : ras) res.add(mkSkyObject(ra, dec));
        return DefaultImList.create(res);
    }

    private ImList<SkyObject> decs(Angle ra, ImList<Angle> decs) {
        List<SkyObject> res = new ArrayList<SkyObject>();
        for (Angle dec : decs) res.add(mkSkyObject(ra, dec));
        return DefaultImList.create(res);
    }

    private ImList<SkyObject> perms(ImList<Angle> a) {
        List<SkyObject> res = new ArrayList<SkyObject>();
        for (Angle ra : a) for (Angle dec : a) res.add(mkSkyObject(ra, dec));
        return DefaultImList.create(res);
    }

    private String name(Angle ra, Angle dec) {
        return "ra=" + ra.toArcmins().getMagnitude() + ",dec=" + dec.toArcmins().getMagnitude();
    }

    private SkyObject mkSkyObject(Angle ra, Angle dec) {
        return mkSkyObject(name(ra, dec), new Coordinates(ra, dec));
    }

    private SkyObject mkSkyObject(String name, Coordinates c) {
        SkyCoordinates sc = new HmsDegCoordinates.Builder(c.getRa(), c.getDec()).build();
        return new SkyObject.Builder(name, sc).build();
    }
}
