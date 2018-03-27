package edu.gemini.spModel.gemini.bhros;

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
//$Id: SeqConfigBHROS.java 7077 2006-05-26 19:56:48Z anunez $

public class SeqConfigBHROS extends SeqConfigObsBase implements PropertyProvider {

    private static final long serialVersionUID = 2L;

    public static final SPComponentType SP_TYPE = SPComponentType.ITERATOR_BHROS;

    public static final ISPNodeInitializer<ISPSeqComponent, SeqConfigBHROS> NI =
        new ComponentNodeInitializer<>(SP_TYPE, () -> new SeqConfigBHROS(), c -> new SeqConfigBHROSCB(c));

    public static final String SYSTEM_NAME = SeqConfigNames.INSTRUMENT_CONFIG_NAME;

    public static final Map<String, PropertyDescriptor> PROPERTY_MAP =
            Collections.unmodifiableMap(
                    PropertySupport.filter(PropertyFilter.ITERABLE_FILTER, InstBHROS.PROPERTY_MAP)
            );


    public SeqConfigBHROS() {
        super(SeqConfigBHROS.SP_TYPE, SeqConfigBHROS.SYSTEM_NAME);
    }

    public Map<String, PropertyDescriptor> getProperties() {
        return PROPERTY_MAP;
    }
}
