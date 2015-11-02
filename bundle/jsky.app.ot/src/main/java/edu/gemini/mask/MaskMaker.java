package edu.gemini.mask;

// XXX comment is out of date, not valid for java version
/**
 * Performs the slit positioning on a GMOS Mask and maximizes the
 * number of the placed slits.
 * Two different optimization methods can be used: Normal_Opt (parameter
 * type=N), which produce a slit catalog of the same X-dimension
 * distribution of the input object catalog and Max_Opt (parameter
 * type=M), which favors smaller catalog objects but performs a
 * better maximization in term of number of placed slits.
 * <p/>
 * The input catalog is composed of these columns:
 * <ul>
 * <li>
 * ID: object identification (INTEGER)
 * <li>
 * RA: object RA in degrees (FLOAT)
 * <li>
 * DEC: object DEC in degrees (FLOAT)
 * <li>
 * x: object x-coordinate in pixels (FOV left down corner = 0,0) (FLOAT)
 * <li>
 * y: object y-coordinate in pixels (FOV left down corner = 0,0) (FLOAT)
 * <li>
 * slitpos_x : x slit offset
 * <li>
 * slitpos_y : y slit offset
 * <li>
 * d: object half spatial dimention in pixels (FLOAT)
 * <li>
 * w: slit width pixels (FLOAT)
 * <li>
 * t: slit angle (FLOAT)
 * <li>
 * l: slit length pixels (FLOAT)
 * <li>
 * p: object priority flag (CHAR): 3=Selected 0=Acq Obj 1=Compulsory, 2=2
 * <li>
 * X=Forbidden. Normally all objects are flagged "S" and only some
 * objects are flagged "0" "1" "2" "X".
 * <li>
 * f: object type flag (CHAR): R=Rectangular, U=UserDefined
 * </ul>
 * <p/>
 * The output is composed of these columns:
 * <ul>
 * <li>
 * ID: object identification (INTEGER)
 * <li>
 * RA: object RA in degrees (FLOAT)
 * <li>
 * DEC: object DEC in degrees (FLOAT)
 * <li>
 * x_cdd: object x-coordinate in pixels (FOV left down corner = 0,0) (FLOAT)
 * <li>
 * y_cdd: object x-coordinate in pixels (FOV left down corner = 0,0) (FLOAT)
 * <li>
 * specpos_x: spectrum offset center x-coordinate in pixels (FLOAT)
 * <li>
 * specpos_y: spectrum offset center x-coordinate in pixels (FLOAT)
 * <li>
 * slitpos_x : x slit offset (in arcsec)
 * <li>
 * slitpos_y : y slit offset (in arcsec)
 * <li>
 * slitsize_x: slit x-length in pixels (FLOAT) (slitWidth)
 * <li>
 * slitsize_y: slit y-length in arcsec (FLOAT) (width of strip - 1pix)
 * <li>
 * slittilt: tilt
 * <li>
 * mag:      magnitude
 * <li>
 * priority: object priority flag (CHAR)
 * <li>
 * slittype: object type flag (CHAR)
 * </ul>
 * <p/>
 * CAUTIONS
 * <ul>
 * <li>
 * Maximum number of objects is 10,000-2
 * <li>
 * Maximum Reference object is 100-2
 * <li>
 * Maximum Arc objects is 5000-2
 * <li>
 * Maximum Compulsory objects is 5000-2
 * <li>
 * X&Y dimensions are: Bottom left (1,1), Top Right (XDIM, YDIM)
 * </ul>
 */
public class MaskMaker {

    private static class OBJ_CAT {
        // -- table column values --
        int ID;		        /* Unique id.		*/
        int index;		    /* Either an array index, or 0=already used */
        int used;	    	/* 0=not used, 1=used.  */
        double ra;		    /* Right Assention.	*/
        double dec;		    /* Declination.		*/
        double specX;		/* X Center of spectrum, takes into account slitpos_x & anamorphic.*/
        double specY;		/* Y Center of spectrum, takes into account slitpos_y.*/
        double x_ccd;		/* Real X obj position.	*/
        double y_ccd;		/* Real Y obj position.	*/
        double slitpos_x;	/* X slit offset.	*/
        double slitpos_y;	/* Y slit offset.	*/
        double len;		    /* Diameter or slitLength*/
        double angle;		/* slit tilt angle	*/
        double width;		/* Slit width. 		*/
        double mag;		    /* Magnitude. 		*/
        String prior;		/* Object priority.	*/
        String type;		/* Object type.		*/

        boolean inBands;	// is this in some band?, note, if not in bandShuffle mode, it should be false

        OBJ_CAT() {
        }
    };

    static class REF_CAT {
        // Number of allocated elements in the array (not counting first and last).
        int num;

        // Array of catalog items.
        OBJ_CAT[] a;

        REF_CAT(int maxSize) {
            a = new OBJ_CAT[maxSize];
        }
    };

    static class SL_NUMS {
        int totalNum;	/* Total Number of slits placed.	*/
        int p1s;		/* Total Number of p1 slits placed.	*/
        int p2s;		/* Total Number of p2 slits placed.	*/
        int p3s;		/* Total Number of p3 slits placed.	*/

        SL_NUMS() {
            totalNum = p1s = p2s = p3s = 0;
        }

        SL_NUMS(SL_NUMS s) {
            totalNum = s.totalNum;
            p1s = s.p1s;
            p2s = s.p2s;
            p3s = s.p3s;
        }
    };

    // Used to hold reference params of type double ported from C code
    private static class DoubleRef {
        private double val;

        private DoubleRef() {
            val = 0;
        }
    };

    // Used to hold reference params of type int ported from C code
    private static class IntRef {
        private int val;

        private IntRef() {
            val = 0;
        }
    };


    private static final int MINL = 150;        /* minimum slit strip width used in matrice.c routine */
    private static final int MAXL = 800;        /* maximum slit strip width used in matrice.c routine */
    private static final int STEPL = 50;        /* step for slit srips used in matrice.c routine */
    private static final int MAXOBJ = 10000;    /* maximum number of catalgue objects */
    private static final int MAXREF = 100;      /* maximum number of catalgue reference object */
    private static final int MAXCOM = 5000;     /* maximum number of catalgue compulsory objects */

    private static final int OVERLAP = 2;
    private static final int USED = 1;	        /* Indicates a obj has been used.	*/
    private static final int TO_BE_USED = 0;	/* Indicates an obj hasn't been used.	*/

    private static final double DSPECT = 2.0f;
    private static final double DSLIT = 2.0f;

    // Holds the input table and mask parameters
    private MaskParams _maskParams;

    private double _dslit = 2.0f;
    private GmCoords _xFov;		// Contain FoV X coordinate values
    private GmCoords _yFov;		// Contain FoV Y coordinate values
    private double _refdim;	/* half slit width for ref. objects.	*/

    private int _ystart, _yend;  		/* Start of empty space, and end of empty */
    private int _skyRegion = 0;		/*  Sky region. (remove for now) */
    private double _slitWidth;		/* Passed in slit width in pix.	       */

    private int _specLen;            /*  Spectrum length	*/

    // Array of result object definition tables, one for each mask
    ObjectTable[] _odfTables;


