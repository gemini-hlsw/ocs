//
// $
//

package edu.gemini.spModel.prog;

import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import junit.framework.TestCase;

/**
 * Test cases for GemSite.
 */
public final class GemSiteTest extends TestCase {
    private void verify(GemSite site, String... names) {
        for (String name : names) {
            assertEquals(new Some<GemSite>(site), GemSite.parse(name));
        }
    }

    public void testSome() {
        verify(GemSite.north, "north", "gn", "n", "NORTH", "GN", "N");
        verify(GemSite.south, "south", "gs", "s", "SOUTH", "GS", "S");
    }

    public void testNone() {
        Option<GemSite> none = None.instance();
        assertEquals(none, GemSite.parse("xyz"));
        assertEquals(none, GemSite.parse("NG"));
        assertEquals(none, GemSite.parse(null));
    }
}
