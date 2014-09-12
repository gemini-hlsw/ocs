/**
 * $Id: ObjectTable.java 6986 2006-05-01 17:05:49Z shane $
 */

package edu.gemini.mask;

import jsky.catalog.skycat.SkycatTable;
import jsky.catalog.skycat.SkycatConfigEntry;
import jsky.catalog.skycat.SkycatCatalog;
import jsky.catalog.skycat.SkycatConfigFile;
import jsky.catalog.gui.CatalogUIHandler;
import jsky.catalog.gui.QueryResultDisplay;
import jsky.catalog.FieldDesc;
import jsky.catalog.MemoryCatalog;
import jsky.catalog.FieldDescAdapter;
import jsky.navigator.NavigatorFITSTable;

import javax.swing.JComponent;
import java.io.IOException;
import java.io.File;
import java.util.Vector;
import java.util.Iterator;
import java.util.Properties;
import java.util.List;
import java.util.Date;
import java.text.SimpleDateFormat;

import nom.tam.fits.FitsException;
import nom.tam.fits.Fits;
import nom.tam.fits.BinaryTable;
import nom.tam.fits.FitsFactory;
import nom.tam.fits.BinaryTableHDU;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.util.BufferedFile;

public class ObjectTable extends SkycatTable implements CatalogUIHandler {

    // plot symbol definition for an ObjectTable (color based on priority)
    private static String _SYMBOL =
            "priority {circle white {} {} {} {priority==\"3\"}} {15 {}} "
            + ": priority {triangle blue {} {} {} {priority==\"1\"}} {15 {}} "
            + ": priority {diamond cyan {} {} {} {priority==\"0\"}} {20 {}} "
            + ": priority {cross yellow {} {} {} {priority==\"X\"}} {15 {}} "
            + ": priority {square green {} {} {} {priority==\"2\"}} {15 {}}";

    // column names
    private static final String ID = "ID";
    private static final String RA = "RA";
    private static final String DEC = "DEC";
    private static final String X_CCD = "x_ccd";
    private static final String Y_CCD = "y_ccd";
    private static final String SPECPOS_X = "specpos_x";
    private static final String SPECPOS_Y = "specpos_y";
    private static final String SLITPOS_X = "slitpos_x";
    private static final String SLITPOS_Y = "slitpos_y";
    private static final String SLITSIZE_X = "slitsize_x";
    private static final String SLITSIZE_Y = "slitsize_y";
    private static final String SLITTILT = "slittilt";
    private static final String MAG = "MAG";
    private static final String PRIORITY = "priority";
    private static final String SLITTYPE = "slittype";

    // column indexes
    private static int _colCtr = 0;
    public static final int ID_COL = _colCtr++;
    public static final int RA_COL = _colCtr++;
    public static final int DEC_COL = _colCtr++;
    public static final int X_CCD_COL = _colCtr++;
    public static final int Y_CCD_COL = _colCtr++;
    public static final int SPECPOS_X_COL = _colCtr++;
    public static final int SPECPOS_Y_COL = _colCtr++;
    public static final int SLITPOS_X_COL = _colCtr++;
    public static final int SLITPOS_Y_COL = _colCtr++;
    public static final int SLITSIZE_X_COL = _colCtr++;
    public static final int SLITSIZE_Y_COL = _colCtr++;
    public static final int SLITTILT_COL = _colCtr++;
    public static final int MAG_COL = _colCtr++;
    public static final int PRIORITY_COL = _colCtr++;
    public static final int SLITTYPE_COL = _colCtr++;

    // Column names
    public static final String[] TABLE_COLUMN_NAMES = {
        ID,
        RA,
        DEC,
        X_CCD,
        Y_CCD,
        SPECPOS_X,
        SPECPOS_Y,
        SLITPOS_X,
        SLITPOS_Y,
        SLITSIZE_X,
        SLITSIZE_Y,
        SLITTILT,
        MAG,
        PRIORITY,
        SLITTYPE,
    };

    public static final int NUM_COLS = TABLE_COLUMN_NAMES.length;

    // Column types
    public static final Class[] TABLE_COLUMN_TYPES = {
        Integer.class, // ID
        Double.class,  // RA
        Double.class,  // DEC
        Double.class,  // X_CCD
        Double.class,  // Y_CCD
        Double.class,  // SPECPOS_X
        Double.class,  // SPECPOS_Y
        Double.class,  // SLITPOS_X
        Double.class,  // SLITPOS_Y
        Double.class,  // SLITSIZE_X
        Double.class,  // SLITSIZE_Y
        Double.class,  // SLITTILT
        Double.class,  // MAG
        String.class,  // PRIORITY
        String.class,  // SLITTYPE
    };

