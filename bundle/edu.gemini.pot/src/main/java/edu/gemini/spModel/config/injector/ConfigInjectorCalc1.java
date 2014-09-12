//
// $
//


package edu.gemini.spModel.config.injector;

import java.beans.PropertyDescriptor;

/**
 * Describes the interface for injecting a sequence item for
 * instruments that calculate it using a single property.
 */
public interface ConfigInjectorCalc1<A, R> {

    /**
     * Gets the property descriptor associated with the property.
     */
    PropertyDescriptor descriptor1();

    /**
     * Gets the name of property to inject in the instrument configuration.
     */
    String resultPropertyName();

    /**
     * Calculates the sequence item from the given property value.
     *
     * @param _1 non-null value for the property
     *
     * @return calculated item
     */
    R apply(A _1);
}
