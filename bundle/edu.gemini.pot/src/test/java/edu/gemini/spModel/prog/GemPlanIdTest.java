//
// $
//

package edu.gemini.spModel.prog;

import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import junit.framework.TestCase;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;

/**
 * Test cases for {@link GemPlanId}.
 */
public class GemPlanIdTest extends TestCase {
    private static final Option<GemPlanId> none = None.instance();

    public void testNormalPlan() throws Exception {
        GemPlanId gpi = GemPlanId.parse("GN-PLAN20090324").getValue();
        assertEquals(gpi.getSite(), GemSite.north);

        Calendar cal = new GregorianCalendar();
        cal.setTimeZone(SimpleTimeZone.getTimeZone("UTC"));
        cal.set(Calendar.YEAR, 2009);
        cal.set(Calendar.MONTH, Calendar.MARCH);
        cal.set(Calendar.DATE, 24);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        assertEquals(cal.getTime(), gpi.getDate());
    }

    public void testBadSite() throws Exception {
        assertEquals(none, GemPlanId.parse(SPProgramID.toProgramID("GX-PLAN20090324")));
    }

    public void testNoPlan() throws Exception {
        assertEquals(none, GemPlanId.parse(SPProgramID.toProgramID("GS-WXYZ20090324")));
    }

    public void testBadDate() throws Exception {
        assertEquals(none, GemPlanId.parse(SPProgramID.toProgramID("GS-PLAN200903241")));
        assertEquals(none, GemPlanId.parse(SPProgramID.toProgramID("GS-PLAN2009")));
    }

    public void testEquals() throws Exception {
        GemPlanId gpi1 = GemPlanId.parse("GN-PLAN20090324").getValue();
        GemPlanId gpi2 = GemPlanId.parse("GN-PLAN20090324").getValue();
        GemPlanId gpi3 = GemPlanId.parse("GN-PLAN20090325").getValue();
        assertEquals(gpi1, gpi2);
        assertEquals(gpi1.hashCode(), gpi2.hashCode());
        assertFalse(gpi1.equals(gpi3));
        assertEquals(gpi1.hashCode(), gpi2.hashCode());
        assertEquals(gpi1.compareTo(gpi2), 0);
    }

    public void testYesterdayAndTomorrow() throws Exception {
        GemPlanId today     = GemPlanId.parse("GS-PLAN20090324").getValue();
        GemPlanId yesterday = today.yesterday();
        GemPlanId tomorrow  = today.tomorrow();

        assertEquals("GS-PLAN20090323", yesterday.toString());
        assertEquals("GS-PLAN20090325", tomorrow.toString());

        assertTrue(today.compareTo(yesterday) > 0);
        assertTrue(yesterday.compareTo(today) < 0);
        assertTrue(today.compareTo(today) == 0);
    }
}
