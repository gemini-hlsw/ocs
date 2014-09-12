//
// $
//

package edu.gemini.shared.skyobject;


import edu.gemini.skycalc.Angle;
import edu.gemini.shared.skyobject.SkyObject.Builder;
import edu.gemini.shared.skyobject.coords.HmsDegCoordinates;
import edu.gemini.shared.skyobject.coords.SkyCoordinates;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.DefaultImList;
import static org.junit.Assert.*;
import org.junit.Test;

import java.util.*;

/**
 *
 */
public class SkyObjectTest {

    private static final SkyCoordinates COORDS =
            (new HmsDegCoordinates.Builder(Angle.ANGLE_0DEGREES, Angle.ANGLE_0DEGREES).build());

    @Test
    public void testBuilderDefaults() {
        Builder b = new Builder("Test", COORDS);
        SkyObject obj = b.build();

        assertEquals("Test", obj.getName());
        assertEquals(COORDS, obj.getCoordinates());
        assertEquals(0, obj.getAttributes().size());
        assertEquals(0, obj.getMagnitudeBands().size());
    }

    @Test
    public void testBuilderNulls() {
        try {
            new Builder(null, COORDS);
            fail();
        } catch (IllegalArgumentException ex) {
            // okay
        }

        try {
            new Builder("Test", null);
            fail();
        } catch (IllegalArgumentException ex) {
            // okay
        }

        Builder b = new Builder("Test", COORDS);

        try {
            b.coordinates(null);
            fail();
        } catch (IllegalArgumentException ex) {
            // okay
        }

        try {
            b.attributes(null);
            fail();
        } catch (NullPointerException ex) {
            // okay
        }

        try {
            b.magnitudes((ImList<Magnitude>)null);
            fail();
        } catch (IllegalArgumentException ex) {
            // okay
        }
    }

    @Test
    public void testUpdateName() {
        // Change the name set in the builder constructor.
        Builder b = new Builder("Test", COORDS).name("Test2");

        // Make sure it builds with the new name.
        SkyObject obj = b.build();
        assertEquals("Test2", obj.getName());

        // Create a new SkyObject with an updated name.
        assertEquals("Test3", obj.withName("Test3").getName());

        // Didn't modify the original object.
        assertEquals("Test2", obj.getName());
    }

    @Test
    public void testUpdateCoordinates() {
        SkyCoordinates COORDS2 =
                (new HmsDegCoordinates.Builder(Angle.ANGLE_PI, Angle.ANGLE_PI).build());

        SkyCoordinates COORDS3 =
                (new HmsDegCoordinates.Builder(Angle.ANGLE_PI_OVER_2, Angle.ANGLE_PI_OVER_2).build());

        // Change the coords set in the builder constructor.
        Builder b = new Builder("Test", COORDS).coordinates(COORDS2);

        // Make sure it builds with the new name.
        SkyObject obj = b.build();
        assertEquals(COORDS2, obj.getCoordinates());

        // Create a new SkyObject with an updated name.
        assertEquals(COORDS3, obj.withCoordinates(COORDS3).getCoordinates());

        // Didn't modify the original object.
        assertEquals(COORDS2, obj.getCoordinates());
    }

    @Test
    public void testAttributes() {
        Map<Object, Object> attrs1 = new HashMap<Object, Object>();
        attrs1.put("attr1", "value1");
        attrs1.put("attr2", "value2");

        Map<Object, Object> attrs2 = new HashMap<Object, Object>();
        attrs2.put("attr1", "newvalue1");
        attrs2.put("attr3", "value3");


        Builder b = new Builder("Test", COORDS).attributes(attrs1);

        // Make sure it builds with the attributes.
        SkyObject obj = b.build();
        assertEquals("value1", obj.getAttribute("attr1").getValue());
        assertEquals("value2", obj.getAttribute("attr2").getValue());
        assertEquals(None.INSTANCE, obj.getAttribute("attr3"));

        // Immutable attributes.
        try {
            obj.getAttributes().put("attr4", "value4");
            fail();
        } catch (UnsupportedOperationException ex) {
            // okay
        }

        // Create a new SkyObject with updated attributes.  Should not modify
        // the original object.
        SkyObject obj2 = obj.withAttributes(attrs2);
        assertEquals("value1", obj.getAttribute("attr1").getValue());
        assertEquals("newvalue1", obj2.getAttribute("attr1").getValue());

        // Create a new SkyObject with a changed attribute value.
        SkyObject obj3 = obj.addAttribute("attr3", "newvalue3");
        assertEquals(None.INSTANCE, obj.getAttribute("attr3"));
        assertEquals("value3",      obj2.getAttribute("attr3").getValue());
        assertEquals("newvalue3",   obj3.getAttribute("attr3").getValue());

        // Share immutable references.  Copy only when necessary.
        SkyObject obj4 = obj.builder().build();
        assertNotSame(obj, obj4);
        assertSame(obj.getAttributes(), obj4.getAttributes());
    }

    @Test
    public void testMagnitudes() {
        Magnitude j  = new Magnitude(Magnitude.Band.J, 1.0);
        Magnitude h1 = new Magnitude(Magnitude.Band.H, 2.0);
        Magnitude h2 = new Magnitude(Magnitude.Band.H, 3.0);
        Magnitude k  = new Magnitude(Magnitude.Band.K, 4.0);

        ImList<Magnitude> mags1 = DefaultImList.create(j, h1);
        Set<Magnitude.Band> bands1 = new HashSet<Magnitude.Band>();
        bands1.add(Magnitude.Band.J);
        bands1.add(Magnitude.Band.H);

        ImList<Magnitude> mags2 = DefaultImList.create(h2, k);
        Set<Magnitude.Band> bands2 = new HashSet<Magnitude.Band>();
        bands2.add(Magnitude.Band.H);
        bands2.add(Magnitude.Band.K);

        Builder b = new Builder("Test", COORDS).magnitudes(mags1);

        // Built with mags1.
        SkyObject obj = b.build();
        assertEquals(mags1, obj.getMagnitudes());
        assertEquals(bands1, obj.getMagnitudeBands());
        assertEquals(j, obj.getMagnitude(Magnitude.Band.J).getValue());
        assertEquals(None.INSTANCE, obj.getMagnitude(Magnitude.Band.K));

        // Create a new SkyObject with updated magnitudes.  Should not modify
        // the original object.
        SkyObject obj2 = obj.withMagnitudes(mags2);
        assertEquals(h1, obj.getMagnitude(Magnitude.Band.H).getValue());
        assertEquals(h2, obj2.getMagnitude(Magnitude.Band.H).getValue());
        assertEquals(bands2, obj2.getMagnitudeBands());

        // Create a new SkyObject with an updated magnitude.
        SkyObject obj3 = obj.addMagnitude(k);
        assertEquals(h1, obj3.getMagnitude(Magnitude.Band.H).getValue());
        assertEquals(k,  obj3.getMagnitude(Magnitude.Band.K).getValue());

        // Share immutable references.  Copy only when necessary.
        SkyObject obj4 = obj.builder().build();
        assertNotSame(obj, obj4);
        assertSame(obj.getMagnitudes(), obj4.getMagnitudes());
    }

}