    /**
     * Calculates the best set of objects whose spectra fit.
     *
     * @param maskParams contains the input FITS table data and mask parameters
     * @param slitWidthPix slit width in pixels (5 pix ~ 1")
     */
    MaskMaker(MaskParams maskParams, double slitWidthPix) {
        _maskParams = maskParams;
        ObjectTable table = _maskParams.getTable();
        int numMasks = _maskParams.getNumMasks();
        _odfTables = new ObjectTable[numMasks];
        _specLen=(int)(_maskParams.getSpecLen()+DSPECT);

        double pixelScale = _maskParams.getPixelScale();
        _refdim = 2.0/pixelScale;	/* Calculate size of Ref object slits.*/

        _slitWidth = slitWidthPix;
        BandDef bandDef = _maskParams.getBandDef();

        // XXX combine these to 1 class?
        _xFov = GmCoords.getX(pixelScale);
        _yFov = GmCoords.getY(pixelScale);

        REF_CAT ref = new REF_CAT(MAXREF);     /* Arry containing ref obj. that fit.*/
        REF_CAT refOrig = new REF_CAT(MAXREF); /* Arry containing all reference obj.*/
        REF_CAT p3D = new REF_CAT(MAXCOM);	 /* Arry containing p3 objects.	     */
        REF_CAT comp = new REF_CAT(MAXCOM);	 /* Arry containing all compulsory obj*/
        REF_CAT obj = new REF_CAT(MAXOBJ);	 /* Arry of selected objects.	     */
        REF_CAT xObj = new REF_CAT(MAXOBJ);	 /* Tmp array of objs in empty space. */

        SL_NUMS nPlaced = new SL_NUMS();	/* Number of objects placed.	     */
        SL_NUMS totObjs = new SL_NUMS();	/* Cumulative objects placed.	     */

        IntRef nOutRange = new IntRef();	/* Num out of range objects.	     */
        IntRef nForb = new IntRef();		/* Num. out of range or forbin       */

        int xstart;  	/* Start of X empty  */
        int xend;       /* end of empty */
        DoubleRef startStrip = new DoubleRef();	/* Starting strip width.	     */
        int index;			/* Index number.		     */


        _dslit = DSLIT / bandDef.getBinning();

        readInObjs(obj, refOrig, comp, p3D, nForb, nOutRange);


        //  Sort only ref array by y coordinate, all the rest by x.

        if (obj.num > 1) {
            sort(obj.num, obj.a, 'x');
        }
        if (refOrig.num > 1) {
            sort(refOrig.num, refOrig.a, 'y');
        }
        if (comp.num > 1) {
            sort(comp.num, comp.a, 'x');
        }
        if (p3D.num > 1) {
            sort(p3D.num, p3D.a, 'x');
        }

        // Get rid of overlaping reference objects.
        checkOverlap(refOrig, ref);

        /*
         *  Print stuff to screen about what you have read in..
         */
        /************* WARNING, should change this to show smaller numbers for each
         mask: and move into for loop.  do that by cycling thru each array
         and count the ones that don't have used set **********/
        System.out.println("-------------------------------------------------");
        System.out.println("##### Starting Breakdown: #####\n");
        System.out.println("Total number of objects:  "
                + (nOutRange.val + refOrig.num + comp.num + p3D.num + obj.num
                + nForb.val));
        System.out.println("Num out-of-range objects: " + nOutRange.val);
        System.out.println("Acquisition objects:  " + refOrig.num);
        System.out.println("Priority 1 :     " + comp.num);
        System.out.println("Priority 2 :     " + p3D.num);
        System.out.println("Priority 3 :     " + obj.num);
        System.out.println("Ignored objects: " + nForb.val);
        System.out.println("-------------------------------------------------");


        /*
         * For 1 to number of masks put in the command line...
         */

        for (int n_exp = 1; n_exp <= numMasks; n_exp++) {
            ObjectTable newTable = new ObjectTable(table);
            _odfTables[n_exp-1] = newTable;
            int refDropped = 0;
            if (bandDef.getShuffleMode() == BandDef.BAND_SHUFFLE) {
                BandDef.Band[] bands = bandDef.getBands();
                for (int bandi = 0; bandi < bands.length; bandi++) {
                    // XXX refactor to method
                    for (int n_ref = 1; n_ref <= ref.num; n_ref++) {
                        // bands use unbinned pixel coords
                        double y0 = bands[bandi].getYPos() / bandDef.getBinning();
                        double y1 = y0 + bandDef.getBandShufflePix() / bandDef.getBinning();

                        if ((ref.a[n_ref].y_ccd >= y0) && (ref.a[n_ref].y_ccd <= y1)) {
                            // then this acquisition object is in the mix
                            Object[] row = new Object[ObjectTable.NUM_COLS];

                            row[ObjectTable.ID_COL] = new Integer(ref.a[n_ref].ID);
                            row[ObjectTable.RA_COL] = new Double(ref.a[n_ref].ra);
                            row[ObjectTable.DEC_COL] = new Double(ref.a[n_ref].dec);
                            row[ObjectTable.X_CCD_COL] = new Double(ref.a[n_ref].x_ccd);
                            row[ObjectTable.Y_CCD_COL] = new Double(ref.a[n_ref].y_ccd);
                            row[ObjectTable.SPECPOS_X_COL] = new Double(ref.a[n_ref].specX - ref.a[n_ref].x_ccd);
                            row[ObjectTable.SPECPOS_Y_COL] = new Double(ref.a[n_ref].specY - ref.a[n_ref].y_ccd);
                            row[ObjectTable.SLITPOS_X_COL] = new Double(ref.a[n_ref].slitpos_x);
                            row[ObjectTable.SLITPOS_Y_COL] = new Double(ref.a[n_ref].slitpos_y);
                            row[ObjectTable.SLITSIZE_X_COL] = new Double(2.0);
                            row[ObjectTable.SLITSIZE_Y_COL] = new Double(2.0);
                            row[ObjectTable.SLITTILT_COL] = new Double(ref.a[n_ref].angle);
                            row[ObjectTable.MAG_COL] = new Double(ref.a[n_ref].mag);
                            row[ObjectTable.PRIORITY_COL] = ref.a[n_ref].prior;
                            row[ObjectTable.SLITTYPE_COL] = ref.a[n_ref].type;

                            newTable.addRow(row);

                            ref.a[n_ref].inBands = true;
                        }
                    }
                }

                refDropped = 0;
                for (int n_ref = 1; n_ref <= ref.num; n_ref++) {
                    if (!ref.a[n_ref].inBands) {
                        System.out.println("!!!!Dropped Acquisition Object: Not in a Shuffle Band: ID = "
                                + ref.a[n_ref].ID);
                        refDropped++;
                    }
                }
            } else {
                // non-band mode
                // XXX refactor to method
                for (int n_ref = 1; n_ref <= ref.num; n_ref++) {
                    Object[] row = new Object[ObjectTable.NUM_COLS];

                    row[ObjectTable.ID_COL] =  new Integer(ref.a[n_ref].ID);
                    row[ObjectTable.RA_COL] =  new Double(ref.a[n_ref].ra);
                    row[ObjectTable.DEC_COL] =  new Double(ref.a[n_ref].dec);
                    row[ObjectTable.X_CCD_COL] =  new Double(ref.a[n_ref].x_ccd);
                    row[ObjectTable.Y_CCD_COL] =  new Double(ref.a[n_ref].y_ccd);
                    row[ObjectTable.SPECPOS_X_COL] = new Double(ref.a[n_ref].specX - ref.a[n_ref].x_ccd);
                    row[ObjectTable.SPECPOS_Y_COL] = new Double(ref.a[n_ref].specY - ref.a[n_ref].y_ccd);
                    row[ObjectTable.SLITPOS_X_COL] = new Double(ref.a[n_ref].slitpos_x);
                    row[ObjectTable.SLITPOS_Y_COL] = new Double(ref.a[n_ref].slitpos_y);
                    row[ObjectTable.SLITSIZE_X_COL] =  new Double(2.0);
                    row[ObjectTable.SLITSIZE_Y_COL] =  new Double(2.0);
                    row[ObjectTable.SLITTILT_COL] =  new Double(ref.a[n_ref].angle);
                    row[ObjectTable.MAG_COL] =  new Double(ref.a[n_ref].mag);
                    row[ObjectTable.PRIORITY_COL] =  ref.a[n_ref].prior;
                    row[ObjectTable.SLITTYPE_COL] =  ref.a[n_ref].type;

                    newTable.addRow(row);
                }
            }

            /*
             * Initialize Variables.
             */

            totObjs.totalNum = totObjs.p1s = totObjs.p2s = totObjs.p3s = 0;

            if (bandDef.getShuffleMode() == BandDef.BAND_SHUFFLE) {
                BandDef.Band[] bands = bandDef.getBands();
                // For each sorted band item do the following:
                for (int bandi = 0; bandi < bands.length; bandi++) {
                    // for each acq/ref object, split the band
                    int yStartLimit, yEndLimit;
                    // these are the limits of the band
                    // (since some acquisition objects are out of this or all bands
                    yStartLimit = (int)(bands[bandi].getYPos() / bandDef.getBinning());
                    yEndLimit = (int)(yStartLimit + bandDef.getBandShufflePix() / bandDef.getBinning());

                    for (int n_ref = 0; n_ref <= ref.num; n_ref++) {
                        /*
                         *  Set YSTART and YEND, defines blank space to fill in with slits.
                         */
                        _ystart = (int)(ref.a[n_ref].specY + _refdim + _dslit / 2);
                        _yend = (int)(ref.a[n_ref + 1].specY - _refdim - _dslit / 2);

                        // conform YSTART/YEND to shuffle band
                        // strip outside bands
                        if (_yend < yStartLimit) {
                            // System.out.println("strip prior to band, try next acq obj.\n");
                            // System.out.println("YS = %d YE= %d, YSL = %d YEL = %d\n", YSTART, YEND, YSTARTLIMIT, YENDLIMIT);
                            continue; // try next acq/ref object
                        }

                        // acq object past band
                        if (_ystart > yEndLimit) {
                            // try next band since were at acq object out of this band's range
                            // System.out.println("object past band\n");
                            // System.out.println( "YS = %d YE= %d, YSL = %d YEL = %d\n", YSTART, YEND, YSTARTLIMIT, YENDLIMIT);
                            break;
                        }

                        // ok, there is overlap, limit strip to band
                        if (_ystart < yStartLimit) {
                            // System.out.println("YS = %d YE= %d, YSL = %d YEL = %d\n", YSTART, YEND, YSTARTLIMIT, YENDLIMIT);
                            _ystart = yStartLimit;
                            // System.out.println("adjusting YSTART = %d\n", YSTART);
                        }
                        if (_yend > yEndLimit) {
                            // System.out.println("YS = %d YE= %d, YSL = %d YEL = %d\n", YSTART, YEND, YSTARTLIMIT, YENDLIMIT);
                            _yend = yEndLimit;
                            // System.out.println("adjusting YEND = %d\n", YEND);
                        }

                        xstart = 0;
                        xend = (int)_xFov.getEnd();

                        /*
                         *  Cycle through compulsory, p3, and object list and find items
                         *  that fall inbetween (y-d-skyRegion > YSTART), (y+d+sky < YEND) and
                         *  x>xstart and x<xend (is within usable area).
                         *  Copy to xObj.  All of these should already be
                         *  sorted in x direction.
                         */

                        if (cycleThruObjs(xstart, xend, _ystart, _yend, comp, p3D, obj,
                                xObj, startStrip) == 0) {
                            /*
                             *  May have run out of objects. continue.
                             */

                            continue;
                        }


                        /*
                         *  Compute best number of objects that fit in various strip
                         *  widths inbetween YSTART and YEND.
                         */

                        nPlaced.totalNum = nPlaced.p1s = nPlaced.p2s = nPlaced.p3s = 0;
                        computeMatrix2(newTable, xObj, n_exp, startStrip, nPlaced);
                        totObjs.totalNum += nPlaced.totalNum;
                        totObjs.p1s += nPlaced.p1s;
                        totObjs.p2s += nPlaced.p2s;
                        totObjs.p3s += nPlaced.p3s;

                        /*
                         *  Now cycle thru xObj and any that are "used" , set the
                         *  same bit in the original array.
                         */
                        for (int n_xobj = 1; n_xobj <= xObj.num; n_xobj++) {
                            if (xObj.a[n_xobj].used != TO_BE_USED) {
                                index = xObj.a[n_xobj].index;
                                if ("1".equals(xObj.a[n_xobj].prior)) {
                                    comp.a[index].used = n_exp;
                                } else if ("2".equals(xObj.a[n_xobj].prior)) {
                                    p3D.a[index].used = n_exp;
                                } else if ("3".equals(xObj.a[n_xobj].prior)) {
                                    obj.a[index].used = n_exp;
                                }
                            }
                        }

                    }/* For each item in ref array.. */
                }
            } else {
                /*
                 *  For each sorted reference item do the following:
                 */
                for (int n_ref = 0; n_ref <= ref.num; n_ref++) {
                    /*
                     *  Set YSTART and YEND, defines blank space to fill in with slits.
                     *  YSTART=y+d+1pixStrip.
                     *  YEND=nexty-(nextd+1pixStrip).
                     *  Don't take into account Sky Region because these are special
                     *  slits, that should have been made big enough anyway.
                     */

                    _ystart = (int)(ref.a[n_ref].specY + _refdim + _dslit / 2);
                    _yend = (int)(ref.a[n_ref + 1].specY - _refdim - _dslit / 2);

                    // System.out.println("YSTART=%d YEND=%d\n", YSTART, YEND);

                    if (bandDef.getShuffleMode() == BandDef.MICRO_SHUFFLE) {
                        // take shuffle amount into consideration on bands
                        _yend -= bandDef.getMicroShufflePix() / bandDef.getBinning();
                    }

                    xstart = 0;
                    xend = (int)_xFov.getEnd();


                    /*
                     *  Cycle through compulsory, p3, and object list and find items
                     *  that fall inbetween (y-d-skyRegion > YSTART), (y+d+sky < YEND) and
                     *  x>xstart and x<xend (is within usable area).
                     *  Copy to xObj.  All of these should already be
                     *  sorted in x direction.
                     */

                    if (cycleThruObjs(xstart, xend, _ystart, _yend, comp, p3D, obj, xObj,
                            startStrip) == 0) {
                        // May have run out of objects. continue.
                        continue;
                    }


                    /*
                     *  Compute best number of objects that fit in various strip
                     *  widths inbetween YSTART and YEND.
                     */

                    nPlaced.totalNum = nPlaced.p1s = nPlaced.p2s = nPlaced.p3s = 0;
                    computeMatrix2(newTable, xObj, n_exp, startStrip, nPlaced);
                    totObjs.totalNum += nPlaced.totalNum;
                    totObjs.p1s += nPlaced.p1s;
                    totObjs.p2s += nPlaced.p2s;
                    totObjs.p3s += nPlaced.p3s;

                    /*
                     *  Now cycle thru xObj and any that are "used" , set the
                     *  same bit in the original array.
                     */
                    for (int n_xobj = 1; n_xobj <= xObj.num; n_xobj++) {
                        if (xObj.a[n_xobj].used != TO_BE_USED) {
                            index = xObj.a[n_xobj].index;
                            if ("1".equals(xObj.a[n_xobj].prior)) {
                                comp.a[index].used = n_exp;
                            } else if ("2".equals(xObj.a[n_xobj].prior)) {
                                p3D.a[index].used = n_exp;
                            } else if ("3".equals(xObj.a[n_xobj].prior)) {
                                obj.a[index].used = n_exp;
                            }
                        }
                    }

                }
            }

            System.out.println("----------------------------------------------------");
            System.out.println("### Mask: ODF" + n_exp + " ###");
            System.out.println("total placed slits:" + (totObjs.totalNum + ref.num));
            System.out.println(" placed acquisition slits: " + (ref.num - refDropped));
            if (bandDef.getShuffleMode() == BandDef.BAND_SHUFFLE) {
                System.out.println("dropped acquisition slits: %d (not in shuffle band) "
                        + refDropped);
            }
            System.out.println("  placed priority 1 slits: " + totObjs.p1s);
            System.out.println("  placed priority 2 slits: " + totObjs.p2s);
            System.out.println("  placed priority 3 slits: " + totObjs.p3s);
            System.out.println("----------------------------------------------------");
        }
    }

