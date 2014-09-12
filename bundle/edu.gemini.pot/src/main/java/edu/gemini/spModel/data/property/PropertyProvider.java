//
// $Id: PropertyProvider.java 7030 2006-05-11 17:55:34Z shane $
//

package edu.gemini.spModel.data.property;

import java.beans.PropertyDescriptor;
import java.util.Map;

/**
 *
 */
public interface PropertyProvider {
    Map<String, PropertyDescriptor> getProperties();
}
