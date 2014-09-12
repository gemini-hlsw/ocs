//
// $
//

package edu.gemini.dataman.gsa.query;

import edu.gemini.dataman.context.TestDatamanConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;

import java.io.IOException;


/**
 *
 */
@Ignore
public abstract class GsaQueryTestBase {
    protected TestDatamanConfig config;

    @Before
    public void setUp() throws Exception {
        config = new TestDatamanConfig();
    }

    @After
    public void tearDown() throws IOException {
        config.cleanup();
    }
}
