// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: SeqConfigAcqCam.java 7038 2006-05-17 14:24:43Z gillies $
//
package edu.gemini.spModel.gemini.acqcam;

import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.data.property.PropertyFilter;
import edu.gemini.spModel.data.property.PropertyProvider;
import edu.gemini.spModel.data.property.PropertySupport;
import edu.gemini.spModel.gemini.init.ComponentNodeInitializer;
import edu.gemini.spModel.seqcomp.SeqConfigNames;
import edu.gemini.spModel.seqcomp.SeqConfigObsBase;

import java.beans.PropertyDescriptor;
import java.util.Collections;
import java.util.Map;


/**
 * The AcqCam configuration iterator.
 */
public class SeqConfigAcqCam extends SeqConfigObsBase implements PropertyProvider {

    private static final long serialVersionUID = 6597610422528265935L;

    /**
     * This iter component's SP type.
     */
    public static final SPComponentType SP_TYPE = SPComponentType.ITERATOR_ACQCAM;

    public static final ISPNodeInitializer<ISPSeqComponent, SeqConfigAcqCam> NI =
        new ComponentNodeInitializer<>(SP_TYPE, () -> new SeqConfigAcqCam(), c -> new SeqConfigAcqCamCB(c));

    // The system name
    public static final String SYSTEM_NAME = SeqConfigNames.INSTRUMENT_CONFIG_NAME;
    // The instrument name
    public static final String INSTRUMENT_NAME = "AcqCam";

    public static final Map<String, PropertyDescriptor> PROPERTY_MAP =
            Collections.unmodifiableMap(
                    PropertySupport.filter(PropertyFilter.ITERABLE_FILTER, InstAcqCam.PROPERTY_MAP)
            );

    /**
     * Default constructor.
     */
    public SeqConfigAcqCam() {
        super(SP_TYPE, SYSTEM_NAME);
    }

    public Map<String, PropertyDescriptor> getProperties() {
        return PROPERTY_MAP;
    }
}
