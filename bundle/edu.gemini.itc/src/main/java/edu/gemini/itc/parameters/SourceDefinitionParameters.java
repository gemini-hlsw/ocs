// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
// $Id: SourceDefinitionParameters.java,v 1.7 2004/01/12 16:31:43 bwalls Exp $
//
package edu.gemini.itc.parameters;

import javax.servlet.http.HttpServletRequest;

import java.util.Iterator;

import edu.gemini.itc.shared.ITCParameters;
import edu.gemini.itc.shared.ITCConstants;
import edu.gemini.itc.shared.ITCMultiPartParser;
import edu.gemini.itc.shared.NoSuchParameterException;
import edu.gemini.itc.shared.FormatStringWriter;


/**
 * This class holds the information from the Source Definition section
 * of an ITC web page.  This object is constructed from a servlet request.
 */
public final class SourceDefinitionParameters extends ITCParameters {
    // ITC web form parameter names.
    // These constants must be kept in sync with the web page form.
    // They are used to parse form data.
    public static final String SOURCE_GEOM = "sourceGeom";
    public static final String SOURCE_NORM_PT_SOURCE = "psSourceNorm";
    public static final String SOURCE_NORM_USB = "usbSourceNorm";
    public static final String SOURCE_NORM_GAUSSIAN = "gaussSourceNorm";
    public static final String SOURCE_UNITS_PT_SOURCE = "psSourceUnits";
    public static final String SOURCE_UNITS_USB = "usbSourceUnits";
    public static final String SOURCE_UNITS_GAUSSIAN = "gaussSourceUnits";
    public static final String SOURCE_FWHM_GAUSSIAN = "gaussFwhm";
    public static final String EXTENDED_SOURCE_TYPE = "extSourceType";
    public static final String NORM_TYPE = "normType";
    public static final String NORM_BAND = "normBand";
    public static final String NORM_WAVELENGTH = "normWavelength";
    public static final String SOURCE_SPEC = "sourceSpec";
    public static final String ST_SPEC_TYPE = "stSpectrumType";
    public static final String NS_SPEC_TYPE = "nsSpectrumType";
    public static final String RECESSION = "recession";

    public static final String Z = "z";
    public static final String V = "v";

    // ITC web form input values.
    // These constants must be kept in sync with the web page form.
    // They are used to parse form data.
    public static final String POINT_SOURCE = "pointSource";
    public static final String EXTENDED_SOURCE = "extendedSource";
    public static final String UNIFORM = "uniform";
    public static final String GAUSSIAN = "gaussian";

    public static final String MAG = "mag";
    public static final String ABMAG = "ABmag";
    public static final String JY = "jy";
    public static final String WATTS = "watts_fd_wavelength";
    public static final String ERGS_WAVELENGTH = "ergs_fd_wavelength";
    public static final String ERGS_FREQUENCY = "ergs_fd_frequency";
    public static final String MAG_PSA = "mag_per_sq_arcsec";
    public static final String ABMAG_PSA = "ABmag_per_sq_arcsec";
    public static final String JY_PSA = "jy_per_sq_arcsec";
    public static final String WATTS_PSA = "watts_fd_wavelength_per_sq_arcsec";
    public static final String ERGS_WAVELENGTH_PSA =
            "ergs_fd_wavelength_per_sq_arcsec";
    public static final String ERGS_FREQUENCY_PSA =
            "ergs_fd_frequency_per_sq_arcsec";
    public static final String WATTS_FLUX = "watts_flux";
    public static final String ERGS_FLUX = "ergs_flux";
    public static final String[] UNITS =
            {MAG, ABMAG, JY, WATTS, ERGS_WAVELENGTH, ERGS_FREQUENCY, MAG_PSA,
                    ABMAG_PSA, JY_PSA, WATTS_PSA, ERGS_WAVELENGTH_PSA, ERGS_FREQUENCY_PSA,
                    WATTS_FLUX, ERGS_FLUX};
    /**
     * Constant defining units
     */
    private static final int ITC_UNITS_MAG = 0;
    private static final int ITC_UNITS_ABMAG = 1;
    private static final int ITC_UNITS_JY = 2;
    private static final int ITC_UNITS_WATTS = 3;
    private static final int ITC_UNITS_ERGS_WAVELENGTH = 4;
    private static final int ITC_UNITS_ERGS_FREQUENCY = 5;
    private static final int ITC_UNITS_MAG_PSA = 6;
    private static final int ITC_UNITS_ABMAG_PSA = 7;
    private static final int ITC_UNITS_JY_PSA = 8;
    private static final int ITC_UNITS_WATTS_PSA = 9;
    private static final int ITC_UNITS_ERGS_WAVELENGTH_PSA = 10;
    private static final int ITC_UNITS_ERGS_FREQUENCY_PSA = 11;
    private static final int ITC_UNITS_WATTS_FLUX = 12;
    private static final int ITC_UNITS_ERGS_FLUX = 13;

