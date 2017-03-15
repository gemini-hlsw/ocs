package edu.gemini.spModel.type;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility methods for working with enumerated types.  These methods are mainly
 * provided to ease migration of the existing SPTypeBaseList dependent code and
 * provide backwards compatibility with pre 2006B code.
 */
public final class SpTypeUtil {
    private static final Logger LOG = Logger.getLogger(SpTypeUtil.class.getName());

    private SpTypeUtil() {
    }

    /**
     * Converts from an enum's display value to the enum itself.  Uses the
     * display value returned by {@link DisplayableSpType#displayValue} if
     * the enum class implements this interface.  Otherwise, uses the enum's
     * name as the display value.
     *
     * @param c class of the enum type
     * @param displayValue displayValue to seek amongst the enum's values
     *
     * @return enum value matching the given display value, if found;
     * <code>null</code> otherwise
     */
    public static <T extends Enum<T>> T displayValueToEnum(Class<T> c, String displayValue) {
        if (displayValue == null) return null;

        T[] constants = c.getEnumConstants();

        if (DisplayableSpType.class.isAssignableFrom(c)) {
            for (T t : constants) {
                //noinspection unchecked
                String curDisplayValue = ((DisplayableSpType) t).displayValue();
                // UX-638: only look at digits and characters, ignore any special characters, blanks etc.
                curDisplayValue = curDisplayValue.replaceAll("[^a-zA-Z0-9]", "");
                displayValue = displayValue.replaceAll("[^a-zA-Z0-9]", "");
                // now compare the "reduced" strings ignoring case
                if (displayValue.equalsIgnoreCase(curDisplayValue)) return t;
            }
        } else {
            for (T t : constants) {
                if (displayValue.equals(t.name())) return t;
            }
        }
        return null;
    }

    // more crap from SPTypeBaseList
    @SuppressWarnings({"unchecked"})
    public static <T extends Enum<T>> String[] getFormattedDisplayValueAndDescriptions(Class<T> c) {
        T[] constants = c.getEnumConstants();

        boolean isObsoletable = ObsoletableSpType.class.isAssignableFrom(c);
        boolean isDisplayable = DisplayableSpType.class.isAssignableFrom(c);
        boolean isDescribable = DescribableSpType.class.isAssignableFrom(c);

        List<String> resList = new ArrayList<>(constants.length);
        for (T val : constants) {
            if (isObsoletable && ((ObsoletableSpType) val).isObsolete()) {
                continue;
            }
            StringBuilder buf = new StringBuilder();

            String displayVal = val.name();
            if (isDisplayable) {
                String tmp = ((DisplayableSpType) val).displayValue();
                if ((tmp != null) && !"".equals(tmp)) {
                    displayVal = tmp;
                }
            }
            buf.append(displayVal);

            if (isDescribable) {
                String tmp = ((DescribableSpType) val).description();
                if ((tmp != null) && !"".equals(tmp)) {
                    buf.append(' ');
                    buf.append(tmp);
                }
            }

            resList.add(buf.toString());
        }
        return resList.toArray(new String[resList.size()]);
    }

    /**
     * Converts an enum name to an enum value, using Enum.valueOf(), but returns
     * <code>null</code> if the matching value is not found.
     *
     * @param c class of the enum type
     * @param name name of the enum value
     *
     * @return matching enum value if possible, <code>null</code> if not
     */
    public static <T extends Enum<T>> T noExceptionValueOf(Class<T> c, String name) {
        T res = null;
        try {
            res = Enum.valueOf(c, name);
        } catch (Exception ex) {
            LOG.fine("Could not find '" + name + "' in class: " + c.getName());
        }
        return res;
    }

    /**
     * An Enum.valueOf() like method that works the way that the old
     * SPTypeBaseList used before the 2006B semester worked.  Namely, after
     * trying to parse the enum name to the enum value, it will try the display
     * value of the enum (which was used prior to 2006B).  If neither matches
     * an enum type, it will return the supplied default value.  In no case will
     * it throw an IllegalArgumentException.
     *
     * @param c class of the enum type
     * @param name name of the enum value (or its display value)
     * @param def default value to return if a matching enum value cannot be
     * located
     *
     * @return matching enum value, or the supplied default if not found
     */
    public static <T extends Enum<T>> T oldValueOf(Class<T> c, String name, T def) {
        return oldValueOf(c, name, def, true);
    }

    public static <T extends Enum<T>> T oldValueOf(Class<T> c, String name, T def, boolean logFailure) {
        T val = noExceptionValueOf(c, name);
        if (val != null) return val;

        val = displayValueToEnum(c, name);
        if (val != null) return val;

        if (logFailure) {
            LOG.warning("Could not convert '" + name + "' in class " +
                    c.getName() + " to an enum value.  Return: " + def);
        }
        return def;
    }

    public static <T extends Enum<T>> T valueOf(Class<T> c, int ordinal, T def) {
        T[] constants = c.getEnumConstants();
        if (ordinal >= constants.length) {
            LOG.warning("Could not return the enum ordinal #" + ordinal + " in class: " +
                    c.getName() + ", returning: " + def);
            return def;
        }
        return constants[ordinal];
    }

    public static <T extends Enum<T>> List<T> getSelectableItems(Class<T> c) {
        T[] members = c.getEnumConstants();
        if (members == null) {
            throw new IllegalArgumentException("Class " + c + " is not an Enum type.");
        }

        List<T> res = new ArrayList<>(Arrays.asList(members));
        if (ObsoletableSpType.class.isAssignableFrom(c)) {
            for (Iterator<T> it=res.iterator(); it.hasNext(); ) {
                if (((ObsoletableSpType) it.next()).isObsolete()) it.remove();
            }
        }
        return res;
    }
}
