/**
 * $Id: Spoc.java 6526 2005-08-03 21:27:13Z brighton $
 */

package edu.gemini.mask;

import java.io.IOException;
import java.io.File;

import nom.tam.fits.FitsException;
import edu.gemini.shared.util.FileUtil;

/**
 *
 */
public class Spoc {
    public static final String SPOC_VERSION = "1.0";
    private static final double DETECTOR_SIZE_ARCSEC = 452.0486;

    private MaskParams _maskParams;

    public Spoc(MaskParams maskParams)
            throws IllegalArgumentException, IOException, FitsException {

        _maskParams = maskParams;
        calcSpectrum();

        // XXX gmmps sets this from extra widget if no slitsize_x col in table
        double slitWidth = 0.;

        //  Convert the SlitWidth to pixels.
        double slitWidthPix = slitWidth / maskParams.getPixelScale();

        //  Extract the exact GMOS field of view, and drop objects that are out of view.
        new GmosFov(maskParams);

        // Run the slit algorithm
        MaskMaker maskMaker = new MaskMaker(maskParams, slitWidthPix);

        // Save the FITS ODF files
        _saveOdf(maskMaker.getOdfTables());
    }

    // Save the FITS ODF files
    private void _saveOdf(ObjectTable[] odfTables) throws IOException, FitsException {
        for (int i = 0; i < odfTables.length; i++) {
            _saveFitsOdf(odfTables[i], i + 1);
        }
    }

    // Save the given table to a FITS file and add the necessary keywords to the header.
    private void _saveFitsOdf(ObjectTable odfTable, int maskNum) throws IOException,
            FitsException {
//        // write out the table as an ascii catalog file for reference
//        String filename = getOdfFileName(_maskParams, maskNum, ".cat");
//        odfTable.saveAs(filename);

        // Save as a FITS table
        String filename = getOdfFileName(_maskParams, maskNum, ".fits");
        odfTable.saveAsFitsTable(filename, _maskParams);
    }


    /**
     * Return the file name for the nth ODF FITS table file (generated from the original
     * input FITS table).
     */
    public static String getOdfFileName(MaskParams maskParams, int n, String suffix) {
        if (!suffix.startsWith(".")) {
            suffix = "." + suffix;
        }
        ObjectTable table = maskParams.getTable();
        String dir = new File(table.getFilename()).getParent();
        String name = FileUtil.removeSuffix(table.getName());
        return dir + File.separator + name + "ODF" + n + suffix;
    }

