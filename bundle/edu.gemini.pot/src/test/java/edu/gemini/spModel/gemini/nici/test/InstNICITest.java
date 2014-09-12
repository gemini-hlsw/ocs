package edu.gemini.spModel.gemini.nici.test;

import edu.gemini.spModel.gemini.nici.InstNICI;
import edu.gemini.spModel.gemini.nici.NICIParams;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.pio.xml.PioXmlFactory;
import edu.gemini.spModel.type.ObsoletableSpType;
import junit.framework.TestCase;

import java.io.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Arrays;

public class InstNICITest extends TestCase {

    private static void assertEquals(double a, double b) {
        InstNICITest.assertEquals(a, b, 0.0001);
    }

    public void testNiciDefaults() {
        InstNICI nici = new InstNICI();
        InstNICITest.assertEquals(nici.getCentralWavelength(), InstNICI.DEF_CENTRAL_WAVELENGTH);
        InstNICITest.assertEquals(nici.getSMRAngle(), 0.0);

        InstNICITest.assertEquals(nici.getFocalPlaneMask(), NICIParams.FocalPlaneMask.DEFAULT);
        InstNICITest.assertEquals(nici.getPupilMask(), NICIParams.PupilMask.DEFAULT);
        InstNICITest.assertEquals(nici.getCassRotator(), NICIParams.CassRotator.DEFAULT);
        InstNICITest.assertEquals(nici.getImagingMode(), NICIParams.ImagingMode.DEFAULT);
        InstNICITest.assertEquals(nici.getDichroicWheel(), NICIParams.DichroicWheel.DEFAULT);
        InstNICITest.assertEquals(nici.getChannel1Fw(), NICIParams.Channel1FW.DEFAULT);
        InstNICITest.assertEquals(nici.getChannel2Fw(), NICIParams.Channel2FW.DEFAULT);
        InstNICITest.assertEquals(nici.getWellDepth(), NICIParams.WellDepth.DEFAULT);
        InstNICITest.assertEquals(nici.getDhsMode(), NICIParams.DHSMode.DEFAULT);
        InstNICITest.assertEquals(nici.getFocs(), NICIParams.Focs.DEFAULT);
        InstNICITest.assertEquals(nici.getPupilImager(), NICIParams.PupilImager.DEFAULT);
        InstNICITest.assertEquals(nici.getSpiderMask(), NICIParams.SpiderMask.DEFAULT);
    }


    public void testImageModeUpdating() throws IOException, ClassNotFoundException {
        InstNICI nici = new InstNICI();

        // Set the imaging mode and make sure everything adjusts.
        nici.setImagingMode(NICIParams.ImagingMode.H1SLA);
        InstNICITest.assertEquals(nici.getDichroicWheel(), NICIParams.DichroicWheel.H5050_BEAMSPLITTER);
        InstNICITest.assertEquals(nici.getChannel1Fw(), NICIParams.Channel1FW.CH4H1S);
        InstNICITest.assertEquals(nici.getChannel2Fw(), NICIParams.Channel2FW.CH4H1L);
        InstNICITest.assertEquals(nici.getPupilImager(), NICIParams.PupilImager.OPEN);

        nici.setImagingMode(NICIParams.ImagingMode.H1SLB);
        InstNICITest.assertEquals(nici.getDichroicWheel(), NICIParams.DichroicWheel.H5050_BEAMSPLITTER);
        InstNICITest.assertEquals(nici.getChannel1Fw(), NICIParams.Channel1FW.CH4H1L);
        InstNICITest.assertEquals(nici.getChannel2Fw(), NICIParams.Channel2FW.CH4H1S);
        InstNICITest.assertEquals(nici.getPupilImager(), NICIParams.PupilImager.OPEN);

        nici.setImagingMode(NICIParams.ImagingMode.H1SPLA);
        InstNICITest.assertEquals(nici.getDichroicWheel(), NICIParams.DichroicWheel.H5050_BEAMSPLITTER);
        InstNICITest.assertEquals(nici.getChannel1Fw(), NICIParams.Channel1FW.CH4H1SP);
        InstNICITest.assertEquals(nici.getChannel2Fw(), NICIParams.Channel2FW.CH4H1L);
        InstNICITest.assertEquals(nici.getPupilImager(), NICIParams.PupilImager.OPEN);

        nici.setImagingMode(NICIParams.ImagingMode.H1SPLB);
        InstNICITest.assertEquals(nici.getDichroicWheel(), NICIParams.DichroicWheel.H5050_BEAMSPLITTER);
        InstNICITest.assertEquals(nici.getChannel1Fw(), NICIParams.Channel1FW.CH4H1L);
        InstNICITest.assertEquals(nici.getChannel2Fw(), NICIParams.Channel2FW.CH4H1SP);
        InstNICITest.assertEquals(nici.getPupilImager(), NICIParams.PupilImager.OPEN);

        nici.setImagingMode(NICIParams.ImagingMode.PUPIL_IMAGING);
        InstNICITest.assertEquals(nici.getDichroicWheel(), NICIParams.DichroicWheel.H5050_BEAMSPLITTER);
        InstNICITest.assertEquals(nici.getChannel1Fw(), NICIParams.Channel1FW.CH4H1SP);
        InstNICITest.assertEquals(nici.getChannel2Fw(), NICIParams.Channel2FW.CH4H1SP);
        InstNICITest.assertEquals(nici.getPupilImager(), NICIParams.PupilImager.PUPIL_IMAGING);
    }