    /**
     * Return an array of object definition tables, one for each mask.
     */
    public ObjectTable[] getOdfTables() {
        return _odfTables;
    }

    private int computeMatrix2(ObjectTable newTable, REF_CAT xObj,
                               int expNum, DoubleRef startStrip, SL_NUMS nPlaced) {

        int[] slit = new int[MAXOBJ]; /* Array of index's to xObj array.*/
        int[] maxSlit = new int[500]; /* Max Array of index's in a strip*/
        IntRef yCord = new IntRef();	/* Start of strip.		*/
        int maxSWidth;	    /* max strip width.	*/
        int sum = 0;			/* Sum of objects selected.	*/
        int largestY;		/* Furthest y+d obj in the strip*/
        int nRealSlit;		/* Number of optimium slits.	*/
        double returnedWeight, maxWeight;	/* Weighed values.		*/
        double y1, y2;
        IntRef nSlits = new IntRef();	/* Length of stripObj array.	*/
        SL_NUMS priorNumbers = new SL_NUMS(); /* Count of slits placed.	*/
        SL_NUMS maxNumbers = new SL_NUMS();	  /* Best count of slits placed.	*/
        int index;				/* Index counter.		*/
        boolean lastOne;			/* Indicates last strip width to try.*/
        IntRef allObjGone = new IntRef();	/* Indicates no more obj in xObj*/
        boolean firstTime = true;			/* Indicates first time thru.	*/
        int ymin;				/* Next minimum ycord value.	*/
        int num;				/* Generic counter.		*/
        double pixelScale = _maskParams.getPixelScale();

        //  Set the starting position to be where the first object is.

        yCord.val = (int)startStrip.val;

        while (yCord.val < _yend) {
            maxWeight = 0;
            maxSWidth = MAXL;
            lastOne = false;
            nRealSlit = 0;

            /** NOTE TO ME:  Add something here that changes yCord to be
             something like the first one that will make a strip. *******/

            /*
             *  For each strip width, find best strip width ...
             */

            for (int stripWidth = MINL;
                 stripWidth <= MAXL && !lastOne; stripWidth += STEPL) {
                if (yCord.val + stripWidth > _yend) {
                    /*
                     *  Make sure that this stripwidth is not too wide, and if
                     *  so then make the stripwidth the remaining room.
                     */

                    stripWidth = _yend - yCord.val;
                    lastOne = true;

                }
                /*
                 *  Select all objects in the current strip width that fit.
                 *  Return a weighted val based on object priorities, and an
                 *  array of indexes(slit) into xObj array that fit in this strip.
                 */

                nSlits.val = 0;
                allObjGone.val = 0;
                returnedWeight = computeSlit2(xObj, slit, nSlits, yCord, stripWidth,
                        priorNumbers, allObjGone);
                if (allObjGone.val != 0) {
                    //  No more objects left to choose from, return.
                    return (sum);
                }

                /*
                 * If the normalized returnedWeight > maxWeight, then..
                 * means have found a maxier one, so copy the array of indexes
                 * to maxSlit.
                 */

                if (returnedWeight / stripWidth > maxWeight / maxSWidth) {
                    maxWeight = returnedWeight;
                    nRealSlit = nSlits.val;
                    maxSWidth = stripWidth;
                    maxNumbers = new SL_NUMS(priorNumbers);
                    for (int n_slit = 1; n_slit <= nRealSlit; n_slit++) {
                        maxSlit[n_slit] = slit[n_slit];
                    }
                }
            }
            y1 = yCord.val;
            y2 = yCord.val;


            /*
             * Now set the "used" item in the xObj array.  This means this
             * object will not be used for any other mask.  At the same time
             * find the largest y coordinate value so that we know where the
             * next strip should start and will use it to determine the
             * real max stripwidth.
             */

            if (nRealSlit != 0) {
                for (int n_slit = 1; n_slit <= nRealSlit; n_slit++) {
                    largestY = (int)(xObj.a[maxSlit[n_slit]].specY
                            + xObj.a[maxSlit[n_slit]].len + _skyRegion + _dslit);
                    if (xObj.a[maxSlit[n_slit]].used != TO_BE_USED) {
                        System.out.println("Another BIG ERROR.");
                    }
                    xObj.a[maxSlit[n_slit]].used = expNum;
                    if (largestY > y2) {
                        y2 = largestY;
                    }
                }

//                /*
//                 *  Now need to change slit length, if in max mode.
//                 */
//
//                if (_maskParams.getBiasType().equals(MaskParams.BIAS_TYPE_MAX)) {
//                    fixSlitLen(nRealSlit, maxSlit, yCord.val, (yCord.val + maxSWidth),
//                            xObj);
//                }
//
//

                /*
                 *  If there is any room left...
                 */

                if (_yend - y2 > 0) {
                    /*
                     * See if there are any objs available in the space from
                     * y2 to the YEND.
                     */

                    if (objsAvailable(xObj, y2, _yend - (int)y2) == 0) {
                        /*
                         *  There are no more useable points in the remaining space
                         *  then make the strip width the maximum that can fit in
                         *  the space left and change y2 to make it finish after
                         *  this one.  BUT keep the same objects.
                         */

                        y2 = (yCord.val + MAXL > _yend) ? _yend : yCord.val + MAXL;
                        maxSWidth = (int)(y2 - y1);
                    }
                }

                if (firstTime) {
                    /*
                     *  If this is the first time thru, then the y1, is
                     *  actually not ystart, but we should make it ystart
                     *  to maximum the length of the slit.
                     */
                    y1 = _ystart;
                    firstTime = false;
                }

                for (int n_slit = 1; n_slit <= nRealSlit; n_slit++) {
                    /*
                     *  We have objects, so write them out to the file.
                     *  Write ID, ra, dec, x, y, x, center of strip,
                     *  slit x length, slit y length,
                     *  object half spatial dimension, prioirty, type
                     */

                    index = maxSlit[n_slit];
//                    if (_maskParams.getBiasType().equals(MaskParams.BIAS_TYPE_MAX)) {
//                        /*
//                         *  Maximum option, so make slit as wide as it can be.
//                         *  Which is maxSWidth minus the room between 2 slits.
//                         */
//
//                        // WARNING, can only increase the length of the slit up to
//                        // the other objects that interfer in that strip.
//
//                        Object[] row = new Object[ObjectTable.NUM_COLS];
//
//                        row[ObjectTable.ID_COL] = new Integer(xObj.a[index].ID);
//                        row[ObjectTable.RA_COL] = new Double(xObj.a[index].ra);
//                        row[ObjectTable.DEC_COL] = new Double(xObj.a[index].dec);
//                        row[ObjectTable.X_CCD_COL] = new Double(xObj.a[index].x_ccd);
//                        row[ObjectTable.Y_CCD_COL] = new Double(xObj.a[index].y_ccd);
//                        row[ObjectTable.SPECPOS_X_COL] =
//                                new Double(xObj.a[index].specX - xObj.a[index].x_ccd);
//                        row[ObjectTable.SPECPOS_Y_COL] =
//                                new Double((y1 + maxSWidth / 2.0) - xObj.a[index].y_ccd);
//                        row[ObjectTable.SLITPOS_X_COL] =
//                                new Double(xObj.a[index].slitpos_x);
//                        row[ObjectTable.SLITPOS_Y_COL] =
//                                new Double(xObj.a[index].slitpos_y);
//                        row[ObjectTable.SLITSIZE_X_COL] =
//                                new Double(xObj.a[index].width * pixelScale);
//                        row[ObjectTable.SLITSIZE_Y_COL] =
//                                new Double((maxSWidth - _dslit / 2) * pixelScale);
//                        row[ObjectTable.SLITTILT_COL] = new Double(xObj.a[index].angle);
//                        row[ObjectTable.MAG_COL] = new Double(xObj.a[index].mag);
//                        row[ObjectTable.PRIORITY_COL] = xObj.a[index].prior;
//                        row[ObjectTable.SLITTYPE_COL] = xObj.a[index].type;
//
//                        newTable.addRow(row);

//                    } else {
                        /*
                         *  Normal option, so make slit only as wide as slitsize_y
                         *  plus skyRegion (minus space between 2 slits>
                         */

                        Object[] row = new Object[ObjectTable.NUM_COLS];

                        row[ObjectTable.ID_COL] = new Integer(xObj.a[index].ID);
                        row[ObjectTable.RA_COL] = new Double(xObj.a[index].ra);
                        row[ObjectTable.DEC_COL] = new Double(xObj.a[index].dec);
                        row[ObjectTable.X_CCD_COL] = new Double(xObj.a[index].x_ccd);
                        row[ObjectTable.Y_CCD_COL] = new Double(xObj.a[index].y_ccd);
                        row[ObjectTable.SPECPOS_X_COL] =
                                new Double(xObj.a[index].specX - xObj.a[index].x_ccd);
                        row[ObjectTable.SPECPOS_Y_COL] =
                                new Double(xObj.a[index].specY - xObj.a[index].y_ccd);
                        row[ObjectTable.SLITPOS_X_COL] =
                                new Double(xObj.a[index].slitpos_x);
                        row[ObjectTable.SLITPOS_Y_COL] =
                                new Double(xObj.a[index].slitpos_y);
                        row[ObjectTable.SLITSIZE_X_COL] =
                                new Double(xObj.a[index].width * pixelScale);

                        BandDef bandDef = _maskParams.getBandDef();
                        if (bandDef.getShuffleMode() == BandDef.MICRO_SHUFFLE) {
                            row[ObjectTable.SLITSIZE_Y_COL] = new Double(bandDef.getSlitLength());
                        } else {
                            row[ObjectTable.SLITSIZE_Y_COL] =
                                    new Double((xObj.a[index].len + _skyRegion) * 2.0
                                    * pixelScale);
                        }

                        row[ObjectTable.SLITTILT_COL] = new Double(xObj.a[index].angle);
                        row[ObjectTable.MAG_COL] = new Double(xObj.a[index].mag);
                        row[ObjectTable.PRIORITY_COL] = xObj.a[index].prior;
                        row[ObjectTable.SLITTYPE_COL] = xObj.a[index].type;

                        newTable.addRow(row);
//                    }
                }

                /*
                 *  Set the starting point for the next strip.  And make sure
                 *  we keep count of the number placed.  The next strip, if in
                 *  MAX mode, is y1+maxSWidth, otherwise it will be the largestY
                 *  that we have had so far.
                 */

//                boolean maxOpt = _maskParams.getBiasType().equals(MaskParams.BIAS_TYPE_MAX);
//                yCord.val = (int)(maxOpt ? y1 + maxSWidth : y2);
                yCord.val = (int)y2;
                sum = sum + maxNumbers.totalNum;
                nPlaced.totalNum += maxNumbers.totalNum;
                nPlaced.p1s += maxNumbers.p1s;
                nPlaced.p2s += maxNumbers.p2s;
                nPlaced.p3s += maxNumbers.p3s;
            } else {
                /*
                 * Got no objects in that strip, increment ycord to the next
                 * min y coordinate(y-d-skyregion) - if there is one.
                 * Otherwise, increment yCord to be > YEND, and loop will be broken.
                 */
                ymin = _yend;
                for (num = 1; num <= xObj.num; num++) {
                    if (xObj.a[num].used == TO_BE_USED && xObj.a[num].specY
                            - xObj.a[num].len - _skyRegion - _dslit > yCord.val + MAXL && xObj.a[num].specY
                            - xObj.a[num].len - _skyRegion - _dslit < ymin) {
                        ymin =
                                (int)(xObj.a[num].specY - xObj.a[num].len
                                - _skyRegion
                                - _dslit);
                    }
                }
                if (ymin == _yend) {
                    ymin++;
                } else {
                    yCord.val = ymin;
                }
            }
        }

        return sum;
    }