    // Column units (when written to a FITS table file)
    public static final String[] TABLE_COLUMN_UNITS = {
        "#",       // ID
        "H",       // RA
        "deg",     // DEC
        "pixels",  // X_CCD
        "pixels",  // Y_CCD
        "pixels",  // SPECPOS_X
        "pixels",  // SPECPOS_Y
        "arcsec",  // SLITPOS_X
        "arcsec",  // SLITPOS_Y
        "arcsec",  // SLITSIZE_X
        "arcsec",  // SLITSIZE_Y
        "deg",     // SLITTILT
        "#",       // MAG
        "c",       // PRIORITY
        "c",       // SLITTYPE
    };

    // Indicates which columns to display for an object table (OT)l
    // Hided SPECPOS_X and SPECPOS_Y, since the are only defined in ODF tables.
    public static final boolean[] OT_TABLE_COLUMNS = {
        true,  // ID
        true,  // RA
        true,  // DEC
        true,  // X_CCD
        true,  // Y_CCD
        false, // SPECPOS_X
        false, // SPECPOS_Y
        true,  // SLITPOS_X
        true,  // SLITPOS_Y
        true,  // SLITSIZE_X
        true,  // SLITSIZE_Y
        true,  // SLITTILT
        true,  // MAG
        true,  // PRIORITY
        true,   // SLITTYPE
    };

    public static final String DEFAULT_SLITTYPE = "R";


    // Column description for new tables
    public static FieldDesc[] TABLE_FIELDS = _makeFields();

    // Cached reference to the widget displaying the query results
    private ObjectTableDisplay _queryResultDisplay;

    // Original FITS header, used to replicate FITS keywords in output tables
    private Header _header;

    // Holds mask related parameters for this table data
    private MaskParams _maskParams;

    /**
     * Create an empty ObjectTable with the standard columns, and settings derived
     * from the given table.
     */
    public ObjectTable(ObjectTable table) {
        super(_makeConfigEntry(table.getFilename()), new Vector(), TABLE_FIELDS);
        setColumnClasses(TABLE_COLUMN_TYPES);
        _header = table._header;
        _maskParams = table._maskParams;
        _queryResultDisplay = table._queryResultDisplay;
    }

    private ObjectTable(ObjectTable table, FieldDesc[] fields, Vector dataRows) {
        super(table, fields, dataRows);
        setColumnClasses(TABLE_COLUMN_TYPES);
        _header = table._header;
        _maskParams = table._maskParams;
        _queryResultDisplay = table._queryResultDisplay;

        setProperties(table.getProperties());
        setFilename(table.getFilename());
    }

    private ObjectTable(String filename, Header header, SkycatConfigEntry configEntry, Vector dataRows,
                        FieldDesc[] fields) {
        super(configEntry, dataRows, fields);
        setFilename(filename);
        setColumnClasses(TABLE_COLUMN_TYPES);
        _header = header;
        _maskParams = new MaskParams(this);
    }

    public MaskParams getMaskParams() {
        return _maskParams;
    }

    public String getTitle() {
        return new File(getFilename()).getName();
    }

    /** These are local FITS files */
    public boolean isLocal() {
        return true;
    }

    /**
     * Returns true if the table is an ODF (based on the name).
     */
    public boolean isODF() {
        return getFilename().matches(".*ODF[0-9]+\\.fits");
    }

    /**
     * Create and return an ObjectTable from the given FITS table file with only the standard
     * columns, adding or removing columns from the input data as needed.
     */
    public static ObjectTable makeObjectTable(String filename)
            throws IOException, FitsException {

        filename = new File(filename).getCanonicalPath();  // get absolute path
        if (!filename.endsWith("fits")) {
            throw new IllegalArgumentException("Expected a FITS (.fits) file, but got: " + filename);
        }

        NavigatorFITSTable fitsTable = NavigatorFITSTable.getFitsTable(filename);
        List oldData = fitsTable.getDataVector();
        List colNames = fitsTable.getColumnIdentifiers();
        _changeRaUnits(oldData, colNames);

        // find the indexes of the standard columns in the input data
        int[] colIndexes = new int[NUM_COLS];
        for(int i = 0; i < NUM_COLS; i++) {
            colIndexes[i] = colNames.indexOf(TABLE_COLUMN_NAMES[i]);
        }

        // create a new data vector with only the standard columns, adding any missing
        // XXX Keep nonstandard columns, append to end?
        Vector newData = new Vector(oldData.size());
        Iterator it = oldData.iterator();
        while(it.hasNext()) {
            Vector oldRow = (Vector)it.next();
            Vector newRow = new Vector(NUM_COLS);
            for(int i = 0; i < NUM_COLS; i++) {
                if (colIndexes[i] != -1) {
                    newRow.add(oldRow.get(colIndexes[i]));
                } else {
                    newRow.add(_getDefaultColumnValue(i));
                }
            }
            newData.add(newRow);
        }

        Header header = fitsTable.getHeader();
        ObjectTable table = new ObjectTable(filename, header, _makeConfigEntry(filename),
                newData, TABLE_FIELDS);
        return table;
    }

