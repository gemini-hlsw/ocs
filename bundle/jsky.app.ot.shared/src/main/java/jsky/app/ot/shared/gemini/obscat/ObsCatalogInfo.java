package jsky.app.ot.shared.gemini.obscat;

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
import edu.gemini.spModel.obscomp.InstConfigInfo;
import jsky.catalog.FieldDescAdapter;

import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

/**
 * Contains static definitions related to the ObsCatalog class.
 */
public final class ObsCatalogInfo {

    /**
     * Min RA coordinate
     */
    public static final String MIN_RA = "Min RA";

    /**
     * Max RA coordinate
     */
    public static final String MAX_RA = "Max RA";

    /**
     * Min Dec coordinate
     */
    public static final String MIN_DEC = "Min Dec";

    /**
     * Max Dec coordinate
     */
    public static final String MAX_DEC = "Max Dec";

    /**
     * The base position target name
     */
    public static final String TARGET_NAME = "Target";

    /**
     * The Gemini science program reference number
     */
    public static final String PROG_REF = "Prog Ref";

    /**
     * Is it an active program?
     */
    public static final String ACTIVE = "Active";

    /**
     * Is the program complete?
     */
    public static final String COMPLETED = "Completed";

    public static final String THESIS = "Thesis";
    public static final String ROLLOVER = "Rollover";
    public static final String TOO = "TOO";

    /**
     * The observation's class (charge class)
     */
    public static final String OBS_CLASS = "Class";

    /**
     * The observation id
     */
    public static final String OBS_ID = "Obs Id";

    /**
     * The last name of the primary investigator
     */
    public static final String PI_LAST_NAME = "PI";

    /**
     * The email address of the PI/NGO/Contact Scientist
     */
    public static final String EMAIL = "Email";

    /**
     * The observing semester (2003B, 2004A, ...)
     */
    public static final String SEMESTER = "Semester";

    /**
     * The partner country responsible for the observation
     */
    public static final String PARTNER_COUNTRY = "Partner";

    /**
     * The Gemini program status
     */
    public static final String PROG_STATUS = "Prog Status";

    /**
     * The Gemini observation status
     */
    public static final String OBS_STATUS = "Obs. Status";

    /**
     * The Gemini observation data status
     */
    public static final String DATAFLOW_STEP = "Dataflow Step";

    /**
     * The science ranking or queue band
     */
    public static final String QUEUE_BAND = "Band";

    /**
     * Observation QA.
     */
    public static final String OBS_QA = "Obs. QA";

    /**
     * The observation priority
     */
    public static final String PRIORITY = "User Prio";

    /**
     * The adaptive optics property
     */
    public static final String AO = "AO";


    /**
     * The required sky background observing condition
     */
    public static final String SKY_BACKGROUND = "Sky BG";

    /**
     * The required water vapor observing condition
     */
    public static final String WATER_VAPOR = "WV";

    /**
     * The required cloud cover observing condition
     */
    public static final String CLOUD_COVER = "Cloud";

    /**
     * The required image quality observing condition
     */
    public static final String IMAGE_QUALITY = "IQ";

    /**
     * parameter: RA coordinate
     */
    public static final String RA = "RA";

    /**
     * parameter: DEC coordinate
     */
    public static final String DEC = "Dec";

    /**
     * parameter: observation group name
     */
    public static final String GROUP = "Group";

    /**
     * parameter: observation group type
     */
    public static final String GROUP_TYPE = "GT";

    /**
     * parameter: elevation constraint
     */
    public static final String ELEVATION_CONSTRAINT = "Elev. Const.";

    /**
     * parameter: timimg constraint
     */
    public static final String TIMING_CONSTRAINT = "Time Const.";

    /**
     * The total planned exposure time for the observation
     */
    public static final String PLANNED_EXEC_TIME = "Planned Exec Time";

    /**
     * The total planned exposure time for the observation to be charged to PI
     */
    public static final String PLANNED_PI_TIME = "Planned PI Time";

    /**
     * The total charged time for the observation.  For a science observation
     * this will come from the program charge class, for a nightime calibration,
     * from the partner charge class.
     */
    public static final String CHARGED_TIME = "Charged Time";

    /**
     * The instrument for the observation
     */
    public static final String INSTRUMENT = "Inst";

    /**
     * Is the observation ready to be observed?
     */
    public static final String READY = "Ready";