    private void checkOverlap(REF_CAT refOrig, REF_CAT ref) {
        int n_ref, i_ref;	/* Refer. array counters.		*/
        int lastOne;	/* Last used one.			*/

        n_ref = 0;

        /*
         *  Looking at the sorted reference object array.  Sorted by Y.
         *  Comparing each item in the array to see if the current items
         *  (Yco-ord. plus the half spatial dimension)  are greater than the
         *  previous items, then keep the item.  Otherwise drop it.
         *  Don't include the Sky_Region for now, because for these types
         *  of objects we have a standard slitlength.
         *  We are always going to use the same set of reference objects for
         *  all mask exposures.
         *
         */

        if (refOrig.num > 0) {
            /*
             *  Find the first one in the array, save it.
             *  WARNING, have left hooks in to have mult ref items.
             */

            n_ref = lastOne = 1;
            ref.a[n_ref++] = refOrig.a[1];
            if (refOrig.num > 1) {
                for (i_ref = 2; i_ref <= refOrig.num; i_ref++) {
                    if ((refOrig.a[lastOne].specY + _refdim)
                            < refOrig.a[i_ref].specY - _refdim) {
                        lastOne = i_ref;
                        ref.a[n_ref++] = refOrig.a[i_ref];

                    } else {
                        System.out.println("Acq Object ID: " + refOrig.a[i_ref].ID
                                + " overlaps id: " + refOrig.a[lastOne].ID);
                        refOrig.a[i_ref].used = (refOrig.a[i_ref].used == USED) ?
                                USED : OVERLAP;
                    }
                }
            }
            n_ref--;
        }


        /*
         *  Set the zeroith item and last item in the ref array, which
         *  will set the edge values.  Each item in the ray represents
         *  the start/end of empty space.  This is where
         *  we want to start looking for room to put slits.
         */

        OBJ_CAT oc = new OBJ_CAT();
        oc.type = "0";
        oc.specY = 1;		/*-(REFDIM+DSLIT/2.0)+1;*/
        oc.len = 0.0;
        oc.used = 0;
        ref.a[0] = oc;

        oc = new OBJ_CAT();
        oc.type = "0";
        oc.specY = _yFov.getDim();	/*+(REFDIM+DSLIT/2.0);*/
        oc.len = 0;
        oc.used = 0;
        ref.a[n_ref + 1] = oc;

        ref.num = n_ref;
        if (n_ref == -1) {
            throw new RuntimeException();
        }

    }


