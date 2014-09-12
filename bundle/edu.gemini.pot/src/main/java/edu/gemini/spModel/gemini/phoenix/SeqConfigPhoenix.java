// Copyright 1997-2002
// Association for Universities for Research in Astronomy, Inc.
//
// $Id: SeqConfigPhoenix.java 7080 2006-05-26 21:10:08Z anunez $
//
package edu.gemini.spModel.gemini.phoenix;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.data.property.PropertyProvider;
import edu.gemini.spModel.data.property.PropertySupport;
import edu.gemini.spModel.data.property.PropertyFilter;
import edu.gemini.spModel.seqcomp.SeqConfigNames;
import edu.gemini.spModel.seqcomp.SeqConfigObsBase;

import java.beans.PropertyDescriptor;
import java.util.Map;
import java.util.Collections;

/**
 * The Phoenix configuration iterator.
 */
public class SeqConfigPhoenix extends SeqConfigObsBase implements PropertyProvider {

    private static final long serialVersionUID = 1L;

    /**
     * This iter component's SP type.
     */
    public static final SPComponentType SP_TYPE =
            SPComponentType.ITERATOR_PHOENIX;

    // The system name
    public static final String SYSTEM_NAME = SeqConfigNames.INSTRUMENT_CONFIG_NAME;

    public static final Map<String, PropertyDescriptor> PROPERTY_MAP =
           Collections.unmodifiableMap(
             PropertySupport.filter(PropertyFilter.ITERABLE_FILTER, InstPhoenix.PROPERTY_MAP)
           );

    /**
     * Default constructor.
     */
    public SeqConfigPhoenix() {
        super(SP_TYPE, SYSTEM_NAME);
    }

    public Map<String, PropertyDescriptor> getProperties() {
        return PROPERTY_MAP;
    }
}
