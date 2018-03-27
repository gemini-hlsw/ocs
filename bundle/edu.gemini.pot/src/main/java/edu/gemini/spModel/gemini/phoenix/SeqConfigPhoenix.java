package edu.gemini.spModel.gemini.phoenix;

import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.data.property.PropertyProvider;
import edu.gemini.spModel.data.property.PropertySupport;
import edu.gemini.spModel.data.property.PropertyFilter;
import edu.gemini.spModel.gemini.init.ComponentNodeInitializer;
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

    public static final ISPNodeInitializer<ISPSeqComponent, SeqConfigPhoenix> NI =
        new ComponentNodeInitializer<>(SP_TYPE, () -> new SeqConfigPhoenix(), c -> new SeqConfigPhoenixCB(c));

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
