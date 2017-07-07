// Copyright 1997-2011
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id:$
//
package edu.gemini.spModel.gemini.calunit.smartgcal.keys;

import edu.gemini.shared.util.immutable.ImCollections;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.gemini.calunit.smartgcal.Calibration;

import java.io.Serializable;
import java.util.*;

/**
 * Wavelength range sets store a set of non-overlapping ranges and a list of calibrations for each of these
 * ranges. Before adding a range it is verified that it does not overlap with any of the already existing
 * ranges in order to avoid ambiguities.
 */
public final class WavelengthRangeSet implements Serializable {

    // set of range/calibration pairs
    private final Map<WavelengthRange, ImList<Calibration>> rangeMap = new HashMap<>();

    /**
     * Constructs a new empty wavelength range set.
     */
    public WavelengthRangeSet() {
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
        if (!rangeMap.containsKey(range)) {
            rangeMap.keySet().forEach(r -> {
                if (range.overlaps(r)) {
                    throw new IllegalArgumentException("range " + range + " overlaps with " + r);
                }
            });
        }

        rangeMap.compute(range, (r, l) -> (l == null) ? ImCollections.singletonList(c) : l.append(c));
    }

     /**
     * Finds the range for a wavelength.
     * @param value
     * @return
     */
    public Option<WavelengthRange> findRange(double value) {
        return lookup(value).map(t -> t.getKey());
    }

    /**
     * Finds the list of calibrations for a wavelength.
     * @param value
     * @return
     */
    public List<Calibration> findCalibrations(double value) {
        return lookup(value).map(t -> t.getValue().toList()).getOrElse(Collections.<Calibration>emptyList());
    }

    /**
     * Finds a range calibration pair for a wavelength.
     * @param value
     * @return
     */
    private Option<Map.Entry<WavelengthRange, ImList<Calibration>>> lookup(double value) {
        // There seems to be no find method so ...
        for (Map.Entry<WavelengthRange, ImList<Calibration>> me : rangeMap.entrySet()) {
            if (me.getKey().contains(value)) return ImOption.apply(me);
        }
        return ImOption.apply(null);
    }

    public Map<WavelengthRange, ImList<Calibration>> getRangeMap() {
        return Collections.unmodifiableMap(rangeMap);
    }
}
