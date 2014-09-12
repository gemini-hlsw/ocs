//
// $
//

package edu.gemini.spModel.gemini.michelle;

import edu.gemini.pot.sp.ISPObsComponent;
import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.spModel.config.ConfigBridge;
import edu.gemini.spModel.config.map.ConfigValMapInstances;
import edu.gemini.spModel.config2.ConfigSequence;
import edu.gemini.spModel.config2.ItemKey;
import edu.gemini.spModel.data.config.DefaultParameter;
import edu.gemini.spModel.data.config.DefaultSysConfig;
import edu.gemini.spModel.data.config.IParameter;
import edu.gemini.spModel.data.config.ISysConfig;
import static edu.gemini.spModel.gemini.michelle.MichelleParams.Filter;
import edu.gemini.spModel.seqcomp.SeqConfigNames;
import edu.gemini.spModel.seqcomp.SeqRepeat;
import edu.gemini.spModel.test.SpModelTestBase;

import java.util.ArrayList;
import java.util.List;

/**
 * Testing the link between michelle disperser and overriding the default
 * central wavelength.
 */
public class DisperserLamdaTest extends SpModelTestBase {

    private ISPObsComponent michelle;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        michelle = addObsComponent(InstMichelle.SP_TYPE);
    }

    public void testStaticComponent() throws Exception {
        InstMichelle dobj = (InstMichelle) michelle.getDataObject();

        // Initialized with the default values.
        assertEquals(MichelleParams.Disperser.DEFAULT, dobj.getDisperser());
        assertEquals(MichelleParams.Disperser.DEFAULT.getLamda(), dobj.getDisperserLambda());

        // Set the disperser to some other value and verify that the disperser
        // lambda value is updated.
        for (MichelleParams.Disperser disp : MichelleParams.Disperser.values()) {
            dobj.setDisperser(disp);
            assertEquals(disp.getLamda(), dobj.getDisperserLambda());

            // Now explicitly set the disperser lambda
            dobj.setDisperserLambda(222.5);
            assertEquals(222.5, dobj.getDisperserLambda());
        }
    }

    // FR 10674
    public void testNestedMichelleIterator() throws Exception {
        // Add a repeat iterator.
        ISPSeqComponent repeatComp;
        repeatComp = addSeqComponent(getObs().getSeqComponent(), SeqRepeat.SP_TYPE);
        SeqRepeat repeatDataObj = (SeqRepeat) repeatComp.getDataObject();
        repeatDataObj.setStepCount(2);
        repeatComp.setDataObject(repeatDataObj);

        // Add a michelle iterator.
        ISPSeqComponent michelleSeqComp;
        michelleSeqComp = addSeqComponent(repeatComp, SeqConfigMichelle.SP_TYPE);
        SeqConfigMichelle michelleDataObj = (SeqConfigMichelle) michelleSeqComp.getDataObject();

        // Add a couple of steps with different filters.
        List<Filter> lst = new ArrayList<Filter>();
        lst.add(Filter.F107B4);
        lst.add(Filter.SI_1);
        IParameter param = DefaultParameter.getInstance(InstMichelle.FILTER_PROP.getName(), lst);

        ISysConfig sysConfig = new DefaultSysConfig(SeqConfigNames.INSTRUMENT_CONFIG_NAME);
        sysConfig.putParameter(param);
        michelleDataObj.setSysConfig(sysConfig);
        michelleSeqComp.setDataObject(michelleDataObj);

        // Verify that the observing wavelength changes
        ConfigSequence seq = ConfigBridge.extractSequence(getObs(),null, ConfigValMapInstances.TO_DISPLAY_VALUE);
        ItemKey instKey = new ItemKey(SeqConfigNames.INSTRUMENT_CONFIG_NAME);
        ItemKey wavelengthKey = new ItemKey(instKey, "observingWavelength");
        assertEquals(Filter.F107B4.getWavelength(), seq.getItemValue(0, wavelengthKey));
        assertEquals(Filter.SI_1.getWavelength(), seq.getItemValue(1, wavelengthKey));
        assertEquals(Filter.F107B4.getWavelength(), seq.getItemValue(2, wavelengthKey));
        assertEquals(Filter.SI_1.getWavelength(), seq.getItemValue(3, wavelengthKey));
    }
}