    public void testProperties() throws Exception {
        InstNICI nici = new InstNICI();

        // Enumerated Types
        testProperty(nici, InstNICI.FOCAL_PLANE_MASK_PROP.getName(),  NICIParams.FocalPlaneMask.values());
        testProperty(nici, InstNICI.PUPIL_MASK_PROP.getName(),  NICIParams.PupilMask.values());
        testProperty(nici, InstNICI.CASS_ROTATOR_PROP.getName(),NICIParams.CassRotator.values());
        testProperty(nici, InstNICI.IMAGING_MODE_PROP.getName(),  NICIParams.ImagingMode.values());
        testProperty(nici, InstNICI.DICHROIC_WHEEL_PROP.getName(),  NICIParams.DichroicWheel.values());
        testProperty(nici, InstNICI.CHANNEL1_FW_PROP.getName(),  NICIParams.Channel1FW.values());
        testProperty(nici, InstNICI.CHANNEL2_FW_PROP.getName(), NICIParams.Channel2FW.values());
        testProperty(nici, InstNICI.WELL_DEPTH_PROP.getName(), NICIParams.WellDepth.values());
        testProperty(nici, InstNICI.DHS_MODE_PROP.getName(), NICIParams.DHSMode.values());

        testProperty(nici, InstNICI.FOCS_PROP.getName(), NICIParams.Focs.values());
        testProperty(nici, InstNICI.PUPIL_IMAGER_PROP.getName(),  NICIParams.PupilImager.values());
        testProperty(nici, InstNICI.SPIDER_MASK_PROP.getName(),  NICIParams.SpiderMask.values());

        // Doubles
        testProperty(nici, InstNICI.EXPOSURES_PROP.getName(), new int []{5});
        testProperty(nici, InstNICI.SMR_ANGLE_PROP.getName(), new double[]{12.11});

    }


