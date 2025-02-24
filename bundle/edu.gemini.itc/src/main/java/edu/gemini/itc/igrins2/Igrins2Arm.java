package edu.gemini.itc.igrins2;

import edu.gemini.spModel.core.MagnitudeBand;

public enum Igrins2Arm {
    H("H",
            MagnitudeBand.unsafeFromString("H"),
            1490.0,
            1650.0,
            1800.0,
            "detector_H",
            0.1,
            2048,
            2048,
            2.5,
            0.0225,
            100000,
            100000,
            80000,
            80000),
    K("K",
            MagnitudeBand.unsafeFromString("K"),
            1960.0,
            2200.0,
            2460.0,
            "detector_K",
            0.1,
            2048,
            2048,
            2.5,
            0.04,
            93600,
            93600,
            75000,
            75000);

    private final String _name;
    private final MagnitudeBand _magnitudeBand;
    private final double _wavelengthStart;  // nanometers
    private final double _wavelengthCentral;  // nanometers
    private final double _wavelengthEnd;  // nanometers
    private final String _detectorFilename;
    private final double _pixelSize;  // arcseconds
    private final int _detXSize;
    private final int _detYSize;
    private final double _gain;  // electrons / ADU
    private final double _wellDepth;  // electrons
    private final double _darkCurrent;  // electrons / second / pixel
    private final int _maxRecommendedFlux;  // electrons
    private final int _saturationLimit;  // electrons
    private final int _linearityLimit;  // electrons

    Igrins2Arm(final String name,
               final MagnitudeBand magnitudeBand,
               final double wavelengthStart,
               final double wavelengthCentral,
               final double wavelengthEnd,
               final String detectorFilename,
               final double pixelSize,
               final int detXSize,
               final int detYSize,
               final double gain,
               final double darkCurrent,
               final int wellDepth,
               final int saturationLimit,
               final int linearityLimit,
               final int maxRecommendedFlux) {
        this._name = name;
        this._magnitudeBand = magnitudeBand;
        this._wavelengthStart = wavelengthStart;
        this._wavelengthCentral = wavelengthCentral;
        this._wavelengthEnd = wavelengthEnd;
        this._detectorFilename = detectorFilename;
        this._pixelSize = pixelSize;
        this._detXSize = detXSize;
        this._detYSize = detYSize;
        this._gain = gain;
        this._darkCurrent = darkCurrent;
        this._wellDepth = wellDepth;
        this._saturationLimit = saturationLimit;
        this._linearityLimit = linearityLimit;
        this._maxRecommendedFlux = maxRecommendedFlux;
    }

    public String getName() { return _name; }
    public MagnitudeBand getMagnitudeBand() { return _magnitudeBand; }
    public double getWavelengthStart() { return _wavelengthStart; }
    public double getWavelengthCentral() { return _wavelengthCentral; }
    public double getWavelengthEnd() { return _wavelengthEnd; }
    public double getPixelSize() { return _pixelSize; }
    public int getDetXSize() { return _detXSize; }
    public int getDetYSize() { return _detYSize; }
    public double getDarkCurrent() { return _darkCurrent;}
    public String getDetectorFilename() { return _detectorFilename; }
    public double getGain() { return _gain;}
    public double getWellDepth() { return _wellDepth;}
    public int getSaturationLimit() { return _saturationLimit; }
    public int getLinearityLimit() { return _linearityLimit; }
    public int get_maxRecommendedFlux() { return _maxRecommendedFlux; }
}
