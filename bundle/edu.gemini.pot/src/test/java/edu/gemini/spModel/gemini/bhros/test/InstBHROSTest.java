//package edu.gemini.spModel.gemini.bhros.test;
//
//import edu.gemini.spModel.gemini.bhros.BHROSParams;
//import edu.gemini.spModel.gemini.bhros.InstBHROS;
//import edu.gemini.spModel.gemini.bhros.ech.HROSHardwareConstants;
//import edu.gemini.spModel.pio.ParamSet;
//import edu.gemini.spModel.pio.PioFactory;
//import edu.gemini.spModel.pio.xml.PioXmlFactory;
//
//import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.io.ObjectInputStream;
//import java.io.ObjectOutputStream;
//import java.lang.reflect.Method;
//import java.util.ArrayList;
//import java.util.Collection;
////$Id: InstBHROSTest.java 7077 2006-05-26 19:56:48Z anunez $
//
//import junit.framework.TestCase;
//
//public class InstBHROSTest extends TestCase {
//
//	private static void assertEquals(double a, double b) {
//		assertEquals(a, b, 0.0001);
//	}
//
//	public void testEchelleDefaults() {
//		InstBHROS bhros = new InstBHROS();
//		assertEquals(bhros.getCentralWavelength(), 0.4334);
//		assertEquals(bhros.getEchelleALT(), -56.988739);
//		assertEquals(bhros.getEchelleAz(), 0.277674);
//		assertEquals(bhros.getGoniAng(), -10.1180084);
//	}
//
//	public void testCCDDefaults() {
//		InstBHROS bhros = new InstBHROS();
//		assertEquals(bhros.getCCDCentreX(), 0.0);
//		assertEquals(bhros.getCCDCentreY(), HROSHardwareConstants.BLUE_YCENTRE);
//		assertEquals(bhros.getCCDAmplifiers(), BHROSParams.CCDReadoutPorts.DEFAULT);
//		assertEquals(bhros.getCCDGain(), BHROSParams.CCDGain.DEFAULT);
//		assertEquals(bhros.getCCDSpeed(), BHROSParams.CCDSpeed.DEFAULT);
//		assertEquals(bhros.getCCDXBinning(), BHROSParams.CCDXBinning.DEFAULT);
//		assertEquals(bhros.getCCDYBinning(), BHROSParams.CCDYBinning.DEFAULT);
//	}
//
//	public void testOtherDefaults() {
//		InstBHROS bhros = new InstBHROS();
//		assertEquals(bhros.getEntranceFibre(), BHROSParams.EntranceFibre.DEFAULT);
////		assertEquals(bhros.getExposureMeterFilter(), BHROSParams.ExposureMeterFilter.DEFAULT);
//		assertEquals(bhros.getHartmannFlap(), BHROSParams.HartmannFlap.DEFAULT);
//		assertEquals(bhros.getISSPort(), BHROSParams.ISSPort.DEFAULT);
//		assertEquals(bhros.getPostSlitFilter(), BHROSParams.PostSlitFilter.DEFAULT);
//		assertEquals(bhros.getROI(), BHROSParams.ROI.DEFAULT);
//	}
//
//	public void testEchelleUpdating() throws IOException, ClassNotFoundException {
//		InstBHROS bhros = new InstBHROS();
//
//		// Set the wavelength and make sure everything adjusts.
//		bhros.setCentralWavelength(0.700);
//		assertEquals(bhros.getEchelleALT(), -56.6204);
//		assertEquals(bhros.getEchelleAz(), 1.2478);
//		assertEquals(bhros.getGoniAng(), -5.6265);
//
//		// Ditto with position.
//		bhros.setCCDCentreX(1.08);
//		bhros.setCCDCentreY(-15.04);
//		assertEquals(bhros.getEchelleALT(), -56.6006);
//		assertEquals(bhros.getEchelleAz(), 1.2650);
//		assertEquals(bhros.getGoniAng(), -5.6996);
//
//		// Make sure these values come back when we serialize.
//		bhros = (InstBHROS) serializeDeserialize(bhros);
//		assertEquals(bhros.getEchelleALT(), -56.6006);
//		assertEquals(bhros.getEchelleAz(), 1.2650);
//		assertEquals(bhros.getGoniAng(), -5.6996);
//
//	}
//
//	public void testProperties() throws Exception {
//		InstBHROS bhros = new InstBHROS();
//
//		// Enumerated Types
//		testProperty(bhros, "CCDReadoutPorts", BHROSParams.CCDReadoutPorts.values());
//		testProperty(bhros, "CCDGain",  BHROSParams.CCDGain.values());
//		testProperty(bhros, "CCDSpeed", BHROSParams.CCDSpeed.values());
//		testProperty(bhros, "CCDXBinning", BHROSParams.CCDXBinning.values());
//		testProperty(bhros, "CCDYBinning",  BHROSParams.CCDYBinning.values());
//		testProperty(bhros, "EntranceFibre",  BHROSParams.EntranceFibre.values());
////		testProperty(bhros, "ExposureMeterFilter", BHROSParams.ExposureMeterFilter.class, BHROSParams.ExposureMeterFilter.TYPES);
//		testProperty(bhros, "HartmannFlap", BHROSParams.HartmannFlap.values());
//		testProperty(bhros, "ISSPort",  BHROSParams.ISSPort.values());
//		testProperty(bhros, "ROI",  BHROSParams.ROI.values());
//
//		// Doubles
//		testProperty(bhros, "CentralWavelength", new double[] {0.4, 0.6, 0.9});
//		testProperty(bhros, "CCDCentreX", new double[] {-5, 0, 5});
//		testProperty(bhros, "CCDCentreY", new double[] {-5, -1});
//
//	}
//
//    /**
//       * Test to see that simple set/get works, as well as serialization of the property,
//       * persistence to/from a ParamSet.
//       */
//      private void testProperty(InstBHROS target, String propName, Class type, Collection values) throws Exception {
//
//          String propertyName = Character.toUpperCase(propName.charAt(0)) + "";
//          if (propName.length() > 1) {
//              propertyName += propName.substring(1);
//          }
//          Method get = target.getClass().getMethod("get" + propertyName);
//          Method set = target.getClass().getMethod("set" + propertyName, type);
//          for (Object val : values) {
//              // Simple get/set
//              set.invoke(target, val);
//              assertEquals(get.invoke(target, new Object[0]), val);
//
//              // Serialize, deserialize, then test property again
//              Object deserializedTarget = serializeDeserialize(target);
//              assertEquals(get.invoke(deserializedTarget, new Object[0]), val);
//
//              // Target and deserializedTarget should be equal() now.
//              assertEquals(target, deserializedTarget);
//
//              // Swizzle to ParamSet, then back
//              PioFactory factory = new PioXmlFactory();
//              ParamSet ps = target.getParamSet(factory);
//              InstBHROS newInstance = new InstBHROS();
//              newInstance.setParamSet(ps);
//              assertEquals(get.invoke(newInstance, new Object[0]), val);
//
//              // Target and deserializedTarget should be equal() now.
//              assertEquals(target, newInstance);
//          }
//      }
//
//      private void testProperty(InstBHROS target, String propName, double[] values) throws Exception {
//          Collection<Double> vals = new ArrayList<Double>(values.length);
//          for (double value : values) vals.add(value);
//          testProperty(target, propName, Double.TYPE, vals);
//      }
//
//       private void testProperty(InstBHROS target, String propName, Object[] values) throws Exception {
//          Collection<Object> vals = new ArrayList<Object>(values.length);
//          if (values.length <= 0) throw new Exception("Collection has zero elements!");
//          Class type = values[0].getClass();
//          for (Object value : values) {
//              vals.add(value);
//          }
//
//          testProperty(target, propName, type , vals);
//      }
//
//    private Object serializeDeserialize(Object o) throws IOException, ClassNotFoundException {
//		ByteArrayOutputStream baos = new ByteArrayOutputStream();
//		ObjectOutputStream oos = new ObjectOutputStream(baos);
//		oos.writeObject(o);
//		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
//		ObjectInputStream ois = new ObjectInputStream(bais);
//		return ois.readObject();
//	}
//
//}
