// Copyright 2011 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id$

package edu.gemini.spModel.gemini.calunit.smartgcal;

import edu.gemini.spModel.gemini.flamingos2.Flamingos2;
import edu.gemini.spModel.gemini.gmos.InstGmosNorth;
import edu.gemini.spModel.gemini.gmos.InstGmosSouth;
import edu.gemini.spModel.gemini.gnirs.InstGNIRS;
import edu.gemini.spModel.gemini.gpi.Gpi;
import edu.gemini.spModel.gemini.igrins2.Igrins2;
import edu.gemini.spModel.gemini.nifs.InstNIFS;
import edu.gemini.spModel.gemini.niri.InstNIRI;

import java.util.*;

/**
 * Definitions for the smart gemini calibrations service.
 */
public class SmartGcalService {

    /**
     * List of all known instruments that use smart calibrations.
     */
    // NOTE: use a list in order to guarantee order of instruments
    private static final List<String> INSTRUMENT_NAMES = new ArrayList<String>() {{
        add(InstGmosNorth.SP_TYPE.readableStr);
        add(InstGmosSouth.SP_TYPE.readableStr);
        add(InstGNIRS.SP_TYPE.readableStr);
        add(InstNIFS.SP_TYPE.readableStr);
        add(InstNIRI.SP_TYPE.readableStr);
        add(Flamingos2.SP_TYPE.readableStr);
        add(Gpi.SP_TYPE.readableStr);
        add(Igrins2.SP_TYPE().readableStr);
    }};

    public static final List<String> getInstrumentNames() {
        return INSTRUMENT_NAMES;
    }

    /**
     * List of all available types per instrument.
     */
    // NOTE: use a list in order to guarantee order of calibration images (e.g. arc before flat)
    private static final Map<String, List<Calibration.Type>> AVAILABLE_TYPES = new HashMap<String, List<Calibration.Type>>();
    static {
        AVAILABLE_TYPES.put(InstGmosNorth.SP_TYPE.readableStr, new ArrayList<Calibration.Type>() {{ add(Calibration.Type.ARC); add(Calibration.Type.FLAT); }} );
        AVAILABLE_TYPES.put(InstGmosSouth.SP_TYPE.readableStr, new ArrayList<Calibration.Type>() {{ add(Calibration.Type.ARC); add(Calibration.Type.FLAT); }} );
        AVAILABLE_TYPES.put(InstGNIRS.SP_TYPE.readableStr, new ArrayList<Calibration.Type>() {{ add(Calibration.Type.ARC); add(Calibration.Type.FLAT); }} );
        AVAILABLE_TYPES.put(InstNIFS.SP_TYPE.readableStr, new ArrayList<Calibration.Type>() {{ add(Calibration.Type.ARC); add(Calibration.Type.FLAT); }} );
        AVAILABLE_TYPES.put(InstNIRI.SP_TYPE.readableStr, new ArrayList<Calibration.Type>() {{ add(Calibration.Type.ARC); add(Calibration.Type.FLAT); }} );
        AVAILABLE_TYPES.put(Flamingos2.SP_TYPE.readableStr, new ArrayList<Calibration.Type>() {{ add(Calibration.Type.ARC); add(Calibration.Type.FLAT); }} );
        AVAILABLE_TYPES.put(Gpi.SP_TYPE.readableStr, new ArrayList<Calibration.Type>() {{ add(Calibration.Type.ARC); add(Calibration.Type.FLAT); }} );
        AVAILABLE_TYPES.put(Igrins2.SP_TYPE().readableStr, new ArrayList<Calibration.Type>() {{ add(Calibration.Type.FLAT); }});
    }

    public static final List<Calibration.Type> getAvailableTypes(String instrument) {
        List<Calibration.Type> types = AVAILABLE_TYPES.get(instrument);
        if (types == null) {
            throw new IllegalArgumentException("missing map entry for instrument: " + instrument);
        }
        return types;
    }

    /**
     * The context name of the smart calibration servlet
     */
    public static final String SERVER_CONTEXT_NAME = "gcal";

    /**
     * valid commands
     */
    public static final String COMMAND_UPLOAD = "upload";
    public static final String COMMAND_DOWNLOAD = "download";
    public static final String COMMAND_VALIDATE = "validate";
    public static final String COMMAND_VERSIONS = "versions";
    public static final String COMMAND_UPDATE_CACHE = "updatecache";
    public static final String COMMAND_CLEAR_CACHE = "clearcache";

    /**
     * valid parameters
     */
    public static final String PARAMETER_COMMAND = "command";
    public static final String PARAMETER_INSTRUMENT = "instrument";
    public static final String PARAMETER_TYPE = "type";
    public static final String PARAMETER_VERSIONED = "versioned";

}