    /**
     * Cycle thru arrays created from the file.
     * Cycle thru compulsory, p3 and obj array and put them in
     * the xObj array if they are within our free strip.
     *
     * @param xstart (in) Start of x empty space.
     * @param xend   (in) end of x empty space.
     * @param ystart (in) Start of Y empty space.
     * @param yend   (in) end of Y empty space.
     * @param p1     (in) Arry of all p1 objects.
     * @param p2     (in) Arry of all p2 objects.
     * @param p3     (in) Arry of all p3 objects.
     * @param xObj   (out) Arry of Selected objects.
     * @param minY   (out) Min. y position.
     * @return xObj.num
     */
    private int cycleThruObjs(int xstart, int xend, int ystart, int yend, REF_CAT p1,
                              REF_CAT p2, REF_CAT p3, REF_CAT xObj, DoubleRef minY) {

        int n_p1, n_p2, n_p3;	/* Array counters.		*/
        int numReturn;		/* Number in xObj returned.	*/
        int yMin;			/* Minimum y found.		*/
        int yBottom;		/* Bottom of y slit:y-d-skyregion.*/

        yMin = yend;

        /*
         *  Cycle through priority 1 object list and find items that fall
         *  inbetween (y-d-skyRegion >YSTART), (y+d+sky <YEND) and
         *  x>xstart and x<xend (is within usable area).
         *  Copy to xObj and increment numReturn .
         */


        numReturn = 0;
        for (n_p1 = 1; n_p1 <= p1.num; n_p1++) {
            yBottom = (int)(p1.a[n_p1].specY - p1.a[n_p1].len - _skyRegion - _dslit);
            if (p1.a[n_p1].used == TO_BE_USED && (yBottom >= ystart
                    && p1.a[n_p1].specY + p1.a[n_p1].len + _skyRegion + _dslit <= yend
                    && p1.a[n_p1].specX >= xstart && p1.a[n_p1].specX <= xend)) {
                xObj.a[++numReturn] = p1.a[n_p1];
                xObj.a[numReturn].used = TO_BE_USED;
                xObj.a[numReturn].index = n_p1;
                if (yBottom < yMin) {
                    yMin = yBottom;
                }
            }

        }


        /*
         *  Cycle thru p2 array, and see if its in the free strip.
         */

        for (n_p2 = 1; n_p2 <= p2.num; n_p2++) {
            yBottom = (int)(p2.a[n_p2].specY - p2.a[n_p2].len - _skyRegion - _dslit);
            if (p2.a[n_p2].used == TO_BE_USED && (yBottom >= ystart
                    && p2.a[n_p2].specY + p2.a[n_p2].len + _skyRegion + _dslit <= yend
                    && p2.a[n_p2].specX >= xstart && p2.a[n_p2].specX <= xend)) {
                xObj.a[++numReturn] = p2.a[n_p2];
                xObj.a[numReturn].used = TO_BE_USED;
                xObj.a[numReturn].index = n_p2;
                if (yBottom < yMin) {
                    yMin = yBottom;
                }
            }
        }


        /*
         *  Cycle thru p3 array, and see if its in the free strip.
         */


        for (n_p3 = 1; n_p3 <= p3.num; n_p3++) {
            yBottom = (int)(p3.a[n_p3].specY - p3.a[n_p3].len - _skyRegion + _dslit);
            if (p3.a[n_p3].used == TO_BE_USED && (yBottom >= ystart
                    && p3.a[n_p3].specY + p3.a[n_p3].len + _skyRegion + _dslit <= yend
                    && p3.a[n_p3].specX >= xstart && p3.a[n_p3].specX <= xend)) {
                xObj.a[++numReturn] = p3.a[n_p3];
                xObj.a[numReturn].used = TO_BE_USED;
                xObj.a[numReturn].index = n_p3;
                if (yBottom < yMin) {
                    yMin = yBottom;
                }
            }

        }
        minY.val = (yMin - 1);

        /*
         *  Fill in the last item and the first item of the xObj array.
         */

        OBJ_CAT oc = new OBJ_CAT();
        oc.type = "0";
        oc.specX = -(_refdim + _dslit / 2.0) + 1;
        oc.used = TO_BE_USED;
        oc.len = 0.0;
        xObj.a[0] = oc;

        oc = new OBJ_CAT();
        oc.type = "0";
        oc.type = "0";
        oc.specX = _yFov.getDim() + (_refdim + _dslit / 2.0);
        xObj.a[numReturn + 1] = oc;

        xObj.num = numReturn;

        return (numReturn);
    }