    public static final String FILTER = "filter";
    public static final String WAVELENGTH = "wavelength";

    public static final String LIBRARY_STAR = "libraryStar";
    public static final String LIBRARY_NON_STAR = "libraryNonStar";
    public static final String BLACK_BODY = "blackBody";
    public static final String BBTEMP = "BBTemp";
    public static final String BBODY = "modelBlackBody";
    public static final String ELINE = "modelEmLine";
    public static final String PLAW = "modelPowerLaw";
    public static final String LINE_WAVELENGTH = "lineWavelength";
    public static final String LINE_FLUX = "lineFlux";
    public static final String LINE_WIDTH = "lineWidth";
    public static final String LINE_FLUX_UNITS = "lineFluxUnits";
    public static final String LINE_CONTINUUM = "lineContinuum";
    public static final String LINE_CONTINUUM_UNITS = "lineContinuumUnits";
    public static final String PLAW_INDEX = "powerIndex";
    public static final String USER_DEFINED_SPECTRUM = "userDefinedSpectrum";
    public static final String USER_DEFINED_SPECTRUM_NAME = "specUserDef";
    public static final int USER_DEFINED_SPECTRUM_INDEX = 0;

    public static final String REDSHIFT = "redshift";
    public static final String VELOCITY = "velocity";

    public static final String SED_FILE_EXTENSION = ".nm";

    /**
     * Location of SED data files
     */
    public static final String STELLAR_LIB = ITCConstants.SED_LIB + "/stellar";
    public static final String NON_STELLAR_LIB =
            ITCConstants.SED_LIB + "/non_stellar";

    // Data members
    private String _sourceGeom;  // point or extended
    private String _extSourceType; // uniform, gaussian, ...
    private double _sourceNorm;  // 19.3 or 2e-17
    private String _units; // unit code
    private double _fwhm;
    private String _normType; // filter or wavelength
    private String _normBand; // U, V, B, ...
    private double _normWavelength; // e.g. 670nm
    private double _bBTemp;
    private double _eLineWavelength;
    private double _eLineWidth;
    private double _eLineFlux;
    private double _eLineContinuumFlux;
    private String _eLineFluxUnits; // units for eline flux
    private String _eLineContinuumFluxUnits;
    private double _pLawIndex;
    private String _sourceSpec;
    private String _specType;
    private String _userDefinedSedString;
    private boolean _isSEDUserDefined = false;

    // resource name of library spectrum
    private String _sedSpectrum;  // /lib/stellar/KOIII.nm

    private double _redshift;  // z

    /**
     * Constructs a SourceDefinitionParameters from a servlet request
     *
     * @param r Servlet request containing the form data.
     * @throws Exception if input data is not parsable.
     */
    public SourceDefinitionParameters(HttpServletRequest r) throws Exception {
        parseServletRequest(r);
    }

    /**
     * Constructs a SourceDefinitionParameters from a MultipartParser
     *
     * @param p MutipartParser that has all of the parameters and files Parsed
     * @throws Exception of cannot parse any of the parameters.
     */

    public SourceDefinitionParameters(ITCMultiPartParser p) throws Exception {
        parseMultipartParameters(p);
    }

