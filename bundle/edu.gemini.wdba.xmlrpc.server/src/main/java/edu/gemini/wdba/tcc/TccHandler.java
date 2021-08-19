//
// $Id: TccXmlRpcHandler.java 887 2007-07-04 15:38:49Z gillies $
//
package edu.gemini.wdba.tcc;

import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.ext.ObservationNode;
import edu.gemini.spModel.ext.ObservationNodeFunctor;
import edu.gemini.spModel.gemini.acqcam.InstAcqCam;
import edu.gemini.spModel.gemini.bhros.InstBHROS;
import edu.gemini.spModel.gemini.flamingos2.Flamingos2;
import edu.gemini.spModel.gemini.ghost.Ghost;
import edu.gemini.spModel.gemini.gmos.InstGmosNorth;
import edu.gemini.spModel.gemini.gmos.InstGmosSouth;
import edu.gemini.spModel.gemini.gnirs.InstGNIRS;
import edu.gemini.spModel.gemini.gpi.Gpi;
import edu.gemini.spModel.gemini.gsaoi.Gsaoi;
import edu.gemini.spModel.gemini.michelle.InstMichelle;
import edu.gemini.spModel.gemini.nici.InstNICI;
import edu.gemini.spModel.gemini.nifs.InstNIFS;
import edu.gemini.spModel.gemini.niri.InstNIRI;
import edu.gemini.spModel.gemini.phoenix.InstPhoenix;
import edu.gemini.spModel.gemini.texes.InstTexes;
import edu.gemini.spModel.gemini.trecs.InstTReCS;
import edu.gemini.spModel.gemini.visitor.VisitorInstrument;
import edu.gemini.wdba.glue.api.WdbaContext;
import edu.gemini.wdba.glue.api.WdbaDatabaseAccessService;
import edu.gemini.wdba.glue.api.WdbaGlueException;
import edu.gemini.wdba.xmlrpc.ITccXmlRpc;
import edu.gemini.wdba.xmlrpc.ServiceException;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of the OCS 1/TCC Coordinate fetch functionality.
 *
 * @author K.Gillies
 */
public final class TccHandler implements ITccXmlRpc {

    private static final Logger LOG = Logger.getLogger(TccHandler.class.getName());

    // The private support map
    private static final Map<SPComponentType, String> SUPPORT_MAP = _initializeSupportList();


    private WdbaContext ctx;

    public TccHandler(WdbaContext ctx) {
        this.ctx = ctx;
    }

    private static boolean _isGoodSupportInterface(String className, String instrumentName) {
        boolean isSupport = false;
        try {
            Class<?> cl = Class.forName(className);
            for (Class<?> itsInterface : cl.getInterfaces()) {
                if (itsInterface == ITccInstrumentSupport.class) {
                    LOG.fine("Registering: " + cl.getName() + " for instrument: " + instrumentName);
                    // Now check that it implements a create factory method
                    cl.getMethod("create", ObservationEnvironment.class);
                    isSupport = true;
                    break;
                }
            }
        } catch (NoSuchMethodException ex) {
            LOG.severe("ITccInstrumentSupport implementer has no create method?");
        } catch (ClassNotFoundException ex) {
            LOG.severe("Suggested support class not found: " + className);
        }
        return isSupport;
    }