    /**
     * Put table data in output objects
     *
     * @param obj     out) Arry of all sel objects
     * @param ref     (out) Arry of reference objects.
     * @param comp    (out) Arry of compulsory objects
     * @param p3D     (out) Arry of p3 objects.
     * @param numForb out) Num forbidden objs.
     * @param numOut  (out) Num out of range objs
     */
    private void readInObjs(REF_CAT obj, REF_CAT ref, REF_CAT comp,
                            REF_CAT p3D, IntRef numForb, IntRef numOut) {

        double xlambdac;
        double lamda1, lamda2, x1, x2;


        // rename this var
        _yend = (int)_yFov.getEnd();

        /*
         *  Open the File.  Format expected on each line:
         *   ID: object identification (INTEGER)
         *   RA: object RA in degrees (FLOAT)
         *   DEC: object DEC in degrees (FLOAT)
         *   x_ccd: object x-coordinate in pixels (FOV left down corner = 0,0)
         *          (FLOAT)
         *   y_ccd: object y-coordinate in pixels (FOV left down corner = 0,0)
         *          (FLOAT)
         *   slitpos_x:slit position x offset in arcsec (float)
         *   slitpos_y:slit position y offset in arcsec (float)
         *   slitsize_x: slit width (FLOAT)
         *   slitsize_y: slit length/2 (FLOAT)
         *   slittilt: slit angle (FLOAT)
         *   prior: object priority flag (CHAR): S=Selected R=Reference 3=P3
         *         C=Compulsory F=Forbidden. Normally all objects are flagged "S" .
         *   slittype : object type flag (CHAR): R=Rectangular A=User-Defined
         *
         * XXX XXX XXX
         *   All the arrays start writing at 1 , because later the 0ith and
         *   last item will be written.
         */


        /*
         *  Initialize variables.  Then read in lines from input file.
         */

        int nObj = 0, nRef = 0, nP3 = 0, nComp = 0;	/* Number of objects .		*/
        int nOutRange = 0;		/* Num out of range objects.	*/
        int nForb = 0;			/* Num. out of range or forbin  */

        ObjectTable table = _maskParams.getTable();
        double pixelScale = _maskParams.getPixelScale();
        double anaMorphic = _maskParams.getAnaMorphic();
        double wavelength = _maskParams.getWavelength();
        double dPix = _maskParams.getDpix();
        double l1max = _maskParams.getLmax();
        double l2min = _maskParams.getLmin();
        BandDef bandDef = _maskParams.getBandDef();

        int numRows = table.getRowCount();
        for (int row = 0; row < numRows; row++) {

            /*
             *  Scan the line for the correct format.  If incorrect then don't
             *  continue.
             */
            OBJ_CAT oc = new OBJ_CAT();
            nObj++;
            obj.a[nObj] = oc;

            oc.ID = table.getId(row);
            oc.ra = table.getRa(row);
            oc.dec = table.getDec(row);
            oc.x_ccd = table.getXCcd(row);
            oc.y_ccd = table.getYCcd(row);
            oc.slitpos_x = table.getSlitPosX(row);
            oc.slitpos_y = table.getSlitPosY(row);
            oc.width = table.getSlitSizeX(row);
            // Divide slitsize_y by 2, so that we get slit length radius.
            oc.len = table.getSlitSizeY(row) / 2.; // XXX see gmmps:vmTableList.tcl: line 1299
            oc.angle = table.getSlitTilt(row);
            oc.mag = table.getMag(row);
            oc.prior = table.getPriority(row);
            oc.type = table.getSlitType(row);

            /*
             *  Fill in other fields, like used, width, x/y position
             */

            oc.used = TO_BE_USED;
            oc.width = (_slitWidth == 0) ? oc.width / pixelScale : _slitWidth;
            double dim = _xFov.getDim();
            xlambdac =
                    dim / 2
                    - (dim / 2 - (oc.x_ccd + oc.slitpos_x / pixelScale))
                    / anaMorphic;
            lamda1 = wavelength - (dim - xlambdac) * dPix;
            lamda1 = (l1max > lamda1) ? l1max : lamda1;
            lamda2 = wavelength + xlambdac * dPix;
            lamda2 = (l2min < lamda2) ? l2min : lamda2;
            x1 = xlambdac + (wavelength - lamda1) / dPix;
            x2 = xlambdac - (lamda2 - wavelength) / dPix;
            oc.specX = (x1 + x2) / 2;          /* Center of spectrum in x */

            // !!! @cba: MicroShuffle mode alternates
            // len and slitpos_y
            if (bandDef.getShuffleMode() == BandDef.MICRO_SHUFFLE) {
                // @@cba: from what I gather, len is an interval radius...
                oc.len = (bandDef.getMicroShufflePix() / bandDef.getBinning()
                        + (bandDef.getSlitLength() / pixelScale)) / 2;
                // override slitpos_y for nod amount
                // moved 1/4 slit (in arcseconds)
                if (!"0".equals(oc.prior)) {
                    oc.slitpos_y += (bandDef.getNodAmount() / 2);
                }
            } else {
                oc.len = oc.len / pixelScale;
            }

            oc.specY = oc.y_ccd + oc.slitpos_y / pixelScale;	/* Center of slit in y */


            /*
             *  Check that the center of the spectrum X and Y co-ordinate are
             *  within range of:
             *  (specX-width/2 > XSTART) and
             *  (specX+width/2 < XEND) and (specYy+slitSize_y < YEND) and
             *  (y-slitSize_y > YBEGIN).
             *  If not then remove the object.
             */
            if (oc.specX - oc.width / 2 < _xFov.getStart()
                    || oc.specX + oc.width / 2 > _xFov.getEnd()
                    || oc.specY - oc.len < _yFov.getStart() || oc.specY + oc.len
                    > _yend) {
                if ("0".equals(oc.prior)) {
                    System.out.println("Acq Object ID: " + oc.ID + " out of range.");
                    //		System.out.println("    xlambdac <%f>= %f - (%f - (x_ccd+slitpos_x/%f) )/anaMorphic\n",
                    //                _xFov.getDim()/2, _xFov.getDim()/2, xlambdac, pixelScale);
                    //		System.out.println("    x1 <%f> = xlambdac + ( cWavelength - lamda1)/dPix\n", x1);
                    //		System.out.println("    x2 <%f> = xlambdac - (lamda2 - cWavelength)/dPix\n", x2);
                    //		System.out.println("    specX <%f> =(x1+x2)/2\n", oc.specX);
                    //		System.out.println("    specY <%f> =y_ccd+slitpos_y/%f\n", oc.specY, pixelScale);
                    //		System.out.println("    width/2=%f, len=%f." +
                    //				oc.width/2, oc.len );
                    //		System.out.println("    specX-width/2 < 928, specX+width/2 > 5291  OR\n");
                    //		System.out.println("    specY-len < 128  OR  specY+len > 4485 \n");
                }
                nObj--;
                nOutRange++;
            }

            /*
             *  Else if this is a Reference Object then store in ref array and
             *  delete from obj.
             */

            else if ("0".equals(oc.prior)) {
                /*
                 *  For reference objects make sure that the slit tilt is 0.
                 */


                nRef++;
                oc.angle = 0;
                ref.a[nRef] = oc;
                nObj--;
            }

            /*
             *  Else if this is a Compulsory Object then store in comp array and
             *  delete it from obj.
             */

            else if ("1".equals(oc.prior)) {
                nComp++;
                comp.a[nComp] = oc;
                nObj--;
            }


            /*
             *  Else if this is a P3 store in p3D array and delete
             *  from obj.
             */

            else if ("2".equals(oc.prior)) {
                nP3++;
                p3D.a[nP3] = oc;
                nObj--;
            }

            /*
             *  Else if this is a forbidden Object then don't add it.
             */

            else if ("X".equals(oc.prior)) {
                nObj--;
                nForb++;
            } else if ("3".equals(oc.prior)) {
            } else {
                /*
                 *  Invalid proirity, assume it is priority 3.
                 */
            }
        }

        comp.num = nComp;
        ref.num = nRef;
        p3D.num = nP3;
        obj.num = nObj;
        numForb.val = nForb;
        numOut.val = nOutRange;
    }