    public void testParamSetIO() {
        InstNICI inst = new InstNICI();
        inst.setFocalPlaneMask(NICIParams.FocalPlaneMask.MASK_1);
        inst.setPupilMask(NICIParams.PupilMask.EIGHTY_FIVE_PERCENT);
        inst.setCassRotator(NICIParams.CassRotator.FOLLOW);
        inst.setExposureTime(300.);
        inst.setCoadds(2);
        inst.setPosAngleDegrees(90.);
        inst.setImagingMode(NICIParams.ImagingMode.MANUAL);
        inst.setDichroicWheel(NICIParams.DichroicWheel.MIRROR);
        inst.setChannel1Fw(NICIParams.Channel1FW.M_PRIMMA);
        inst.setChannel2Fw(NICIParams.Channel2FW.FE_II);
        inst.setExposures(5);
        inst.setWellDepth(NICIParams.WellDepth.SHALLOW);
        inst.setDhsMode(NICIParams.DHSMode.SAVE);

        inst.setFocs(NICIParams.Focs.GRID);
        inst.setPupilImager(NICIParams.PupilImager.PUPIL_IMAGING);
        inst.setSpiderMask(NICIParams.SpiderMask.FIXED);
        inst.setSMRAngle(55.69);

        ParamSet p = inst.getParamSet(new PioXmlFactory());

        InstNICI copy = new InstNICI();
        copy.setParamSet(p);

        assertEquals(copy.getFocalPlaneMask(), inst.getFocalPlaneMask());
        assertEquals(copy.getPupilMask(), inst.getPupilMask());
        assertEquals(copy.getCassRotator(), inst.getCassRotator());
        assertEquals(copy.getPosAngleDegrees(), inst.getPosAngleDegrees());
        assertEquals(copy.getImagingMode(), inst.getImagingMode());
        assertEquals(copy.getDichroicWheel(), inst.getDichroicWheel());
        assertEquals(copy.getChannel1Fw(), inst.getChannel1Fw());
        assertEquals(copy.getChannel2Fw(), inst.getChannel2Fw());
        assertEquals(copy.getExposureTime(), inst.getExposureTime());
        assertEquals(copy.getCoadds(), inst.getCoadds());
        assertEquals(copy.getExposures(), inst.getExposures());
        assertEquals(copy.getWellDepth(), inst.getWellDepth());
        assertEquals(copy.getDhsMode(), inst.getDhsMode());
        assertEquals(copy.getFocs(), inst.getFocs());
        assertEquals(copy.getPupilImager(), inst.getPupilImager());
        assertEquals(copy.getSpiderMask(), inst.getSpiderMask());
        assertEquals(copy.getSMRAngle(), inst.getSMRAngle());
    }


    /**
     * Test to see that simple set/get works, as well as serialization of the property,
     * persistence to/from a ParamSet.
     */
    private void testProperty(InstNICI target, String propName, Class type, Collection values) throws Exception {

        String propertyName = Character.toUpperCase(propName.charAt(0)) + "";
        if (propName.length() > 1) {
            propertyName += propName.substring(1);
        }
        Method get = target.getClass().getMethod("get" + propertyName);
        Method set = target.getClass().getMethod("set" + propertyName, type);
        for (Object val : values) {
            // skip obsolete values
            if ((val instanceof ObsoletableSpType) && ((ObsoletableSpType) val).isObsolete()) {
                continue;
            }

            // Simple get/set
            set.invoke(target, val);
            InstNICITest.assertEquals(get.invoke(target), val);

            // Serialize, deserialize, then test property again
            Object deserializedTarget = serializeDeserialize(target);
            InstNICITest.assertEquals(get.invoke(deserializedTarget), val);

            // Swizzle to ParamSet, then back
            PioFactory factory = new PioXmlFactory();
            ParamSet ps = target.getParamSet(factory);
            InstNICI newInstance = new InstNICI();
            newInstance.setParamSet(ps);
            InstNICITest.assertEquals(get.invoke(newInstance), val);
        }
    }

    private void testProperty(InstNICI target, String propName, double[] values) throws Exception {
        Collection<Double> vals = new ArrayList<Double>(values.length);
        for (double value : values) vals.add(value);
        testProperty(target, propName, Double.TYPE, vals);
    }

     private void testProperty(InstNICI target, String propName, Object[] values) throws Exception {
        Collection<Object> vals = new ArrayList<Object>(values.length);
        if (values.length <= 0) throw new Exception("Collection has zero elements!");
        Class type = values[0].getClass();
        vals.addAll(Arrays.asList(values));

         if (type.getSuperclass().isEnum()) {
             type = type.getSuperclass();
         }

        testProperty(target, propName, type , vals);
    }

    private void testProperty(InstNICI target, String propName, int[] values) throws Exception {
        Collection<Integer> vals = new ArrayList<Integer>(values.length);
        for (int value : values) vals.add(value);
        testProperty(target, propName, Integer.TYPE, vals);
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
