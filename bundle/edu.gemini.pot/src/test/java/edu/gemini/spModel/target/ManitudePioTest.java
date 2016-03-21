package edu.gemini.spModel.target;

import edu.gemini.shared.util.immutable.*;
import edu.gemini.spModel.core.Magnitude;
import edu.gemini.spModel.core.MagnitudeBand;
import edu.gemini.spModel.core.MagnitudeSystem;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.pio.xml.PioXmlFactory;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import org.junit.Test;

/**
 * Test cases for {@link MagnitudePio}.
 */
public final class ManitudePioTest {

    private final PioFactory fact = new PioXmlFactory();

    @Test
    public void testMagnitudeIO() throws Exception {
        // Without error.
        Magnitude mag1 = new Magnitude(1, MagnitudeBand.J$.MODULE$);

        ParamSet  pset = MagnitudePio.instance.toParamSet(fact, mag1);
        Magnitude mag2 = MagnitudePio.instance.toMagnitude(pset);

        assertEquals(mag1, mag2);
        assertNotSame(mag1, mag2);
    }

    @Test
    public void testError() throws Exception {
        Magnitude mag1 = new Magnitude(1, MagnitudeBand.J$.MODULE$, 0.1);

        ParamSet  pset = MagnitudePio.instance.toParamSet(fact, mag1);
        Magnitude mag2 = MagnitudePio.instance.toMagnitude(pset);

        assertEquals(mag1, mag2);
        assertNotSame(mag1, mag2);
    }

    @Test
    public void testEmptyList() throws Exception {
        ImList<Magnitude> empty1 = ImCollections.emptyList();

        ParamSet pset = MagnitudePio.instance.toParamSet(fact, empty1);
        ImList<Magnitude> empty2 = MagnitudePio.instance.toList(pset);

        assertEquals(0, empty2.size());
    }

    @Test
    public void testList() throws Exception {
        Magnitude magJ1 = new Magnitude(1, MagnitudeBand.J$.MODULE$);
        Magnitude magK2 = new Magnitude(2, MagnitudeBand.K$.MODULE$);

        ImList<Magnitude> lst1 = DefaultImList.create(magJ1, magK2);
        ParamSet pset = MagnitudePio.instance.toParamSet(fact, lst1);
        ImList<Magnitude> lst2 = MagnitudePio.instance.toList(pset);

        assertEquals(2, lst2.size());

        // Order isn't necessarily preserved.
        assertEquals(new Some<Magnitude>(magJ1), lst2.find(new PredicateOp<Magnitude>() {
            @Override public Boolean apply(Magnitude magnitude) {
                return magnitude.band() == MagnitudeBand.J$.MODULE$;
            }
        }));
        assertEquals(new Some<Magnitude>(magK2), lst2.find(new PredicateOp<Magnitude>() {
            @Override public Boolean apply(Magnitude magnitude) {
                return magnitude.band() == MagnitudeBand.K$.MODULE$;
            }
        }));
    }

    @Test
    public void testListWithSystem() throws Exception {
        Magnitude magJ1 = new Magnitude(1, MagnitudeBand.J$.MODULE$, MagnitudeSystem.AB$.MODULE$);
        Magnitude magK2 = new Magnitude(2, MagnitudeBand.K$.MODULE$, MagnitudeSystem.Jy$.MODULE$);

        ImList<Magnitude> lst1 = DefaultImList.create(magJ1, magK2);
        ParamSet pset = MagnitudePio.instance.toParamSet(fact, lst1);
        ImList<Magnitude> lst2 = MagnitudePio.instance.toList(pset);

        assertEquals(2, lst2.size());

        // Order isn't necessarily preserved.
        assertEquals(new Some<>(magJ1), lst2.find(
            magnitude -> magnitude.band() == MagnitudeBand.J$.MODULE$ && magnitude.system() == MagnitudeSystem.AB$.MODULE$
        ));
        assertEquals(new Some<>(magK2), lst2.find(
            magnitude -> magnitude.band() == MagnitudeBand.K$.MODULE$ && magnitude.system() == MagnitudeSystem.Jy$.MODULE$
        ));
    }
}
