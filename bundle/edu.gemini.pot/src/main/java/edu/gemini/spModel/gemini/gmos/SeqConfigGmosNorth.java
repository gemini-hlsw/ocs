// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: SeqConfigGmosNorth.java 7064 2006-05-25 19:48:25Z shane $
//
package edu.gemini.spModel.gemini.gmos;

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
 * The GMOS configuration iterator.
 */
public class SeqConfigGmosNorth extends SeqConfigObsBase implements PropertyProvider {

    /**
     * This iter component's SP type.
     */
    public static final SPComponentType SP_TYPE =
            SPComponentType.ITERATOR_GMOS;

    public static final ISPNodeInitializer<ISPSeqComponent, SeqConfigGmosNorth> NI =
        new ComponentNodeInitializer<>(SP_TYPE, () -> new SeqConfigGmosNorth(), c -> new SeqConfigGmosNorthCB(c));

    // The system name
    public static final String SYSTEM_NAME = SeqConfigNames.INSTRUMENT_CONFIG_NAME;
    // The instrument name
    public static final String INSTRUMENT_NAME = InstGmosNorth.INSTRUMENT_NAME_PROP;

    public static final Map<String, PropertyDescriptor> PROPERTY_MAP =
           Collections.unmodifiableMap(
             PropertySupport.filter(PropertyFilter.ITERABLE_FILTER, InstGmosNorth.PROPERTY_MAP)
           );

    /**
     * Default constructor.
     */
    public SeqConfigGmosNorth() {
        super(SP_TYPE, SYSTEM_NAME);
    }


    public Map<String, PropertyDescriptor> getProperties() {
        return PROPERTY_MAP;
    }
}
