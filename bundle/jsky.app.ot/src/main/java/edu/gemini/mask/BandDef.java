/**
 * $Id: BandDef.java 6526 2005-08-03 21:27:13Z brighton $
 */

package edu.gemini.mask;

import nom.tam.fits.Header;

import javax.swing.event.SwingPropertyChangeSupport;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.ArrayList;

/**
 * Band definitions for nod&shuffle modes.
 */
public class BandDef {
    // Shuffel mode values
    public static final int NO_SHUFFLE = 0;
    public static final int MICRO_SHUFFLE = 1;
    public static final int BAND_SHUFFLE = 2;

    // property names
    static final String SHUFFLE_MODE = "shuffleMode";
    static final String SLIT_LENGTH = "slitLenth";
    static final String BAND_SIZE = "bandSize";
    static final String BINNING = "binning";
    static final String MICRO_SHUFFLE_PIX = "microShufflePix";
    static final String BAND_SHUFFLE_PIX = "bandShufflePix";
    static final String MICRO_SHUFFLE_AMOUNT = "microShuffleAmount";
    static final String BAND_SHUFFLE_AMOUNT = "bandShuffleAmount";
    static final String NOD_AMOUNT = "nodAmount";
    static final String BANDS_Y_AMOUNT = "bandsYAmount";
    static final String BANDS = "bands";
    static final String PIXEL_SCALE = "pixelScale";
    static final String IMAGE_HEIGHT = "imageHeight";

    // extra separation between bands of two pixels
    private static final int EXTRABANDSEP = 2;

    public static class Band {
        private int _num = 1;
        private String _name;
        private double _yPos;
        private double _height;

        public Band(int num, String name, double yPos, double height) {
            _num = num;
            _name = name;
            _yPos = yPos;
            _height = height;
        }

        public int getNum() {
            return _num;
        }

        public String getName() {
            return _name;
        }

        public double getYPos() {
            return _yPos;
        }

        public double getHeight() {
            return _height;
        }
    };

    // One of NO_SHUFFLE, MICRO_SHUFFLE, BAND_SHUFFLE
    private int _shuffleMode = NO_SHUFFLE;


    // -- microshuffle values

    private int _slitLength;

    // Shuffle amount in arcsec
    private double _microShuffleAmount;

    // Shuffle amount in unbinned pixels
    private double _microShufflePix;

    private int _nodAmount;

    // -- band shuffle values

    private int _bandSize;

    // Shuffle amount in arcsec
    private double _bandShuffleAmount;

    // Shuffle amount in unbinned pixels
    private double _bandShufflePix;

    private int _bandsYOffset = 0;

    // calculated band offsets
    private Band[] _bands= new Band[0];

    // -- other values

    // Binning factor (from image FITS header)
    private int _binning = 1;

    // BImage height in pixels (from image FITS header)
    private int _imageHeight;

    // parameter (comes from FITS keyword)
    private double _pixelScale;

    // If true, don't calculate arcmin/pix values for shuffle amount
    private boolean _noCalc = false;


    // This object serves as part of the model for the MaskDialog GUI class
    private SwingPropertyChangeSupport _propertyChangeSupport
            = new SwingPropertyChangeSupport(this);


    // --


    public BandDef(double pixelScale) {
        _pixelScale = pixelScale;
        resetToDefault(MICRO_SHUFFLE);
        resetToDefault(BAND_SHUFFLE);
    }


    public void resetToDefault(int shuffleMode) {
        if (shuffleMode == MICRO_SHUFFLE) {
            setSlitLength(5);
            setMicroShuffleAmount(5);
            setNodAmount(0);
            _calcShufflePixels(MICRO_SHUFFLE);
        } else if (shuffleMode == BAND_SHUFFLE) {
            setBandSize(1535);
            setBandShufflePix(1535);
            setBandsYOffset(0);
            _calcShuffleAmount(BAND_SHUFFLE);
        }
    }


    public int getShuffleMode() {
        return _shuffleMode;
    }

    public void setShuffleMode(int shuffleMode) {
        int oldValue = _shuffleMode;
        _shuffleMode = shuffleMode;
        _firePropertyChange(SHUFFLE_MODE, new Integer(oldValue), new Integer(_shuffleMode));
        if (_shuffleMode == BAND_SHUFFLE) {
            _updateBands();
        }
    }

