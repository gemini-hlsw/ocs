//
// $
//

package edu.gemini.spModel.gemini.gsaoi;

import edu.gemini.shared.util.immutable.DefaultImList;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.gemini.BeanPropertyTestBase;
import static edu.gemini.spModel.gemini.gsaoi.Gsaoi.*;
import edu.gemini.spModel.obscomp.InstConstants;
import edu.gemini.spModel.telescope.IssPort;

/**
 * Test cases for {@link Gsaoi} properties.
 */
public class GsaoiPropertyTest extends BeanPropertyTestBase {
//    private static final Option<UtilityWheel> NO_UTILITY_WHEEL = None.instance();
//    private static final Option<OdgwSize> NO_ODGW_SIZE = None.instance();

    private final ImList<PropertyTest> tests = DefaultImList.create(
        new PropertyTest(FILTER_PROP, Filter.DEFAULT, Filter.H20_ICE, Filter.CH4_LONG),
        new PropertyTest(READ_MODE_PROP, Filter.DEFAULT.readMode(), ReadMode.BRIGHT),
        new PropertyTest(PORT_PROP, IssPort.UP_LOOKING, IssPort.SIDE_LOOKING),

// TODO: support Option types
//        new PropertyTest<Option<UtilityWheel>>(UTILITY_WHEEL_PROP, NO_UTILITY_WHEEL, new Some<UtilityWheel>(UtilityWheel.EXTRAFOCAL_LENS_1)),
//        new PropertyTest<Option<OdgwSize>>(ODGW_SIZE_PROP, NO_ODGW_SIZE, new Some<OdgwSize>(OdgwSize.SIZE_4)),

        new PropertyTest(COADDS_PROP, InstConstants.DEF_COADDS, 42),
        new PropertyTest(POS_ANGLE_PROP, 0.0, 10.0),
//        new PropertyTest(EXPOSURE_TIME_PROP, getRecommendedExposureTimeSecs(Filter.DEFAULT, Filter.DEFAULT.readMode()) , 123.0)
        new PropertyTest(EXPOSURE_TIME_PROP, 60.0 , 123.0) // REL-445
    );

    protected final ISPDataObject createBean() {
        return new Gsaoi();
    }

    public void testGetSet() throws Exception {
        testGetSet(tests);
    }

    public void testParamSet() throws Exception {
        testParamSet(tests);
    }

    public void testSysConfig() throws Exception {
        testSysConfig(tests);
    }
}
