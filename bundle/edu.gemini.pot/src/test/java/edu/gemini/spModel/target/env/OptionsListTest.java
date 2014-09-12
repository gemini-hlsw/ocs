//
// $
//

package edu.gemini.spModel.target.env;

import edu.gemini.shared.util.immutable.DefaultImList;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Some;
import static edu.gemini.spModel.target.env.OptionsList.UpdateOps.*;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Test cases for {@link OptionsListImpl}.
 */
public class OptionsListTest {

    @Test
    public void testCreateEmptyVarargs() {
        OptionsList<String> olst = OptionsListImpl.create();

        // creates an empty list with no primary
        assertEquals(0, olst.getOptions().size());
        assertEquals(None.STRING, olst.getPrimary());
    }

    @Test
    public void testCreateVarargs() {
        OptionsList<String> olst = OptionsListImpl.create("a", "b", "c");
        assertEquals(DefaultImList.create("a", "b", "c"), olst.getOptions());
        assertEquals("a", olst.getPrimary().getValue());
        assertEquals(new Some<Integer>(0), olst.getPrimaryIndex());
    }

    @Test
    public void testCreateEmptyImList() {
        ImList<String> lst = DefaultImList.create();
        OptionsList<String> olst = OptionsListImpl.create(lst);

        // creates an empty list with no primary
        assertEquals(0, olst.getOptions().size());
        assertEquals(None.STRING, olst.getPrimary());
    }

    @Test
    public void testCreateImList() {
        ImList<String> lst = DefaultImList.create("a", "b", "c");
        OptionsList<String> olst = OptionsListImpl.create(lst);
        assertEquals(DefaultImList.create("a", "b", "c"), olst.getOptions());
        assertEquals("a", olst.getPrimary().getValue());
        assertEquals(new Some<Integer>(0), olst.getPrimaryIndex());
    }

    @Test
    public void testeCreatePrimaryIntVarargs() {
        try {
            OptionsListImpl.createP(0);
            fail("should be out of range");
        } catch (IllegalArgumentException ex) {
            // ok
        }

        OptionsList<String> olst = OptionsListImpl.createP(1, "a", "b", "c");
        assertEquals(DefaultImList.create("a", "b", "c"), olst.getOptions());
        assertEquals("b", olst.getPrimary().getValue());
        assertEquals(new Some<Integer>(1), olst.getPrimaryIndex());

        try {
            OptionsListImpl.createP(3, "a", "b", "c");
            fail("should be out of range");
        } catch (IllegalArgumentException ex) {
            // ok
        }
    }

    @Test
    public void testeCreatePrimaryOptionVarargs() {
        try {
            OptionsListImpl.create(new Some<Integer>(0));
            fail("should be out of range");
        } catch (IllegalArgumentException ex) {
            // ok
        }

        // 1 is the primary index
        OptionsList<String> olst = OptionsListImpl.create(new Some<Integer>(1), "a", "b", "c");
        assertEquals(DefaultImList.create("a", "b", "c"), olst.getOptions());
        assertEquals("b", olst.getPrimary().getValue());
        assertEquals(new Some<Integer>(1), olst.getPrimaryIndex());

        // No primary index
        olst = OptionsListImpl.create(None.INTEGER, "a", "b", "c");
        assertEquals(DefaultImList.create("a", "b", "c"), olst.getOptions());
        assertEquals("x", olst.getPrimary().getOrElse("x"));
        assertEquals(None.INTEGER, olst.getPrimaryIndex());

        try {
            OptionsListImpl.create(new Some<Integer>(3), "a", "b", "c");
            fail("should be out of range");
        } catch (IllegalArgumentException ex) {
            // ok
        }
    }

    @Test
    public void testCreatePrimaryIntImList() {
        try {
            OptionsListImpl.create(0, DefaultImList.create());
            fail("should be out of range");
        } catch (IllegalArgumentException ex) {
            // ok
        }

        ImList<String> lst = DefaultImList.create("a", "b", "c");
        OptionsList<String> olst = OptionsListImpl.create(1, lst);
        assertEquals(DefaultImList.create("a", "b", "c"), olst.getOptions());
        assertEquals("b", olst.getPrimary().getValue());
        assertEquals(new Some<Integer>(1), olst.getPrimaryIndex());

        try {
            OptionsListImpl.create(3, lst);
            fail("should be out of range");
        } catch (IllegalArgumentException ex) {
            // ok
        }
    }

