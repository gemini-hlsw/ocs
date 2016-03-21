//package edu.gemini.shared.skyobject;
//
//import edu.gemini.shared.util.immutable.None;
//import edu.gemini.shared.util.immutable.Some;
//import edu.gemini.spModel.core.MagnitudeSystem;
//import org.junit.Test;
//
//import static edu.gemini.shared.skyobject.Magnitude.Band;
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.fail;
//
//
///**
// *
// */
//public class MagnitudeTest {
//    private static final double DELTA = 0.000001;
//
//    @Test
//    public void testConstructionWithoutError() {
//        Magnitude m = new Magnitude(Band.J, 1.0);
//        assertEquals(Band.J, m.getBand());
//        assertEquals(1.0, m.getBrightness(), DELTA);
//        assertEquals(None.INSTANCE, m.getError());
//        assertEquals(MagnitudeSystem.Default(), m.getSystem());
//
//        try {
//            new Magnitude(null, 1.0);
//            fail();
//        } catch (RuntimeException ex) {
//            // expected
//        }
//    }
//
//    @Test
//    public void testConstructionWithError() {
//        Magnitude m = new Magnitude(Band.J, 1.0, 0.1);
//        assertEquals(Band.J, m.getBand());
//        assertEquals(1.0, m.getBrightness(), DELTA);
//        assertEquals(0.1, m.getError().getValue(), DELTA);
//    }
//
//    @Test
//    public void testConstructionWithSystem() {
//        Magnitude m = new Magnitude(Band.J, 1.0, 0.1, MagnitudeSystem.Jy$.MODULE$);
//        assertEquals(Band.J, m.getBand());
//        assertEquals(1.0, m.getBrightness(), DELTA);
//        assertEquals(0.1, m.getError().getValue(), DELTA);
//        assertEquals(MagnitudeSystem.Jy$.MODULE$, m.getSystem());
//    }
//
//    @Test
//    public void testConstructionWithOptionalError() {
//        @SuppressWarnings({"unchecked"}) Magnitude m = new Magnitude(Band.J, 1.0, None.INSTANCE);
//        assertEquals(Band.J, m.getBand());
//        assertEquals(1.0, m.getBrightness(), DELTA);
//        assertEquals(None.INSTANCE, m.getError());
//
//        m = new Magnitude(Band.J, 1.0, new Some<>(0.1));
//        assertEquals(Band.J, m.getBand());
//        assertEquals(1.0, m.getBrightness(), DELTA);
//        assertEquals(0.1, m.getError().getValue(), DELTA);
//
//        try {
//            new Magnitude(null, 1.0, new Some<>(0.1));
//            fail();
//        } catch (RuntimeException ex) {
//            // expected
//        }
//
//        try {
//            new Magnitude(Band.J, 1.0, None.INSTANCE, null);
//            fail();
//        } catch (RuntimeException ex) {
//            // expected
//        }
//    }
//
//    @Test
//    public void testComparable() {
//        Magnitude m = new Magnitude(Band.H, 1.0, 0.1);
//
//        assertEquals( 0, m.compareTo(new Magnitude(Band.H, 1.0, 0.1)));
//
//        // Bands sort by letter.
//        assertEquals(-1, m.compareTo(new Magnitude(Band.K, 1.0, 0.1)));
//        assertEquals( 1, m.compareTo(new Magnitude(Band.J, 1.0, 0.1)));
//
//        // Brigher objects come first.
//        assertEquals(-1, m.compareTo(new Magnitude(Band.H, 2.0, 0.1)));
//        assertEquals( 1, m.compareTo(new Magnitude(Band.H, 0.0, 0.1)));
//
//        // Smaller error comes first.
//        assertEquals(-1, m.compareTo(new Magnitude(Band.H, 1.0, 0.2)));
//        assertEquals( 1, m.compareTo(new Magnitude(Band.H, 1.0, 0.0)));
//    }
//}