    // XXX Can this be done in a more standard way?
    private void sort(int numObjs, OBJ_CAT[] obj, char flag) {
        OBJ_CAT objTmp = new OBJ_CAT();	/* Temporary catalog object.	*/
        int i_half, j_half;
        int half, nObjTmp;
        double THRESHOLD = 0.000001;

        /*
         * Initialize variables.
         */

        half = numObjs / 2 + 1;
        nObjTmp = numObjs;


        /*
         *  Cycle around till entire passed in structure is sorted.
         */

        while (true) {
            if (half > 1) {
                half = half - 1;
                objTmp = obj[half];
            } else {
                objTmp = obj[nObjTmp];
                obj[nObjTmp] = obj[1];
                nObjTmp = nObjTmp - 1;
                if (nObjTmp == 1) {
                    obj[1] = objTmp;
                    break;
                }
            }
            i_half = half;
            j_half = half + half;


            if (flag == 'x') {
                /*
                 *  Sorting by specX
                 */

                while (j_half <= nObjTmp) {
                    if (j_half < nObjTmp) {
                        if (obj[j_half + 1].specX - obj[j_half].specX > THRESHOLD) {
                            j_half = j_half + 1;
                        }
                    }
                    if (obj[j_half].specX - objTmp.specX > THRESHOLD ) {
                        obj[i_half] = obj[j_half];
                        i_half = j_half;
                        j_half = j_half + j_half;
                    } else {
                        j_half = nObjTmp + 1;
                    }
                }
            } else if (flag == 'y') {
                /*
                 *  ELSE Sorting by specY.
                 */

                while (j_half <= nObjTmp) {
                    if (j_half < nObjTmp) {
                        if (obj[j_half + 1].specY - obj[j_half].specY > THRESHOLD ) {
                            j_half = j_half + 1;
                        }
                    }
                    if (obj[j_half].specY - objTmp.specY > THRESHOLD ) {
                        obj[i_half] = obj[j_half];
                        i_half = j_half;
                        j_half = j_half + j_half;
                    } else {
                        j_half = nObjTmp + 1;
                    }
                }
            }
            obj[i_half] = objTmp;

        }
    }


    // Determine if there are objs left in the space.
    // Cycle through all objects, see if there are any left
    // in the area provided.  Each that obj is not used, and
    // y-d >= yStart+sky  AND y+d <= yStart+stripwidth+sky.
    private int objsAvailable(REF_CAT xObj, double yStart, int stripWidth) {
        for (int num = 1; num <= xObj.num; num++) {
            // See if this object fits in the strip.
            OBJ_CAT oc = xObj.a[num];
            if (oc.used == TO_BE_USED
                    && oc.specY - oc.len - _skyRegion - _dslit >= yStart
                    && oc.specY + oc.len + _skyRegion + _dslit <= yStart + stripWidth) {
                return (1);
            }
        }
        return (0);
    }


