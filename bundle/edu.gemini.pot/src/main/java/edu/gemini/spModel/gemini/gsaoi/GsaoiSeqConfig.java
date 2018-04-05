//
// $
//

package edu.gemini.spModel.gemini.gsaoi;

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
 * The GSAOI configuration builder.
 */
public final class GsaoiSeqConfig extends SeqConfigObsBase implements PropertyProvider {

    public static final SPComponentType SP_TYPE =
            SPComponentType.ITERATOR_GSAOI;

    public static final ISPNodeInitializer<ISPSeqComponent, GsaoiSeqConfig> NI =
        new ComponentNodeInitializer<>(SP_TYPE, () -> new GsaoiSeqConfig(), c -> new GsaoiSeqConfigCB(c));

    public static final String SYSTEM_NAME = SeqConfigNames.INSTRUMENT_CONFIG_NAME;
    public static final String INSTRUMENT_NAME = "GSAOI";

    public static final Map<String, PropertyDescriptor> PROPERTY_MAP =
            Collections.unmodifiableMap(
                PropertySupport.filter(PropertyFilter.ITERABLE_FILTER, Gsaoi.PROPERTY_MAP)
            );

    public GsaoiSeqConfig() {
        super(SP_TYPE, SYSTEM_NAME);
    }

    public Map<String, PropertyDescriptor> getProperties() {
        return PROPERTY_MAP;
    }
}