    @Test
    public void testCreatePrimaryOptionImList() {
        try {
            OptionsListImpl.create(new Some<Integer>(0), DefaultImList.create());
            fail("should be out of range");
        } catch (IllegalArgumentException ex) {
            // ok
        }

        // 1 is the primary index
        ImList<String> lst = DefaultImList.create("a", "b", "c");
        OptionsList<String> olst = OptionsListImpl.create(new Some<Integer>(1), lst);
        assertEquals(lst, olst.getOptions());
        assertEquals("b", olst.getPrimary().getValue());
        assertEquals(new Some<Integer>(1), olst.getPrimaryIndex());

        // No primary index
        olst = OptionsListImpl.create(None.INTEGER, lst);
        assertEquals(lst, olst.getOptions());
        assertEquals("x", olst.getPrimary().getOrElse("x"));
        assertEquals(None.INTEGER, olst.getPrimaryIndex());

        try {
            OptionsListImpl.create(new Some<Integer>(3), lst);
            fail("should be out of range");
        } catch (IllegalArgumentException ex) {
            // ok
        }
    }

    @Test
    public void testSelectPrimaryOption() {
        OptionsList<String> olst = OptionsListImpl.createP(0, "a", "b", "c");
        assertSame(olst, olst.selectPrimary(new Some<String>("a")));
        assertEquals("b", olst.selectPrimary(new Some<String>("b")).getPrimary().getValue());

        try {
            olst.selectPrimary(new Some<String>("d"));
            fail("selected an element that doesn't exist in list");
        } catch (IllegalArgumentException ex) {
            // ok
        }

        assertTrue(olst.selectPrimary(None.STRING).getPrimary().isEmpty());
    }

    @Test
    public void testSelectPrimaryValue() {
        OptionsList<String> olst = OptionsListImpl.createP(0, "a", "b", "c");
        assertSame(olst, olst.selectPrimary("a"));
        assertEquals("b", olst.selectPrimary("b").getPrimary().getValue());

        try {
            olst.selectPrimary("d");
            fail("selected an element that doesn't exist in list");
        } catch (IllegalArgumentException ex) {
            // ok
        }
    }

    @Test
    public void testSetPrimary() {
        OptionsList<String> olst = OptionsListImpl.createP(0, "a", "b", "c");

        olst = olst.setPrimary("x");
        assertEquals("x", olst.getPrimary().getValue());
        assertEquals(DefaultImList.create("x", "b", "c"), olst.getOptions());

        // remove the selection
        olst = olst.selectPrimary(None.STRING);

        // set the primary
        olst = olst.setPrimary("y");
        assertEquals("y", olst.getPrimary().getValue());
        assertEquals(DefaultImList.create("x", "b", "c", "y"), olst.getOptions());
    }

    @Test
    public void testSetPrimaryIndexOption() {
        OptionsList<String> olst = OptionsListImpl.createP(0, "a", "b", "c");
        assertSame(olst, olst.setPrimaryIndex(new Some<Integer>(0)));
        assertEquals("b", olst.setPrimaryIndex(new Some<Integer>(1)).getPrimary().getValue());

        // remove the selection
        olst = olst.setPrimaryIndex(None.INTEGER);
        assertEquals(None.INTEGER, olst.getPrimaryIndex());
        assertEquals(None.STRING, olst.getPrimary());

        try {
            olst.setPrimaryIndex(new Some<Integer>(3));
            fail("set to an element that doesn't exist in list");
        } catch (IllegalArgumentException ex) {
            // ok
        }
    }

    @Test
    public void testSetPrimaryIndexInt() {
        OptionsList<String> olst = OptionsListImpl.createP(0, "a", "b", "c");
        assertSame(olst, olst.setPrimaryIndex(0));
        assertEquals("b", olst.setPrimaryIndex(1).getPrimary().getValue());

        try {
            olst.setPrimaryIndex(3);
            fail("set to an element that doesn't exist in list");
        } catch (IllegalArgumentException ex) {
            // ok
        }
    }

    @Test
    public void testIterator() {
        OptionsList<String> olst = OptionsListImpl.create("a", "b", "c");

        ImList<String> lst = DefaultImList.create();
        for (String s : olst) {
            lst = lst.cons(s);
        }
        assertEquals(lst.reverse(), olst.getOptions());
    }

