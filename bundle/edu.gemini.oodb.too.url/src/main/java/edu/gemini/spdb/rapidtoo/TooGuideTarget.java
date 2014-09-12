//
// $Id: TooGuideTarget.java 270 2006-01-25 18:06:39Z shane $
//

package edu.gemini.spdb.rapidtoo;

/**
 * A description of the guide star position.
 */
public interface TooGuideTarget extends TooTarget {

    /**
     * Valid guide probe names.
     */
    enum GuideProbe {
        PWFS1, PWFS2, OIWFS, AOWFS
    }

    GuideProbe getGuideProbe();

    String getMagnitude();
}
