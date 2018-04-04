package edu.gemini.spModel.ictd;

/**
 * Marks a type, typically a Java enum of some instrument feature, as a Set
 * whose elements are tracked in the ICTD.
 */
public interface IctdType {

    /**
     * Describes how to termine the availability of the associated instrument
     * feature.
     */
    IctdTracking ictdTracking();

}
