// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
// $Id: SEDFactory.java,v 1.6 2003/11/21 14:31:02 shane Exp $
//
package edu.gemini.itc.shared;

import edu.gemini.itc.parameters.SourceDefinitionParameters;

import java.io.CharArrayReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class encapsulates the process of creating a Spectral Energy
 * Distribution (SED).  (e.g. from a data file)
 * As written it demands a certain format to the data file.
 * Each row must contain two doubles separated by whitespace or comma,
 * the first is a wavelength in nanometers, the second is the energy in
 * arbitrary units.  Since a SED will be normalized before it is used,
 * the scale is arbitrary.
 * <p/>
 * Programmer's note: There is no need for a factory.  A factory is for
 * creating something when the client does not know which concrete type
 * to create.  Since we don't have different types of SEDs at this point,
 * we could directly create a SED.
 * Maybe this is for future support of data files in different units.
 */
public class SEDFactory {
    /**
     * Returns a SED constructed with specified values.
     */
    public SampledSpectrum getSED(double[] flux, double wavelengthStart,
                                  double wavelengthInterval) {
        return new DefaultSampledSpectrum(flux, wavelengthStart,
                wavelengthInterval);
    }

    /**
     * Returns a SED read from specified data file.
     * The format of the file is as follows:
     * A line containing a double specifying the wavelength interval
     * followed by lines containing two doubles
     * separated by whitespace or commas.  The first is wavelength
     * in nm.  The second is flux in arbitrary units.  e.g.
     * <pre>
     * # Wavelength sampling size in nm
     * 0.5
     * # The data, wavelengths are in nm, flux units unknown
     *  115.0  0.181751
     *  115.5  0.203323
     *  116.0  0.142062
     *  ...
     * </pre>
     */
    public static VisitableSampledSpectrum getSED(String fileName)
            throws Exception {
        return getSED(fileName, -1.0);  // Get sampling interval from file
    }

    /**
     * Returns a SED read from specified data file.
     * The format of the file is as follows:
     * A line containing a double specifying the wavelength interval
     * followed by lines containing two doubles
     * separated by whitespace or commas.  The first is wavelength
     * in nm.  The second is flux in arbitrary units.  e.g.
     * <pre>
     * # Wavelength sampling size in nm
     * 0.5
     * # The data, wavelengths are in nm, flux units unknown
     *  115.0  0.181751
     *  115.5  0.203323
     *  116.0  0.142062
     *  ...
     * </pre>
     */
    public static VisitableSampledSpectrum getSED(String fileName,
                                                  double wavelengthInterval)
            throws Exception {
        TextFileReader dfr = null;
        try {
            dfr = new TextFileReader(fileName);
        } catch (Exception e) {
            System.out.println("SED file not found: " + fileName);
            throw e;
        }

        // These lists hold doubles from the data file.
        List wavelengths = new ArrayList();
        List fluxDensities = new ArrayList();

        double wavelength = 0;
        double flux = 0;

        // if interval is not specified, assume it is in the file
        if (wavelengthInterval <= 0) {
            try {
                if (dfr.countTokens() != 1) {
                    throw new Exception("First line of spectral file " + fileName
                            + " must be sampling interval.");
                }
                wavelengthInterval = dfr.readDouble();
            } catch (ParseException e) {
                throw e;
            } catch (IOException e) {
                throw new Exception("First line of spectral file " + fileName
                        + " must be sampling interval.");
            }
        }

        try {
            while (true) {
                if (dfr.countTokens() != 2) {
                    throw new Exception("Line " + dfr.getLineNumber()
                            + " of spectral file " + fileName
                            + " does not contain two values.");
                }
                wavelength = dfr.readDouble();
                wavelengths.add(new Double(wavelength));
                flux = dfr.readDouble();
                fluxDensities.add(new Double(flux));
            }
        } catch (ParseException e) {
            throw e;
        } catch (IOException e) {
            //System.out.println(e.toString());
        }

        if (fluxDensities.size() < 1) {
            throw new Exception("SED data for " + fileName + " is empty.");
        }

        if (fluxDensities.size() != wavelengths.size()) {
            String s = "SED data for " + fileName + " not consistent.  ";
            s += fluxDensities.size() + " x values, " + wavelengths.size() +
                    " y values.";
            System.out.println("flux[0]: " + fluxDensities.get(0));
            System.out.println("wave[0]: " + wavelengths.get(0));
            throw new Exception(s);
        }

        DefaultArraySpectrum as = new DefaultArraySpectrum(wavelengths,
                fluxDensities);
        wavelengths.clear();
        wavelengths = null;
        fluxDensities.clear();
        fluxDensities = null;

        return new DefaultSampledSpectrum(as, wavelengthInterval);
    }


