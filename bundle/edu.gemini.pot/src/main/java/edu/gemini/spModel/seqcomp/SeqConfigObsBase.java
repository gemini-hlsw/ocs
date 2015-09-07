// Copyright 1997-2000
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: SeqConfigObsBase.java 7030 2006-05-11 17:55:34Z shane $
//
package edu.gemini.spModel.seqcomp;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.data.config.EditableParameter;
import edu.gemini.spModel.data.config.IParameter;
import edu.gemini.spModel.data.property.PropertySupport;
import edu.gemini.spModel.obscomp.InstConstants;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A configuration iterator base class for iterators that include
 * exposure time repeat and coadd attributes.  This is designed for
 * iterators that use the IConfigProvider interface.
 */
public class SeqConfigObsBase extends SeqConfigComp {

    // for serialization
    private static final long serialVersionUID = 1L;

    /**
     * Construct given a specific type.
     */
    public SeqConfigObsBase(SPComponentType spType) {
        super(spType);
    }

    /**
     * Construct given a type and the name for the configuration.
     */
    public SeqConfigObsBase(SPComponentType spType, String configName) {
        super(spType, configName);
    }

    /**
     * Get the exposure time IParameter.
     */
    protected IParameter getExposureTimeParameter() {
        // Since it has a wide range of values, the value is a null
        return EditableParameter.getInstance(InstConstants.EXPOSURE_TIME_PROP);
    }

    /**
     * Get the coadds IParameter.
     */
    protected IParameter getCoaddsParameter() {
        // Since it has a wide range of values, the value is a null
        return EditableParameter.getInstance(InstConstants.COADDS_PROP);
    }

    /**
     * Get the repeatCount IParameter.
     */
    protected IParameter getRepeatCountParameter() {
        // Since it has a wide range of values, the value is a null
        return EditableParameter.getInstance(InstConstants.REPEAT_COUNT_PROP);
    }

    public String getEditableTitle() {
        String s = getType().readableStr;
        String title = super.getTitle();
        if (s.equals(title)) {
            return title; // default title
        }
        // Get the instrument name part of the readable string
        s = s.replaceAll(" Sequence", "");
        String regExp = "^" + s + ": ";
        // Remove the "instName: " part from the start of the title, since it is
        // automatically inserted
        return title.replaceAll(regExp, "");
    }

    public void setTitle(String newValue) {
        String s = getType().readableStr;
        if (s.equals(newValue)) {
            // Use the default title
            super.setTitle(newValue);
            return;
        }
        // make sure string starts with "instName: "
        s = s.replaceAll(" Sequence", "");
        String regExp = "^" + s + ": ";
        super.setTitle(s + ": " + newValue.replaceAll(regExp, ""));
    }

    protected static Map<String, PropertyDescriptor> mapIterableProperties(Collection<PropertyDescriptor> allProps) {
        List<PropertyDescriptor> tmp = new ArrayList<PropertyDescriptor>(allProps.size());
        for (PropertyDescriptor pd : allProps) {
            if (PropertySupport.isIterable(pd)) tmp.add(pd);
        }
        return PropertySupport.map(tmp);
    }
}
