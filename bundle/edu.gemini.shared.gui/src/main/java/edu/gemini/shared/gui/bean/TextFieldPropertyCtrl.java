//
// $
//

package edu.gemini.shared.gui.bean;

import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultFormatter;
import javax.swing.text.Document;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyDescriptor;
import java.text.*;
import java.util.logging.Logger;

/**
 * A PropertyCtrl implementation for text fields.  Currently support is
 * provided for editing integers and doubles.
 * @param <B> bean class
 * @param <T> property type
 */
public final class TextFieldPropertyCtrl<B, T> extends PropertyCtrl<B, T> {
    private static final Logger LOG = Logger.getLogger(TextFieldPropertyCtrl.class.getName());

    /**
     * An encapsulation of all the information required in order to format and
     * parse the value in the text field.
     * @param <T> property type
     */
    public interface FormatSupport<T> {
        /**
         * Provides the format that is used by the JFormattedTextField for
         * reading and displaying the value.
         */
        Format getFormat();

        /**
         * Converts from the type provided by the formatter to the property
         * type.  For example, a NumberFormat will parse a String into a
         * Number object, and this method would convert the Number into the
         * type of the property (e.g., <code>int</code>).
         */
        T toType(Object val);

        /**
         * Converts from the property type into a type used by the Format
         * instance.
         */
        Object fromType(T val);
    }

    public static <B> TextFieldPropertyCtrl<B, String> createStringInstance(PropertyDescriptor desc) {
        FormatSupport<String> fs = new FormatSupport<String>() {
            public Format getFormat() {
                return null;
            }

            public String toType(Object val) {
                return (String) val;
            }

            public Object fromType(String val) {
                return val;
            }
        };

        return new TextFieldPropertyCtrl<B, String>(fs, desc);
    }

    /**
     * Creates an instance configured to edit <code>int</code> properties.
     *
     * @param desc property descriptor
     *
     * @param <B> bean class
     */
    public static <B> TextFieldPropertyCtrl<B, Integer> createIntegerInstance(PropertyDescriptor desc) {
        FormatSupport<Integer> fs = new FormatSupport<Integer>() {
            private NumberFormat nf = NumberFormat.getIntegerInstance();
            {
                nf.setGroupingUsed(false);
            }

            public Format getFormat() {
                return nf;
            }

            public Integer toType(Object val) {
                return ((Number) val).intValue();
            }

            public Object fromType(Integer val) {
                return val;
            }
        };
        return new TextFieldPropertyCtrl<B, Integer>(fs, desc);
    }

    /**
     * A decimal formatter that handles Option<Integer> values.
     */
    private static class OptionalIntegerFormat extends Format {
        private NumberFormat nf = NumberFormat.getIntegerInstance();

        OptionalIntegerFormat() {
            nf.setGroupingUsed(false);
        }


        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (!None.instance().equals(obj)) {
                Some some = (Some) obj;
                nf.format(some.getValue(), toAppendTo, pos);
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            if ((source == null) || "".equals(source)) return None.instance();
            Object res = nf.parseObject(source, pos);
            if (res == null) return None.instance();
            return new Some<Integer>(((Number)res).intValue());
        }

        public Object parseObject(String source) throws ParseException {
            ParsePosition pos = new ParsePosition(0);
            Object result = parseObject(source, pos);
            if (result != None.instance() && (pos.getIndex() == 0)) {
                throw new ParseException("Format.parseObject(String) failed",
                    pos.getErrorIndex());
            }
            return result;
        }
    }

    /**
     * Creates an instance configured to edit <code>int</code> properties wrapped in an Option.
     *
     * @param desc property descriptor
     *
     * @param <B> bean class
     */
    public static <B> TextFieldPropertyCtrl<B, Option<Integer>> createOptionIntegerInstance(PropertyDescriptor desc) {
        FormatSupport<Option<Integer>> fs = new FormatSupport<Option<Integer>>() {
            private OptionalIntegerFormat nf = new OptionalIntegerFormat();

            public Format getFormat() {
                return nf;
            }

            public Option<Integer> toType(Object val) {
                //noinspection unchecked
                return (Option<Integer>) val;
            }

            public Object fromType(Option<Integer> val) {
                return val;
            }
        };
        return new TextFieldPropertyCtrl<B, Option<Integer>>(fs, desc);
    }

    /**
     * Creates an instance configured to edit <code>double</code> properties.
     *
     * @param desc property descriptor
     * @param fractionalDigits minimum number of digits to display after the
     * decimal
     *
     * @param <B> bean class
     */
    public static <B> TextFieldPropertyCtrl<B, Double> createDoubleInstance(PropertyDescriptor desc, final int fractionalDigits) {
        FormatSupport<Double> fs = new FormatSupport<Double>() {
            private DecimalFormat df = new DecimalFormat();
            {
                df.setMinimumFractionDigits(fractionalDigits);
                df.setGroupingUsed(false);
            }

            public Format getFormat() {
                return df;
            }

            public Double toType(Object val) {
                return ((Number) val).doubleValue();
            }

            public Object fromType(Double val) {
                return val;
            }
        };
        return new TextFieldPropertyCtrl<B, Double>(fs, desc);
    }