    public int getSlitLength() {
        return _slitLength;
    }

    public void setSlitLength(int slitLength) {
        int oldValue = _slitLength;
        _slitLength = slitLength;
        _firePropertyChange(SLIT_LENGTH, new Integer(oldValue), new Integer(_slitLength));
    }

    public int getBandSize() {
        return _bandSize;
    }

    public void setBandSize(int bandSize) {
        int oldValue = _bandSize;
        _bandSize = bandSize;
        _firePropertyChange(BAND_SIZE, new Integer(oldValue), new Integer(_bandSize));
    }

    public int getBinning() {
        return _binning;
    }

    public void setBinning(int binning) {
        int oldValue = _binning;
        _binning = binning;
        _firePropertyChange(BINNING, new Integer(oldValue), new Integer(_binning));
    }

    public double getMicroShufflePix() {
        return _microShufflePix;
    }

    public void setMicroShufflePix(double shufflePix) {
        double oldValue = _microShufflePix;
        _microShufflePix = shufflePix;
        _firePropertyChange(MICRO_SHUFFLE_PIX, new Double(oldValue), new Double(_microShufflePix));
        _calcShuffleAmount(_shuffleMode);
    }

    public double getBandShufflePix() {
        return _bandShufflePix;
    }

    public void setBandShufflePix(double shufflePix) {
        double oldValue = _bandShufflePix;
        _bandShufflePix = shufflePix;
        _firePropertyChange(BAND_SHUFFLE_PIX, new Double(oldValue), new Double(_bandShufflePix));
        _calcShuffleAmount(_shuffleMode);
    }

    public double getMicroShuffleAmount() {
        return _microShuffleAmount;
    }

    public void setMicroShuffleAmount(double shuffleAmount) {
        double oldValue = _microShuffleAmount;
        _microShuffleAmount = shuffleAmount;
        _firePropertyChange(MICRO_SHUFFLE_AMOUNT, new Double(oldValue), new Double(_microShuffleAmount));
        _calcShufflePixels(_shuffleMode);
    }

    public double getBandShuffleAmount() {
        return _bandShuffleAmount;
    }

    public void setBandShuffleAmount(double shuffleAmount) {
        double oldValue = _bandShuffleAmount;
        _bandShuffleAmount = shuffleAmount;
        _firePropertyChange(BAND_SHUFFLE_AMOUNT, new Double(oldValue), new Double(_bandShuffleAmount));
        _calcShufflePixels(_shuffleMode);
    }

    public int getNodAmount() {
        return _nodAmount;
    }

    public void setNodAmount(int nodAmount) {
        int oldValue = _nodAmount;
        _nodAmount = nodAmount;
        _firePropertyChange(NOD_AMOUNT, new Integer(oldValue), new Integer(_nodAmount));
    }

    public int getBandsYOffset() {
        return _bandsYOffset;
    }

    public void setBandsYOffset(int bandsYOffset) {
        int oldValue = _bandsYOffset;
        _bandsYOffset = bandsYOffset;
        _firePropertyChange(BANDS_Y_AMOUNT, new Integer(oldValue), new Integer(_bandsYOffset));
    }

    public Band[] getBands() {
        return _bands;
    }

    public void setBands(Band[] bands) {
        Band[] oldValue = _bands;
        _bands = bands;
        _firePropertyChange(BANDS, oldValue, _bands);
    }


    public void setPixelScale(double pixelScale) {
        double oldValue = _pixelScale;
        _pixelScale = pixelScale;
        _firePropertyChange(PIXEL_SCALE, new Double(oldValue), new Double(_pixelScale));
    }

    public void setImageHeight(int imageHeight) {
        double oldValue = _imageHeight;
        _imageHeight = imageHeight;
        _firePropertyChange(IMAGE_HEIGHT, new Double(oldValue), new Double(_imageHeight));
    }