    // This internal method buidls a Map of SPComponentTypes and implementation
    // class names.  Eventually, this will be built from XML files
    private static Map<SPComponentType, String> _initializeSupportList() {
        Map<SPComponentType,String> supportMap = new HashMap<>();
        // Add NIRI support
        supportMap.put(InstNIRI.SP_TYPE, "edu.gemini.wdba.tcc.NIRISupport");
        // Add GMOS North support
        supportMap.put(InstGmosNorth.SP_TYPE, "edu.gemini.wdba.tcc.GMOSSupport");
        // Add GMOS South support
        supportMap.put(InstGmosSouth.SP_TYPE, "edu.gemini.wdba.tcc.GMOSSupport");
        // Add TReCS Support
        supportMap.put(InstTReCS.SP_TYPE, "edu.gemini.wdba.tcc.TRECSSupport");
        // Add Phoenix Support
        supportMap.put(InstPhoenix.SP_TYPE, "edu.gemini.wdba.tcc.PhoenixSupport");
        // Add AcqCam Support
        supportMap.put(InstAcqCam.SP_TYPE, "edu.gemini.wdba.tcc.AcquisitionCameraSupport");
        // Add the GNIRS Support
        supportMap.put(InstGNIRS.SP_TYPE, "edu.gemini.wdba.tcc.GNIRSSupport");
        // Add the BHROS Support
        supportMap.put(InstBHROS.SP_TYPE, "edu.gemini.wdba.tcc.BHROSSupport");
        // Add the NIFS Support
        supportMap.put(InstNIFS.SP_TYPE, "edu.gemini.wdba.tcc.NIFSSupport");
        // Add Michelle Support
        supportMap.put(InstMichelle.SP_TYPE, "edu.gemini.wdba.tcc.MichelleSupport");
         // Add the Flamingos2 Support
        supportMap.put(Flamingos2.SP_TYPE, "edu.gemini.wdba.tcc.Flamingos2Support");
        // Add the NICI Support
        supportMap.put(InstNICI.SP_TYPE, "edu.gemini.wdba.tcc.NICISupport");
        // Add Texes Support
        supportMap.put(InstTexes.SP_TYPE, "edu.gemini.wdba.tcc.TexesSupport");
        // Add Gsaoi Support
        supportMap.put(Gsaoi.SP_TYPE, "edu.gemini.wdba.tcc.GsaoiSupport");
        // Add Gpi Support
        supportMap.put(Gpi.SP_TYPE, "edu.gemini.wdba.tcc.GpiSupport");
        // Add GHOST Support
        supportMap.put(Ghost.SP_TYPE, "edu.gemini.wdba.tcc.GhostSupport");
        // Add Visitor Instrument Support
        supportMap.put(VisitorInstrument.SP_TYPE, "edu.gemini.wdba.tcc.VisitorInstrumentSupport");

        // Check that they are proper interfaces and remove if not
        // Keep a list of removeables
        List<SPComponentType> remove = new ArrayList<>();
        try {
        for (SPComponentType key : supportMap.keySet()) {
            String className = supportMap.get(key);
            if (!_isGoodSupportInterface(className, key.narrowType)) {
                LOG.severe("Suggested support class does not support ITccInstrumentSupport: " + className);
                remove.add(key);
            }
        }
        } catch (Throwable ex) {
            ex.printStackTrace();
            LOG.log(Level.SEVERE, "shit", ex);
        }
        // Now remove all
        for (SPComponentType spType :remove) {
            supportMap.remove(spType);
        }

        return Collections.unmodifiableMap(supportMap);
    }

    /**
     * Returns the XML coordinate data for a specific observation id.
     *
     * @param observationId the observation that should be returned
     * @return an XML document that contains the coordinates.
     */
    public String getCoordinates(String observationId) throws ServiceException {
        if (observationId == null) {
            String message = "Observation ID was null in getCoordinates.";
            LOG.severe(message);
            throw new ServiceException(message);
        }

        final WdbaDatabaseAccessService dbAccess = ctx.getWdbaDatabaseAccessService();
        final Site site = ctx.getSite();

        // Result string
        String result;
        ObservationNode obsNode;
        try {
            LOG.fine("Fetching tcc: " + observationId);
            ISPObservation spObs = dbAccess.getObservation(observationId);
            IDBDatabaseService db = dbAccess.getDatabase();
            obsNode = ObservationNodeFunctor.getObservationNode(db, spObs, ctx.getUser());
        } catch (WdbaGlueException ex) {
            throw new ServiceException("Database Exception building config: ", ex);
        }

        try {
            // Build the XML TCC config document
            TccConfig tc = new TccConfig(site);
            tc.build(obsNode, SUPPORT_MAP);
            result = tc.configToString();
        } catch (WdbaGlueException ex) {
            throw new ServiceException("");
        }

        return result;
    }
}
