/**
 * $Id: MaskParams.java 7064 2006-05-25 19:48:25Z shane $
 */

package edu.gemini.mask;

import edu.gemini.spModel.gemini.gmos.GmosCommonType;
import edu.gemini.spModel.gemini.gmos.GmosNorthType;
import nom.tam.fits.Header;

import javax.swing.event.SwingPropertyChangeSupport;
import java.beans.PropertyChangeListener;

/**
 * Holds information used in calculating the mask slits. These values are also included
 * as FITS keywords in the output ODF FITS tables.
 */
public class MaskParams {

    // The application version
    public static final String VERSION = "1.0";

    // Bias option to produce a slit catalog of the same X-dimension distribution as the
    // input object catalog
    public static final String BIAS_TYPE_NORMAL = "Normal_Opt";

//    // Bias option favors smaller catalog objects but performs a better maximization in
//    // term of number of placed slits.
//    public static final String BIAS_TYPE_MAX = "Max_Opt";

    // default pixelScale conversion factor (get from FITS header)
    public static final double DEFAULT_PIXEL_SCALE = 0.0727;

    // property names
    static final String TABLE = "table";
    static final String NUM_MASKS = "numMasks";
    static final String WAVELENGTH = "wavelength";
    static final String INSTRUMENT = "instrument";
    static final String DISPERSER = "disperser";
    static final String FILTER = "filter";
    static final String PA = "pa";
    static final String PIXEL_SCALE = "pixelScale";
    static final String SPEC_LEN = "specLen";
    static final String ANAMORPHIC = "anaMorphic";
    static final String BIAS_TYPE = "biasType";
    static final String DPIX = "dpix";
    static final String LMAX = "lmax";
    static final String LMIN = "lmin";


    // The input table (read from FITS table)
    private ObjectTable _table;

    // Number of masks to create
    private int _numMasks = 1;

    // central wavelength
    private double _wavelength = 600.;

    // Instrument name, set from FITS header
    private String _instrument = "GMOS-N";

    // The selected disperser
    private GmosCommonType.Disperser _disperser = GmosNorthType.DisperserNorth.B600_G5303;

    // The selected filter
    private GmosCommonType.Filter _filter = GmosNorthType.FilterNorth.NONE;

    // Parallactic angle
    private double _pa = 0.0;

    // pixelScale conversion factor, get from FITS header
    private double _pixelScale = DEFAULT_PIXEL_SCALE;

    // spectra length(pix)
    private double _specLen;

    // spectra anamorphic factor
    private double _anaMorphic;

    // type of algorithm:
    // BIAS_TYPE_NORMAL = do not change object X-dimension distribution
    // BIAS_TYPE_MAX = get more objects but small ones are favored
    private String _biasType = BIAS_TYPE_NORMAL;

    // Linear dispersion
    private double _dpix;

    // Lamda 1 max
    private double _lmax;

    // Lamda 2 min
    private double _lmin;

    // Band Structure (only used in nod&shuffle mode)
    private BandDef _bandDef = new BandDef(_pixelScale);

    // This object serves as the model for the MaskDialog GUI class
    private SwingPropertyChangeSupport _propertyChangeSupport
            = new SwingPropertyChangeSupport(this);


    // --

    public MaskParams(ObjectTable table) {
        _table = table;

        _updateFromFitsHeader();
    }

    // Initialize values from the table's FITS header
    private void _updateFromFitsHeader() {
        Header header = _table.getHeader();
        _pixelScale = header.getDoubleValue("PIXSCALE", _pixelScale);
        _bandDef.setPixelScale(_pixelScale);

        _instrument = header.getStringValue("INSTRUME");
        if (_instrument == null) {
            _instrument = "GMOS-S";
        }

        if (_table.isODF()) {
            _wavelength = header.getDoubleValue("WAVELENG", _wavelength);
            _specLen = header.getDoubleValue("SPEC_LEN", _specLen);
            _anaMorphic = header.getDoubleValue("ANAMORPH", _anaMorphic);
            _biasType = header.getStringValue("SPOCMODE");
            _pa = header.getDoubleValue("MASK_PA", _pa);
        }

        // update Nod&Shuffle keywords
        _bandDef.updateFromFitsHeader(header);
    }