    /**
     * Returns a SED read from a user submitted Data file.
     * The format of the file is as follows:
     * A line containing a double specifying the wavelength interval
     * followed by lines containing two doubles
     * separated by whitespace or commas.  The first is wavelength
     * in nm.  The second is flux in arbitrary units.  e.g.
     * <pre>
     * # Wavelength sampling size in nm
     * 0.5
     * # The data, wavelengths are in nm, flux units unknown
     *  115.0  0.181751
     *  115.5  0.203323
     *  116.0  0.142062
     *  ...
     * </pre>
     */
    public static VisitableSampledSpectrum getSED(String fileName, String userSED,
                                                  double wavelengthInterval)
            throws Exception {
        TextFileReader dfr = null;
        try {
            dfr = new TextFileReader(new CharArrayReader(userSED.toCharArray()));
        } catch (Exception e) {
            System.out.println("SED file not found: " + fileName);
            throw e;
        }

        // These lists hold doubles from the data file.
        List wavelengths = new ArrayList();
        List fluxDensities = new ArrayList();

        double wavelength = 0;
        double flux = 0;

        // if interval is not specified, assume it is in the file
        if (wavelengthInterval <= 0) {
            try {
                if (dfr.countTokens() != 1) {
                    throw new Exception("First line of spectral file " + fileName
                            + " must be sampling interval.");
                }
                wavelengthInterval = dfr.readDouble();
            } catch (ParseException e) {
                throw e;
            } catch (IOException e) {
                throw new Exception("First line of spectral file " + fileName
                        + " must be sampling interval.");
            }
        }

        try {
            while (true) {
                if (dfr.countTokens() != 2) {
                    throw new Exception("Line " + dfr.getLineNumber()
                            + " of spectral file " + fileName
                            + " does not contain two values.");
                }
                wavelength = dfr.readDouble();
                wavelengths.add(new Double(wavelength));
                flux = dfr.readDouble();
                fluxDensities.add(new Double(flux));
            }
        } catch (ParseException e) {
            throw e;
        } catch (IOException e) {
            //System.out.println(e.toString());
        }

        if (fluxDensities.size() < 1) {
            throw new Exception("SED data for " + fileName + " is empty.");
        }

        if (fluxDensities.size() != wavelengths.size()) {
            String s = "SED data for " + fileName + " not consistent.  ";
            s += fluxDensities.size() + " x values, " + wavelengths.size() +
                    " y values.";
            System.out.println("flux[0]: " + fluxDensities.get(0));
            System.out.println("wave[0]: " + wavelengths.get(0));
            throw new Exception(s);
        }

        DefaultArraySpectrum as = new DefaultArraySpectrum(wavelengths,
                fluxDensities);
        wavelengths.clear();
        wavelengths = null;
        fluxDensities.clear();
        fluxDensities = null;

        return new DefaultSampledSpectrum(as, wavelengthInterval);
    }


    public static VisitableSampledSpectrum getSED(SourceDefinitionParameters sdp,
                                                  Instrument instrument) throws Exception {

        if (sdp.getSpectrumResource().equals(sdp.BBODY)) {
            return new
                    BlackBodySpectrum(sdp.getBBTemp(),
                    instrument.getObservingStart(),
                    instrument.getObservingEnd(),
                    instrument.getSampling(),
                    sdp.getSourceNormalization(),
                    sdp.getUnits(),
                    sdp.getNormBand(),
                    sdp.getRedshift());

        } else if (sdp.getSpectrumResource().equals(sdp.ELINE)) {
            return new
                    EmissionLineSpectrum(sdp.getELineWavelength(),
                    sdp.getELineWidth(),
                    sdp.getELineFlux(),
                    sdp.getELineContinuumFlux(),
                    sdp.getELineFluxUnits(),
                    sdp.getELineContinuumFluxUnits(),
                    sdp.getRedshift(),
                    instrument.getSampling());


        } else if (sdp.getSpectrumResource().equals(sdp.PLAW)) {
            return new
                    PowerLawSpectrum(sdp.getPowerLawIndex(),
                    instrument.getObservingStart(),
                    instrument.getObservingEnd(),
                    instrument.getSampling(),
                    sdp.getRedshift());
        } else {
            VisitableSampledSpectrum temp;
            if (sdp.isSedUserDefined()) {
                temp = getSED(sdp.getSpectrumResource(),
                        sdp.getUserDefinedSpectrum(),
                        instrument.getSampling());
            } else {
                temp = getSED(sdp.getSpectrumResource(),
                        instrument.getSampling());
            }
            temp.applyWavelengthCorrection();

            return temp;
            // return getSED(sdp.getSpectrumResource(),
            //	instrument.getSampling());
        }
    }


    //Added to allow creation of an SED spanning more than one filter for NICI
    public static VisitableSampledSpectrum getSED(SourceDefinitionParameters sdp,
                                                  double sampling, double observingStart, double observingEnd) throws Exception {

        if (sdp.getSpectrumResource().equals(sdp.BBODY)) {
            return new
                    BlackBodySpectrum(sdp.getBBTemp(),
                    observingStart,
                    observingEnd,
                    sampling,
                    sdp.getSourceNormalization(),
                    sdp.getUnits(),
                    sdp.getNormBand(),
                    sdp.getRedshift());

        } else if (sdp.getSpectrumResource().equals(sdp.ELINE)) {
            return new
                    EmissionLineSpectrum(sdp.getELineWavelength(),
                    sdp.getELineWidth(),
                    sdp.getELineFlux(),
                    sdp.getELineContinuumFlux(),
                    sdp.getELineFluxUnits(),
                    sdp.getELineContinuumFluxUnits(),
                    sdp.getRedshift(),
                    sampling);


        } else if (sdp.getSpectrumResource().equals(sdp.PLAW)) {
            return new
                    PowerLawSpectrum(sdp.getPowerLawIndex(),
                    observingStart,
                    observingEnd,
                    sampling,
                    sdp.getRedshift());
        } else {
            VisitableSampledSpectrum temp;
            if (sdp.isSedUserDefined()) {
                temp = getSED(sdp.getSpectrumResource(),
                        sdp.getUserDefinedSpectrum(),
                        sampling);
            } else {
                temp = getSED(sdp.getSpectrumResource(),
                        sampling);
            }
            temp.applyWavelengthCorrection();

            return temp;
            // return getSED(sdp.getSpectrumResource(),
            //	instrument.getSampling());
        }
    }

}
