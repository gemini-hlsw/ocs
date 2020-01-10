//
// $Id: Pio.java 45395 2012-05-22 16:00:37Z swalker $
//
package edu.gemini.spModel.pio;

import scala.Tuple2;
import squants.Dimension;
import squants.Quantity;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// This class represents a design tradeoff.  Though it might be easier for
// the client of PIO to use if all these convenience methods were placed
// directly in the Param, ParamSet, etc. classes, it would represent a burden
// on each implementation and a larger API for each interface.

/**
 * Pio is a utility class for working with the Parameter I/O package (PIO).
 * It gathers the implementation of several commonly used multi-step
 * operations in one place.  These are essentially convenience methods written
 * in terms of the {@link Param}, {@link ParamSet}, etc. interfaces.  The same
 * things may be accomplished using only the interfaces.
 */
public final class Pio {

    /**
     * Gets the first (or only) value of the first (or only) Param identified
     * by <code>path</code> (rooted at <code>context</code>).  If there is no
     * such node, or if the <code>path</code> identifies a node that is not a
     * <code>Param</code>, then <code>null</code> is returned.
     *
     * <p>This method is the same as calling
     * {@link #getValue(PioNodeParent, String, String)} with a <code>null</code>
     * default value.
     *
     * @param context serves as the point of reference the <code>path</code>
     * argument, assuming that <code>path</code> represents a relative path
     * @param path identifies the Param element whose (first) value should be
     * returned
     *
     * @return value of the Param identified by <code>path</code> if any;
     * <code>null</code> otherwise
     */
    public static String getValue(PioNodeParent context, String path) {
        return getValue(context, path, null);
    }

    /**
     * Gets the first (or only) value of the first (or only) Param identified
     * by <code>pd</code> (rooted at <code>context</code>).  If there is no
     * such node, or if the <code>pd</code> identifies a node that is not a
     * <code>Param</code>, then <code>null</code> is returned.
     *
     * @param context serves as the point of reference the <code>path</code>
     * argument, assuming that <code>path</code> represents a relative path
     * @param pd PropertyDescriptor whose name identifies the Param element
     * whose (first) value should be returned
     *
     * @return value of the Param identified by <code>pd</code> if any;
     * <code>null</code> otherwise
     */
    public static String getValue(PioNodeParent context, PropertyDescriptor pd) {
        return getValue(context, pd.getName());
    }

    /**
     * Gets the first (or only) value of the first (or only) Param identified
     * by <code>path</code> (rooted at <code>context</code>).  If there is no
     * such node, or if the <code>path</code> identifies a node that is not a
     * <code>Param</code>, then <code>def</code> is returned.
     *
     * @param context serves as the point of reference the <code>path</code>
     * argument, assuming that <code>path</code> represents a relative path
     * @param path identifies the Param element whose (first) value should be
     * returned
     * @param def default value that should be returned if the indicated
     * Param does not exist or has no values
     *
     * @return value of the Param identified by <code>path</code> if any;
     * <code>def</code> otherwise
     */
    public static String getValue(PioNodeParent context, String path, String def) {

        // Get the Param corresponding to the path.
        PioPath pp   = new PioPath(path);
        PioNode node = context.lookupNode(pp);
        if (!(node instanceof Param)) return def;

        // Return the value.
        String value = ((Param) node).getValue();
        if (value == null) value = def;
        return value;
    }

    /**
     * Gets the first (or only) value of the first (or only) Param identified
     * by <code>path</code> (rooted at <code>context</code>) interpreted as a
     * boolean.  If there is no such node, or if the <code>path</code>
     * identifies a node that is not a <code>Param</code>, then <code>def</code>
     * is returned.
     *
     * @param context serves as the point of reference the <code>path</code>
     * argument, assuming that <code>path</code> represents a relative path
     * @param path identifies the Param element whose (first) value should be
     * returned
     * @param def default value to return if there is no such Param object, or
     * if the value is not either <code>true</code> or <code>false</code>
     *
     * @return values of the Param identified by <code>path</code> if any;
     * <code>null</code> otherwise
     */
    @SuppressWarnings("unchecked")
	public static <E extends Enum<E>> E getEnumValue(PioNodeParent context, String path, E def) {
    	String name = getValue(context, path);
    	if (name == null) return def;

        Class<E> enumClass = getEnumClass((Class<E>) def.getClass());
        if (enumClass == null) return def;

    	try {
    		return Enum.valueOf(enumClass, name);
    	} catch (IllegalArgumentException iae) {
    		return def;
    	}
    }

