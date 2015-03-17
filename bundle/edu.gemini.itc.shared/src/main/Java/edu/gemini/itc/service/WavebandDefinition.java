package edu.gemini.itc.service;

/**
 * This class represents the definition of standard "wavebands",
 * U, B, V, R, I, J, H, K, L',M',N,Q
 * Standard units will be nm.
 * All classes should use this class for waveband information rather
 * than re-implementing the definitions and possibly causing an error.
 * <p/>
 * Values for Sloan filters (g', r', i', z') taken from Fukugita et al. (1996)
 */
public enum WavebandDefinition {
    U("U",      360,    75),
    B("B",      440,    90),
    V("V",      550,    85),
    R("R",      670,   100),
    I("I",      870,   100),
    J("J",     1250,   240),
    H("H",     1650,   300),
    K("K",     2200,   410),
    L("L'",    3760,   700),
    M("M'",    4770,   240),
    N("N",    10470,  5230),
    Q("Q",    20130,  1650),
    g("g'",     483,    99),
    r("r'",     626,    96),
    i("i'",     767,   106),
    z("z'",     910,   125)
    ;

    public final String name;
    public final int center;
    public final int width;

    private WavebandDefinition(final String name, final int center, final int width) {
        this.name   = name;
        this.center = center;
        this.width  = width;
    }

    /** Returns the center of this waveband in nm. */
    public double getCenter() {
        return center;
    }

    /** Returns the width of this waveband in nm. */
    public double getWidth() {
        return width;
    }

    /** Returns the lower limit of this waveband in nm. */
    public double getStart() {
        return center - (width / 2);
    }

    /** Returns the upper limit of this waveband in nm. */
    public double getEnd() {
        return center + (width / 2);
    }
}