    /**
     * Parse parameters from a servlet request.
     */
    public void parseServletRequest(HttpServletRequest r) throws Exception {
        // Parse the source definition section of the form.

        // Get source geometry type
        _sourceGeom = r.getParameter(SOURCE_GEOM);
        if (_sourceGeom == null) {
            ITCParameters.notFoundException(SOURCE_GEOM);
        }
        String sourceNorm, sourceUnits, fwhm;
        if (_sourceGeom.equals(POINT_SOURCE)) {

            String norm = r.getParameter(SOURCE_NORM_PT_SOURCE);
            if (norm == null) {
                ITCParameters.notFoundException(SOURCE_NORM_PT_SOURCE);
            }
            _sourceNorm = ITCParameters.parseDouble(norm, "Integrated brightness");

            // Get units
            _units = r.getParameter(SOURCE_UNITS_PT_SOURCE);
            if (_units == null) {
                ITCParameters.notFoundException(SOURCE_UNITS_PT_SOURCE);
            }
            if (getUnitCode(_units) < 0) {
                throw new Exception("Unrecognized units: " + _units);
            }
        } else if (_sourceGeom.equals(EXTENDED_SOURCE)) {
            _extSourceType = r.getParameter(EXTENDED_SOURCE_TYPE);
            if (_extSourceType == null) {
                ITCParameters.notFoundException(EXTENDED_SOURCE_TYPE);
            }
            if (_extSourceType.equals(GAUSSIAN)) {
                fwhm = r.getParameter(SOURCE_FWHM_GAUSSIAN);
                if (fwhm == null) {
                    ITCParameters.notFoundException(SOURCE_FWHM_GAUSSIAN);
                }
                _fwhm = ITCParameters.parseDouble(fwhm, "Full Width Half Max");
                if (_fwhm < .1)
                    throw new Exception("Please use a Gaussian FWHM greater than 0.1");
                sourceNorm = r.getParameter(SOURCE_NORM_GAUSSIAN);
                _sourceNorm = ITCParameters.parseDouble(sourceNorm,
                        "Integrated brightness");
                _units = r.getParameter(SOURCE_UNITS_GAUSSIAN);
                if (_units == null) {
                    ITCParameters.notFoundException(SOURCE_UNITS_GAUSSIAN);
                }
                if (getUnitCode(_units) < 0) {
                    throw new Exception("Unrecognized units: " + _units);
                }
            } else if (_extSourceType.equals(UNIFORM)) {
                sourceNorm = r.getParameter(SOURCE_NORM_USB);
                _sourceNorm = ITCParameters.parseDouble(sourceNorm,
                        "Integrated brightness");
                _units = r.getParameter(SOURCE_UNITS_USB);
                if (_units == null) {
                    ITCParameters.notFoundException(SOURCE_UNITS_USB);
                }
                if (getUnitCode(_units) < 0) {
                    throw new Exception("Unrecognized units: " + _units);
                }
            } else {
                throw new Exception("Unrecognized extended source geometry: " +
                        _extSourceType);
            }
        } else {
            throw new Exception("Unrecognized source geometry: " +
                    getSourceGeometry());
        }

        // the following code was copied into both the point source and
        // the uniform surface brightness. if they don't work uncomment

        // String norm = r.getParameter(sourceNorm);
        //if (norm == null) {
        //	 ITCParameters.notFoundException(sourceNorm);
        //}
        //_sourceNorm = ITCParameters.parseDouble(norm, "Integrated brightness");

        // Get units
        //_units = r.getParameter(sourceUnits);
        //if (_units == null) {
        //	 ITCParameters.notFoundException(sourceUnits);
        //}
        //if (getUnitCode(_units) < 0) {
        //	 throw new Exception("Unrecognized units: " + _units);
        //}

        // Get normalization info
        _normType = r.getParameter(NORM_TYPE);
        if (_normType == null) {
            ITCParameters.notFoundException(NORM_TYPE);
        }
        if (_normType.equals(FILTER)) {
            _normBand = r.getParameter(NORM_BAND);
            if (_normBand == null) {
                ITCParameters.notFoundException(NORM_BAND);
            }
        } else if (_normType.equals(WAVELENGTH)) {
            String wavelen = r.getParameter(NORM_WAVELENGTH);
            if (wavelen == null) {
                ITCParameters.notFoundException(NORM_WAVELENGTH);
            }
            _normWavelength =
                    ITCParameters.parseDouble(wavelen, "Normalization wavelength");
        } else {
            throw new Exception("Unrecognized normalization type: " +
                    getNormType());
        }

        // Get sed spectrum resource
        String sourceSpec = r.getParameter(SOURCE_SPEC);
        if (sourceSpec == null) {
            ITCParameters.notFoundException(SOURCE_SPEC);
        }
        _sourceSpec = sourceSpec;
        String specType;
        String spectrum;
        if (sourceSpec.equals(LIBRARY_STAR)) {
            specType = r.getParameter(ST_SPEC_TYPE);
            if (specType == null) {
                ITCParameters.notFoundException(ST_SPEC_TYPE);
            }
            _specType = specType;
            _sedSpectrum = STELLAR_LIB + "/" + specType.toLowerCase() +
                    SED_FILE_EXTENSION;
        } else if (sourceSpec.equals(LIBRARY_NON_STAR)) {
            specType = r.getParameter(NS_SPEC_TYPE);
            if (specType == null) {
                ITCParameters.notFoundException(NS_SPEC_TYPE);
            }
            _specType = specType;
            _sedSpectrum = NON_STELLAR_LIB + "/" + specType +
                    SED_FILE_EXTENSION;

        } else if (sourceSpec.equals(ELINE)) {
            String wavelen, width, flux, contFlux;

            wavelen = r.getParameter(LINE_WAVELENGTH);
            if (wavelen == null) {
                ITCParameters.notFoundException(LINE_WAVELENGTH);
            }
            _eLineWavelength = ITCParameters.parseDouble(wavelen,
                    "Line wavelength");

            width = r.getParameter(LINE_WIDTH);
            if (width == null) {
                ITCParameters.notFoundException(LINE_WIDTH);
            }
            _eLineWidth = ITCParameters.parseDouble(width, "Line Width");

            flux = r.getParameter(LINE_FLUX);
            if (flux == null) {
                ITCParameters.notFoundException(LINE_FLUX);
            }
            _eLineFlux = ITCParameters.parseDouble(flux, "Line Flux");

            contFlux = r.getParameter(LINE_CONTINUUM);
            if (contFlux == null) {
                ITCParameters.notFoundException(LINE_CONTINUUM);
            }
            _eLineContinuumFlux = ITCParameters.parseDouble(contFlux,
                    "Continuum Flux");

            _eLineFluxUnits = r.getParameter(LINE_FLUX_UNITS);
            if (_eLineFluxUnits == null) {
                ITCParameters.notFoundException(LINE_FLUX_UNITS);
            }

            _eLineContinuumFluxUnits = r.getParameter(LINE_CONTINUUM_UNITS);

            if (_eLineContinuumFluxUnits == null) {
                ITCParameters.notFoundException(LINE_CONTINUUM_UNITS);
            }


            _sourceSpec = ELINE;
            _sedSpectrum = ELINE;
        } else if (sourceSpec.equals(PLAW)) {
            String pLawIndex;
            pLawIndex = r.getParameter(PLAW_INDEX);
            if (pLawIndex == null) {
                ITCParameters.notFoundException(PLAW_INDEX);
            }
            _pLawIndex = ITCParameters.parseDouble(pLawIndex, "Power Law Index");
            _sourceSpec = PLAW;
            _sedSpectrum = PLAW;
        } else if (sourceSpec.equals(BBODY)) {
            String temp;
            temp = r.getParameter(BBTEMP);
            if (temp == null) {
                ITCParameters.notFoundException(BBTEMP);
            }
            _bBTemp = ITCParameters.parseDouble(temp, "Black Body Temp");
            _sourceSpec = BBODY;
            _sedSpectrum = BBODY;
        } else {
            throw new Exception("Unrecognized spectrum type: " +
                    sourceSpec);
        }

        // get redshift
        String recession = r.getParameter(RECESSION);
        if (recession == null) {
            ITCParameters.notFoundException(RECESSION);
        }
        String shift;
        if (recession.equals(REDSHIFT)) {
            shift = r.getParameter(Z);
            if (shift == null) {
                ITCParameters.notFoundException(Z);
            }
            _redshift = ITCParameters.parseDouble(shift, "Redshift");
        } else if (recession.equals(VELOCITY)) {
            shift = r.getParameter(V);
            if (shift == null) {
                ITCParameters.notFoundException(V);
            }
            _redshift = ITCParameters.parseDouble(shift, "Redshift velocity")
                    / ITCConstants.C;
        } else {
            throw new Exception("Unrecognized redshift method: " +
                    recession);
        }
    }

