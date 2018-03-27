package edu.gemini.spModel.gemini.gpi;

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

public class SeqConfigGpi extends SeqConfigObsBase implements PropertyProvider {

    private static final long serialVersionUID = 1;

    /**
     * This iter component's SP type.
     */
    public static final SPComponentType SP_TYPE =
            SPComponentType.ITERATOR_GPI;

    public static final ISPNodeInitializer<ISPSeqComponent, SeqConfigGpi> NI =
        new ComponentNodeInitializer<>(SP_TYPE, () -> new SeqConfigGpi(), c -> new SeqConfigGpiCB(c));

    public static final String SYSTEM_NAME = SeqConfigNames.INSTRUMENT_CONFIG_NAME;
    public static final Map<String, PropertyDescriptor> PROPERTY_MAP =
           Collections.unmodifiableMap(
                   PropertySupport.filter(PropertyFilter.ITERABLE_FILTER, Gpi.PROPERTY_MAP)
           );

    /**
     * Default constructor.
     */
    public SeqConfigGpi() {
        super(SeqConfigGpi.SP_TYPE, SeqConfigGpi.SYSTEM_NAME);
    }

    public Map<String, PropertyDescriptor> getProperties() {
        return PROPERTY_MAP;
    }
}
