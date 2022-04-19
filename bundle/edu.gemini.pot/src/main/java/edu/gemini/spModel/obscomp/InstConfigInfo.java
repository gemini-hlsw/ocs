package edu.gemini.spModel.obscomp;

import edu.gemini.spModel.type.ObsoletableSpType;
import edu.gemini.spModel.data.property.PropertySupport;
import edu.gemini.shared.util.immutable.Option;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Simple class describing instrument specific configuration options.
 */
public final class InstConfigInfo {
    // The display name of the configuration parameter
    private final String _name;

    // Description of the configuration parameter
    private final String _description;

    // The property name for the parameter value, as used in calls to SPInstObsComp.doAddToMap(Map)
    private final String _propertyName;

    // This is an optional property.
    private final boolean _optional;

    // The list of possible values for the parameter, or null if not known
    private final Class<?>  _enumType;
    private final Enum<?>[] _allEnumValues;
    private final Enum<?>[] _validEnumValues;

    // If true, item can be used as a query parameter, otherwise only in the results
    private final boolean _queryable;

    public <T extends Enum<T>> InstConfigInfo(
      String    name,
      String    propertyName,
      String    description,
      boolean   optional,
      Class<T>  enumType,
      T[]       allEnumValues,
      T[]       validEnumValues,
      boolean   queryable
    ) {
        _name            = name;
        _propertyName    = propertyName;
        _description     = description;
        _optional        = optional;
        _enumType        = enumType;
        _allEnumValues   = allEnumValues;
        _validEnumValues = validEnumValues;
        _queryable       = queryable;
    }

    /**
     * Initializes the InstInfoConfig taking all the values from the provided
     * PropertyDescriptor.
     */
    public InstConfigInfo(PropertyDescriptor pd, boolean queryable) {
        _name         = pd.getDisplayName();
        _propertyName = pd.getName();
        _description  = pd.getShortDescription();

        Class<?> c = pd.getPropertyType();
        _optional        = Option.class.isAssignableFrom(c);
        _enumType        = getEnumType(pd);
        _allEnumValues   = allEnumValues(_enumType);
        _validEnumValues = validEnumValues(_enumType);
        _queryable       = queryable;
    }

    /**
     * Initializes the InstInfoConfig taking all the values from the provided
     * PropertyDescriptor.
     */
    public InstConfigInfo(PropertyDescriptor pd) {
        this(pd, true);
    }

    private static Class<?> getEnumType(PropertyDescriptor pd) {
        Class<?> c = pd.getPropertyType();
        if (Option.class.isAssignableFrom(c)) {
            c = PropertySupport.getWrappedType(pd);
        }
        return c.isEnum() ? c : null;
    }

    public Class<?> getEnumType() {
        return _enumType;
    }

    /** The display name of the configuration parameter */
    public String getName() {
        return _name;
    }

    /** Return a description of the configuration parameter */
    public String getDescription() {
        return _description;
    }

    /** The property name for the parameter value, as used in calls to SPInstObsComp.doAddToMap(Map) */
    public String getPropertyName() {
        return _propertyName;
    }

    public Enum<?>[] getAllTypes() {
        return _allEnumValues == null ? null : Arrays.copyOf(_allEnumValues, _allEnumValues.length);
    }

    private static Enum<?>[] allEnumValues(Class<?> enumType) {
        return (enumType == null) ? null : (Enum<?>[]) enumType.getEnumConstants();
    }

    public Enum<?>[] getValidTypes() {
        return _validEnumValues == null ? null : Arrays.copyOf(_validEnumValues, _validEnumValues.length);
    }

    public static Enum<?>[] validEnumValues(Class<?> enumType) {
        if (enumType == null) return null;

        if (!ObsoletableSpType.class.isAssignableFrom(enumType)) {
            return allEnumValues(enumType);
        }

        Enum<?>[] consts = (Enum<?>[]) enumType.getEnumConstants();
        List<Enum<?>> res = new ArrayList<>(consts.length);

        for (Enum<?> val : consts) {
            if (((ObsoletableSpType) val).isObsolete()) continue;
            res.add(val);
        }

        return res.toArray((Enum<?>[]) Array.newInstance(enumType, res.size()));
    }

    /**
     * Return true if the item can be used as a query parameter, otherwise it is only used
     * in the results
     */
    public boolean isQueryable() {
        return _queryable;
    }

    /**
     * True if this is an optional property.
     */
    public boolean isOptional() {
        return _optional;
    }
}
