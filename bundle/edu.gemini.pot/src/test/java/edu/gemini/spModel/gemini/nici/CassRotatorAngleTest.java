//
// $
//

package edu.gemini.spModel.gemini.nici;

import static edu.gemini.spModel.gemini.nici.NICIParams.CassRotator.*;
import edu.gemini.spModel.test.SpModelTestBase;
import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.skycalc.Angle;
import static edu.gemini.skycalc.Angle.Unit.DEGREES;
import org.junit.Before;
import org.junit.Test;


public final class CassRotatorAngleTest extends SpModelTestBase {
    private InstNICI dataObj;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        ISPObsComponent obsComp = addObsComponent(InstNICI.SP_TYPE);
        dataObj = (InstNICI) obsComp.getDataObject();
    }

    @Test
    public void testDefault() {
        Angle a = FIXED.defaultAngle();
        assertEquals(a, dataObj.getCassRotatorFixedAngle());
        assertEquals(a.getMagnitude(), dataObj.getPosAngleDegrees(), 0.000001);

        a = FOLLOW.defaultAngle();
        assertEquals(a, dataObj.getCassRotatorFollowAngle());
    }

    @Test
    public void testUpdateCurrent() {
        // Set the cass rotator angle to some non-standard value
        Angle a = new Angle(42, DEGREES);
        dataObj.setCassRotatorFixedAngle(a);
        assertEquals(a, dataObj.getCassRotatorFixedAngle());

        // Should also update the pos angle, because cass rotator fixed is
        // the default.
        assertEquals(FIXED, dataObj.getCassRotator());
        assertEquals(a.getMagnitude(), dataObj.getPosAngleDegrees(), 0.000001);

        // Doesn't change the follow angle
        a = FOLLOW.defaultAngle();
        assertEquals(a, dataObj.getCassRotatorFollowAngle());
    }

    @Test
    public void testUpdateAlternate() {
        // Set the cass rotator angle to some non-standard value
        Angle a = new Angle(42, DEGREES);
        dataObj.setCassRotatorFollowAngle(a);
        assertEquals(a, dataObj.getCassRotatorFollowAngle());

        // Should not update the pos angle, because cass rotator fixed is
        // the default.
        Angle def = FIXED.defaultAngle();
        assertEquals(FIXED, dataObj.getCassRotator());
        assertEquals(def.getMagnitude(), dataObj.getPosAngleDegrees(), 0.000001);
        assertEquals(def, dataObj.getCassRotatorFixedAngle());
    }

    @Test
    public void testUpdateCassRotator() {
        // Update the cass rotator value
        dataObj.setCassRotator(FOLLOW);

        // Make sure that both defaults apply
        Angle def = FIXED.defaultAngle();
        assertEquals(def, dataObj.getCassRotatorFixedAngle());
        def = FOLLOW.defaultAngle();
        assertEquals(def, dataObj.getCassRotatorFollowAngle());

        // Pos angle switched to the default for follow.
        assertEquals(def.getMagnitude(), dataObj.getPosAngleDegrees(), 0.000001);
    }

    @Test
    public void testUpdatePosAngle() {
        dataObj.setPosAngleDegrees(42.0);

        // Should have updated the fixed angle
        assertEquals(42.0, dataObj.getCassRotatorFixedAngle().getMagnitude(), 0.000001);

        // But not the follow angle.
        Angle def = FOLLOW.defaultAngle();
        assertEquals(def, dataObj.getCassRotatorFollowAngle());
    }
}