    /**
     * Returns the original FITS header from the input FITS table file
     */
    public Header getHeader() {
        return _header;
    }

    /**
     * Save this table as a FITS binary table in the given file.
     * The file is overwritten, if it exists, and will contain only the FITS table and
     * the primary HDU.
     *
     * @param filename The name of the new FITS file to create
     * @param maskParams contains original input table and mask parameters
     */
    public void saveAsFitsTable(String filename, MaskParams maskParams)
            throws FitsException, IOException {
        Fits fits = new Fits();

        // make the new table
        BinaryTable binTable = new BinaryTable();
        FitsFactory.setUseAsciiTables(false);
        int ncols = getColumnCount();
        for (int col = 0; col < ncols; col++) {
            Object data = _getColumnArray(col);
            binTable.addColumn(data);
        }

        BinaryTableHDU hdu = (BinaryTableHDU) Fits.makeHDU(binTable);
        Header header = hdu.getHeader();
        for (int col = 0; col < ncols; col++) {
            hdu.setColumnName(col, getColumnName(col), null);
            header.addValue("TUNIT"+(col+1), TABLE_COLUMN_UNITS[col], null);
        }

        _updateHeader(header, filename, maskParams);

        fits.addHDU(hdu);
        new File(filename).delete();
        BufferedFile bf = new BufferedFile(filename, "rw");
        fits.write(bf);
        bf.close();
    }

    // Update the FITS table header to include the original table keys and some that we want
    // to add here.
    private void _updateHeader(Header header, String filename, MaskParams maskParams)
            throws FitsException {

        header.addValue("EXTNAME", "MINIMAL.TAB", "name of this binary table extension");

        // Add header keys from original table header
        Header originalHeader = maskParams.getTable().getHeader();
        Iterator it = originalHeader.iterator();
        while(it.hasNext()) {
            HeaderCard card = (HeaderCard)it.next();
            String key = card.getKey();
            if (!header.containsKey(key) && !_ignoreKey(key)) { // don't overwrite
                header.addValue(card.getKey(), card.getValue(), card.getComment());
            }
        }

        // Other values
        header.addValue("FILTSPEC", maskParams.getFilter().name(), null);
        header.addValue("GRATING", maskParams.getDisperser().name(), null);
        header.addValue("WAVELENG", maskParams.getWavelength(), null);
        header.addValue("SPEC_LEN", maskParams.getSpecLen(), null);
        header.addValue("ANAMORPH", maskParams.getAnaMorphic(), null);
        header.addValue("SPOCMODE", maskParams.getBiasType(), null);
        header.addValue("GMMPSVER", MaskParams.VERSION, null);
        header.addValue("MASK_PA", maskParams.getPa(), null);
        header.addValue("PERS_ODF", System.getProperty("user.name"), null);
        header.addValue("DATE_ODF", new Date().toString(), null);
        header.addValue("TIME_ODF", _formatTime(new Date().getTime()), null);

        // File name
        String s = filename;
        if (s.length() > HeaderCard.MAX_VALUE_LENGTH) {
            s = filename.substring(0, HeaderCard.MAX_VALUE_LENGTH);
        }
        header.addValue("FILENAME", s, null);
        header.addValue("FILE_ODF", new File(filename).getName(), null);

        // Nod&Shuffle keywords
        BandDef bandDef = maskParams.getBandDef();
        if (bandDef != null) {
            int shuffleMode = bandDef.getShuffleMode();
            if (shuffleMode == BandDef.BAND_SHUFFLE) {
                header.addValue("BANDSIZE", bandDef.getBandSize(), null);
                BandDef.Band[] bands = bandDef.getBands();
                for(int i = 0; i < bands.length; i++) {
                    header.addValue("BAND"+(i+1)+"Y", bands[i].getYPos(), null);
                }
                header.addValue("SHUFSIZE", bandDef.getBandShufflePix(), null);
            } else if (shuffleMode == BandDef.MICRO_SHUFFLE) {
                header.addValue("NODSIZE", bandDef.getNodAmount(), null);
                header.addValue("SHUFSIZE", bandDef.getMicroShufflePix(), null);
            }
        }
    }

