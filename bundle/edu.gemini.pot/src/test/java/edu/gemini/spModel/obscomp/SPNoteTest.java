//
// $
//

package edu.gemini.spModel.obscomp;

import junit.framework.TestCase;

/**
 * Test cases for {@link SPNote}.
 */
public class SPNoteTest extends TestCase {

    public void testClone() throws Exception {
        final String title = "My Note Title";
        final String group = "My Note Group";
        final String text  = "My note text.";

        SPNote orig = new SPNote();
        orig.setTitle(title);
        orig.setGroup(group);
        orig.setNote(text);

        SPNote clone = (SPNote) orig.clone();
        assertEquals(title, clone.getTitle());
        assertEquals(group, clone.getGroup());
        assertEquals(text, clone.getNote());

        clone.setTitle("New Title");
        clone.setGroup("New Group");
        clone.setNote("New text.");

        assertEquals(title, orig.getTitle());
        assertEquals(group, orig.getGroup());
        assertEquals(text,  orig.getNote());
    }
}
