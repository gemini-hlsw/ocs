//
// $
//

package edu.gemini.dataman.context;

/**
 * Test version of the {@link DatamanContext}
 */
public class TestDatamanContext extends TestDatamanServices implements DatamanContext {
    private TestDatamanState state;

    public TestDatamanContext() throws Exception {
        super(new TestDatamanConfig());
        state = new TestDatamanState();
    }

    public DatamanState getState() {
        return state;
    }
}
