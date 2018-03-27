// Copyright 1997-2002
// Association for Universities for Research in Astronomy, Inc.,
//
// $Id: SeqConfigGPOL.java 7083 2006-05-26 22:44:42Z shane $
//
package edu.gemini.spModel.gemini.gpol;

import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.data.property.PropertyProvider;
import edu.gemini.spModel.data.property.PropertySupport;
import edu.gemini.spModel.gemini.gpol.GPOLParams.Angle;
import edu.gemini.spModel.gemini.gpol.GPOLParams.Calibrator;
import edu.gemini.spModel.gemini.gpol.GPOLParams.Modulator;
import edu.gemini.spModel.gemini.init.ComponentNodeInitializer;
import edu.gemini.spModel.seqcomp.SeqConfigComp;

import java.beans.PropertyDescriptor;
import java.util.Map;
import java.util.TreeMap;

/**
 * The Gemini GPOL configuration iterator.
 */
public class SeqConfigGPOL extends SeqConfigComp implements PropertyProvider {

    private static class GPOLDummy {
        public Modulator getModulator() {
            return null;
        }
        public void setModulator(Modulator modulator) {
        }
        public Calibrator getCalibrator() {
            return null;
        }
        public void setCalibrator(Calibrator calibrator) {
        }
        public Angle getAngle() {
            return null;
        }
        public void setAngle(Angle angle) {
        }
    }


    /**
     * This iter component's SP type.
     */
    public static final SPComponentType SP_TYPE = SPComponentType.ITERATOR_GPOL;

    public static final ISPNodeInitializer<ISPSeqComponent, SeqConfigGPOL> NI =
        new ComponentNodeInitializer<>(SP_TYPE, () -> new SeqConfigGPOL(), c -> new SeqConfigGPOLCB(c));

    public static final PropertyDescriptor ANGLE_PROP      = PropertySupport.init("angle", GPOLDummy.class, false, true);
    public static final PropertyDescriptor CALIBRATOR_PROP = PropertySupport.init("calibrator", GPOLDummy.class, false, true);
    public static final PropertyDescriptor MODULATOR_PROP  = PropertySupport.init("modulator", GPOLDummy.class, false, true);


    /**
     * Default constructor.
     */
    public SeqConfigGPOL() {
        super(SP_TYPE);
    }

    public Map<String, PropertyDescriptor> getProperties() {
        Map<String, PropertyDescriptor> res;
        res = new TreeMap<String, PropertyDescriptor>();

        res.put(ANGLE_PROP.getName(),      ANGLE_PROP);
        res.put(CALIBRATOR_PROP.getName(), CALIBRATOR_PROP);
        res.put(MODULATOR_PROP.getName(),  MODULATOR_PROP);

        return res;
    }
}
