//
// $
//

package edu.gemini.catalog.skycat.binding.skyobj;

import edu.gemini.catalog.skycat.binding.skyobj.BrightnessFilter.Op;
import edu.gemini.shared.skyobject.Magnitude;
import static edu.gemini.shared.skyobject.Magnitude.Band.J;
import edu.gemini.shared.util.immutable.DefaultImList;
import edu.gemini.shared.util.immutable.ImCollections;
import edu.gemini.shared.util.immutable.ImList;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 */
public class BrightnessFilterTest {

    private static Magnitude j0 = new Magnitude(J, 0.0);
    private static Magnitude j1 = new Magnitude(J, 1.0);
    private static Magnitude j2 = new Magnitude(J, 2.0);
    private static final ImList<Magnitude> magList = DefaultImList.create(j0, j1, j2);

    private void doTest(Op op, double limit, ImList<Magnitude> expected) {
        assertEquals(expected, magList.filter(new BrightnessFilter(op, limit)));
    }

    @SuppressWarnings({"unchecked"})
    @Test
    public void testApply() {

        doTest(Op.lt, -1.0, ImCollections.EMPTY_LIST);
        doTest(Op.le, -1.0, ImCollections.EMPTY_LIST);
        doTest(Op.ge, -1.0, magList);
        doTest(Op.gt, -1.0, magList);

        doTest(Op.lt, 0.0, ImCollections.EMPTY_LIST);
        doTest(Op.le, 0.0, DefaultImList.create(j0));
        doTest(Op.ge, 0.0, magList);
        doTest(Op.gt, 0.0, DefaultImList.create(j1, j2));

        doTest(Op.lt, 1.0, DefaultImList.create(j0));
        doTest(Op.le, 1.0, DefaultImList.create(j0, j1));
        doTest(Op.ge, 1.0, DefaultImList.create(j1, j2));
        doTest(Op.gt, 1.0, DefaultImList.create(j2));

        doTest(Op.lt, 2.0, DefaultImList.create(j0, j1));
        doTest(Op.le, 2.0, magList);
        doTest(Op.ge, 2.0, DefaultImList.create(j2));
        doTest(Op.gt, 2.0, ImCollections.EMPTY_LIST);

        doTest(Op.lt, 3.0, magList);
        doTest(Op.le, 3.0, magList);
        doTest(Op.ge, 3.0, ImCollections.EMPTY_LIST);
        doTest(Op.gt, 3.0, ImCollections.EMPTY_LIST);
    }

}