    public void parseMultipartParameters(ITCMultiPartParser p) throws Exception {
        // Parse source definition section of the form.
        try {
            _sourceGeom = p.getParameter(SOURCE_GEOM);
            if (_sourceGeom.equals(POINT_SOURCE)) {
                _sourceNorm = ITCParameters.parseDouble(p.getParameter(SOURCE_NORM_PT_SOURCE), "Integrated Brightness");
                _units = p.getParameter(SOURCE_UNITS_PT_SOURCE);
                if (getUnitCode(_units) < 0) {
                    throw new Exception("Unrecognized units: " + _units);
                }
            } else if (_sourceGeom.equals(EXTENDED_SOURCE)) {
                _extSourceType = p.getParameter(EXTENDED_SOURCE_TYPE);
                if (_extSourceType.equals(GAUSSIAN)) {
                    _fwhm = ITCParameters.parseDouble(p.getParameter(SOURCE_FWHM_GAUSSIAN), "Full Width Half Max");
                    if (_fwhm < 0.1) throw new Exception("Please use a Gaussian FWHM greater than 0.1");
                    _sourceNorm = ITCParameters.parseDouble(p.getParameter(SOURCE_NORM_GAUSSIAN), "Integrated Brightness");
                    _units = p.getParameter(SOURCE_UNITS_GAUSSIAN);
                    if (getUnitCode(_units) < 0) {
                        throw new Exception("Unrecognized units: " + _units);
                    }
                } else if (_extSourceType.equals(UNIFORM)) {
                    _sourceNorm = ITCParameters.parseDouble(p.getParameter(SOURCE_NORM_USB), "Integrated Brightness");
                    _units = p.getParameter(SOURCE_UNITS_USB);
                    if (getUnitCode(_units) < 0) {
                        throw new Exception("Unrecognized units: " + _units);
                    }
                } else {
                    throw new Exception("Unrecognized extended source geometry: " + _extSourceType);
                }
            } else {
                throw new Exception("Unrecognized source geometry: " + getSourceGeometry());
            }

            // Get Normalization info
            _normType = p.getParameter(NORM_TYPE);
            if (_normType.equals(FILTER)) {
                _normBand = p.getParameter(NORM_BAND);
            } else if (_normType.equals(WAVELENGTH)) {
                _normWavelength = ITCParameters.parseDouble(p.getParameter(NORM_WAVELENGTH), "Normalization Wavelength");
            } else {
                throw new Exception("Unrecognized normalization type: " + getNormType());
            }

            // Get Spectrum Resource
            _sourceSpec = p.getParameter(SOURCE_SPEC);
            if (_sourceSpec.equals(LIBRARY_STAR)) {
                _specType = p.getParameter(ST_SPEC_TYPE);
                _sedSpectrum = STELLAR_LIB + "/" + _specType.toLowerCase() + SED_FILE_EXTENSION;
            } else if (_sourceSpec.equals(LIBRARY_NON_STAR)) {
                _specType = p.getParameter(NS_SPEC_TYPE);
                _sedSpectrum = NON_STELLAR_LIB + "/" + _specType + SED_FILE_EXTENSION;
            } else if (_sourceSpec.equals(ELINE)) {
                _eLineWavelength = ITCParameters.parseDouble(p.getParameter(LINE_WAVELENGTH), "Line Wavelength");
                _eLineWidth = ITCParameters.parseDouble(p.getParameter(LINE_WIDTH), "Line Width");
                _eLineFlux = ITCParameters.parseDouble(p.getParameter(LINE_FLUX), "Line Flux");
                _eLineContinuumFlux = ITCParameters.parseDouble(p.getParameter(LINE_CONTINUUM), "Line Continuum");
                _eLineFluxUnits = p.getParameter(LINE_FLUX_UNITS);
                _eLineContinuumFluxUnits = p.getParameter(LINE_CONTINUUM_UNITS);
                _sourceSpec = ELINE;
                _sedSpectrum = ELINE;
                //if the desired linewidth is too small throw an exception
                //if (_eLineWidth < (3E5 / (_eLineWavelength*1000))) {
                //    throw new Exception("Please use a model line width > 1 nm to avoid undersampling of the line profile when convolved with the transmission response");
                //}
            } else if (_sourceSpec.equals(BBODY)) {
                _bBTemp = ITCParameters.parseDouble(p.getParameter(BBTEMP), "Black Body Temp");
                _sourceSpec = BBODY;
                _sedSpectrum = BBODY;
            } else if (_sourceSpec.equals(PLAW)) {
                _pLawIndex = ITCParameters.parseDouble(p.getParameter(PLAW_INDEX), "Power Law Index");
                _sourceSpec = PLAW;
                _sedSpectrum = PLAW;
            } else if (_sourceSpec.equals(USER_DEFINED_SPECTRUM)) {
                _sourceSpec = USER_DEFINED_SPECTRUM;
                _sedSpectrum = p.getRemoteFileName(USER_DEFINED_SPECTRUM_NAME);
                _userDefinedSedString = p.getTextFile(USER_DEFINED_SPECTRUM_NAME);
                _isSEDUserDefined = true;
            } else {
                throw new Exception("Unrecognized spectrum type: " + _sourceSpec);
            }

            //Get Redshift
            String recession = p.getParameter(RECESSION);
            if (recession.equals(REDSHIFT)) {
                _redshift = ITCParameters.parseDouble(p.getParameter(Z), "Redshift");
            } else if (recession.equals(VELOCITY)) {
                _redshift = ITCParameters.parseDouble(p.getParameter(V), "Redshift Velocity") / ITCConstants.C;
            } else {
                throw new Exception("Unrecognized redshift method: " + recession);
            }

        } catch (NoSuchParameterException e) {
            throw new Exception("The parameter " + e.parameterName + " could not be found in the Telescope" +
                    " Parameters Section of the form.  Either add this value or Contact the Helpdesk.");
        }
    }

