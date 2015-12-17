package edu.gemini.spModel.gemini.texes.test;

import junit.framework.TestCase;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.pio.xml.PioXmlFactory;
import edu.gemini.spModel.gemini.texes.InstTexes;
import edu.gemini.spModel.gemini.texes.TexesParams;

import java.io.*;
import java.util.Collection;
import java.util.ArrayList;
import java.lang.reflect.Method;
import java.util.Collections;

public class InstTexesTest extends TestCase {

    private static void assertEquals(double a, double b) {
        InstTexesTest.assertEquals(a, b, 0.0001);
    }

    private static void assertEquals(double[] a, double[] b) {
        if (a.length != b.length) assertFalse(false);
        for (int i = 0; i < a.length; i ++) {
            assertEquals(a[i], b[i]);
        }
    }

    public void testDefaultsValues() {
        InstTexes texes = new InstTexes();
        InstTexesTest.assertEquals(texes.getDisperser(), TexesParams.Disperser.DEFAULT);
    }

    public void testSlitModeUpdating() throws IOException, ClassNotFoundException {
        InstTexes inst = new InstTexes();

        // Set the disperser and see that the slit adjusts
        inst.setDisperser(TexesParams.Disperser.D_32_LMM);
        assertEquals(inst.getScienceArea(), new double[]{0.5, 4.0});

        inst.setDisperser(TexesParams.Disperser.D_75_LMM);
        assertEquals(inst.getScienceArea(), new double[]{0.5, 1.7});
    }

    public void testProperties() throws Exception {
        InstTexes texes = new InstTexes();

        // Enumerated Types
        testProperty(texes, InstTexes.DISPERSER_PROP.getName(), TexesParams.Disperser.values());

        // Doubles
        testProperty(texes, InstTexes.WAVELENGTH_PROP.getName(), new double []{1.1});

    }

    public void testParamSetIO() {
        InstTexes inst = new InstTexes();
        inst.setDisperser(TexesParams.Disperser.D_75_LMM);
        inst.setExposureTime(300.);
        inst.setCoadds(2);
        inst.setPosAngleDegrees(90.);
        inst.setWavelength(11.1);
        ParamSet p = inst.getParamSet(new PioXmlFactory());

        InstTexes copy = new InstTexes();
        copy.setParamSet(p);

        assertEquals(copy.getDisperser(), inst.getDisperser());
        assertEquals(copy.getPosAngleDegrees(), inst.getPosAngleDegrees());
        assertEquals(copy.getExposureTime(), inst.getExposureTime());
        assertEquals(copy.getCoadds(), inst.getCoadds());
        assertEquals(copy.getWavelength(), inst.getWavelength());
    }

    /**
     * Test to see that simple set/get works, as well as serialization of the property,
     * persistence to/from a ParamSet.
     */
    private <E> void testProperty(InstTexes target, String propName, Class<?> type, Collection<E> values) throws Exception {

        String propertyName = Character.toUpperCase(propName.charAt(0)) + "";
        if (propName.length() > 1) {
            propertyName += propName.substring(1);
        }
        Method get = target.getClass().getMethod("get" + propertyName);
        Method set = target.getClass().getMethod("set" + propertyName, type);
        for (E val : values) {
            // Simple get/set
            set.invoke(target, val);
            InstTexesTest.assertEquals(get.invoke(target, new Object[0]), val);

            // Serialize, deserialize, then test property again
            Object deserializedTarget = serializeDeserialize(target);
            InstTexesTest.assertEquals(get.invoke(deserializedTarget, new Object[0]), val);

            // Target and deserializedTarget should be equal() now.

// RCN: no, actually they won't be equal. What does this even mean?
//            InstTexesTest.assertEquals(target, deserializedTarget);

            // Swizzle to ParamSet, then back
            PioFactory factory = new PioXmlFactory();
            ParamSet ps = target.getParamSet(factory);
            InstTexes newInstance = new InstTexes();
            newInstance.setParamSet(ps);
            InstTexesTest.assertEquals(get.invoke(newInstance, new Object[0]), val);

            // Target and deserializedTarget should be equal() now.
//            InstTexesTest.assertEquals(target, newInstance);
        }
    }

    private void testProperty(InstTexes target, String propName, double[] values) throws Exception {
        Collection<Double> vals = new ArrayList<>(values.length);
        for (double value : values) vals.add(value);
        testProperty(target, propName, Double.TYPE, vals);
    }

    private void testProperty(InstTexes target, String propName, TexesParams.Disperser[] values) throws Exception {
        Collection<TexesParams.Disperser> vals = new ArrayList<>(values.length);
        Collections.addAll(vals, values);
        testProperty(target, propName, TexesParams.Disperser.class, vals);
    }

    private Object serializeDeserialize(Object o) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(o);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        return ois.readObject();
    }

}
