// Copyright (c) 2016-2018 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.spModel.config.injector;

import java.beans.PropertyDescriptor;

/**
 * Describes the interface for injecting a sequence item for instruments,
 * calculating the value using four properties.
 */
public interface ConfigInjectorCalc4<A, B, C, D, R> {

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
     * Gets the property descriptor associated with the fourth property as
     * defined by the {@link #apply} method.
     */
    PropertyDescriptor descriptor4();

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
     * @param _4 non-null value for the fourth property
     *
     * @return calculated item
     */
    R apply(A _1, B _2, C _3, D _4);

}