    /**
     * Creates an instance configured to edit <code>double</code> properties.
     *
     * @param desc property descriptor
     * @param minFractionalDigits minimum number of digits to display after the decimal
     * @param maxFractionalDigits maximum number of digits to display after the decimal
     *
     * @param <B> bean class
     */
    public static <B> TextFieldPropertyCtrl<B, Double> createDoubleInstance(PropertyDescriptor desc,
                                                                            final int minFractionalDigits,
                                                                            final int maxFractionalDigits) {
        FormatSupport<Double> fs = new FormatSupport<Double>() {
            private DecimalFormat df = new DecimalFormat();
            {
                df.setMinimumFractionDigits(minFractionalDigits);
                df.setMaximumFractionDigits(maxFractionalDigits);
                df.setGroupingUsed(false);
            }

            public Format getFormat() {
                return df;
            }

            public Double toType(Object val) {
                return ((Number) val).doubleValue();
            }

            public Object fromType(Double val) {
                return val;
            }
        };
        return new TextFieldPropertyCtrl<B, Double>(fs, desc);
    }

    /**
     * A decimal formatter that handles Option<Double> values.
     */
    private static class OptionalDecimalFormat extends Format {
        private DecimalFormat df;

        OptionalDecimalFormat(int fractionalDigits) {
            df = new DecimalFormat();
            df.setMinimumFractionDigits(fractionalDigits);
            df.setGroupingUsed(false);
        }


        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            if (!None.instance().equals(obj)) {
                Some some = (Some) obj;
                df.format(some.getValue(), toAppendTo, pos);
            }
            return toAppendTo;
        }

        public Object parseObject(String source, ParsePosition pos) {
            if ((source == null) || "".equals(source)) return None.instance();
            Object res = df.parseObject(source, pos);
            if (res == null) return None.instance();
            return new Some<Double>(((Number)res).doubleValue());
        }

        public Object parseObject(String source) throws ParseException {
            ParsePosition pos = new ParsePosition(0);
            Object result = parseObject(source, pos);
            if (result != None.instance() && (pos.getIndex() == 0)) {
                throw new ParseException("Format.parseObject(String) failed",
                    pos.getErrorIndex());
            }
            return result;
        }
    }

    public static <B> TextFieldPropertyCtrl<B, Option<Double>> createOptionDoubleInstance(PropertyDescriptor desc, final int fractionalDigits) {
        FormatSupport<Option<Double>> fs = new FormatSupport<Option<Double>>() {
            private OptionalDecimalFormat df = new OptionalDecimalFormat(fractionalDigits);

            public Format getFormat() {
                return df;
            }

            public Option<Double> toType(Object val) {
                //noinspection unchecked
                return (Option<Double>) val;
            }

            public Object fromType(Option<Double> val) {
                return val;
            }
        };
        return new TextFieldPropertyCtrl<B, Option<Double>>(fs, desc);
    }

    private final JFormattedTextField field;
    private final FormatSupport<T> formatSupport;

    private final DocumentListener componentListener = new DocumentListener() {
        private void update(DocumentEvent evt) {
            try {
                Document doc = evt.getDocument();
                String strVal = doc.getText(0, doc.getLength());
                Object val = field.getFormatter().stringToValue(strVal);
                setVal(formatSupport.toType(val));
            } catch (Exception ex) {
                 // ignore
            }
        }

        public void insertUpdate(DocumentEvent e) { update(e); }
        public void removeUpdate(DocumentEvent e) { update(e); }
        public void changedUpdate(DocumentEvent e) { update(e); }
    };

    public TextFieldPropertyCtrl(final FormatSupport<T> formatSupport, PropertyDescriptor desc) {
        super(desc);

        this.formatSupport = formatSupport;
        
        field = new JFormattedTextField(formatSupport.getFormat());
        ((DefaultFormatter) field.getFormatter()).setCommitsOnValidEdit(true);
        ((DefaultFormatter) field.getFormatter()).setOverwriteMode(false);


        field.addPropertyChangeListener("value", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                Object oldValObject = evt.getOldValue();
                Object newValObject = evt.getNewValue();

                T oldValue = null;
                if (oldValObject != null) {
                    oldValue = formatSupport.toType(oldValObject);
                }

                T newValue = null;
                if (newValObject != null) {
                    newValue = formatSupport.toType(newValObject);
                }

                fireEditEvent(oldValue, newValue);
            }
        });
    }

    public void setColumns(int columns) {
        field.setColumns(columns);
        field.setMinimumSize(field.getPreferredSize());
    }

    public void updateComponent() {
        T val = getVal();
        field.setValue(formatSupport.fromType(val));
    }

    public void updateBean() {
        setVal(formatSupport.toType(field.getValue()));
    }

    public final JFormattedTextField getTextField() {
        return field;
    }

    @Override
    protected void addComponentChangeListener() {
        field.getDocument().addDocumentListener(componentListener);
    }

    @Override
    protected void removeComponentChangeListener() {
        field.getDocument().removeDocumentListener(componentListener);
    }

    public final JComponent getComponent() {
        return getTextField();
    }
}
