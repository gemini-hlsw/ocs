package edu.gemini.itc.base;

import edu.gemini.itc.operation.Slit;

/**
 * Representation of a disperser element.
 * This defines the wavelength range for which the disperser lets light pass and also the dispersion and
 * resolution which is used by the slit visitor to calculate the signal and background strength.
 * For most dispersers the resolution is defined in nm for a 0.5-arcsec slit and the actual resolution
 * for the given slit width is then extrapolated from this value; note that the methods provided here also allow
 * to account for the case where the image quality is smaller than the slit; by default it uses the image quality
 * instead of the slit width to set the size of the resolution element in that case.
 * Some dispersers may need to implement a different behavior for how the resolution and dispersion are calculated
 * by overriding these methods, see e.g. the NIRI grisms (which don't use interpolation from the half arcsec slit
 * width) or GNIRS (which has to take a camera scale factor and the cross dispersion order into account).
 */
public interface Disperser {

    /** Wavelength in [nm] at which this element starts letting light through. */
    double getStart();

    /** Wavelength in [nm] at which this element stop letting light through. */
    double getEnd();

    /** Dispersion of this disperser in [nm/pixel]. */
    double dispersion(double wv);


    /** Spectral resolution in nm for a 0.5-arcsec slit.
     *  This value can be used to extrapolate the resolution for any given slit which is the default
     *  behavior for most instruments and dispersers. */
    double resolutionHalfArcsecSlit();

    /** Calculates the size of a spectral resolution element in [nm] for the given slit.
     *  By default this is done by extrapolating from the 0.5-arcsec resolution. */
    default double resolution(final Slit slit) {
        return resolutionHalfArcsecSlit() * slit.width() / 0.5;
    }

    /** Calculates the size of a spectral resolution element in [nm] for the source taking the image quality
     *  into account. By default if the image quality is smaller than the slit width the image quality is used
     *  as the slit width. */
    default double resolution(final Slit slit, final double imgQuality) {
        //if image size is less than the slit width it will determine the resolution
        final double width = imgQuality < slit.width() ? imgQuality : slit.width();
        return resolutionHalfArcsecSlit() * width / 0.5;
    }

}
