package edu.gemini.itc.altair;

import edu.gemini.itc.shared.FormatStringWriter;
import edu.gemini.itc.shared.ITCParameters;
import edu.gemini.spModel.gemini.altair.AltairParams;


/**
 * This class holds the information from the Altair section
 * of an ITC web page.  This object is constructed from a servlet request.
 */
public final class AltairParameters extends ITCParameters {

    private final boolean _altairUsed;
    private final AltairParams.GuideStarType _wfsMode;
    private final double _guideStarSeperation;
    private final double _guideStarMagnitude;
    private final AltairParams.FieldLens _fieldLens;

    /**
     * Constructs a AltairParameters from a servlet request
     *
     * @throws Exception if input data is not parsable.
     */
    public AltairParameters(
            double guideStarSeperation,
            double guideStarMagnitude,
            AltairParams.FieldLens fieldLens,
            AltairParams.GuideStarType wfsMode,
            boolean altairUsed) {
        _guideStarSeperation = guideStarSeperation;
        _guideStarMagnitude = guideStarMagnitude;
        _fieldLens = fieldLens;
        _wfsMode = wfsMode;
        _altairUsed = altairUsed;

        // validation
        if (_guideStarSeperation < 0 || _guideStarSeperation > 25)
            throw new IllegalArgumentException(" Altair Guide star distance must be between 0 and 25 arcsecs.");

        if (_wfsMode.equals(AltairParams.GuideStarType.LGS) && _guideStarMagnitude > 19.5)
            throw new IllegalArgumentException(" Altair Guide star Magnitude must be <= 19.5 in R for LGS mode. ");

        if (_wfsMode.equals(AltairParams.GuideStarType.NGS) && _guideStarMagnitude > 15.5)
            throw new IllegalArgumentException(" Altair Guide star Magnitude must be <= 15.5 in R for NGS mode. ");

        if (_wfsMode.equals(AltairParams.GuideStarType.LGS) && _fieldLens.equals(AltairParams.FieldLens.OUT))
            throw new IllegalArgumentException("The field Lens must be IN when Altair is in LGS mode.");
    }


    public boolean altairIsUsed() {
        return _altairUsed;
    }

    public double getGuideStarSeperation() {
        return _guideStarSeperation;
    }

    public double getGuideStarMagnitude() {
        return _guideStarMagnitude;
    }

    public AltairParams.FieldLens getFieldLens() { return _fieldLens; }

    public AltairParams.GuideStarType getWFSMode() {
        return _wfsMode;
    }

    /**
     * Return a human-readable string for debugging
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Guide Star Seperation:\t" + getGuideStarSeperation() + "\n");
        sb.append("Guide Star Magnitude:\t" + getGuideStarMagnitude() + "\n");
        sb.append("Field Lens:\t" + _fieldLens + "\n");
        if (getWFSMode().equals(AltairParams.GuideStarType.NGS))
            sb.append("Altair Mode:\t Natural guide star");
        else
            sb.append("Altair Mode:\t Laser guide star");

        sb.append("\n");
        return sb.toString();
    }

}