    public static <E extends Enum<E>> E getEnumValue(PioNodeParent context, String path, Class<E> enumType) {
        final String name = getValue(context, path);
        if (name == null) return null;

        try {
            return Enum.valueOf(enumType, name);
        } catch (final IllegalArgumentException iae) {
            return null;
        }
    }

    // UX-1505.  An anonymous subclass of an enum value can't be used with
    // Enum.valueOf so we have to get its enclosing enum class in this case.
    private static <E extends Enum<E>> Class<E> getEnumClass(Class<E> c) {
        if (c.isEnum()) return c;
        Class<E> enc = (Class<E>) c.getEnclosingClass();
        return (enc == null) ? null : getEnumClass(enc);
    }

    /**
     * Adds a parameter with name <code>name</code> and boolean value
     * <code>value</code> to the ParamSet <code>parent</code>.
     *
     * @param factory PioFactory that should be used to create the parameter
     * @param parent node to which the new {@link Param} should be added
     * @param name name of the new {@link Param}
     * @param value initial Enum value of the new {@link Param}
     *
     * @return newly created {@link Param} that was added to {@link ParamSet}
     */
    public static <E extends Enum<E>> Param addEnumParam(PioFactory factory, ParamSet parent, String name, E value) {
        Param param = factory.createParam(name);
        param.setValue(value.name());
        parent.addParam(param);
        return param;
    }

    /**
     * Gets all of the values of the first (or only) Param identified
     * by <code>path</code> (rooted at <code>context</code>).  If there is no
     * such node, or if the <code>path</code> identifies a node that is not a
     * <code>Param</code>, then <code>null</code> is returned.
     *
     * @param context serves as the point of reference the <code>path</code>
     * argument, assuming that <code>path</code> represents a relative path
     * @param path identifies the Param element whose (first) value should be
     * returned
     *
     * @return values of the Param identified by <code>path</code> if any;
     * <code>null</code> otherwise
     */
    public static List<String> getValues(PioNodeParent context, String path) {
        // Get the Param corresponding to the path.
        PioPath pp   = new PioPath(path);
        PioNode node = context.lookupNode(pp);
        if (!(node instanceof Param)) return null;

        // Return the values.
        return ((Param) node).getValues();
    }

    public static <E extends Enum<E>> List<E> getEnumValues(PioNodeParent context, String path, Class<E> enumType) {
        List<String> strValues = getValues(context, path);
        if (strValues == null) return Collections.emptyList();

        List<E> res = new ArrayList<E>(strValues.size());
        try {
            for (String s : strValues) res.add(Enum.valueOf(enumType, s));
        } catch (IllegalArgumentException ex) {
            return Collections.emptyList();
        }
        return res;
    }

    /**
     * Gets the first (or only) value of the first (or only) Param identified
     * by <code>path</code> (rooted at <code>context</code>) interpreted as a
     * boolean.  If there is no such node, or if the <code>path</code>
     * identifies a node that is not a <code>Param</code>, then <code>def</code>
     * is returned.
     *
     * @param context serves as the point of reference the <code>path</code>
     * argument, assuming that <code>path</code> represents a relative path
     * @param path identifies the Param element whose (first) value should be
     * returned
     * @param def default value to return if there is no such Param object, or
     * if the value is not either <code>true</code> or <code>false</code>
     *
     * @return values of the Param identified by <code>path</code> if any;
     * <code>null</code> otherwise
     */
    public static boolean getBooleanValue(PioNodeParent context, String path, boolean def) {
        String strValue = getValue(context, path);
        if (strValue == null) return def;
        return "true".equalsIgnoreCase(strValue);
    }