    // Calculate the spectrum values in _maskParams: anaMorphic, dpix, lmax, lmin, and
    // specLen.
    private void calcSpectrum() {
        double wavelength = _maskParams.getWavelength();
        if (_maskParams.getInstrument().startsWith("GMOS")) {
            if (wavelength < 350 || wavelength > 1050) {
                throw new IllegalArgumentException("Central wavelength " + wavelength +
                        " outside useful range for GMOS");
            }
        }

        FilterInfo filterInfo = FilterInfo.getFilterInfo(_maskParams.getFilter());
        double fl1 = filterInfo.getLambda1();
        double fl2 = filterInfo.getLambda2();

        DisperserInfo disperserInfo = DisperserInfo.getDisperserInfo(_maskParams.getDisperser());
        int gnm = disperserInfo.getGrating();
        int gl1 = disperserInfo.getLambda1();
        int gl2 = disperserInfo.getLambda2();

        double pixelScale = _maskParams.getPixelScale();
        double detSize = DETECTOR_SIZE_ARCSEC / pixelScale;
        double halfDetSize = detSize / 2.;

        //  Calculate length of spectrum, based on grating, filter & wavelength
        //  Determine gRequest, gTilt, Anamorphic factor, linear disperson.
        double gRequest = wavelength * gnm / 1000000.;
        double gTilt = GmosTiltInfo.calcGTilt(gRequest);

        double anaMorphic = Math.sin((gTilt + 50.0) * Math.PI / 180.0) /
                Math.sin(gTilt * Math.PI / 180.0);

        double dpix = ((anaMorphic * pixelScale * wavelength * 80.0 *
                Math.sin(gTilt * Math.PI / 180.0)) / (180.0 / Math.PI * 3600.0 *
                gRequest));

        // Now comes the really dumb part, I have to determine the specturm
        // length, and to do that I have to choose 2 random positions on the
        // ccd, I say random because they should be close to the center, and
        // it doesn't make sense to go thru all objects.....
        // So I choose 1/2 det size + 16%, and 1/2 det size - 16%
        double smallBit = halfDetSize * 0.16;

        // Calc. lambdaDec at slit positions halfDetSize-smallbit and halfDetSize+smallBit
        double xlamCentrala = halfDetSize - (smallBit) / anaMorphic;
        double xlamCentralb = halfDetSize + (smallBit) / anaMorphic;
        double dec1a = wavelength - (detSize - xlamCentrala) * dpix;
        double dec1b = wavelength - (detSize - xlamCentralb) * dpix;
        double dec2a = wavelength + xlamCentrala * dpix;
        double dec2b = wavelength + xlamCentralb * dpix;

        //  Calculate Lamda1&2, which is the max/min of filter, grating, dec, and other limits.
        double lmax = fl1;
        double l1 = fl1;
        if (gl1 > l1) {
            l1 = gl1;
            lmax = gl1;
        }
        if (320 > l1) {
            l1 = 320;
            lmax = 320;
        }
        if (dec1a > l1) {
            l1 = dec1a;
        }
        if (dec1b > l1) {
            l1 = dec1b;
        }

        double lmin = fl2;
        double l2 = fl2;
        if (gl2 < l2) {
            l2 = gl2;
            lmin = gl2;
        }
        if (1050 < l2) {
            l2 = 1050;
            lmin = 1050;
        }
        if (dec2a < l2) {
            l2 = dec2a;
        }
        if (dec2b < l2) {
            l2 = dec2b;
        }

        double specLen = (l2 - l1) / dpix;

        System.out.println("XXX calcSpectrum:"
                + " _anaMorphic = " + anaMorphic
                + ", _dpix = " + dpix
                + ", _lmax = " + lmax
                + ", _lmin = " + lmin
                + ", _wavelength = " + wavelength
                + ", _specLen = " + specLen
                + ", gTilt = " + gTilt
                + ", gRequest = " + gRequest
                + ", gnm = " + gnm
                + ", gl1 = " + gl1
                + ", gl2 = " + gl2
                + ", detSize = " + detSize);

        //  Determine if the spectrum length calculated is valid.
        if (specLen < 1) {
            throw new IllegalArgumentException("Calculated spectrum length invalid: " +
                    specLen + " Select new filter/grating");
        }

        _maskParams.setAnaMorphic(anaMorphic);
        _maskParams.setDpix(dpix);
        _maskParams.setLmax(lmax);
        _maskParams.setLmin(lmin);
        _maskParams.setSpecLen(specLen);
    }

    /**
     * Test main.
     *
     * @param args one arg: the object table file
     */
    public static void main(final String args[]) {
        try {
            File file = new File(args[0]);
            ObjectTable table = ObjectTable.makeObjectTable(file.getAbsolutePath());

            MaskParams maskParams = table.getMaskParams();
            maskParams.setNumMasks(3);

            BandDef bandDef = maskParams.getBandDef();

//            bandDef.setShuffleMode(BandDef.MICRO_SHUFFLE);
//            bandDef.setSlitlen(5);
//            bandDef.setBinning(1);
//            bandDef.setShufflePx(69);
//            bandDef.setShuffleAmt(5);
//            bandDef.setNodAmt(0);

            bandDef.setShuffleMode(BandDef.BAND_SHUFFLE);
            bandDef.setBandSize(1535);
            bandDef.setBinning(1);
            bandDef.setBandShufflePix(1535);
            bandDef.setBandShuffleAmount(111.59450);
            bandDef.setBandsYOffset(0);
            bandDef.setBands(new BandDef.Band[]{new BandDef.Band(1, "band1", 1536, 1535)});

            new Spoc(maskParams);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }
}
