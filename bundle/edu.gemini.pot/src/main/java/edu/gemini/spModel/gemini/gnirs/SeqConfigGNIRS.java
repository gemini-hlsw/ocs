package edu.gemini.spModel.gemini.gnirs;

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
 * The GNIRS configuration iterator.
 */
public class SeqConfigGNIRS extends SeqConfigObsBase implements PropertyProvider {

    private static final long serialVersionUID = 2500198025349451694L;

    /**
     * This iter component's SP type.
     */
    public static final SPComponentType SP_TYPE = SPComponentType.ITERATOR_GNIRS;

    public static final ISPNodeInitializer<ISPSeqComponent, SeqConfigGNIRS> NI =
        new ComponentNodeInitializer<>(SP_TYPE, () -> new SeqConfigGNIRS(), c -> new SeqConfigGNIRSCB(c));

    // The system name
    public static final String SYSTEM_NAME = SeqConfigNames.INSTRUMENT_CONFIG_NAME;


    private String _VERISON =  "2010B-2";

    public static final Map<String, PropertyDescriptor> PROPERTY_MAP =
         Collections.unmodifiableMap(
           PropertySupport.filter(PropertyFilter.ITERABLE_FILTER, InstGNIRS.PROPERTY_MAP)
         );


    /**
     * Default constructor.
     */
    public SeqConfigGNIRS() {
        super(SP_TYPE, SYSTEM_NAME);
        setVersion(_VERISON);
    }


    public Map<String, PropertyDescriptor> getProperties() {
        return PROPERTY_MAP;
    }

}
