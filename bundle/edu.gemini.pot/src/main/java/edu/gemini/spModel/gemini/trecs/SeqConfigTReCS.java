// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: SeqConfigTReCS.java 7066 2006-05-25 20:38:59Z shane $
//

package edu.gemini.spModel.gemini.trecs;

import edu.gemini.pot.sp.ISPNodeInitializer;
import edu.gemini.pot.sp.ISPSeqComponent;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.data.property.PropertyProvider;
import edu.gemini.spModel.data.property.PropertySupport;
import edu.gemini.spModel.data.property.PropertyFilter;
import edu.gemini.spModel.data.config.ISysConfig;
import edu.gemini.spModel.gemini.init.ComponentNodeInitializer;
import edu.gemini.spModel.seqcomp.SeqConfigNames;
import edu.gemini.spModel.seqcomp.SeqConfigObsBase;

import java.util.Map;
import java.util.Collections;
import java.beans.PropertyDescriptor;

/**
 * The TReCS configuration iterator.
 */
public class SeqConfigTReCS extends SeqConfigObsBase implements PropertyProvider {

    private static final long serialVersionUID = 1L;

    /**
     * This iter component's SP type.
     */
    public static final SPComponentType SP_TYPE = SPComponentType.ITERATOR_TRECS;

    public static final ISPNodeInitializer<ISPSeqComponent, SeqConfigTReCS> NI =
        new ComponentNodeInitializer<>(SP_TYPE, () -> new SeqConfigTReCS(), c -> new SeqConfigTReCSCB(c));

    public static final Map<String, PropertyDescriptor> PROPERTY_MAP;

    static {
        Map<String, PropertyDescriptor> map;
        map = PropertySupport.filter(PropertyFilter.ITERABLE_FILTER, InstTReCS.PROPERTY_MAP);
        map.putAll(PropertySupport.filter(PropertyFilter.ITERABLE_FILTER, InstEngTReCS.PROPERTY_MAP));
        PROPERTY_MAP = Collections.unmodifiableMap(map);
    }


    // The system name
    public static final String SYSTEM_NAME = SeqConfigNames.INSTRUMENT_CONFIG_NAME;
    // The instrument name
    public static final String INSTRUMENT_NAME = InstTReCS.INSTRUMENT_NAME_PROP;

    /**
     * Default constructor.
     */
    public SeqConfigTReCS() {
        super(SP_TYPE, SYSTEM_NAME);
    }

    public Map<String, PropertyDescriptor> getProperties() {
        return PROPERTY_MAP;
    }

    public ISysConfig getSysConfig() {
         ISysConfig sc = super.getSysConfig();
         // XXX temp: This parameter was removed by request: see OT-172
         if (sc.containsParameter(InstEngTReCS.EXPOSURE_TIME_PROP.getName())) {
             sc.removeParameter(InstEngTReCS.EXPOSURE_TIME_PROP.getName());
             setSysConfig(sc);
         }
         // XXX temp
         return sc;
     }


}
