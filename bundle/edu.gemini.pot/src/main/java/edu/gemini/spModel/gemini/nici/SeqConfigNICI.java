package edu.gemini.spModel.gemini.nici;

import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.data.property.PropertyProvider;
import edu.gemini.spModel.data.property.PropertySupport;
import edu.gemini.spModel.data.property.PropertyFilter;
import edu.gemini.spModel.gemini.init.ComponentNodeInitializer;
import edu.gemini.spModel.seqcomp.SeqConfigNames;
import edu.gemini.spModel.seqcomp.SeqConfigObsBase;
import java.util.Map;
import java.util.Collections;
import java.beans.PropertyDescriptor;

public class SeqConfigNICI extends SeqConfigObsBase implements PropertyProvider {

    private static final long serialVersionUID = 2L;

    public static final SPComponentType SP_TYPE = SPComponentType.ITERATOR_NICI;

    public static final ISPNodeInitializer<ISPSeqComponent, SeqConfigNICI> NI =
        new ComponentNodeInitializer<>(SP_TYPE, () -> new SeqConfigNICI(), c -> new SeqConfigNICICB(c));

    public static final String SYSTEM_NAME = SeqConfigNames.INSTRUMENT_CONFIG_NAME;

    public static final Map<String, PropertyDescriptor> PROPERTY_MAP =
           Collections.unmodifiableMap(
             PropertySupport.filter(PropertyFilter.ITERABLE_FILTER, InstNICI.PROPERTY_MAP)
           );

    public SeqConfigNICI() {
        super(SeqConfigNICI.SP_TYPE, SeqConfigNICI.SYSTEM_NAME);
    }

    public Map<String, PropertyDescriptor> getProperties() {
        return PROPERTY_MAP;
    }
}