    /**
     * The available instruments to choose from
     */
    public static final String[] INSTRUMENTS = {
            InstAcqCam.SP_TYPE.readableStr,
            InstBHROS.SP_TYPE.readableStr,
            Flamingos2.SP_TYPE.readableStr,
            Ghost.SP_TYPE.readableStr,
            InstGmosNorth.SP_TYPE.readableStr,
            InstGmosSouth.SP_TYPE.readableStr,
            InstGNIRS.SP_TYPE.readableStr,
            Gsaoi.SP_TYPE.readableStr,
            Gpi.SP_TYPE.readableStr,
            InstMichelle.SP_TYPE.readableStr,
            InstNICI.SP_TYPE.readableStr,
            InstNIFS.SP_TYPE.readableStr,
            InstNIRI.SP_TYPE.readableStr,
            InstPhoenix.SP_TYPE.readableStr,
            InstTexes.SP_TYPE.readableStr,
            InstTReCS.SP_TYPE.readableStr,
            VisitorInstrument.SP_TYPE.readableStr
    };

    /**
     * An array of table column names in the result of a query to this class
     * (not including the instrument specific columns).
     */
    public static final String[] TABLE_COLUMNS = {
            PROG_REF, OBS_ID, PI_LAST_NAME,
            INSTRUMENT, TARGET_NAME,
            RA, DEC,
            QUEUE_BAND, PARTNER_COUNTRY,
            OBS_STATUS, OBS_QA, DATAFLOW_STEP,
            PLANNED_EXEC_TIME, PLANNED_PI_TIME, CHARGED_TIME, OBS_CLASS,
            SKY_BACKGROUND, WATER_VAPOR,
            CLOUD_COVER, IMAGE_QUALITY, PRIORITY, AO, GROUP,
            GROUP_TYPE, ELEVATION_CONSTRAINT, TIMING_CONSTRAINT,
            READY,
    };

    /**
     * Index of RA column in query results
     */
    public static final int RA_COL = 4;

    /**
     * Index of Dec column in query results
     */
    public static final int DEC_COL = 5;

    // Array of table columns in the result of a query (all of them)
    private static String[] _tableColumns;

    // Array of additional instrument specific table columns
    private static String[] _instTableColumns;

    // Describes the table columns in the result of a query of this catalog
    private static FieldDescAdapter[] _fieldDesc;


    /**
     * Return the instrument configuration information of the named instrument.
     */
    public static List<InstConfigInfo> getInstConfigInfoList(String instName) {
        // XXX This could be replaced by something using reflection to avoid hard coding the inst names
        List<InstConfigInfo> instConfigInfoList = null;
        if (instName.equals(InstBHROS.SP_TYPE.readableStr))
            instConfigInfoList = InstBHROS.getInstConfigInfo();
        else if (instName.equals(Flamingos2.SP_TYPE.readableStr))
            instConfigInfoList = Flamingos2.getInstConfigInfo();
        else if (instName.equals(InstNICI.SP_TYPE.readableStr))
            instConfigInfoList = InstNICI.getInstConfigInfo();
        else if (instName.equals(InstGmosNorth.SP_TYPE.readableStr))
            instConfigInfoList = InstGmosNorth.getInstConfigInfo();
        else if (instName.equals(InstGmosSouth.SP_TYPE.readableStr))
            instConfigInfoList = InstGmosSouth.getInstConfigInfo();
        else if (instName.equals(InstNIRI.SP_TYPE.readableStr))
            instConfigInfoList = InstNIRI.getInstConfigInfo();
        else if (instName.equals(InstNIFS.SP_TYPE.readableStr))
            instConfigInfoList = InstNIFS.getInstConfigInfo();
        else if (instName.equals(InstGNIRS.SP_TYPE.readableStr))
            instConfigInfoList = InstGNIRS.getInstConfigInfo();
        else if (instName.equals(InstAcqCam.SP_TYPE.readableStr))
            instConfigInfoList = InstAcqCam.getInstConfigInfo();
        else if (instName.equals(InstPhoenix.SP_TYPE.readableStr))
            instConfigInfoList = InstPhoenix.getInstConfigInfo();
        else if (instName.equals(InstTexes.SP_TYPE.readableStr))
            instConfigInfoList = InstTexes.getInstConfigInfo();
        else if (instName.equals(InstTReCS.SP_TYPE.readableStr))
            instConfigInfoList = InstTReCS.getInstConfigInfo();
        else if (instName.equals(InstMichelle.SP_TYPE.readableStr))
            instConfigInfoList = InstMichelle.getInstConfigInfo();
            //        else if (instName.equals(InstAltair.SP_TYPE.getReadable()))
            //            instConfigInfoList = InstAltair.getInstConfigInfo();
        else if (instName.equals(Gsaoi.SP_TYPE.readableStr))
            instConfigInfoList = Gsaoi.getInstConfigInfo();
        else if (instName.equals(VisitorInstrument.SP_TYPE.readableStr))
            instConfigInfoList = VisitorInstrument.getInstConfigInfo();
        else if (instName.equals(Gpi.SP_TYPE.readableStr))
            instConfigInfoList = Gpi.getInstConfigInfo();
        else if (instName.equals(Ghost.SP_TYPE.readableStr))
            instConfigInfoList = Ghost.getInstConfigInfo();
        return instConfigInfoList;
    }