    /**
     * Constructs a SourceDefinitionParameters from a servlet request
     *
     * @throws Exception if input data is not parsable.
     */
    public SourceDefinitionParameters(String sourceGeometry,
                                      String extSourceType,
                                      double sourceNorm,
                                      String units,
                                      double fwhm,
                                      String normType,
                                      String normBand,
                                      double normWavelength,
                                      double redshift,
                                      String spectrumResource,
                                      double bBTemp,
                                      double eLineWavelength,
                                      double eLineWidth,
                                      double eLineFlux,
                                      double eLineContinuumFlux,
                                      String eLineFluxUnits,
                                      String eLineContinuumFluxUnits,
                                      double pLawIndex,
                                      String sourceSpec) {
        _sourceGeom = sourceGeometry;
        _extSourceType = extSourceType;
        _sourceNorm = sourceNorm;
        _units = units;
        _fwhm = fwhm;
        _normType = normType;
        _normBand = normBand;
        _normWavelength = normWavelength;
        _redshift = redshift;
        _sedSpectrum = spectrumResource;
        _bBTemp = bBTemp;
        _eLineWavelength = eLineWavelength;
        _eLineWidth = eLineWidth;
        _eLineFlux = eLineFlux;
        _eLineContinuumFlux = eLineContinuumFlux;
        _eLineFluxUnits = eLineFluxUnits;
        _eLineContinuumFluxUnits = eLineContinuumFluxUnits;
        _pLawIndex = pLawIndex;
        _sourceSpec = sourceSpec;
    }