    // calculate shufflePixels from shuffleAmount
    private void _calcShufflePixels(int shuffleMode) {
        if (_noCalc) {
            return;
        }
        _noCalc = true;
        try {
            // note: shuffle pixels are always UNBINNED!, thus binning is
            // factored in below
            if (shuffleMode == MICRO_SHUFFLE) {
                double spx = _microShuffleAmount * _binning / _pixelScale;
                int spxf = (int)Math.floor(spx);
                if (spxf < spx) {
                    spxf = spxf + 1;
                }
                setMicroShufflePix(spxf);

            } else if (shuffleMode == BAND_SHUFFLE) {
                double spx = _bandShuffleAmount * _binning / _pixelScale;
                int spxf = (int)Math.floor(spx);
                if (spxf < spx) {
                    spxf = spxf + 1;
                }
                setBandShufflePix(spxf);
                setBandSize(spxf);
                _updateBands();
            }
        } finally {
            _noCalc = false;
        }
    }

    // calculate shuffleAmount from shufflePixels
    private void _calcShuffleAmount(int shuffleMode) {
        if (_noCalc) {
            return;
        }
        _noCalc = true;
        try {
            if (shuffleMode == MICRO_SHUFFLE) {
                setMicroShuffleAmount(_microShufflePix * _pixelScale / _binning);
            } else if (shuffleMode == BAND_SHUFFLE) {
                setBandShuffleAmount(_bandShufflePix * _pixelScale / _binning);
                _updateBands();
            }
        } finally {
            _noCalc = false;
        }
    }

    private void _updateBands() {
        setBands(_getBands());
    }

    // Returns an array of band y offsets
     private Band[] _getBands() {
         // note: shufflePx is given in unbinned pixels
         double shuffPx = getBandShufflePix();
         int yOffset = getBandsYOffset();
         double height = getBandSize();
         int iHt = _imageHeight * _binning;
         double maxY = iHt - height - shuffPx - (EXTRABANDSEP / 2);
         List<Band> bands = new ArrayList<>();
         double yStart = shuffPx + (EXTRABANDSEP / 2) + yOffset;
         double yInc = shuffPx + height + EXTRABANDSEP;

         int num = 0;
         for (double y = yStart; y <= maxY; y += yInc) {
             num++;
             bands.add(new Band(num, "band" + num, y, height));
         }
         Band[] ar = new Band[bands.size()];
         bands.toArray(ar);
         return ar;
     }

    /**
     * Validates band definition values.
     * @return a string containing an error message, or null if there are no errors
     */
    public String validate() {
        if (_shuffleMode == MICRO_SHUFFLE) {
            if (_slitLength < 0) {
                return "Slit length must be > 0";
            }

            if (_microShufflePix < 0) {
                return "Shuffle amount (unbined pixels) must be > 0";
            }

        } else if (_shuffleMode == BAND_SHUFFLE) {
            if (_bandSize < 0) {
                return "Band size must be > 0";
            }

            if (_bandShufflePix < 0) {
                return "Shuffle amount (unbined pixels) must be > 0";
            }

            if (_bandSize > _bandShufflePix) {
                return "Band size must be less than shuffle amount (unbined pixels)";
            }
        }

        return null;
    }

    // Update this object from the FITS keywords in the given header
    void updateFromFitsHeader(Header header) {
        if (header.containsKey("BANDSIZE")) {
            _shuffleMode = BAND_SHUFFLE;
            _bandSize = header.getIntValue("BANDSIZE", _bandSize);

            // restore the bands array
            int i = 0;
            List<Band> bandList = new ArrayList<>();
            do {
                String key = "BAND" + ++i + "Y";
                if (!header.containsKey(key)) {
                    break;
                }
                double yPos = header.getDoubleValue(key);
                Band band = new Band(i, "band+i", yPos, _bandSize);
                bandList.add(band);
            } while(true);
            _bands = new Band[bandList.size()];
            bandList.toArray(_bands);

            _bandShufflePix = header.getDoubleValue("SHUFSIZE", _bandShufflePix);
        } else if (header.containsKey("NODSIZE")) {
            _shuffleMode = MICRO_SHUFFLE;
            _nodAmount = header.getIntValue("NODSIZE", _nodAmount);
            _microShufflePix = header.getDoubleValue("SHUFSIZE", _microShufflePix);
        }
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
