package edu.gemini.spModel.gemini;

import edu.gemini.shared.util.immutable.ApplyOp;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.data.config.IConfigProvider;
import edu.gemini.spModel.data.config.IParameter;
import edu.gemini.spModel.data.config.ISysConfig;
import edu.gemini.spModel.pio.Param;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.pio.xml.PioXmlFactory;
import junit.framework.TestCase;

import java.beans.*;
import java.lang.reflect.Method;

/**
 * Test cases for {@link edu.gemini.spModel.gemini.gsaoi.Gsaoi} properties.
 */
public abstract class BeanPropertyTestBase<B extends ISPDataObject> extends TestCase {

    // A collection of information required to execute a test on a property of
    // the bean.
    protected static final class PropertyTest {
        final PropertyDescriptor desc;
        final Object defaultValue;
        final Object newValue;
        final Object[] updates;

        public PropertyTest(PropertyDescriptor desc, Object defaultValue, Object newValue, Object... updates) {
            this.desc         = desc;
            this.defaultValue = defaultValue;
            this.newValue     = newValue;
            this.updates      = updates;
        }
    }

    // An interface that abstracts the application of a particular instance of
    // a test.
    private interface ApplyTest {
        void apply(PropertyDescriptor desc, Object oldValue, Object newValue) ;
    }


    // The general algorithm for running a PropertyTest.  It is configured with
    // a particular ApplyTest.
    private class PropertyTestExecutor implements ApplyOp<PropertyTest> {
        private ApplyTest test;

        PropertyTestExecutor(ApplyTest test) {
            this.test = test;
        }

        public void apply(PropertyTest pt) {
            test.apply(pt.desc, pt.defaultValue, pt.newValue);
            if (pt.updates != null) {
                for (Object update : pt.updates) {
                    test.apply(pt.desc, pt.defaultValue, update);
                }
            }
        }
    }


    protected B bean;
    private final PioFactory factory;

    protected BeanPropertyTestBase() {
        factory = new PioXmlFactory();
    }

    protected void setUp() throws Exception {
        super.setUp();
        bean = createBean();
    }

    protected abstract B createBean();

    protected Object getValue(PropertyDescriptor desc) throws Exception {
        Method m = desc.getReadMethod();
        return m.invoke(bean);
    }

    protected void setValue(PropertyDescriptor desc, Object value) throws Exception {
        Method m = desc.getWriteMethod();
        m.invoke(bean, value);
    }

    // Wraps the Enum.getValue() method in a PropertyEditorSupport so that we
    // can extract the string value of enum types using property editors.
    private class EnumEditor<T extends Enum<T>> extends PropertyEditorSupport {
        private final Class<T> propertyType;

        EnumEditor(Object source, Class<T> propertyType) {
            super(source);
            this.propertyType = propertyType;
        }

        public String getAsText() {
            return ((Enum<?>) getValue()).name();
        }

        public void setAsText(String text) {
            //noinspection unchecked
            setValue(Enum.valueOf(propertyType, text));
        }
    }

    @SuppressWarnings("rawtypes")
    private PropertyEditor getPropertyEditor(PropertyDescriptor desc) throws Exception {
        PropertyEditor ped = PropertyEditorManager.findEditor(desc.getPropertyType());
        if (ped == null) {
            Class<?> propType = desc.getPropertyType();
            if (propType.isEnum()) {
                ped = new EnumEditor(bean, propType);
            } else {
                fail("Could not get a property editor for prop " + desc.getDisplayName());
            }
        }
        return ped;
    }

    // Converts the given property value to a string, or throws an exception if
    // it cannot do so.
    private String toString(PropertyDescriptor desc, Object value) throws Exception {
        PropertyEditor ped = getPropertyEditor(desc);
        ped.setValue(value);
        return ped.getAsText();
    }

    // Converts the given string to a property value, or throws an exception if
    // it cannot do so.
    private Object fromString(PropertyDescriptor desc, String strValue) throws Exception {
        PropertyEditor ped = getPropertyEditor(desc);
        ped.setAsText(strValue);
        return ped.getValue();
    }