    public String getSourceGeometry() {
        return _sourceGeom;
    }

    public String getSourceGeometryStr() {
        return "pointSource".equals(_sourceGeom) ? "point source" : "extended source";
    }

    public String getExtendedSourceType() {
        return _extSourceType;
    }

    public double getSourceNormalization() {
        return _sourceNorm;
    }

    public String getUnits() {
        return _units;
    }

    public double getFWHM() {
        return _fwhm;
    }

    public String getNormType() {
        return _normType;
    }

    public String getNormBand() {
        return _normBand;
    }

    public double getNormWavelength() {
        return _normWavelength;
    }

    public double getRedshift() {
        return _redshift;
    }

    public String getSourceSpec() {
        return _sourceSpec;
    }

    public String getSpecType() {
        return _specType;
    }

    public String getSpectrumResource() {
        return _sedSpectrum;
    }

    public double getBBTemp() {
        return _bBTemp;
    }

    public double getELineWavelength() {
        return _eLineWavelength;
    }

    public double getELineWidth() {
        return _eLineWidth;
    }

    public double getELineFlux() {
        return _eLineFlux;
    }

    public double getELineContinuumFlux() {
        return _eLineContinuumFlux;
    }

    public String getELineFluxUnits() {
        return _eLineFluxUnits;
    }

    public String getELineContinuumFluxUnits() {
        return _eLineContinuumFluxUnits;
    }

    public double getPowerLawIndex() {
        return _pLawIndex;
    }

    public boolean isSedUserDefined() {
        return _isSEDUserDefined;
    }

