package edu.gemini.spModel.gemini.ghost;

public enum DetectorManufacturer {
    BLUE("E2V_CCD231-84-1-G57", "Blue", DetectorManufacturer.E2V_PIXEL_SIZE, 4096, 4112, 0.0003263888888888889),
    RED ("E2V_CCD231-C6-1-G58", "Red", DetectorManufacturer.E2V_PIXEL_SIZE, 6144, 4096, 0.00022916666666666666);

    public static final double E2V_PIXEL_SIZE = 0.4; // arcsec/pixel

    public static final DetectorManufacturer DEFAULT = RED;

    private final String _displayValue;
    private final String _manufacter;
    private final double _pixelSize;
    private final int _xSize;
    private final int _ySize;

    private final double _darkCurrent;

    DetectorManufacturer(final String manufacter, final String displayValue,
                         final double pixelSize, final int xSize, final int ySize, final double darkCurrent) {
        this._manufacter = manufacter;
        this._displayValue = displayValue;
        this._pixelSize = pixelSize;
        this._xSize = xSize;
        this._ySize = ySize;
        this._darkCurrent = darkCurrent;
    }

    public String displayValue() {
        return _displayValue;
    }

    public int getXsize() {
        return _xSize;
    }

    public double getDarkCurrent() { return _darkCurrent;}

    public String getManufacter() {
        return _manufacter;
    }

}
