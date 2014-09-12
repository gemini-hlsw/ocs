package edu.gemini.spModel.gemini.bhros.ech;

/**
 * Constants describing the physical attributes of the bHROS instrument.
 */
public interface HROSHardwareConstants {
	
	/* CCD pixel size, in microns */
	 double PIXELSIZE = 13.5;

	/* Pixel size in mm. */
	 double PIXELMM = PIXELSIZE / 1000.0D;

	/* Minimum Y pixel number */
	 int YMINPIX = 0;

	/* Maximum Y pixel number */
	 int YMAXPIX = 2047;

	/* Total number of Y pixels on chip */
	 int CHIP_YPIX = (YMAXPIX - YMINPIX + 1);

	/* Minimum X pixel number */
	 int XMINPIX = 0;

	/* Maximum X pixel number */
	 int XMAXPIX = 4607;

	/* Total number of X pixels on chip */
	 int CHIP_XPIX = (XMAXPIX - XMINPIX + 1);

	/* Inter-chip gap, in equivalent pixels */
	 int GAPNUM = 44;

	/* Inter-chip gap in Y direction, mm. */
	 double CHIP_GAP = GAPNUM * PIXELMM;

	/* CCD chip X size in mm. */
	 double CHIP_XSIZE = CHIP_XPIX * PIXELMM;

	/* Blue CCD chip Y size in mm. */
	 double BLUE_CHIP_YSIZE = CHIP_YPIX * PIXELMM;

	/* Some rows (and cols?) on the blue chip are non-functional. These are in pixel coords.
	 * Note that these are rows in the echellogram but are columns in the FITs files, so the
	 * SSAs and engineers will probably use the opposite terminology. 0,0 is the lower-right
	 * corner.
	 */
	int[] BLUE_CHIP_BROKEN_ROWS = new int[] { 876, 877, 935, 936, 937, 1668, 1669, 1670, 1671 };
	int[] BLUE_CHIP_BROKEN_COLS = new int[] { };
	 
	// jira:OT-427
	// only one CCD in bHROS is usable. CCD2, which we thought had one good amp, is 
	// actually completely unusable according to Manuel.
	double RED_CHIP_YSIZE = 0.0;
	double Y_MIN = CHIP_GAP * 0.5 + BLUE_CHIP_YSIZE;
	double Y_MAX = -CHIP_GAP * 0.5;
	/* Red CCD chip Y size in mm; top half is not working. */	 
	//double RED_CHIP_YSIZE = 0.5 * CHIP_YPIX * PIXELMM;

	/* CCD total Y size in mm.: 2 chips, including allowance for inter-chip gap */
	 double CCD_YSIZE = (BLUE_CHIP_YSIZE + RED_CHIP_YSIZE) + CHIP_GAP;

	/*
	 * Physical Y co-ordinate on chip (for Blue) where Echelle should
	 * position the central wavelength. Co-ordinate (0.0) is centre, between
	 * chips.
	 * <p>
	 * TODO: Should this be adjusted for the inter-chip gap? 
	 */
	 double BLUE_YCENTRE = -1.0D * BLUE_CHIP_YSIZE / 2.0D;

	/*
	 * Physical Y co-ordinate on chip (for Red) where Echelle should
	 * position the central wavelength. Co-ordinate (0.0) is centre, between
	 * chips
	 * <p>
	 * TODO: Should this be adjusted for the inter-chip gap? 
	 */
	 double RED_YCENTRE = RED_CHIP_YSIZE / 2.0D;

	 /* Min permitted input wavelength in Angstroms. */
	 double WAVELENMIN = 2930.0;

	 /* Max permitted input wavelength in Angstroms. */
	 double WAVELENMAX = 11610.0;

	 /* Echelle Az zero position in degs. */
	 double ECHAZ_ZERO = 0.487;

	 /* Az. Range +/- movement */
	 double ECHAZ_RANGE = 3.6;

	 /* Echelle Alt zero position in degs. */
	 double ECHALT_ZERO = -56.482;

	 /* Alt. Range +/- movement */
	 double ECHALT_RANGE = 6.4;

	/* Zero point for Image Rotator Position in degs. */
	/* Engineering decided that it makes more sense to center at at zero. */
	double IS_ROT_ZERO = 0.0; //  -9.30;

	/* Image Rotator Position range (+/-) in degs. */
	double IS_ROT_RANGE = 15.0;

}
