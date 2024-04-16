// Copyright 2011 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id$

package edu.gemini.spModel.smartgcal;

import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationFile;
import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationMap;
import edu.gemini.spModel.gemini.calunit.smartgcal.Version;
import edu.gemini.spModel.gemini.calunit.smartgcal.maps.*;
import edu.gemini.spModel.gemini.flamingos2.Flamingos2;
import edu.gemini.spModel.gemini.gmos.InstGmosNorth;
import edu.gemini.spModel.gemini.gmos.InstGmosSouth;
import edu.gemini.spModel.gemini.gnirs.InstGNIRS;
import edu.gemini.spModel.gemini.gpi.Gpi;
import edu.gemini.spModel.gemini.igrins2.Igrins2;
import edu.gemini.spModel.gemini.nifs.InstNIFS;
import edu.gemini.spModel.gemini.niri.InstNIRI;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 */
public class CalibrationMapFactory {
    private static final Logger LOG = Logger.getLogger(CalibrationMapFactory.class.getName());

    public static CalibrationMap createFromData(String instrument, CalibrationFile file) {
        final CalibrationMap map = createEmpty(instrument, file.getVersion());
        final CalibrationMapReader reader = new CalibrationMapReader(map);
        reader.read(file.getData().getBytes());
        // the data we receive here must not contain errors, it has to be checked previously
        // in case the data can not be successfully parsed we throw an exception
        if (reader.hasErrors()) {
            LOG.log(Level.INFO, "could not read calibration data " + reader.getErrors());
            throw new RuntimeException("could not read calibration data");
        }
        return map;
    }

    public static CalibrationMap createEmpty(String instrument, Version version) {
        // ------ GNIRS
        if (instrument.equals(InstGNIRS.SP_TYPE.readableStr)) {
            return new GNIRSCalibrationMap(version);
        // ------ GMOS-N
        } else if (instrument.equals(InstGmosNorth.SP_TYPE.readableStr)) {
            return new GMOSNCalibrationMap(version);
        // ------ GMOS-S
        } else if (instrument.equals(InstGmosSouth.SP_TYPE.readableStr)) {
            return new GMOSSCalibrationMap(version);
        // ------ NIFS
        } else if (instrument.equals(InstNIFS.SP_TYPE.readableStr)) {
            return new NIFSCalibrationMap(version);
        // ------ NIRI
        } else if (instrument.equals(InstNIRI.SP_TYPE.readableStr)) {
            return new NIRICalibrationMap(version);
        // ------ FLAMINGOS2
        } else if (instrument.equals(Flamingos2.SP_TYPE.readableStr)) {
            return new Flamingos2CalibrationMap(version);
        // ------ GPI
        } else if (instrument.equals(Gpi.SP_TYPE.readableStr)) {
            return new GpiCalibrationMap(version);
        // ------ Igrins2
        } else if (instrument.equals(Igrins2.SP_TYPE().readableStr)) {
            return new Igrins2CalibrationMap(version);

        // ------ add special maps for other instruments here....

        // ------ ups, don't know what to do with this...
        } else {
            throw new RuntimeException("unknown instrument " + instrument);
        }

    }
}
