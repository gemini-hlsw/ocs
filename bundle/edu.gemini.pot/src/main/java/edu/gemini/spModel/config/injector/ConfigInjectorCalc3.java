//
// $
//

package edu.gemini.spModel.config.injector;

import java.beans.PropertyDescriptor;

/**
 * Describes the interface for injecting a sequence item for
 * instruments that calculate it using three properties.
 */
public interface ConfigInjectorCalc3<A, B, C, R> {

    /**
     * Gets the property descriptor associated with the first property as
     * defined by the {@link #apply} method.
     */
    PropertyDescriptor descriptor1();

    /**
     * Gets the property descriptor associated with the second property as
     * defined by the {@link #apply} method.
     */
    PropertyDescriptor descriptor2();

    /**
     * Gets the property descriptor associated with the third property as
     * defined by the {@link #apply} method.
     */
    PropertyDescriptor descriptor3();

    /**
     * Gets the name of property to inject in the instrument configuration.
     */
    String resultPropertyName();

    /**
     * Calculates the sequence item from the given property values.
     *
     * @param _1 non-null value for the first property
     * @param _2 non-null value for the second property
     * @param _3 non-null value for the third property
     *
     * @return calculated item
     */
    R apply(A _1, B _2, C _3);
}