    public String getUserDefinedSpectrum() {
        return _userDefinedSedString;
    }

    // function to be used to Supply human readable units.  Will be printed out in results
    public String getPrettyUnits() {
        if (getUnits().equals(SourceDefinitionParameters.JY))
            return "Jy";
        else
            return getUnits();
    }

    /**
     * Return a human-readable string for debugging
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Source Geometry:\t" + getSourceGeometry() + "\n");
        sb.append("Extended source type:\t" + getExtendedSourceType() + "\n");
        sb.append("Source Normalization:\t" + getSourceNormalization() + "\n");
        sb.append("Units:\t\t\t" + getUnits() + "\n");
        sb.append("Gaussian FWHM:\t" + getFWHM() + "\n");
        sb.append("Normalization Type:\t" + getNormType() + "\n");
        sb.append("Normalization WaveBand:\t" + getNormBand() + "\n");
        sb.append("Normalization Wavelen:\t" + getNormWavelength() + "\n");
        sb.append("Redshift:\t\t" + getRedshift() + "\n");
        sb.append("Spectrum Resource:\t" + getSpectrumResource() + "\n");
        sb.append("Black Body Temp:\t" + getBBTemp() + "\n");
        sb.append("Emission Line Central Wavelen:\t" + getELineWavelength() +
                "\n");
        sb.append("Emission Line Width:\t" + getELineWidth() + "\n");
        sb.append("Emission Line Flux:\t" + getELineFlux() + "\n");
        sb.append("Emission Line Continuum Flux:\t" + getELineContinuumFlux() +
                "\n");
        sb.append("Emission Line Units:" + getELineFluxUnits() + "\n");
        sb.append("Emission Line Cont Units:" + getELineContinuumFluxUnits() +
                "\n");
        sb.append("Power Law Index:" + getPowerLawIndex() + "\n");
        sb.append("\n");
        return sb.toString();
    }

    public String printParameterSummary() {
        StringBuffer sb = new StringBuffer();

        // This object is used to format numerical strings.
        FormatStringWriter device = new FormatStringWriter();
        device.setPrecision(4);  // four decimal places
        device.clear();

        sb.append("Source spatial profile, brightness, and spectral distribution: \n");
        sb.append("  The z = ");
        sb.append(getRedshift());
        sb.append(" ");
        sb.append(getSourceGeometryStr());
        sb.append(" is a");
        if (getSourceSpec().equals(ELINE)) {
            sb.append("n emission line, at a wavelength of " + device.toString(getELineWavelength()));
            device.setPrecision(2);
            device.clear();
            sb.append(" microns, and with a width of " + device.toString(getELineWidth()) + " km/s.\n  It's total flux is " +
                    device.toString(getELineFlux()) + " " + getELineFluxUnits() + " on a flat continuum of flux density " +
                    device.toString(getELineContinuumFlux()) + " " + getELineContinuumFluxUnits() + ".");
        } else if (getSourceSpec().equals(BBODY)) {
            sb.append(" " + getBBTemp() + "K Blackbody, at " + getSourceNormalization() +
                    " " + getPrettyUnits() + " in the " + getNormBand() + " band.");
        } else if (getSourceSpec().equals(LIBRARY_STAR)) {
            sb.append(" " + getSourceNormalization() + " " + getPrettyUnits() + " " + getSpecType() +
                    " star in the " + getNormBand() + " band.");
        } else if (getSourceSpec().equals(LIBRARY_NON_STAR)) {
            sb.append(" " + getSourceNormalization() + " " + getPrettyUnits() + " " + getSpecType() +
                    " in the " + getNormBand() + " band.");
        } else if (isSedUserDefined()) {
            sb.append(" a user defined spectrum with the name: " + getSpectrumResource());
        } else if (getSourceSpec().equals(PLAW)) {
            sb.append(" Power Law Spectrum, with an index of " + getPowerLawIndex()
                      + " and " + getSourceNormalization() + " mag in the " + getNormBand() + " band.");
        }
        sb.append("\n");
        return sb.toString();

    }

    /**
     * Returns the unit code corresponding to the specified units string.
     * If the string is not recognized, -1 is returned with no exception.
     */
    public int getUnitCode(String units) {
        if (units == null) return -1;
        for (int i = 0; i < UNITS.length; ++i) {
            if (units.equals(UNITS[i])) return i;
        }
        return -1;
    }
}
