package edu.gemini.ags.gems;

import static edu.gemini.shared.skyobject.Magnitude.Band;

import edu.gemini.catalog.api.MagnitudeLimits;
import edu.gemini.catalog.api.MagnitudeLimits.FaintnessLimit;
import edu.gemini.catalog.api.MagnitudeLimits.SaturationLimit;
import edu.gemini.catalog.api.RadiusLimits;
import edu.gemini.skycalc.Angle;
import edu.gemini.skycalc.Offset;
import edu.gemini.shared.skyobject.Magnitude;
import edu.gemini.shared.skyobject.SkyObject;
import edu.gemini.shared.skyobject.coords.HmsDegCoordinates;
import edu.gemini.shared.skyobject.coords.SkyCoordinates;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import edu.gemini.shared.util.immutable.Some;
import org.junit.Test;

/**
 */
public class CatalogSearchCriterionTest {
    private final MagnitudeLimits magLimits = new MagnitudeLimits(Band.J, new FaintnessLimit(10.), new SaturationLimit(2.));

    @Test
    public void testSimpleSearch() {
        String name = "test";
        RadiusLimits radiusLimits = new RadiusLimits(new Angle(10., Angle.Unit.ARCMINS), new Angle(2., Angle.Unit.ARCMINS));
        Option<Offset> offset = None.instance();
        Option<Angle> posAngle = None.instance();
        CatalogSearchCriterion crit = new CatalogSearchCriterion("test", magLimits, radiusLimits,
                offset, posAngle);
        Angle baseRA = new Angle(10., Angle.Unit.DEGREES);
        Angle baseDec = new Angle(15., Angle.Unit.DEGREES);
        SkyCoordinates base = new HmsDegCoordinates.Builder(baseRA, baseDec).build();
        CatalogSearchCriterion.Matcher matcher = crit.matcher(base);

        // base pos is not in radius limit
        assertFalse(matcher.matches(
                new SkyObject.Builder("testObj", new HmsDegCoordinates.Builder(baseRA, baseDec).build())
                        .magnitudes(new Magnitude(Band.J, 3.)).build()));


        // Should be in mag and radius limit
        Angle ra = baseRA;
        Angle dec = baseDec.add(new Angle(3.0, Angle.Unit.ARCMINS));
        SkyCoordinates pos = new HmsDegCoordinates.Builder(ra, dec).build();
        assertTrue(matcher.matches(
                new SkyObject.Builder("testObj", new HmsDegCoordinates.Builder(ra, dec).build())
                        .magnitudes(new Magnitude(Band.J, 3.)).build()));

        // mag out of range
        assertFalse(matcher.matches(
                new SkyObject.Builder("testObj", new HmsDegCoordinates.Builder(ra, dec).build())
                        .magnitudes(new Magnitude(Band.J, 11.)).build()));
    }

    @Test
    public void testOffsetSearch() {
        String name = "test";
        RadiusLimits radiusLimits = new RadiusLimits(new Angle(10., Angle.Unit.ARCMINS), new Angle(1., Angle.Unit.ARCMINS));
        Option<Offset> offset = new Some<Offset>(new Offset(new Angle(1, Angle.Unit.ARCMINS), new Angle(1, Angle.Unit.ARCMINS)));
        Option<Angle> posAngle = None.instance();
        CatalogSearchCriterion crit = new CatalogSearchCriterion("test", magLimits, radiusLimits,
                offset, posAngle);
        Angle baseRA = new Angle(10., Angle.Unit.DEGREES);
        Angle baseDec = new Angle(15., Angle.Unit.DEGREES);
        SkyCoordinates base = new HmsDegCoordinates.Builder(baseRA, baseDec).build();
        CatalogSearchCriterion.Matcher matcher = crit.matcher(base);

        // base pos is in radius limit with offset
        assertTrue(matcher.matches(
                new SkyObject.Builder("testObj", new HmsDegCoordinates.Builder(baseRA, baseDec).build())
                        .magnitudes(new Magnitude(Band.J, 3.)).build()));


        // Should be in mag and radius limit
        Angle ra = baseRA;
        Angle dec = baseDec.add(new Angle(11.0, Angle.Unit.ARCMINS));
        SkyCoordinates pos = new HmsDegCoordinates.Builder(ra, dec).build();
        assertTrue(matcher.matches(
                new SkyObject.Builder("testObj", new HmsDegCoordinates.Builder(ra, dec).build())
                        .magnitudes(new Magnitude(Band.J, 3.)).build()));

        // Should be in mag and radius limit
        ra = baseRA.add(new Angle(11.0, Angle.Unit.ARCMINS));
        dec = baseDec;
        pos = new HmsDegCoordinates.Builder(ra, dec).build();
        assertTrue(matcher.matches(
                new SkyObject.Builder("testObj", new HmsDegCoordinates.Builder(ra, dec).build())
                        .magnitudes(new Magnitude(Band.J, 3.)).build()));
    }


    @Test
    public void testOffsetPosAngleSearch() {
        String name = "test";
        RadiusLimits radiusLimits = new RadiusLimits(new Angle(10., Angle.Unit.ARCMINS), new Angle(2., Angle.Unit.ARCMINS));
        Option<Offset> offset = new Some<Offset>(new Offset(new Angle(1, Angle.Unit.ARCMINS), new Angle(1, Angle.Unit.ARCMINS)));
        Option<Angle> posAngle = new Some<Angle>(new Angle(90., Angle.Unit.DEGREES));
        CatalogSearchCriterion crit = new CatalogSearchCriterion("test", magLimits, radiusLimits,
                offset, posAngle);
        Angle baseRA = new Angle(10., Angle.Unit.DEGREES);
        Angle baseDec = new Angle(15., Angle.Unit.DEGREES);
        SkyCoordinates base = new HmsDegCoordinates.Builder(baseRA, baseDec).build();
        CatalogSearchCriterion.Matcher matcher = crit.matcher(base);

        // base pos is not in radius limit
        assertFalse(matcher.matches(
                new SkyObject.Builder("testObj", new HmsDegCoordinates.Builder(baseRA, baseDec).build())
                        .magnitudes(new Magnitude(Band.J, 3.)).build()));


        // Should not be in radius limit
        Angle ra = baseRA;
        Angle dec = baseDec.add(new Angle(11.0, Angle.Unit.ARCMINS));
        SkyCoordinates pos = new HmsDegCoordinates.Builder(ra, dec).build();
        assertFalse(matcher.matches(
                new SkyObject.Builder("testObj", new HmsDegCoordinates.Builder(ra, dec).build())
                        .magnitudes(new Magnitude(Band.J, 3.)).build()));

        // Should be in mag and radius limit
        ra = baseRA.add(new Angle(11.0, Angle.Unit.ARCMINS));
        dec = baseDec;
        pos = new HmsDegCoordinates.Builder(ra, dec).build();
        assertTrue(matcher.matches(
                new SkyObject.Builder("testObj", new HmsDegCoordinates.Builder(ra, dec).build())
                        .magnitudes(new Magnitude(Band.J, 3.)).build()));
    }

}