    /**
     * Return the table columns corresponding to the given instrument.
     * If instName is null, the non-instrument specific columns are returned.
     */
    public static String[] getTableColumns(String instName) {
        if (instName == null)
            return TABLE_COLUMNS;

        Vector<String> v = new Vector<>();
        for (String inst : INSTRUMENTS) {
            if (inst.equals(instName)) {
                List<InstConfigInfo> l = getInstConfigInfoList(inst);
                for (InstConfigInfo info: l) {
                    v.add(info.getName());
                }
                String[] ar = new String[v.size()];
                return v.toArray(ar);
            }
        }
        return null;
    }

    /**
     * Return the array of table columns in a query result
     */
    public static String[] getTableColumns() {
        if (_tableColumns == null)
            getFieldDescr();
        return _tableColumns;
    }


    /**
     * Return an array of objects describing the table columns in the result of a
     * query to this catalog.
     */
    public static FieldDescAdapter[] getFieldDescr() {
        if (_fieldDesc == null) {
            _getInstTableColumns();
            int n = TABLE_COLUMNS.length + _instTableColumns.length;
            _tableColumns = new String[n];
            _fieldDesc = new FieldDescAdapter[n];
            for (int i = 0; i < TABLE_COLUMNS.length; i++) {
                _fieldDesc[i] = new FieldDescAdapter(TABLE_COLUMNS[i]);
                _tableColumns[i] = TABLE_COLUMNS[i];
                if (TABLE_COLUMNS[i].equals(RA)) {
                    _fieldDesc[i].setIsRA(true);
                } else if (TABLE_COLUMNS[i].equals(DEC))
                    _fieldDesc[i].setIsDec(true);
            }
            for (int i = 0; i < _instTableColumns.length; i++) {
                int index = TABLE_COLUMNS.length + i;
                _fieldDesc[index] = new FieldDescAdapter(_instTableColumns[i]);
                _tableColumns[index] = _instTableColumns[i];
            }
        }
        return _fieldDesc;
    }

    /**
     * Return the number of table columns specific to the given instrument, or
     * if the instrument name is null, the number of non-instrument specific columns.
     */
    public static int getNumTableColumns(String instName) {
        if (instName == null)
            return TABLE_COLUMNS.length;

        for (String inst : INSTRUMENTS) {
            if (inst.equals(instName)) {
                List<InstConfigInfo> l = getInstConfigInfoList(inst);
                return l.size();
            }
        }
        return 0;
    }


    /**
     * Return the index of the given instrument, or -1 if not known
     */
    public static int getInstIndex(String inst) {
        for (int i = 0; i < INSTRUMENTS.length; i++)
            if (INSTRUMENTS[i].equals(inst))
                return i;
        return -1;
    }


    /**
     * Determine the instrument specific table columns (all instruments combined)
     */
    private static void _getInstTableColumns() {
        Vector<String> instColName = new Vector<>();
        for (String inst : INSTRUMENTS) {
            List<InstConfigInfo> l = getInstConfigInfoList(inst);
            instColName.addAll(l.stream().map(InstConfigInfo::getName).collect(Collectors.toList()));
        }
        String[] ar = new String[instColName.size()];
        _instTableColumns = instColName.toArray(ar);
    }

    /**
     * Return the array of instrument specific table columns in a query result
     */
    public static String[] getInstTableColumns() {
        if (_instTableColumns == null)
            getFieldDescr();
        return _instTableColumns;
    }
}

