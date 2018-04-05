/**
 * $Id: SeqConfigFlamingos2.java 18463 2009-03-05 13:47:36Z swalker $
 */

package edu.gemini.spModel.gemini.flamingos2;

import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.data.property.PropertySupport;
import edu.gemini.spModel.data.property.PropertyFilter;
import edu.gemini.spModel.data.property.PropertyProvider;
import edu.gemini.spModel.gemini.init.ComponentNodeInitializer;
import edu.gemini.spModel.seqcomp.SeqConfigNames;
import edu.gemini.spModel.seqcomp.SeqConfigObsBase;

import java.beans.PropertyDescriptor;
import java.util.Map;
import java.util.Collections;

public class SeqConfigFlamingos2 extends SeqConfigObsBase implements PropertyProvider {

    private static final long serialVersionUID = 1;

    /**
     * This iter component's SP type.
     */
    public static final SPComponentType SP_TYPE =
            SPComponentType.ITERATOR_FLAMINGOS2;

    public static final ISPNodeInitializer<ISPSeqComponent, SeqConfigFlamingos2> NI =
        new ComponentNodeInitializer<>(SP_TYPE, () -> new SeqConfigFlamingos2(), c -> new SeqConfigFlamingos2CB(c));

    public static final String SYSTEM_NAME = SeqConfigNames.INSTRUMENT_CONFIG_NAME;
    public static final Map<String, PropertyDescriptor> PROPERTY_MAP =
           Collections.unmodifiableMap(
             PropertySupport.filter(PropertyFilter.ITERABLE_FILTER, Flamingos2.PROPERTY_MAP)
           );

    /**
     * Default constructor.
     */
    public SeqConfigFlamingos2() {
        super(SeqConfigFlamingos2.SP_TYPE, SeqConfigFlamingos2.SYSTEM_NAME);
    }

    public Map<String, PropertyDescriptor> getProperties() {
        return PROPERTY_MAP;
    }
}