    /**
     * Gets the first (or only) value of the first (or only) Param identified
     * by <code>path</code> (rooted at <code>context</code>) interpreted as an
     * int.  If there is no such node, or if the <code>path</code> identifies a
     * node that is not a <code>Param</code>, then <code>def</code> is returned.
     *
     * @param context serves as the point of reference the <code>path</code>
     * argument, assuming that <code>path</code> represents a relative path
     * @param path identifies the Param element whose (first) value should be
     * returned
     * @param def default value to return if there is no such Param object, or
     * if the value is not parseable as an int
     *
     * @return values of the Param identified by <code>path</code> if any;
     * <code>null</code> otherwise
     */
    public static int getIntValue(PioNodeParent context, String path, int def) {
        String strValue = getValue(context, path);
        if (strValue == null) return def;
        try {
            return Integer.parseInt(strValue);
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    /**
     * Gets the first (or only) value of the first (or only) Param identified
     * by <code>path</code> (rooted at <code>context</code>) interpreted as a
     * double.  If there is no such node, or if the <code>path</code> identifies
     * a node that is not a <code>Param</code>, then <code>def</code> is
     * returned.
     *
     * @param context serves as the point of reference the <code>path</code>
     * argument, assuming that <code>path</code> represents a relative path
     * @param path identifies the Param element whose (first) value should be
     * returned
     * @param def default value to return if there is no such Param object, or
     * if the value is not parseable as a double
     *
     * @return values of the Param identified by <code>path</code> if any;
     * <code>null</code> otherwise
     */
    public static double getDoubleValue(PioNodeParent context, String path, double def) {
        String strValue = getValue(context, path);
        if (strValue == null) return def;
        try {
            return Double.parseDouble(strValue);
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    /**
     * Gets the first (or only) value of the first (or only) Param identified
     * by <code>path</code> (rooted at <code>context</code>) interpreted as a
     * long.  If there is no such node, or if the <code>path</code> identifies
     * a node that is not a <code>Param</code>, then <code>def</code> is
     * returned.
     *
     * @param context serves as the point of reference the <code>path</code>
     * argument, assuming that <code>path</code> represents a relative path
     * @param path identifies the Param element whose (first) value should be
     * returned
     * @param def default value to return if there is no such Param object, or
     * if the value is not parseable as a long
     *
     * @return values of the Param identified by <code>path</code> if any;
     * <code>null</code> otherwise
     */
    public static long getLongValue(PioNodeParent context, String path, long def) {
        String strValue = getValue(context, path);
        if (strValue == null) return def;
        try {
            return Long.parseLong(strValue);
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    // RCN: we should probably change all the methods to return boxed types, now that
    // the compiler does auto-boxing. Having this variant is kind of sneaky.
    public static Long getLongValue(PioNodeParent context, String path, Long def) {
        String strValue = getValue(context, path);
        if (strValue == null) return def;
        try {
            return Long.valueOf(strValue);
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    /**
     * Adds a parameter with name <code>name</code> and value <code>value</code>
     * to the ParamSet <code>parent</code>.
     *
     * @param factory PioFactory that should be used to create the parameter
     * @param parent node to which the new {@link Param} should be added
     * @param name name of the new {@link Param}
     * @param value initial value of the new {@link Param}
     *
     * @return newly created {@link Param} that was added to {@link ParamSet}
     */
    public static Param addParam(PioFactory factory, ParamSet parent, String name, String value) {
        Param param = factory.createParam(name);
        param.setValue(value == null ? "" : value);
        parent.addParam(param);
        return param;
    }

    /**
     * Adds a parameter using the PropertyDescriptor <code>pd</code> and value <code>value</code>
     * to the ParamSet <code>parent</code>.
     *
     * @param factory PioFactory that should be used to create the parameter
     * @param parent node to which the new {@link Param} should be added
     * @param pd the {@link PropertyDescriptor} that has the name of the property to use.
     * @param value initial value of the new {@link Param}
     *
     * @return newly created {@link Param} that was added to {@link ParamSet}
     */
    public static Param addParam(PioFactory factory, ParamSet parent, PropertyDescriptor pd, String value) {
        return addParam(factory, parent, pd.getName(), value);
    }


    /**
     * Adds a parameter with name <code>name</code> and value <code>value</code>
     * to the ParamSet <code>parent</code>.
     *
     * @param factory PioFactory that should be used to create the parameter
     * @param parent node to which the new {@link Param} should be added
     * @param name name of the new {@link Param}
     * @param value initial value of the new {@link Param}
     * @param units a string describing the units of the value
     *
     * @return newly created {@link Param} that was added to {@link ParamSet}
     */
    public static Param addParam(PioFactory factory, ParamSet parent, String name,
                                 String value, String units) {
        Param param = factory.createParam(name);
        param.setValue(value == null ? "" : value);
        param.setUnits(units);
        parent.addParam(param);
        return param;
    }

    /**
     * Adds a parameter with name <code>name</code> and values <code>values</code>
     * to the ParamSet <code>parent</code>.
     *
     * @param factory PioFactory that should be used to create the parameter
     * @param parent node to which the new {@link Param} should be added
     * @param name name of the new {@link Param}
     * @param values initial values of the new {@link Param}
     *
     * @return newly created {@link Param} that was added to {@link ParamSet}
     */
    public static Param addListParam(PioFactory factory, ParamSet parent, String name, List<String> values) {
        Param param = factory.createParam(name);
        if (values != null) param.setValues(values);
        parent.addParam(param);
        return param;
    }

    public static <E extends Enum<E>> Param addEnumListParam(PioFactory factory, ParamSet parent, String name, List<E> values) {
        List<String> strValues = new ArrayList<String>(values.size());
        for (E e : values) {
            strValues.add(e.name());
        }
        return addListParam(factory, parent, name, strValues);
    }

    /**
     * Adds a parameter with name <code>name</code> and boolean value
     * <code>value</code> to the ParamSet <code>parent</code>.
     *
     * @param factory PioFactory that should be used to create the parameter
     * @param parent node to which the new {@link Param} should be added
     * @param name name of the new {@link Param}
     * @param value initial boolean value of the new {@link Param}
     *
     * @return newly created {@link Param} that was added to {@link ParamSet}
     */
    public static Param addBooleanParam(PioFactory factory, ParamSet parent, String name, boolean value) {
        Param param = factory.createParam(name);
        param.setValue(String.valueOf(value));
        parent.addParam(param);
        return param;
    }

    /**
     * Adds a parameter with name <code>name</code> and int value
     * <code>value</code> to the ParamSet <code>parent</code>.
     *
     * @param factory PioFactory that should be used to create the parameter
     * @param parent node to which the new {@link Param} should be added
     * @param name name of the new {@link Param}
     * @param value initial int value of the new {@link Param}
     *
     * @return newly created {@link Param} that was added to {@link ParamSet}
     */
    public static Param addIntParam(PioFactory factory, ParamSet parent, String name, int value) {
        Param param = factory.createParam(name);
        param.setValue(String.valueOf(value));
        parent.addParam(param);
        return param;
    }

    /**
     * Adds a parameter with name <code>name</code> and double value
     * <code>value</code> to the ParamSet <code>parent</code>.
     *
     * @param factory PioFactory that should be used to create the parameter
     * @param parent node to which the new {@link Param} should be added
     * @param name name of the new {@link Param}
     * @param value initial double value of the new {@link Param}
     *
     * @return newly created {@link Param} that was added to {@link ParamSet}
     */
    public static Param addDoubleParam(PioFactory factory, ParamSet parent, String name, double value) {
        Param param = factory.createParam(name);
        param.setValue(String.valueOf(value));
        parent.addParam(param);
        return param;
    }

    /**
     * Adds a parameter with name <code>name</code> and long value
     * <code>value</code> to the ParamSet <code>parent</code>.
     *
     * @param factory PioFactory that should be used to create the parameter
     * @param parent node to which the new {@link Param} should be added
     * @param name name of the new {@link Param}
     * @param value initial long value of the new {@link Param}
     *
     * @return newly created {@link Param} that was added to {@link ParamSet}
     */
    public static Param addLongParam(PioFactory factory, ParamSet parent, String name, long value) {
        Param param = factory.createParam(name);
        param.setValue(String.valueOf(value));
        parent.addParam(param);
        return param;
    }

    // RCN: I wanted to fill out the API but didn't feel like adding comments, sorry.

    public static Param addByteParam(PioFactory factory, ParamSet parent, String name, byte value) {
        Param param = factory.createParam(name);
        param.setValue(String.valueOf(value));
        parent.addParam(param);
        return param;
    }

    public static byte getByteValue(PioNodeParent context, String path, byte def) {
        String strValue = getValue(context, path);
        if (strValue == null) return def;
        try {
            return Byte.parseByte(strValue);
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    /**
     * Adds a squants quantity with name <code>name</code>.
     * @param factory PioFactory that should be used to create the parameter
     * @param parent node to which the new {@link Param} should be added
     * @param name name of the new {@link Param}
     * @param quantity the quantity to be stored (value and units)
     * @param <Q> a squants quantity
     * @return newly created {@link Param} that was added to {@link ParamSet}
     */
    public static  <Q extends Quantity<Q>> Param addQuantity(final PioFactory factory, final ParamSet parent, final String name, final Q quantity) {
        final Tuple2<Object, String> t = quantity.toTuple();
        final String value = t._1().toString();
        final String units = t._2();
        return addParam(factory, parent, name, value, units);
    }

    /**
     * Gets a squants quantity with the given name from the given context.
     * Returns 0 of primary unit of the given squants dimension in case the element does not exist or could not be parsed.
     * @param context the node context
     * @param path the node path
     * @param dimension a squants dimension
     * @param <Q> a squants quantity
     * @return quantity parsed from XML structure
     */
    public static <Q extends Quantity<Q>> Q getQuantity(final PioNodeParent context, final String path, final Dimension<Q> dimension) {
        try {
            // Get the Param corresponding to the path.
            final PioPath pp = new PioPath(path);
            final PioNode node = context.lookupNode(pp);
            // Return the value.
            final String value = ((Param) node).getValue();
            final String units = ((Param) node).getUnits();
            return dimension.parse(value + " " + units).get();

        } catch (final Exception e) {
            // doing this is lame but in sync with existing Pio methods: swallow error and return a default value
            return dimension.parse("0 " + dimension.primaryUnit().symbol()).get();
        }
    }

}
