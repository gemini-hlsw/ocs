package edu.gemini.spModel.util;


import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public final class VersionTokenTest {

    @Test
    public void testComparableByNext() {
        final VersionToken a = new VersionToken(1);
        final VersionToken b = new VersionToken(1);
        assertEquals(0, a.compareTo(b));

        b.next();
        assertTrue(a.compareTo(b) < 0);

        a.next();
        assertEquals(0, a.compareTo(b));
    }

    @Test
    public void testComparableBySegmentLength() {
        final VersionToken vt1 = new VersionToken(1);
        final VersionToken vt1_1 = vt1.next();
        final VersionToken vt1_1_1 = vt1_1.next();

        assertTrue(vt1.compareTo(vt1_1) < 0);
        assertTrue(vt1.compareTo(vt1_1_1) < 0);
        assertTrue(vt1_1.compareTo(vt1_1_1) < 0);
    }

    @Test
    public void testComparable() {
        final VersionToken vt1 = new VersionToken(1);

        final VersionToken vt1_1 = vt1.next();
        final VersionToken vt1_1_1 = vt1_1.next();
        final VersionToken vt1_1_2 = vt1_1.next();
        final VersionToken vt1_1_3 = vt1_1.next();

        final VersionToken vt1_2 = vt1.next();
        final VersionToken vt1_2_1 = vt1_2.next();
        final VersionToken vt1_2_2 = vt1_2.next();
        final VersionToken vt1_2_3 = vt1_2.next();

        final VersionToken vt2 = new VersionToken(2);
        final VersionToken vt2_1 = vt2.next();
        final VersionToken vt2_1_1 = vt2_1.next();

        final VersionToken[] tokens = new VersionToken[]{
                vt2_1,
                vt1_1_1,
                vt1_2_1,
                vt2,
                vt1_1_3,
                vt1_2_2,
                vt1_2,
                vt2_1_1,
                vt1_1,
                vt1_1_2,
                vt1_2_3,
                vt1,
        };

        Arrays.sort(tokens);

        final VersionToken[] expected = new VersionToken[]{
                vt1,
                vt1_1,
                vt1_1_1,
                vt1_1_2,
                vt1_1_3,
                vt1_2,
                vt1_2_1,
                vt1_2_2,
                vt1_2_3,
                vt2,
                vt2_1,
                vt2_1_1,
        };

        assertArrayEquals(expected, tokens);
    }
}
