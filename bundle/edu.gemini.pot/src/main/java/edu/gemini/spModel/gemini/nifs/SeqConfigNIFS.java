// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: SeqConfigNIFS.java 7069 2006-05-25 21:51:12Z shane $
//
package edu.gemini.spModel.gemini.nifs;

import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.data.property.PropertySupport;
import edu.gemini.spModel.data.property.PropertyFilter;
import edu.gemini.spModel.data.property.PropertyProvider;
import edu.gemini.spModel.gemini.init.ComponentNodeInitializer;
import edu.gemini.spModel.seqcomp.SeqConfigNames;
import edu.gemini.spModel.seqcomp.SeqConfigObsBase;

import java.util.Map;
import java.util.Collections;
import java.beans.PropertyDescriptor;

/**
 * The NIFS configuration iterator.
 */
public class SeqConfigNIFS extends SeqConfigObsBase implements PropertyProvider {

    private static final long serialVersionUID = 3L;

    /**
     * This iter component's SP type.
     */
    public static final SPComponentType SP_TYPE =
            SPComponentType.ITERATOR_NIFS;

    public static final ISPNodeInitializer<ISPSeqComponent, SeqConfigNIFS> NI =
        new ComponentNodeInitializer<>(SP_TYPE, () -> new SeqConfigNIFS(), c -> new SeqConfigNIFSCB(c));

    // The system name
    public static final String SYSTEM_NAME = SeqConfigNames.INSTRUMENT_CONFIG_NAME;

    public static final Map<String, PropertyDescriptor> PROPERTY_MAP;

    static {
        Map<String, PropertyDescriptor> map;
        map = PropertySupport.filter(PropertyFilter.ITERABLE_FILTER, InstNIFS.PROPERTY_MAP);
        map.putAll(PropertySupport.filter(PropertyFilter.ITERABLE_FILTER, InstEngNifs.PROPERTY_MAP));
        PROPERTY_MAP = Collections.unmodifiableMap(map);
    }

    // The instrument name
    public static final String INSTRUMENT_NAME = "NIFS";

    /**
     * Default constructor.
     */
    public SeqConfigNIFS() {
        super(SeqConfigNIFS.SP_TYPE, SeqConfigNIFS.SYSTEM_NAME);
    }

    public Map<String, PropertyDescriptor> getProperties() {
        return PROPERTY_MAP;
    }
}