    public ObjectTable getTable() {
        return _table;
    }

    public void setTable(ObjectTable table) {
        ObjectTable oldValue = _table;
        _table = table;
        _firePropertyChange(TABLE, oldValue, _table);
    }

    public int getNumMasks() {
        return _numMasks;
    }

    public void setNumMasks(int numMasks) {
        int oldValue = _numMasks;
        _numMasks = numMasks;
        _firePropertyChange(NUM_MASKS, oldValue, _numMasks);
    }

    public double getWavelength() {
        return _wavelength;
    }

    public void setWavelength(double wavelength) {
        double oldValue = _wavelength;
        _wavelength = wavelength;
        _firePropertyChange(WAVELENGTH, oldValue, _wavelength);
    }

    public String getInstrument() {
        return _instrument;
    }

    public void setInstrument(String instrument) {
        String oldValue = _instrument;
        _instrument = instrument;
        _firePropertyChange(INSTRUMENT, oldValue, _instrument);
    }

    public GmosCommonType.Disperser getDisperser() {
        return _disperser;
    }

    public void setDisperser(GmosCommonType.Disperser disperser) {
        DisperserInfo.getDisperserInfo(disperser); // throws an exception if not supported
        GmosCommonType.Disperser oldValue = _disperser;
        _disperser = disperser;
        _firePropertyChange(DISPERSER, oldValue, _disperser);
    }

    public GmosCommonType.Filter getFilter() {
        return _filter;
    }

    public void setFilter(GmosCommonType.Filter filter) {
        FilterInfo.getFilterInfo(filter); // throws an exception if not supported
        GmosCommonType.Filter oldValue = _filter;
        _filter = filter;
        _firePropertyChange(FILTER, oldValue, _filter);
    }

    public double getPa() {
        return _pa;
    }

    public void setPa(double pa) {
        double oldValue = _pa;
        _pa = pa;
        _firePropertyChange(PA, oldValue, _pa);
    }

    public double getPixelScale() {
        return _pixelScale;
    }

    public void setPixelScale(double pixelScale) {
        double oldValue = _pixelScale;
        _pixelScale = pixelScale;
        _firePropertyChange(PIXEL_SCALE, oldValue, _pixelScale);
    }

    public double getSpecLen() {
        return _specLen;
    }

    public void setSpecLen(double specLen) {
        double oldValue = _specLen;
        _specLen = specLen;
        _firePropertyChange(SPEC_LEN, oldValue, _specLen);
    }

    public double getAnaMorphic() {
        return _anaMorphic;
    }

    public void setAnaMorphic(double anaMorphic) {
        double oldValue = _anaMorphic;
        _anaMorphic = anaMorphic;
        _firePropertyChange(ANAMORPHIC, oldValue, _anaMorphic);
    }

    public String getBiasType() {
        return _biasType;
    }

    public void setBiasType(String biasType) {
        String oldValue = _biasType;
        _biasType = biasType;
        _firePropertyChange(BIAS_TYPE, oldValue, _biasType);
    }

    public double getDpix() {
        return _dpix;
    }

    public void setDpix(double dpix) {
        double oldValue = _dpix;
        _dpix = dpix;
        _firePropertyChange(DPIX, oldValue, _dpix);
    }

    public double getLmax() {
        return _lmax;
    }

    public void setLmax(double lmax) {
        double oldValue = _lmax;
        _lmax = lmax;
        _firePropertyChange(LMAX, oldValue, _lmax);
    }

    public double getLmin() {
        return _lmin;
    }

    public void setLmin(double lmin) {
        double oldValue = _lmin;
        _lmin = lmin;
        _firePropertyChange(LMIN, oldValue, _lmin);
    }

    public BandDef getBandDef() {
        return _bandDef;
    }


    // ---- Property Change Support ----


    public void addPropertyChangeListener(PropertyChangeListener listener) {
        _propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        _propertyChangeSupport.removePropertyChangeListener(listener);
    }

    private void _firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        if (newValue != null && newValue.equals(oldValue)) {
            return;
        }
        _propertyChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }
}
