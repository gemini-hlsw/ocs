package edu.gemini.spModel.gemini.igrins2;

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

final public class SeqConfigIgrins2 extends SeqConfigObsBase implements PropertyProvider {

    public static final SPComponentType SP_TYPE =
        SPComponentType.ITERATOR_IGRINS2;

    public static final ISPNodeInitializer<ISPSeqComponent, SeqConfigIgrins2> NI =
        new ComponentNodeInitializer<>(SP_TYPE, SeqConfigIgrins2::new, SeqConfigIgrins2CB::new);

    public static final String SYSTEM_NAME = SeqConfigNames.INSTRUMENT_CONFIG_NAME;

    public static final Map<String, PropertyDescriptor> PROPERTY_MAP =
        Collections.unmodifiableMap(
            PropertySupport.filter(PropertyFilter.ITERABLE_FILTER, Igrins2$.MODULE$.PropertyMap())
        );

    public SeqConfigIgrins2() {
        super(SP_TYPE, SYSTEM_NAME);
    }

    @Override
    public Map<String, PropertyDescriptor> getProperties() {
        return PROPERTY_MAP;
    }
}