    protected void testGetSet(PropertyDescriptor desc, Object oldValue, Object newValue) throws Exception {

        String testName = "Test Get/Set: " + desc.getDisplayName();

        // The test won't work if the two values are the same, so rule that out.
        assertFalse(testName, oldValue.equals(newValue));

        // Make sure we start with the old value.
        assertEquals(testName, oldValue, getValue(desc));

        // Add a listener.
        class TestPropertyChangeListener implements PropertyChangeListener {
            private int count;
            private Object oldValue;
            private Object newValue;
            public void propertyChange(PropertyChangeEvent evt) {
                ++count;
                oldValue = evt.getOldValue();
                newValue = evt.getNewValue();
            }
        }
        TestPropertyChangeListener tpcl = new TestPropertyChangeListener();
        bean.addPropertyChangeListener(tpcl);

        // Set the new value
        setValue(desc, newValue);
        assertEquals(testName, newValue, getValue(desc));

        // Make sure the listener was called.
        assertEquals(testName, 1, tpcl.count);
        assertEquals(testName, oldValue, tpcl.oldValue);
        assertEquals(testName, newValue, tpcl.newValue);

        // Set the value to the same new value.
        setValue(desc, newValue);

        // Make sure the listener was not called.
        assertEquals(testName, 1, tpcl.count);

        // Remove the listener.
        bean.removePropertyChangeListener(tpcl);

        // Set it back to the old value
        setValue(desc, oldValue);
        assertEquals(testName, oldValue, getValue(desc));

        // Make sure the listener was not called.
        assertEquals(testName, 1, tpcl.count);
    }

    private final PropertyTestExecutor getSetTestExecutor = new PropertyTestExecutor((desc, oldValue, newValue) -> {
        try {
            testGetSet(desc, oldValue, newValue);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    });

    protected void testGetSet(ImList<PropertyTest> tests) throws Exception {
        tests.foreach(getSetTestExecutor);
    }


    protected void testParamSet(PropertyDescriptor desc, Object newValue) throws Exception {
        String testName = "Test ParamSet: " + desc.getDisplayName();

        // Verify that the current value doesn't equal the new value.  We need
        // new value to be different to ensure that the setParamSet method is
        // working.
        Object curValue = getValue(desc);
        assertFalse(testName, curValue.equals(newValue));

        // Get the param set for the bean, extract the param value for the
        // property that goes with this descriptor, and make sure it eqauals
        ParamSet pset = bean.getParamSet(factory);
        Param p = pset.getParam(desc.getName());
        String paramVal = p.getValue();
        assertEquals(testName, curValue, fromString(desc, paramVal));

        // Now set the parameter value to the new value and apply the change.
        p.setValue(toString(desc, newValue));
        bean.setParamSet(pset);

        // Make sure the bean was updated.
        assertEquals(testName, newValue, getValue(desc));
    }

    private final PropertyTestExecutor paramSetTestExecutor = new PropertyTestExecutor((desc, oldValue, newValue) -> {
        try {
            testParamSet(desc, newValue);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    });

    protected void testParamSet(ImList<PropertyTest> tests) throws Exception {
        tests.foreach(paramSetTestExecutor);
    }


    protected void testSysConfig(PropertyDescriptor desc, Object newValue) throws Exception {
        String testName = "Test SysConfig: " + desc.getDisplayName();

        // If using this test, the bean must be an IConfigProvider.
        assertTrue(testName, bean instanceof IConfigProvider);

        // Update the value.
        setValue(desc, newValue);

        // Compare to the parameter value in the sys config.
        ISysConfig sc = ((IConfigProvider) bean).getSysConfig();
        IParameter param = sc.getParameter(desc.getName());
        assertEquals(testName, newValue, param.getValue());
    }

    private final PropertyTestExecutor sysConfigTestExecutor = new PropertyTestExecutor((desc, oldValue, newValue) -> {
        try {
            testSysConfig(desc, newValue);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    });

    protected void testSysConfig(ImList<PropertyTest> tests) throws Exception {
        tests.foreach(sysConfigTestExecutor);
    }
}
