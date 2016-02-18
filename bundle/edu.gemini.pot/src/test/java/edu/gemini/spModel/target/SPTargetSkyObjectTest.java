//
// $
//

package edu.gemini.spModel.target;

import edu.gemini.shared.util.immutable.None;
import edu.gemini.skycalc.Angle;
import edu.gemini.shared.skyobject.Magnitude;
import edu.gemini.shared.skyobject.SkyObject;
import edu.gemini.shared.skyobject.coords.HmsDegCoordinates;
import edu.gemini.shared.skyobject.coords.HmsDegCoordinates.Epoch;
import static edu.gemini.shared.skyobject.coords.HmsDegCoordinates.Epoch.Type.JULIAN;
import edu.gemini.shared.util.immutable.ApplyOp;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.shared.util.immutable.DefaultImList;
import edu.gemini.spModel.target.system.CoordinateParam;
import edu.gemini.spModel.target.system.CoordinateTypes;
import edu.gemini.spModel.target.system.HmsDegTarget;
import static org.junit.Assert.*;
import org.junit.Test;

import java.util.Set;

/**
 * {@link SPTarget} tests related to SkyObject.  Added very late in the SPTarget
 * lifecycle, we are only testing code related to the new addition of
 * {@link SkyObject} and {@link Magnitude}.
 */
public final class SPTargetSkyObjectTest {
    private static final Epoch[] EPOCHS = {
            Epoch.J2000,
            new Epoch(JULIAN, 2000.5),
    };

    private static final Magnitude magJ1 = new Magnitude(Magnitude.Band.J, 1);
    private static final Magnitude magK2 = new Magnitude(Magnitude.Band.K, 2);
    private static final Magnitude magJ3 = new Magnitude(Magnitude.Band.J, 3);


    private static HmsDegCoordinates createCoords(double ra, double dec, Epoch epoch) {
        Angle raAngle  = new Angle(ra, Angle.Unit.DEGREES);
        Angle decAngle = new Angle(dec, Angle.Unit.DEGREES);
        return new HmsDegCoordinates.Builder(raAngle, decAngle).epoch(epoch).build();
    }

    @Test
    public void testExtractSkyObjectCoords() throws Exception {

        Angle pmRa  = new Angle(1, Angle.Unit.MILLIARCSECS);
        Angle pmDec = new Angle(2, Angle.Unit.MILLIARCSECS);


        for (Epoch e : EPOCHS) {

            HmsDegCoordinates coords = createCoords(15.0, 20.0, e);
            coords = coords.builder().pmRa(pmRa).pmDec(pmDec).build();

            SkyObject   obj = new SkyObject.Builder("xyz", coords).build();
            SPTarget target = new SPTarget(HmsDegTarget.fromSkyObject(obj));

            assertEquals(0, target.getTarget().getMagnitudes().size());
            assertEquals(0, target.getTarget().getMagnitudeBands().size());

            HmsDegTarget hmsDeg = (HmsDegTarget) target.getTarget();

            assertEquals("xyz", target.getTarget().getName());
            assertEquals(15.0, target.getTarget().getRaDegrees(None.instance()).getValue(), 0.000001);
            assertEquals(20.0, target.getTarget().getDecDegrees(None.instance()).getValue(), 0.000001);
            assertEquals(1.0, hmsDeg.getPM1().getValue(), 0.000001);
            assertEquals(2.0, hmsDeg.getPM2().getValue(), 0.000001);

            CoordinateTypes.Epoch spEpoch = hmsDeg.getEpoch();
            assertEquals(e.getYear(), spEpoch.getValue(), 0.000001);

        }
    }

    private SkyObject createSkyObject(ImList<Magnitude> magList) {
        HmsDegCoordinates coords = createCoords(15.0, 20.0, Epoch.J2000);
        return new SkyObject.Builder("xyz", coords).magnitudes(magList).build();
    }

    private void testSkyObjectConstructorMags(ImList<Magnitude> input, ImList<Magnitude> expected) throws Exception {
        SkyObject obj = createSkyObject(input);
        final SPTarget target = new SPTarget(HmsDegTarget.fromSkyObject(obj));

        final ImList<Magnitude> mags = target.getTarget().getMagnitudes();
        assertEquals(expected.size(), mags.size());

        final Set<Magnitude.Band> bands = target.getTarget().getMagnitudeBands();
        assertEquals(expected.size(), bands.size());
        expected.foreach(new ApplyOp<Magnitude>() {
            @Override public void apply(Magnitude magnitude) {
                assertTrue(bands.contains(magnitude.getBand()));
                assertEquals(magnitude, target.getMagnitude(magnitude.getBand()).getValue());
            }
        });

        // okay don't call this method with "M" in the list of expected or input
        assertTrue(target.getMagnitude(Magnitude.Band.M).isEmpty());
    }

    @Test
    public void testMagnitudeInfoInSkyObjectConstructor() throws Exception {
        ImList<Magnitude> input = DefaultImList.create(magJ1, magK2);
        testSkyObjectConstructorMags(input, input);
    }

    @Test
    public void testFilterDuplicateMagnitudes() throws Exception {
        ImList<Magnitude> input    = DefaultImList.create(magJ1, magK2, magJ3);
        ImList<Magnitude> expected = DefaultImList.create(magJ1, magK2);
        testSkyObjectConstructorMags(input, expected);
    }

    @Test
    public void testPutMagnitude() throws Exception {
        SkyObject   obj = createSkyObject(DefaultImList.create(magJ1));
        SPTarget target = new SPTarget(HmsDegTarget.fromSkyObject(obj));

        // Put a new magnitude for the K band.
        target.putMagnitude(magK2);
        ImList<Magnitude> magList = target.getTarget().getMagnitudes();

        assertEquals(2, magList.size());
        assertEquals(magJ1, target.getMagnitude(Magnitude.Band.J).getValue());
        assertEquals(magK2, target.getMagnitude(Magnitude.Band.K).getValue());

        // Replace the existing J band mag with a new one.
        target.putMagnitude(magJ3);
        magList = target.getTarget().getMagnitudes();
        assertEquals(2, magList.size());
        assertEquals(magJ3, target.getMagnitude(Magnitude.Band.J).getValue());
        assertEquals(magK2, target.getMagnitude(Magnitude.Band.K).getValue());
    }

    @Test
    public void testSetMagnitudes() throws Exception {
        SkyObject   obj = createSkyObject(DefaultImList.create(magJ1));
        SPTarget target = new SPTarget(HmsDegTarget.fromSkyObject(obj));

        // Put a new magnitude for the K band.
        target.setMagnitudes(DefaultImList.create(magJ3, magK2));
        ImList<Magnitude> magList = target.getTarget().getMagnitudes();

        assertEquals(2, magList.size());
        assertEquals(magJ3, target.getMagnitude(Magnitude.Band.J).getValue());
        assertEquals(magK2, target.getMagnitude(Magnitude.Band.K).getValue());
    }
}
