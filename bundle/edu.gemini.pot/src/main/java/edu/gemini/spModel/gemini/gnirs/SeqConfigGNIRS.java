// Copyright 1997 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: SeqConfigGNIRS.java 27463 2010-10-13 17:52:24Z swalker $
//
package edu.gemini.spModel.gemini.gnirs;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.data.property.PropertyFilter;
import edu.gemini.spModel.data.property.PropertyProvider;
import edu.gemini.spModel.data.property.PropertySupport;
import edu.gemini.spModel.gemini.gnirs.GNIRSParams.Order;
import edu.gemini.spModel.seqcomp.SeqConfigNames;
import edu.gemini.spModel.seqcomp.SeqConfigObsBase;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

    // Return a list of default wavelengths
    private List _getDefaultWavelengths() {
        int n = GNIRSParams.Order.values().length - 2; // skip orders 7 and 8
        ArrayList<String> l = new ArrayList<String>(n);
        for (int i = 0; i < n; i++) {
            Order order = Order.values()[i];
            l.add(String.valueOf(order.getDefaultWavelength()));
        }
        return l;
    }

    public Map<String, PropertyDescriptor> getProperties() {
        return PROPERTY_MAP;
    }

    /**
     * Return a configuration of available iterable items.
     */
    /**
    public ISysConfig getAvailableSysConfig(boolean isOnSite, boolean hasEngComp) {
        // Parent has no iterable items
        DefaultSysConfig sc = new DefaultSysConfig(SYSTEM_NAME);

        // Add iterable components.
        sc.putParameter(getExposureTimeParameter());
        sc.putParameter(EditableParameter.getInstance(GNIRSConstants.CENTRAL_WAVELENGTH_PROP, _getDefaultWavelengths()));
        sc.putParameter(EditableParameter.getInstance(GNIRSConstants.COADDS_PROP));
        sc.putParameter(DefaultParameter.getInstance(GNIRSConstants.FILTER_PROP, Arrays.asList(GNIRSParams.Filter.values())));
        sc.putParameter(DefaultParameter.getInstance(GNIRSConstants.ACQUISITION_MIRROR_PROP, Arrays.asList(GNIRSParams.AcquisitionMirror.values())));
        sc.putParameter(DefaultParameter.getInstance(GNIRSConstants.SLIT_WIDTH_PROP, Arrays.asList(SlitWidth.values())));
        sc.putParameter(DefaultParameter.getInstance(GNIRSConstants.DECKER_PROP, Arrays.asList(GNIRSParams.Decker.values())));
        sc.putParameter(DefaultParameter.getInstance(GNIRSConstants.CAMERA_PROP, Arrays.asList(GNIRSParams.Camera.values())));
        sc.putParameter(DefaultParameter.getInstance(GNIRSConstants.READ_MODE_PROP, Arrays.asList(GNIRSParams.ReadMode.values())));
        sc.putParameter(DefaultParameter.getInstance(GNIRSConstants.DISPERSER_PROP, Arrays.asList(Disperser.values())));
        sc.putParameter(DefaultParameter.getInstance(GNIRSConstants.CROSS_DISPERSED_PROP, Arrays.asList(GNIRSParams.CrossDispersed.values())));

        return sc;
    }
     **/
}
