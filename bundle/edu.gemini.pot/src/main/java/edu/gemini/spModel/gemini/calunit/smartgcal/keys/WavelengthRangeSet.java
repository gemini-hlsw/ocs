// Copyright 1997-2011
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id:$
//
package edu.gemini.spModel.gemini.calunit.smartgcal.keys;

import edu.gemini.spModel.gemini.calunit.smartgcal.Calibration;

import java.io.Serializable;
import java.util.*;

/**
 * Wavelength range sets store a set of non-overlapping ranges and a list of calibrations for each of these
 * ranges. Before adding a range it is verified that it does not overlap with any of the already existing
 * ranges in order to avoid disambiguities.
 */
public class WavelengthRangeSet implements Serializable {

    /**
     * Internal object for linking a wavelength range to a list of calibrations.
     */
    private final class RangeCalibrationTuple {
        private final WavelengthRange range;
        private final List<Calibration> calibrations;
        public RangeCalibrationTuple(WavelengthRange range) {
            this.range = range;
            this.calibrations = new ArrayList<Calibration>();
        }
    }

    // set of range/calibration pairs
    private final List<RangeCalibrationTuple> ranges;

    /**
     * Constructs a new empty wavelength range set.
     */
    public WavelengthRangeSet() {
        ranges = new ArrayList<RangeCalibrationTuple>();
    }

    /**
     * Adds a calibration for a given range.
     * If the range exists already, the calibration will be added to the existing range.
     * For new ranges it is verified that the new range does not overlap with any of the existing ranges and
     * then it is added with the calibration as its first calibration entry.
     * @param range
     * @param c
     */
    public void add(WavelengthRange range, Calibration c) {
        RangeCalibrationTuple existing = findTuple(range);
        if (existing == null) {
            for (RangeCalibrationTuple t : ranges) {
                if (range.getMin() < t.range.getMax() && range.getMax() >= t.range.getMin()) {
                    throw new IllegalArgumentException("range " +  range + " overlaps with " + t.range);
                }
            }
            existing = new RangeCalibrationTuple(range);
            ranges.add(existing);
        }
        existing.calibrations.add(c);
    }

     /**
     * Finds the range for a wavelength.
     * @param value
     * @return
     */
    public WavelengthRange findRange(double value) {
        RangeCalibrationTuple t = findTuple(value);
        if (t != null) {
            return t.range;
        } else {
            return null;
        }
    }

    /**
     * Finds the list of calibrations for a wavelength.
     * @param value
     * @return
     */
    public List<Calibration> findCalibrations(double value) {
        RangeCalibrationTuple t = findTuple(value);
        if (t != null) {
            return t.calibrations;
        } else {
            return new ArrayList<Calibration>();
        }
    }

    /**
     * Finds a range calibration pair for a range.
     * @param r
     * @return
     */
    private RangeCalibrationTuple findTuple(WavelengthRange r) {
        for (RangeCalibrationTuple t : ranges) {
            if (t.range.equals(r)) {
                return t;
            }
        }
        return null;
    }

    /**
     * Finds a range calibration pair for a wavelength.
     * @param value
     * @return
     */
    private RangeCalibrationTuple findTuple(double value) {
        for (RangeCalibrationTuple t : ranges) {
            if (value >= t.range.getMin() && value < t.range.getMax()) {
                return t;
            }
        }
        return null;
    }

}