    /**
     * Compute number of slits in a strip.
     *
     * @param xObj       (in)  Array to objects to choose from.
     * @param slit       (out) Array of index's that pass.
     * @param numSlit    (out) Num in above array.
     * @param yStart     (in)  YCoordinate.
     * @param stripWidth (in)  Strip width.
     * @param numType    (out) Arry of count of types in slit
     * @param objsGone   (out) Are there objects left.
     * @return number of slits in strip
     */
    private double computeSlit2(REF_CAT xObj, int[] slit, IntRef numSlit,
                                IntRef yStart, int stripWidth, SL_NUMS numType, IntRef objsGone) {
        double weight;	/* Weighted value.			*/
        int numComp;	/* Num items of compul extracted.       */
        int numP3;		/* Num items of p3 extracted.           */
        int numSel;	/* Num items of selected extracted.     */
        int num;		/* Total number extracted.              */

        OBJ_CAT p;		/* Ptrs to catalog object.		*/
        OBJ_CAT test;		/* Ptr to testing object in catalog.	*/
        boolean skip;		/* Test to skip object.			*/

        int nslit;		/* Number of obj in slit array.		*/
        int k;		/* Counter.				*/
        double tXslit;		/* Test center of x slit.		*/
        double xslit;		/* center of x slit.			*/
        double pixelScale = _maskParams.getPixelScale();

        /*
         * Cycle through all objects that are in our range.
         * For each object see if the object fits within:
         *   y-d >= yStart+sky  AND y+d <= yStart+stripwidth+sky.
         * All objects in the xObj array are put in order of priority,
         * which means the comp. are first and are fitted in first.
         * The xObj array starts at 1.  Save the index into the
         * xObj array.
         */

        numComp = numP3 = numSel = 0;
        weight = 0.0;
        objsGone.val = 1;
        nslit = 0;


        /*
         *  For each object in the xObj array...
         */

        for (num = 1; num <= xObj.num; num++) {
            if (xObj.a[num].used != TO_BE_USED
                    || xObj.a[num].specY - xObj.a[num].len - _skyRegion - _dslit
                    < yStart.val) {
                /*
                 *  This obj is either already used or the y is < the current
                 *  y starting position.
                 */
            } else {
                /*
                 *  Check that the y co-ordinate fits in.
                 */

                objsGone.val = 0;
                if (xObj.a[num].specY - xObj.a[num].len - _skyRegion - _dslit
                        >= yStart.val
                        && xObj.a[num].specY + xObj.a[num].len + _skyRegion + _dslit
                        <= yStart.val + stripWidth) {
                    /*
                     *  Fits into the empty spot(yStart to +stripWidth, now lets
                     *  see there is no overlap with already selected objects.
                     */

                    skip = false;
                    test = xObj.a[num];
                    tXslit = test.x_ccd + (test.slitpos_x / pixelScale);
                    for (k = 1; k <= nslit; k++) {
                        p = xObj.a[slit[k]];

                        /*
                         *  First check to see if the slits overlaps.
                         *  (note, spectrum may not lie overtop of slit.)
                         *  If there is overlap in X AND in Y then skip it.
                         *  XOverlap:
                         *  IF pLeft < testLeft-2 && pRight >= testLeft-2 ||
                         *  IF pLeft > testLeft-2 && pLeft  <= testRight+2 )
                         *  YOverlap:
                         *  If pTop < testTop+2 && pTop > testBottom-2 ||
                         *  If pTop > testTop+2 && pBottom <= testTop+2
                         */
                        xslit = p.x_ccd + (p.slitpos_x / pixelScale);
                        if ((xslit - p.width / 2 < tXslit - test.width / 2 - DSPECT
                                && xslit + p.width / 2
                                >= tXslit - test.width / 2 - DSPECT)
                                || (xslit - p.width / 2
                                > tXslit - test.width / 2 - DSPECT
                                && xslit - p.width / 2
                                <= tXslit + test.width / 2 + DSPECT)) {
                            if ((p.specY + p.len + _skyRegion
                                    < test.specY + test.len + _skyRegion + _dslit
                                    && p.specY + p.len + _skyRegion
                                    >= test.specY - test.len - _skyRegion - _dslit)
                                    || (p.specY + p.len + _skyRegion
                                    > test.specY + test.len + _skyRegion + _dslit
                                    && p.specY - p.len - _skyRegion
                                    <= test.specY + test.len + _skyRegion + _dslit)) {
                                skip = true;
                                break;
                            }

                        }

                        /*
                         *  Now check spectrum overlap.
                         *  If there is overlap in X AND in Y then skip it.
                         *  XOverlap:
                         *  IF pLeft < testLeft-2 && pRight >= testLeft-2 ||
                         *  IF pLeft > testLeft-2 && pLeft  <= testRight+2 )
                         *  YOverlap:
                         *  If pTop < testTop+2 && pTop > testBottom-2 ||
                         *  If pTop > testTop+2 && pBottom <= testTop+2
                         */

                        if (((p.specX - _specLen / 2 < test.specX - _specLen / 2 - DSPECT
                                && p.specX + _specLen / 2
                                >= test.specX - _specLen / 2 - DSPECT)
                                || (p.specX - _specLen / 2
                                > test.specX - _specLen / 2 - DSPECT
                                && p.specX - _specLen / 2
                                <= test.specX + _specLen / 2 + DSPECT))
                                && ((p.specY + p.len + _skyRegion
                                < test.specY + test.len + _skyRegion + _dslit
                                && p.specY + p.len + _skyRegion
                                >= test.specY - test.len - _skyRegion - _dslit)
                                || (p.specY + p.len + _skyRegion
                                > test.specY + test.len + _skyRegion + _dslit
                                && p.specY - p.len - _skyRegion
                                <= test.specY + test.len + _skyRegion + _dslit))) {
                            skip = true;
                            break;
                        }
                    }

                    if (!skip) {
                        if ("1".equals(xObj.a[num].prior)) {
                            numComp++;
                        } else if ("2".equals(xObj.a[num].prior)) {
                            numP3++;
                        } else if ("3".equals(xObj.a[num].prior)) {
                            numSel++;
                        }

                        slit[++nslit] = num;
                    }
                }
            }
        }

        /*
         *  Determine a weighted counter to return.
         */

        weight = numComp * 10000 + numP3 * 1000 + numSel;
        numSlit.val = nslit;
        numType.totalNum = nslit;
        numType.p1s = numComp;
        numType.p2s = numP3;
        numType.p3s = numSel;
        return (weight);
    }


//
//    /**
//     * place slits for the (N) Normal_Opt case
//     * This is max mode, so try to change slitsize_y to max it can go.
//     *
//     * @param num        (in) Number in array.
//     * @param maxSlit    (in) Array of indexes.
//     * @param startStrip (in) Y Coordinate of start of strip
//     * @param endStrip   (in) Y Coordinate of end of strip
//     * @param xObj       (in) objs being considered.
//     */
//    private void fixSlitLen(int num, int[] maxSlit, int startStrip, int endStrip,
//                            REF_CAT xObj) {
//        /*
//         * First need to sort the maxSlit array of index by y.
//         * Copy all the maxSlits to tmp, and then use the
//         * sort function to sort it.
//         */
//
//        OBJ_CAT[] tmp = new OBJ_CAT[num];	/* Temporary array.	*/
//        double s1, s2;			/* 2 Temp varaibles.	*/
//        double delta;			/* Diff between s2-s1   */
//
//        for (int i = 1; i <= num; i++) {
//            /*
//             * copy to tmp array, and save the index into xObj in the
//             * index field of each tmp object.
//             */
//            tmp[i] = xObj.a[maxSlit[i]];
//            tmp[i].index = maxSlit[i];
//        }
//        sort(num, tmp, 'y'); // XXX original had &tmp[1]!!!
//
//
//        /*
//         *  Now is sorted by Y.
//         *  Take the first item and extend the length of the slit right down
//         *  to the start of the strip.  Do this by adjusting slitpos_y and
//         *  slitsize_y.
//         */
//
//        s1 = startStrip + _dslit;
//        s2 = tmp[1].specY - tmp[1].len - _skyRegion;
//        delta = s2 - s1;
//        if (delta > 0) {
//            tmp[1].specY -= delta / 2;
//            tmp[1].len += delta / 2;
//        }
//
//
//        /*
//         *  Cycle thru
//         *  For each object in the tmp array...
//         */
//
//        recursiveLook(tmp, 1, endStrip, 1);
//    }
//
//    /**
//     * Look recursively for best slit lengthl
//     *
//     * @param tmp      (in) Temporary array ptr.
//     * @param incFlag  (in) Amount to incr, up or down.
//     * @param limit
//     * @param currItem
//     */
//    private void recursiveLook(OBJ_CAT[] tmp, int incFlag, int limit, int currItem) {
//        OBJ_CAT current = tmp[currItem];
//        for (int n = currItem + incFlag; n <= limit; n += incFlag) {
//            //  If the "used" flag has already been set, then skip it.
//            if (tmp[n].used != 0) {
//                continue;
//            }
//
//            //  Otherwise call the routine to hunt for any conflicting slits/spec's.
//            huntForLen(current, tmp[n]);   // XXX doesn't seem to do anything!!!
//        }
//    }

//    /**
//     * Hunt for next conflicting slit.
//     */
//    private void huntForLen(OBJ_CAT test, OBJ_CAT p) {
//
//        // XXX this method doesn't seem to do anything!!!
//
//        double xslit;
//        boolean overlap = false;
//        double tXslit;		/* Test center of x slit.		*/
//
//        /*
//         * Want to compare .
//         */
//
//        /*
//         *  First check to see if there is any object in the
//         *  same x as this objects.
//         *  XOverlap:
//         *  IF pLeft < testLeft-2 && pRight >= testLeft-2 ||
//         *  IF pLeft > testLeft-2 && pLeft  <= testRight+2 )
//         *  And if so, then adjust
//         *
//         *  YOverlap:
//         *  If pTop < testTop+2 && pTop > testBottom-2 ||
//         *  If pTop > testTop+2 && pBottom <= testTop+2
//         */
//        double pixelScale = _maskParams.getPixelScale();
//        xslit = p.x_ccd + (p.slitpos_x / pixelScale);
//        tXslit = test.x_ccd + (test.slitpos_x / pixelScale);
//
//        if ((xslit - p.width / 2 < tXslit - test.width / 2 - DSPECT
//                && xslit + p.width / 2 >= tXslit - test.width / 2 - DSPECT)
//                || (xslit - p.width / 2 > tXslit - test.width / 2 - DSPECT
//                && xslit - p.width / 2 <= tXslit + test.width / 2 + DSPECT)) {
//            if ((p.specY + p.len + _skyRegion < test.specY + test.len + _skyRegion + _dslit
//                    && p.specY + p.len + _skyRegion
//                    >= test.specY - test.len - _skyRegion - _dslit)
//                    || (p.specY + p.len + _skyRegion
//                    > test.specY + test.len + _skyRegion + _dslit
//                    && p.specY - p.len - _skyRegion
//                    <= test.specY + test.len + _skyRegion + _dslit)) {
//                /*
//                 *  There is X & Y overlap.
//                 */
//                overlap = true;
//            }
//        }
//
//        /*
//         *  Now check spectrum overlap.
//         *  If there is overlap in X AND in Y then skip it.
//         *  XOverlap:
//         *  IF pLeft < testLeft-2 && pRight >= testLeft-2 ||
//         *  IF pLeft > testLeft-2 && pLeft  <= testRight+2 )
//         *  YOverlap:
//         *  If pTop < testTop+2 && pTop > testBottom-2 ||
//         *  If pTop > testTop+2 && pBottom <= testTop+2
//         */
//        else if (((p.specX - _specLen / 2 < test.specX - _specLen / 2 - DSPECT
//                && p.specX + _specLen / 2 >= test.specX - _specLen / 2 - DSPECT)
//                || (p.specX - _specLen / 2 > test.specX - _specLen / 2 - DSPECT
//                && p.specX - _specLen / 2 <= test.specX + _specLen / 2 + DSPECT))
//                && ((p.specY + p.len + _skyRegion
//                < test.specY + test.len + _skyRegion + _dslit
//                && p.specY + p.len + _skyRegion
//                >= test.specY - test.len - _skyRegion - _dslit)
//                || (p.specY + p.len + _skyRegion
//                > test.specY + test.len + _skyRegion + _dslit
//                && p.specY - p.len - _skyRegion
//                <= test.specY + test.len + _skyRegion + _dslit))) {
//            overlap = true;
//        }
//
//
//        if (overlap) {
//            /*
//             * This one overlaps, so we will now look the otherway
//             *
//             recursiveLook( tmp, +1, endStrip, 1 );
//             */
//        }
//    }
}