    // Return true if the FITS keyword should be ignored (not copied from the input
    // to the output table)
    private boolean _ignoreKey(String key) {
        return key.startsWith("TUNIT")
            || key.startsWith("TFORM")
            || key.startsWith("TDIM")
            || key.startsWith("TDISP")
            || key.startsWith("TTYPE")
            || key.equals("FILENAME")
            || key.equals("END");
    }

    /**
     * Return a string with the given time in the format 2005-07-21T12:16:17.000,
     * given the current time in ms.
     */
    private static String _formatTime(long time) {
        Date date = new Date(time);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
//        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(date);
    }

    public Class getColumnClass(int columnIndex) {
        return TABLE_COLUMN_TYPES[columnIndex];
    }


    // Return the values in the given column as a primitive array
    private Object _getColumnArray(int col) {
        int nrows = getRowCount();
        Class c = getColumnClass(col);

        if (c.equals(Float.class) || c.equals(Double.class)) {
            float[] ar = new float[nrows];
            for (int row = 0; row < nrows; row++) {
                ar[row] = _getFloatValue(row, col);
            }
            if (col == RA_COL) {
                // need to change units on RA col
                _changeRaUnits(ar);
            }
            return ar;
        }

        if (c.equals(Integer.class)) {
            int[] ar = new int[nrows];
            for (int row = 0; row < nrows; row++) {
                ar[row] = _getIntValue(row, col);
            }
            return ar;
        }

        if (c.equals(String.class)) {
            String[] ar = new String[nrows];
            for (int row = 0; row < nrows; row++) {
                ar[row] = _getStringValue(row, col);
            }
            return ar;
        }

        throw new IllegalArgumentException("Unexpected class type: " + c.getName());
    }


    private static Object _getDefaultColumnValue(int col) {
        if (col == SLITTYPE_COL) {
            return DEFAULT_SLITTYPE;
        }
        return null;
    }


    protected MemoryCatalog makeQueryResult(FieldDesc[] fields, Vector dataRows) {
        ObjectTable table = new ObjectTable(this, fields, dataRows);
        return table;
    }

    // Create and return the default column descriptions for a new table
    private static FieldDesc[] _makeFields() {
        FieldDesc[] ar = new FieldDesc[NUM_COLS];
        for (int i = 0; i < NUM_COLS; i++) {
            ar[i] = new FieldDescAdapter(TABLE_COLUMN_NAMES[i]);
            // XXX call ar[i].setDescription()
        }
        return ar;
    }

    /**
     * Implement the {@link CatalogUIHandler} interface to get a custom GUI
     */
    public JComponent makeComponent(QueryResultDisplay display) {
        if (_queryResultDisplay == null) {
            _queryResultDisplay = new ObjectTableDisplay(this, display);
        } else {
            _queryResultDisplay.setQueryResult(this);
        }
        return _queryResultDisplay;
    }

    /**
     * Make a plottable skycat catalog from the given FITS table file
     */
    public static SkycatCatalog makeCatalog(String filename)
            throws IOException, FitsException {
        ObjectTable table = makeObjectTable(filename);

        SkycatConfigEntry configEntry = _makeConfigEntry(filename);
        return new SkycatCatalog(configEntry, table);
    }

    // Change the units of the RA column from hours to deg, since the catalog classes
    // expect that. This is called after reading the data from a FITS table.
    private static void _changeRaUnits(List rows, List colNames) {
        for (int col = 0; col < colNames.size(); col++) {
            if (((String)colNames.get(col)).equalsIgnoreCase("ra")) {
                Iterator it = rows.iterator();
                while (it.hasNext()) {
                    Vector row = (Vector)it.next();
                    Object o = row.get(col);
                    if (o instanceof Float) {
                        Float f = (Float)o;
                        row.setElementAt(new Float(f.floatValue() * 15.), col);
                    }
                }
                break;
            }
        }
    }

    // Change the units of the RA column from deg to hours. This is called when writing
    // to a FITS table
    private static void _changeRaUnits(float[] raValues) {
        for(int i = 0; i < raValues.length; i++) {
            raValues[i] = (float)(raValues[i] / 15.0);
        }
    }


    // Return a catalog configuration entry for a local catalog file
    // with plot symbol information
    private static SkycatConfigEntry _makeConfigEntry(String filename) {
        Properties props = new Properties();
        props.setProperty(SkycatConfigFile.SERV_TYPE, "local");
        props.setProperty(SkycatConfigFile.LONG_NAME, filename);
        props.setProperty(SkycatConfigFile.SYMBOL, _SYMBOL);
        return new SkycatConfigEntry(props);
    }