    @Test
    public void testSetOptions() {
        OptionsList<String> olst = OptionsListImpl.createP(2, "a", "b", "c");

        // Set with a list of the same size
        ImList<String> lst = DefaultImList.create("x", "y", "z");
        OptionsList<String> updated = olst.setOptions(lst);
        assertEquals(lst, updated.getOptions());
        assertEquals("z", updated.getPrimary().getValue());

        // Set with a longer list -- keep the primary index
        lst = DefaultImList.create("w", "x", "y", "z");
        updated = olst.setOptions(lst);
        assertEquals(lst, updated.getOptions());
        assertEquals("y", updated.getPrimary().getValue());

        // Set with a shorter list -- move the primary index back to
        // accomodate
        lst = DefaultImList.create("r", "s");
        updated = olst.setOptions(lst);
        assertEquals(lst, updated.getOptions());
        assertEquals("s", updated.getPrimary().getValue());

        // Set with an empty list -- remove the primary
        lst = DefaultImList.create();
        updated = olst.setOptions(lst);
        assertEquals(lst, updated.getOptions());
        assertEquals(None.STRING, updated.getPrimary());
    }

    @Test
    public void testAppendOp() {
        OptionsList<String> olst = OptionsListImpl.createP(2, "a", "b", "c");

        olst = olst.update(append("d"));
        assertEquals(DefaultImList.create("a", "b", "c", "d"), olst.getOptions());
        assertEquals("c", olst.getPrimary().getValue());
    }

    @Test
    public void testAppendAsPrimaryOp() {
        OptionsList<String> olst = OptionsListImpl.createP(2, "a", "b", "c");

        olst = olst.update(appendAsPrimary("d"));
        assertEquals(DefaultImList.create("a", "b", "c", "d"), olst.getOptions());
        assertEquals("d", olst.getPrimary().getValue());
    }

    @Test
    public void testRemoveOp() {
        final OptionsList<String> olst = OptionsListImpl.createP(0, "a", "b", "c");

        OptionsList<String> nlst;

        // Test remove after primary
        nlst = olst.update(remove("c"));
        assertEquals(DefaultImList.create("a", "b"), nlst.getOptions());
        assertEquals("a", nlst.getPrimary().getValue());

        // Test remove at primary
        nlst = olst.update(remove("a"));
        assertEquals(DefaultImList.create("b", "c"), nlst.getOptions());
        assertEquals("b", nlst.getPrimary().getValue());

        // Test remove before primary
        nlst = olst.selectPrimary("b").update(remove("a"));
        assertEquals(DefaultImList.create("b", "c"), nlst.getOptions());
        assertEquals("b", nlst.getPrimary().getValue());

        // Test remove primary at end
        nlst = olst.selectPrimary("c").update(remove("c"));
        assertEquals(DefaultImList.create("a", "b"), nlst.getOptions());
        assertEquals("b", nlst.getPrimary().getValue());

        // Test remove single element
        nlst = OptionsListImpl.create("a").update(remove("a"));
        ImList<String> empty = DefaultImList.create();
        assertEquals(empty, nlst.getOptions());
        assertEquals(None.STRING, nlst.getPrimary());

        // Test remove when there is no primary
        nlst = olst.setPrimaryIndex(None.INTEGER).update(remove("a"));
        assertEquals(DefaultImList.create("b", "c"), nlst.getOptions());
        assertEquals(None.STRING, nlst.getPrimary());

        // Test remove an object that doesn't exist
        nlst = olst.update(remove("x"));
        assertEquals(DefaultImList.create("a", "b", "c"), nlst.getOptions());
        assertEquals("a", nlst.getPrimary().getValue());
    }

    @Test
    public void testToggleOp() {
        final OptionsList<String> olst = OptionsListImpl.createP(0, "a", "b", "c");

        OptionsList<String> nlst;

        // Toggle off the primary
        nlst = olst.update(togglePrimary("a"));
        assertEquals(None.STRING, nlst.getPrimary());

        // Toggle on another element
        nlst = olst.update(togglePrimary("b"));
        assertEquals("b", nlst.getPrimary().getValue());

        // Fail to toggle an element that doesn't exist
        try {
            olst.update(togglePrimary("x"));
            fail("can't toggle non-existent object");
        } catch (IllegalArgumentException ex) {
            // ok
        }
    }
}
