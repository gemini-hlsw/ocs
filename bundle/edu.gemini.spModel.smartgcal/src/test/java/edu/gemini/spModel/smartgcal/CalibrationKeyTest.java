package edu.gemini.spModel.smartgcal;

import edu.gemini.spModel.gemini.calunit.smartgcal.keys.WavelengthRange;
import edu.gemini.spModel.gemini.calunit.smartgcal.keys.WavelengthRangeSet;
import org.junit.Test;
import org.junit.Assert;

/**
 */
public class CalibrationKeyTest {

    @Test
    public void canCreateValidRange() {
        WavelengthRange[] ranges = {
                WavelengthRange.parse("0-100"),
                WavelengthRange.parse("0 - 100"),
                WavelengthRange.parse("0.0 - 100.0"),
                WavelengthRange.parse("  0.0    -   100.0   "),
                new WavelengthRange(0.0d, 100.0d)
        };

        for (WavelengthRange range : ranges) {
            Assert.assertEquals(0.0d, range.getMin(), 0.01);
            Assert.assertEquals(100.0d, range.getMax(), 0.01);
        }
    }

    @Test
    public void canNotCreateInvalidRange() {
        String[] invalidStrings = {
            "0 - 0",        // min must be > max
            "100 - 0",      // min must be < than max
            " 7 8a 830",    // garbage
            " - 3939",      // garbage
            "0-"            // garbage
        };

        for (String invalidString : invalidStrings) {
            try {
                WavelengthRange range = WavelengthRange.parse(invalidString);
            } catch (IllegalArgumentException e) {
                // yep, we expected that...
                continue;
            }
            // no exception thrown -> something went wrong
            Assert.fail();
        }
    }

    @Test
    public void canCreateValidRangeSet() {
        WavelengthRangeSet rangeSet = new WavelengthRangeSet();
        WavelengthRange[] ranges = {
            new WavelengthRange(10.0d, 20.0d),
            new WavelengthRange(20.0d, 30.0d),
            new WavelengthRange(30.0d, 40.0d),
            new WavelengthRange(50.0d, 60.0d)
        };
        for (WavelengthRange range : ranges) {
            rangeSet.add(range, null);
        }

        Assert.assertEquals(ranges[0], rangeSet.findRange(10.0d).getValue());
        Assert.assertEquals(ranges[0], rangeSet.findRange(15.0d).getValue());
        Assert.assertEquals(ranges[1], rangeSet.findRange(20.0d).getValue());
        Assert.assertEquals(ranges[2], rangeSet.findRange(37.5d).getValue());

        Assert.assertTrue(rangeSet.findRange(0.0d).isEmpty());
        Assert.assertTrue(rangeSet.findRange(40.0d).isEmpty());
        Assert.assertTrue(rangeSet.findRange(42.0d).isEmpty());
        Assert.assertTrue(rangeSet.findRange(60.0d).isEmpty());
    }

    @Test
    public void canNotCreateInvalidRangeSet() {
        WavelengthRangeSet rangeSet = new WavelengthRangeSet();
        rangeSet.add(new WavelengthRange(10.0d, 20.0d), null);

        WavelengthRange[] ranges = {
            new WavelengthRange(05.0d, 25.0d),
            new WavelengthRange(11.0d, 12.0d),
            new WavelengthRange( 9.0d, 11.0d),
            new WavelengthRange(18.0d, 22.0d),
        };
        for (WavelengthRange range : ranges) {
            try {
                rangeSet.add(range, null);
            } catch (IllegalArgumentException e) {
                // yep, we expected that...
                continue;
            }
            // no exception thrown -> something went wrong
            Assert.fail();
        }
    }

}
