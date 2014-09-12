//
// $Id: PropertySupport.java 38242 2011-10-26 12:55:06Z abrighton $
//

package edu.gemini.spModel.data.property;

import edu.gemini.shared.util.StringUtil;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.type.SpTypeUtil;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public final class PropertySupport {
    private static final Logger LOG = Logger.getLogger(PropertySupport.class.getName());

    private static final String ENGINEERING_ATTR  = "engineering";
    private static final String QUERYABLE_ATTR    = "queryable";
    private static final String ITERABLE_ATTR     = "iterable";
    private static final String VOLATILE_ATTR     = "volatile";
    private static final String WRAPPED_TYPE_ATTR = "wrappedType";

    // Gets the value of a boolean custom attribute, using defaultVal if not
    // specified.
    private static boolean isBooleanAttr(PropertyDescriptor pd, String attr, boolean defaultVal) {
        Boolean b = (Boolean) pd.getValue(attr);
        if (b == null) return defaultVal;
        return b;
    }

    // Sets the value of a boolean custom attribute.
    private static void setBooleanAttr(PropertyDescriptor pd, String attr, boolean newValue) {
        pd.setValue(attr, newValue);
    }

    public static boolean isEngineering(PropertyDescriptor pd) {
        return isBooleanAttr(pd, ENGINEERING_ATTR, false);
    }

    public static void setEngineering(PropertyDescriptor pd, boolean isEngineering) {
        setBooleanAttr(pd, ENGINEERING_ATTR, isEngineering);
    }

    /**
     * Returns <code>true</code> if the property changes automatically as a
     * result of updates to other properties in the same bean.
     */
    public static boolean isVolatile(PropertyDescriptor pd) {
        return isBooleanAttr(pd, VOLATILE_ATTR, false);
    }

    /**
     * Sets whether this property changes automatically as a result of updates
     * to other properties in the same bean.
     */
    public static void setVolatile(PropertyDescriptor pd, boolean isVolatile) {
        setBooleanAttr(pd, VOLATILE_ATTR, isVolatile);
    }

    public static boolean isQueryable(PropertyDescriptor pd) {
        return isBooleanAttr(pd, QUERYABLE_ATTR, true);
    }

    public static void setQueryable(PropertyDescriptor pd, boolean isQueryable) {
        setBooleanAttr(pd, QUERYABLE_ATTR, isQueryable);
    }

    public static boolean isIterable(PropertyDescriptor pd) {
        return isBooleanAttr(pd, ITERABLE_ATTR, true);
    }

    public static void setIterable(PropertyDescriptor pd, boolean isIterable) {
        setBooleanAttr(pd, ITERABLE_ATTR, isIterable);
    }

    /**
     * Returns the type of properties whose first order type wraps another
     * object.  For example, {@link edu.gemini.shared.util.immutable.Option Option}
     * types contain other types. Given a property whose type is, for instance,
     * <code>Option&lt;UtilityWheel&gt;</code>, due to type erasure we have no
     * way at runtime to know the fundamental underlying type is actually
     * <code>UtilityWheel</code>.
     *
     * <p>The "wrapped type" attribute of property descriptors will be used to
     * keep this information so that it is available at runtime.
     */
    public static Class getWrappedType(PropertyDescriptor pd) {
        return (Class) pd.getValue(WRAPPED_TYPE_ATTR);
    }

    /**
     * See {@link #getWrappedType(java.beans.PropertyDescriptor)}.
     */
    public static void setWrappedType(PropertyDescriptor pd, Class type) {
        pd.setValue(WRAPPED_TYPE_ATTR, type);
    }

    public static Map<String, PropertyDescriptor> map(Collection<PropertyDescriptor> props) {
        Map<String, PropertyDescriptor> res;
        res = new TreeMap<String, PropertyDescriptor>();

        for (PropertyDescriptor pd : props) {
            res.put(pd.getName(), pd);
        }

        return res;
    }

    public static List<PropertyDescriptor> sortByDisplayName(Collection<PropertyDescriptor> props) {
        List<PropertyDescriptor> res;
        res = new ArrayList<PropertyDescriptor>(props);

        Collections.sort(res, new Comparator<PropertyDescriptor>() {
            public int compare(PropertyDescriptor pd1, PropertyDescriptor pd2) {
                return pd1.getDisplayName().compareTo(pd2.getDisplayName());
            }
        });

        return res;
    }

    public static Collection<PropertyDescriptor> filter(PropertyFilter filter, Collection<PropertyDescriptor> props) {
        ArrayList<PropertyDescriptor> res;
        res = new ArrayList<PropertyDescriptor>(props.size());

        for (PropertyDescriptor pd : props) {
            if (filter.accept(pd)) res.add(pd);
        }

        res.trimToSize();
        return res;
    }

    public static Map<String, PropertyDescriptor> filter(PropertyFilter filter, Map<String, PropertyDescriptor> props) {
        return map(filter(filter, props.values()));
    }

    private static void _logEnumConvertFailure(String text, Class<Enum> propertyType) {
        LOG.log(Level.WARNING, "Could not convert '" + text + "' for type: " +
                              propertyType.getName());
    }

//    private static void _logEnumConvertSuccess(String text, Class<Enum> propertyType, Enum e) {
//        LOG.log(Level.WARNING, "* Converted '" + text + "' for type: " +
//                              propertyType.getName() + ", to '" + e + "'");
//    }

    private static Object getEnumValue(String text, Class<Enum> propertyType) {
        // Handle empty strings.
        if (text == null) return null;
        text = text.trim();
        if ("".equals(text)) return null;

        // For backwards compatibility, call this method instead of just
        // Enum.valueOf().  Maybe after 2006B we can drop this.  It will try
        // Enum.valueOf(), but failing that attempt to look up the corresponding
        // enum value based upon the display name (which is what we stored in
        // XML prior to 2006B).
        Object val = SpTypeUtil.oldValueOf(propertyType, text, null, false);
        if (val != null) return val;

        // Now we get a bit whacky and try to call a static method which has
        // to be called "get<propertytype>".  This method is where conversion
        // from old SPType values that are no longer supported to new values
        // typically takes place.  This is another thing that would be nice to
        // get rid of for 2007A.
        String tmp = propertyType.getName();

        // trim off everything up to the last .  For example, turn
        // edu.gemini.spModel.gemini.niri.Niri$Filter into
        // Niri$Filter
        int i = tmp.lastIndexOf('.');
        tmp = tmp.substring(i+1);

        // Now trim everything before and including the last $
        i = tmp.lastIndexOf('$');
        tmp = tmp.substring(i + 1);

        String methodName = "get" + tmp;

        try {
            // ex: Filter.getFilter("H", Filter.J);  // explicit default value
            Method m = propertyType.getMethod(methodName, String.class, propertyType);
            if (m != null) {
                Object res = m.invoke(null, text, null);
                if (res != null) return res;
//                if (res != null) {
//                    _logEnumConvertSuccess(text, propertyType, res);
//                }
//                return res;
            }
        } catch(Exception ex) {
            // give up
        }
        _logEnumConvertFailure(text, propertyType);
        return null;
    }

    private static final String NONE_STR = "";

    private static Object _stringToValue(String text, PropertyEditor ed) {
        if (ed == null) return null;
        try {
            ed.setAsText(text);
            return ed.getValue();
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static Object _stringToValue(String text, Class propertyType) {
        if (propertyType.isEnum()) {
            //noinspection unchecked
            return getEnumValue(text, (Class<Enum>) propertyType);
        }
        return _stringToValue(text, PropertyEditorManager.findEditor(propertyType));
    }

    public static Object stringToValue(String text, PropertyDescriptor pd) {
        if (text == null) return null;

        Class propertyType = pd.getPropertyType();

        if (Option.class.isAssignableFrom(propertyType)) {
            if (NONE_STR.equals(text)) return None.instance();
            propertyType = getWrappedType(pd);

            Object res = _stringToValue(text, propertyType);
            if (res == null) return None.instance();
            return new Some<Object>(res);
        }

        return _stringToValue(text, propertyType);
    }


    public static List<Object> stringToValue(Collection<String> strings, PropertyDescriptor desc) {
        List<Object> res = new ArrayList<Object>(strings.size());
        for (String str : strings) {
            res.add(stringToValue(str, desc));
        }
        return res;
    }


    private static String _valueToString(Object value, PropertyEditor ed) {
        if (ed == null) {
            return (value == null) ? "" : value.toString();
        } else if (value == null) {
            return "";
        }
        ed.setValue(value);
        return ed.getAsText();
    }

    public static String valueToString(Object value, PropertyDescriptor desc) {
        if (value == null) return "";
        if (desc == null) return value.toString();

        Class propertyType = desc.getPropertyType();

        if (Option.class.isAssignableFrom(propertyType)) {
            if (None.instance().equals(value)) return NONE_STR;
            propertyType = getWrappedType(desc);

            // Unwrap the value
            value = ((Some) value).getValue();
        }

        if (propertyType.isEnum()) {
            return ((Enum) value).name();
        }

        PropertyEditor ed = PropertyEditorManager.findEditor(propertyType);
        return _valueToString(value, ed);
    }

    public static List<String> valueToString(Collection<Object> values, PropertyDescriptor desc) {
        List<String> res = new ArrayList<String>(values.size());
        for (Object val : values) {
            res.add(valueToString(val, desc));
        }
        return res;
    }

    public static PropertyDescriptor init(String propertyName, Class beanClass,
                                          boolean isQueryable, boolean isIterable) {
        try {
            PropertyDescriptor pd = new PropertyDescriptor(propertyName, beanClass);
            pd.setDisplayName(StringUtil.toDisplayName(propertyName));
            setQueryable(pd, isQueryable);
            setIterable(pd, isIterable);
            return pd;
        } catch (IntrospectionException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static PropertyDescriptor init(String propertyName, String displayName, Class beanClass,
                                          boolean isQueryable, boolean isIterable) {
        try {
            PropertyDescriptor pd = new PropertyDescriptor(propertyName, beanClass);
            pd.setDisplayName(displayName);
            setQueryable(pd, isQueryable);
            setIterable(pd, isIterable);
            return pd;
        } catch (IntrospectionException ex) {
            throw new RuntimeException(ex);
        }
    }
}
