package edu.gemini.wdba.tcc;

import edu.gemini.shared.util.immutable.Pair;
import edu.gemini.spModel.gemini.flamingos2.Flamingos2;
import edu.gemini.spModel.telescope.IssPort;
import org.junit.Test;
import scala.actors.threadpool.Arrays;

import static edu.gemini.spModel.gemini.flamingos2.Flamingos2.Disperser;
import static edu.gemini.spModel.gemini.flamingos2.Flamingos2.Filter;

/**
 * Test cases for {@link Flamingos2Support}.
 */
public final class Flamingos2SupportTest extends InstrumentSupportTestBase<Flamingos2> {

    public Flamingos2SupportTest() {
        super(Flamingos2.SP_TYPE);
    }

    public void testF2_SIDE() throws Exception {
        final Flamingos2 flam2 = getInstrument();
        flam2.setIssPort(IssPort.SIDE_LOOKING);
        assertEquals(flam2.getDisperser(), Flamingos2.Disperser.NONE);
        setInstrument(flam2);

        verifyInstrumentConfig(getSouthResults(), "F25");
    }

    public void testF2_UP() throws Exception {
        final Flamingos2 flam2 = getInstrument();
        flam2.setIssPort(IssPort.UP_LOOKING);
        assertEquals(flam2.getDisperser(), Flamingos2.Disperser.NONE);
        setInstrument(flam2);

        verifyInstrumentConfig(getSouthResults(), "F2");
    }

    public void testF2_SIDE_SPEC() throws Exception {
        final Flamingos2 flam2 = getInstrument();
        flam2.setIssPort(IssPort.SIDE_LOOKING);
        flam2.setDisperser(Flamingos2.Disperser.R3000);
        setInstrument(flam2);

//        verifyInstrumentConfig(getSouthResults(), "F25_SPEC");
        verifyInstrumentConfig(getSouthResults(), "F25");
    }

    public void testF2_UP_SPEC() throws Exception {
        final Flamingos2 flam2 = getInstrument();
        flam2.setIssPort(IssPort.UP_LOOKING);
        flam2.setDisperser(Flamingos2.Disperser.R3000);
        setInstrument(flam2);

//        verifyInstrumentConfig(getSouthResults(), "F2_SPEC");
        verifyInstrumentConfig(getSouthResults(), "F2");
    }

    public void testWavelength() throws Exception {
        final Flamingos2 flam2 = getInstrument();
        flam2.setFilter(Filter.OPEN);
        flam2.setDisperser(Disperser.NONE);
        setInstrument(flam2);

        // Use imaging mode wavelength if no disperser and no filter.
        assertEquals("1.6", getWavelength(getSouthResults()));

        // Use the disperser wavelength in spectroscopy mode with no filter
        flam2.setFilter(Filter.OPEN);

        final Pair[] dtA = new Pair[] {
            new Pair<>(Disperser.R1200JH, "1.39"),
            new Pair<>(Disperser.R1200HK, "1.871"),
            new Pair<>(Disperser.R3000,   "1.65"),
        };

        //noinspection unchecked
        for (final Pair<Disperser, String> t : (Pair<Disperser, String>[]) dtA ) {
            flam2.setDisperser(t._1());
            setInstrument(flam2);
            assertEquals(t._2(), getWavelength(getSouthResults()));
        }

        // Use the filter wavelength even when in spectroscopy mode when the
        // filter is specified.
        flam2.setDisperser(Disperser.R3000);

        final Pair[] ftA = new Pair[] {
            new Pair<>(Filter.Y,       "1.02"),
            new Pair<>(Filter.J_LOW,   "1.15"),
            new Pair<>(Filter.J,       "1.25"),
            new Pair<>(Filter.H,       "1.65"),
            new Pair<>(Filter.K_LONG,  "2.0" ),
            new Pair<>(Filter.K_SHORT, "2.15"),
            new Pair<>(Filter.JH,      "1.39"),
            new Pair<>(Filter.HK,      "1.871"),
        };

        //noinspection unchecked
        for (final Pair<Filter, String> t : (Pair<Filter, String>[]) ftA ) {
            flam2.setFilter(t._1());
            setInstrument(flam2);
            assertEquals(t._2(), getWavelength(getSouthResults()));
        }

        // Use the filter when in imaging mode.
        flam2.setDisperser(Disperser.NONE);

        //noinspection unchecked
        for (final Pair<Filter, String> t : (Pair<Filter, String>[]) ftA ) {
            flam2.setFilter(t._1());
            setInstrument(flam2);
            assertEquals(t._2(), getWavelength(getSouthResults()));
        }


        // For Darks, anything goes but make sure it doesn't crash.
        flam2.setFilter(Filter.DARK);
        setInstrument(flam2);
        try {
            Double.parseDouble(getWavelength(getSouthResults()));
        } catch (Exception ex) {
            fail("dark wavelength not set");
        }
    }

    @Test public void testNoAoPointOrig() throws Exception {
        verifyPointOrig(getSouthResults(), "f2");
    }

    @Test public void testLgsPointOrig() throws Exception {
        addGems();
        verifyPointOrig(getSouthResults(), "lgs2f2");
    }

}
