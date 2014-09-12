package edu.gemini.spModel.core;

import org.junit.Test;

import java.text.ParseException;

import static org.junit.Assert.*;

/**
 * Class SiteTest
 *
 * @author Nicolas A. Barriga
 *         Date: 4/8/11
 */
public class SiteTest {
    private final String[] northNames = new String[]{"GN", "mk", "MKO", "north", "MaunaKea", "gemini_north"};
    private final String[] southNames = new String[]{"Gs", "cp", "CPO", "south", "Cerro Pachon", "gemini_south"};
    private final String[] wrongNames = new String[]{"", "La Serena", "Gemini", "mauna", "pachon"};

    @Test
    public void testParse() throws ParseException {
        for (String name : northNames) {
            assertEquals(Site.GN, Site.parse(name));
        }
        for (String name : southNames) {
            assertEquals(Site.GS, Site.parse(name));
        }
    }

    @Test
    public void testWrongParse() {
        int count = 0;
        for (String name : wrongNames) {
            try {
                assertEquals(Site.GN, Site.parse(name));
            } catch (ParseException ex) {
                count++;
            }
        }
        assertEquals(count, wrongNames.length);
    }
}
