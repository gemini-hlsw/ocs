// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: SeqConfigNIRI.java 7030 2006-05-11 17:55:34Z shane $
//
package edu.gemini.spModel.gemini.niri;

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
 * The NIRI configuration iterator.
 */
public class SeqConfigNIRI extends SeqConfigObsBase implements PropertyProvider {

    private static final long serialVersionUID = 2216318242869064507L;

    /**
     * This iter component's SP type.
     */
    public static final SPComponentType SP_TYPE =
            SPComponentType.ITERATOR_NIRI;

    public static final ISPNodeInitializer<ISPSeqComponent, SeqConfigNIRI> NI =
        new ComponentNodeInitializer<>(SP_TYPE, () -> new SeqConfigNIRI(), c -> new SeqConfigNIRICB(c));

    // The system name
    public static final String SYSTEM_NAME = SeqConfigNames.INSTRUMENT_CONFIG_NAME;
    // The instrument name
    public static final String INSTRUMENT_NAME = "NIRI";

    public static final Map<String, PropertyDescriptor> PROPERTY_MAP =
        Collections.unmodifiableMap(
          PropertySupport.filter(PropertyFilter.ITERABLE_FILTER, InstNIRI.PROPERTY_MAP)
        );


    /**
     * Default constructor.
     */
    public SeqConfigNIRI() {
        super(SP_TYPE, SYSTEM_NAME);
    }

    public Map<String, PropertyDescriptor> getProperties() {
        return PROPERTY_MAP;
    }
}