    private int _getIntValue(int row, int col) {
        Integer i = (Integer)getValueAt(row, col);
        if (i == null) {
            return 0;
        }
        return i.intValue();
    }

    private void _setIntValue(int i, int row, int col) {
        setValueAt(new Integer(i), row, col);
    }

    private String _getStringValue(int row, int col) {
        String s = getValueAt(row, col).toString();
        if (s == null) {
            return null;
        }
        return s;
    }

    private void _setStringValue(String s, int row, int col) {
        setValueAt(s, row, col);
    }

    private double _getDoubleValue(int row, int col) {
        Number n = (Number)getValueAt(row, col);
        if (n == null) {
            return 0.;
        }
        return n.doubleValue();
    }

    private void _setDoubleValue(double d, int row, int col) {
        setValueAt(new Double(d), row, col);
    }

    private float _getFloatValue(int row, int col) {
        Number n = (Number)getValueAt(row, col);
        if (n == null) {
            return 0.0F;
        }
        return n.floatValue();
    }

//    private void _setFloatValue(float d, int row, int col) {
//        setValueAt(new Float(d), row, col);
//    }

    public int getId(int row) {
        return _getIntValue(row, ID_COL);
    }

    public void setId(int row, int id) {
        _setIntValue(id, row, ID_COL);
    }


    public double getRa(int row) {
        return _getDoubleValue(row, RA_COL);
    }

    public void setRa(int row, double ra) {
        _setDoubleValue(ra, row, RA_COL);
    }


    public double getDec(int row) {
        return _getDoubleValue(row, DEC_COL);
    }

    public void setDec(int row, double dec) {
        _setDoubleValue(dec, row, DEC_COL);
    }


    public double getXCcd(int row) {
        return _getDoubleValue(row, X_CCD_COL);
    }

    public void setXCcd(int row, double xCcd) {
        _setDoubleValue(xCcd, row, X_CCD_COL);
    }


    public double getYCcd(int row) {
        return _getDoubleValue(row, Y_CCD_COL);
    }

    public void setYCcd(int row, double yCcd) {
        _setDoubleValue(yCcd, row, Y_CCD_COL);
    }


    public double getSpecPosX(int row) {
        return _getDoubleValue(row, SPECPOS_X_COL);
    }

    public void setSpecPosX(int row, double specPosX) {
        _setDoubleValue(specPosX, row, SPECPOS_X_COL);
    }


    public double getSpecPosY(int row) {
        return _getDoubleValue(row, SPECPOS_Y_COL);
    }

    public void setSpecPosY(int row, double specPosY) {
        _setDoubleValue(specPosY, row, SPECPOS_Y_COL);
    }


    public double getSlitPosX(int row) {
        return _getDoubleValue(row, SLITPOS_X_COL);
    }

    public void setSlitPosX(int row, double slitPosX) {
        _setDoubleValue(slitPosX, row, SLITPOS_X_COL);
    }


    public double getSlitPosY(int row) {
        return _getDoubleValue(row, SLITPOS_Y_COL);
    }

    public void setSlitPosY(int row, double slitPosY) {
        _setDoubleValue(slitPosY, row, SLITPOS_Y_COL);
    }


    public double getSlitSizeX(int row) {
        return _getDoubleValue(row, SLITSIZE_X_COL);
    }

    public void setSlitSizeX(int row, double slitSizeX) {
        _setDoubleValue(slitSizeX, row, SLITSIZE_X_COL);
    }


    public double getSlitSizeY(int row) {
        return _getDoubleValue(row, SLITSIZE_Y_COL);
    }

    public void setSlitSizeY(int row, double slitSizeY) {
        _setDoubleValue(slitSizeY, row, SLITSIZE_Y_COL);
    }


    public double getSlitTilt(int row) {
        return _getDoubleValue(row, SLITTILT_COL);
    }

    public void setSlitTilt(int row, double slitTilt) {
        _setDoubleValue(slitTilt, row, SLITTILT_COL);
    }


    public double getMag(int row) {
        return _getDoubleValue(row, MAG_COL);
    }

    public void setMag(int row, double mag) {
        _setDoubleValue(mag, row, MAG_COL);
    }


    public String getPriority(int row) {
        return _getStringValue(row, PRIORITY_COL);
    }

    public void setPriority(int row, String priority) {
        _setStringValue(priority, row, PRIORITY_COL);
    }


    public String getSlitType(int row) {
        return _getStringValue(row, SLITTYPE_COL);
    }

    public void setSlitType(int row, String slitType) {
        _setStringValue(slitType, row, SLITTYPE_COL);
    }

}
