/*
 * Copyright 2002 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: I18N.java 4414 2004-02-03 16:21:36Z brighton $
 */

package jsky.util;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;


/**
 * Simple utility class for accessing property file resource bundles for
 * internationalization.
 * <p>
 * This class assumes one property file per package, as needed. The convention
 * used here is to store the property files in a subdirectory of the package
 * named <code>i18n</code>. The base name is <code>gui</code>, so the default
 * property file is <code>i18n/gui.properties</code>. The German version would then
 * be <code>i18n/gui_de.properties</code> and the French version would be
 * <code>i18n/gui_fr.properties</code>. The property files need to be installed
 * in the same relative directory in the classes dir or jar file before use.
 *
 * @author  Allan Brighton (modified original version by Guillaume Helle)
 * @version $Revision: 4414 $
 */
public class I18N {

    // The base name of the I18N properties files
    private String _baseName;

    private Class _class;

    // Current Locale
    private Locale _locale = Locale.getDefault();

    /**
     * Return an instance of I18N, initialized to use i18n/gui_<locale>.properties,
     * relative to the package directory for the given class.
     */
    public static I18N getInstance(Class c) {
        return new I18N(c);
    }

    // Initialize to use i18n/gui_<locale>.properties, relative to the
    // package directory for the given class.
    private I18N(Class c) {
        _class    = c;
        _baseName = c.getPackage().getName() + ".i18n.gui";
    }

    /** Set the current locale. */
    public void setLocale(Locale locale) {
        _locale = locale;
    }

    /** Return the string for the specified key in the current locale. */
    public String getString(String key) {
        try {
            ResourceBundle rb = ResourceBundle.getBundle(_baseName, _locale, _class.getClassLoader());
            String text = rb.getString(key);
            return text != null ? text : key;
        } catch (Exception e) {
            e.printStackTrace();
            return key;
        }
    }

    /**
     * Return the string for the specified key in the current locale after substituting
     * the given parameters using the MessageFormat class.
     *
     * @see java.text.MessageFormat
     */
    public String getString(String key, Object[] params) {
        String pattern = getString(key);
        if (pattern == null) return null;
        MessageFormat mf = new MessageFormat(pattern);
        mf.setLocale(_locale);
        String result = mf.format(params);
        return result;
    }

    /**
     * Return the string for the specified key in the current locale after substituting
     * the given parameter using the MessageFormat class.
     *
     * @see java.text.MessageFormat
     */
    public String getString(String key, Object p1) {
        return getString(key, new Object[]{p1});
    }

    /**
     * Return the string for the specified key in the current locale after substituting
     * the given parameters (p1 and p2) using the MessageFormat class.
     *
     * @see java.text.MessageFormat
     */
    public String getString(String key, Object p1, Object p2) {
        return getString(key, new Object[]{p1, p2});
    }

    /**
     * Return the string for the specified key in the current locale after substituting
     * the given parameters (p1 and p2) using the MessageFormat class.
     *
     * @see java.text.MessageFormat
     */
    public String getString(String key, Object p1, Object p2, Object p3) {
        return getString(key, new Object[]{p1, p2, p3});
    }


    /**
     * test main
     */
    public static void main(String[] args) {
        // Base name for the default I18N property file
        I18N i18n = I18N.getInstance(I18N.class);

        System.out.println("hello = " + i18n.getString("hello"));
        System.out.println("test1 = " + i18n.getString("test1"));
        System.out.println("test1 with 2 args = " + i18n.getString("test1", "One", "Two"));
        System.out.println("test1 with 3 args = " + i18n.getString("test1", "One", "Two", new Integer(3)));
    }
}
