//
// $
//

package edu.gemini.spModel.config.injector;

import java.beans.PropertyDescriptor;

/**
 * Describes the interface for injecting a sequence item for
 * instruments that calculate it using two properties.
 */
public interface ConfigInjectorCalc2<A, B, R> {

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
     * Gets the name of property to inject in the instrument configuration.
     */
    String resultPropertyName();

    /**
     * Calculates the sequence item from the given property values.
     *
     * @param _1 non-null value for the first property
     * @param _2 non-null value for the second property
     *
     * @return calculated item
     */
    R apply(A _1, B _2);
}
