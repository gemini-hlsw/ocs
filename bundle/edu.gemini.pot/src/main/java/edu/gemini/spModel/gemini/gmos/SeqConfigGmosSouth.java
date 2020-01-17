// Copyright 2002 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
//
// $Id: SeqConfigGmosSouth.java 26147 2010-05-28 18:40:54Z swalker $
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
public class SeqConfigGmosSouth extends SeqConfigObsBase implements PropertyProvider {

    /**
     * This iter component's SP type.
     */
    public static final SPComponentType SP_TYPE =
            SPComponentType.ITERATOR_GMOSSOUTH;

    public static final ISPNodeInitializer<ISPSeqComponent, SeqConfigGmosSouth> NI =
        new ComponentNodeInitializer<>(SP_TYPE, SeqConfigGmosSouth::new, SeqConfigGmosSouthCB::new);

    // The instrument name
    public static final String SYSTEM_NAME = SeqConfigNames.INSTRUMENT_CONFIG_NAME;
    public static final String INSTRUMENT_NAME = InstGmosSouth.INSTRUMENT_NAME_PROP;

    public static final Map<String, PropertyDescriptor> PROPERTY_MAP =
           Collections.unmodifiableMap(
             PropertySupport.filter(PropertyFilter.ITERABLE_FILTER, InstGmosSouth.PROPERTY_MAP)
           );

    /**
     * Default constructor.
     */
    public SeqConfigGmosSouth() {
        super(SP_TYPE, SYSTEM_NAME);
    }

    public Map<String, PropertyDescriptor> getProperties() {
        return PROPERTY_MAP;
    }
}


