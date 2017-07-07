// Copyright 2011 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id$

package edu.gemini.spModel.gemini.calunit.smartgcal;

import edu.gemini.shared.util.immutable.DefaultImList;
import edu.gemini.shared.util.immutable.ImList;
import edu.gemini.spModel.gemini.calunit.CalUnitParams;
import edu.gemini.spModel.type.SpTypeUtil;

import java.util.*;

/**
 * Implementation class for Calibration interface.
 * Note that in order to reduce the memory footprint of the smart calibrations this code tries to reuse calibrations
 * where possible using the disjointCalibrations hash map to fish for duplicates. This allows to reduce the number
 * of calibration objects that have to be held in memory by a factor of 10 (8000 down to 800). Use the parse() factory
 * method if you want to take advantage of this.
 */
public final class CalibrationImpl implements Calibration {

    private static Map<CalibrationImpl, CalibrationImpl> disjointCalibrations = new HashMap<CalibrationImpl, CalibrationImpl>();

    /**
     * Possible values for base calibration setting
     */
    public static enum Basecal {
        DAY,
        NIGHT,
        NONE
    }

    /**
     * Enumeration of all possible values for a Gemini calibration.
     * The string representation of these value will be used to refer to the column name in the calibration data file.
     */
    public static enum Values implements ConfigurationKey.Values {
        LAMPS("Calibration Lamps"),
        SHUTTER("Calibration Shutter"),
        FILTER("Calibration Filter"),
        DIFFUSER("Calibration Diffuser"),
        OBSERVE("Calibration Observe"),
        EXPOSURE_TIME("Calibration Exposure Time"),
        COADDS("Calibration Coadds"),
        BASECAL("Calibration Basecal");

        private String name;

        private Values(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    private final CalUnitParams.Shutter shutter;
    private final CalUnitParams.Filter filter;
    private final CalUnitParams.Diffuser diffuser;
    private final int observe;
    private final double exposureTime;
    private final int coadds;

    private final Set<CalUnitParams.Lamp> lamps;
    private final boolean isArc;
    private final Set<Basecal> basecals;

    /**
     * Parses and creates a calibration from values passed in as string properties.
     * Use this method when creating calibrations while digesting calibration tables. This factory method will
     * reuse any already existing calibration objects in order to reduce the memory footprint of the smart
     * calibrations.
     * @param properties
     * @return
     */
    public synchronized static Calibration parse(Properties properties) {
        final CalibrationImpl calibration = new CalibrationImpl(properties);
        if (!disjointCalibrations.containsKey(calibration)) {
            disjointCalibrations.put(calibration, calibration);
        }
        return disjointCalibrations.get(calibration);
    }

    private CalibrationImpl(Properties properties) {
        this.lamps = parseLamps(properties.getProperty(Values.LAMPS.toString()));
        this.isArc = isArc(lamps);
        this.basecals = parseBasecals(properties.getProperty(Values.BASECAL.toString()));
        this.shutter = getValue(CalUnitParams.Shutter.class, properties, Values.SHUTTER);
        this.filter = getValue(CalUnitParams.Filter.class, properties, Values.FILTER);
        this.diffuser = getValue(CalUnitParams.Diffuser.class, properties, Values.DIFFUSER);
        this.observe = Integer.parseInt(properties.getProperty(Values.OBSERVE.toString()));
        this.exposureTime = Double.parseDouble(properties.getProperty(Values.EXPOSURE_TIME.toString()));
        this.coadds = Integer.parseInt(properties.getProperty(Values.COADDS.toString()));
    }

    /**
     * Creates a calibration.
     * Use this for testing only, use parse method when reading calibration tables in order to re-use calibrations.
     * @param lamps
     * @param shutter
     * @param filter
     * @param diffuser
     * @param observe
     * @param exposureTime
     * @param coadds
     * @param basecals
     */
    public CalibrationImpl(
            CalUnitParams.Lamp[] lamps,
            CalUnitParams.Shutter shutter,
            CalUnitParams.Filter filter,
            CalUnitParams.Diffuser diffuser,
            int observe,
            float exposureTime,
            int coadds,
            Basecal[] basecals) {
        this(
                new HashSet<CalUnitParams.Lamp>(Arrays.asList(lamps)),
                shutter, filter, diffuser, observe, exposureTime, coadds,
                new HashSet<Basecal>(Arrays.asList(basecals))
        );
    }

    /**
     * Creates a calibration.
     * Use this for testing only, use parse method when reading calibration tables in order to re-use calibrations.
     * @param lamps
     * @param shutter
     * @param filter
     * @param diffuser
     * @param observe
     * @param exposureTime
     * @param coadds
     * @param basecals
     */
    private CalibrationImpl(
            Set<CalUnitParams.Lamp> lamps,
            CalUnitParams.Shutter shutter,
            CalUnitParams.Filter filter,
            CalUnitParams.Diffuser diffuser,
            int observe,
            float exposureTime,
            int coadds,
            Set<Basecal> basecals) {

        this.lamps = lamps;
        this.isArc = isArc(lamps);
        this.shutter = shutter;
        this.filter = filter;
        this.diffuser = diffuser;
        this.observe = observe;
        this.exposureTime = exposureTime;
        this.coadds = coadds;
        this.basecals = basecals;
    }

    private <T extends Enum<T>> T getValue(Class<T> c, Properties properties, Values name) {
        String valueString = properties.getProperty(name.toString());
        if (valueString == null) {
            throw new IllegalArgumentException("value for '" + name + "' is missing");
        }
        T value = SpTypeUtil.displayValueToEnum(c, valueString);
        if (value == null) {
            throw new IllegalArgumentException("illegal value for '" + name + "': '" + valueString +"'");
        }
        return value;
    }

    private static Set<CalUnitParams.Lamp> parseLamps(String lampsString) {
        // create a set with all lamps from the string
        final Set<CalUnitParams.Lamp> lamps = new HashSet<CalUnitParams.Lamp>();
        StringTokenizer tokenizer = new StringTokenizer(lampsString, "+");
        while (tokenizer.hasMoreTokens()) {
            String lampName = tokenizer.nextToken().trim();
            CalUnitParams.Lamp lamp = CalUnitParams.Lamp.getLamp(lampName, null);
            if (lamp == null) {
                throw new RuntimeException("unknown lamp " + lampName);
            }
            lamps.add(lamp);
        }

        // do some additional checks...
        return lamps;
    }

    private static boolean isArc(Set<CalUnitParams.Lamp> lamps) {
        // check some conditions before setting the lamps
        boolean hasArcs = false;
        boolean hasFlats = false;
        for (CalUnitParams.Lamp lamp : lamps) {
            if (lamp.isArc()) {
                hasArcs = true;
            } else {
                hasFlats = true;
            }
        }

        if (!hasArcs && !hasFlats) {
            throw new RuntimeException("at least one lamp must be selected");
        }
        if (hasArcs && hasFlats) {
            throw new RuntimeException("mixing of arcs and flats is not allowed");
        }
        if (hasFlats && lamps.size() > 1) {
            throw new RuntimeException("only one flat lamp at a time allowed");
        }

        return hasArcs;
    }

    private static Set<Basecal> parseBasecals(String basecalsString) {
        // create a set with all lamps from the string
        Set<Basecal> basecals = new HashSet<Basecal>();
        StringTokenizer tokenizer = new StringTokenizer(basecalsString, "+");
        while (tokenizer.hasMoreTokens()) {
            String basecalName = tokenizer.nextToken().trim();
            Basecal basecal = Enum.valueOf(Basecal.class, basecalName.toUpperCase());
            basecals.add(basecal);
        }
        return basecals;
    }

    @Override
    public Set<CalUnitParams.Lamp> getLamps() {
        return lamps;
    }

    @Override
    public CalUnitParams.Shutter getShutter() {
        return shutter;
    }

    @Override
    public CalUnitParams.Filter getFilter() {
        return filter;
    }

    @Override
    public CalUnitParams.Diffuser getDiffuser() {
        return diffuser;
    }

    @Override
    public Integer getObserve() {
        return observe;
    }

    @Override
    public Double getExposureTime() {
        return exposureTime;
    }

    @Override
    public Integer getCoadds() {
        return coadds;
    }

    @Override
    public Boolean isFlat() {
        return !isArc;
    }

    @Override
    public Boolean isArc() {
        return isArc;
    }

    @Override
    public Boolean isBasecalNight() {
        return basecals.contains(Basecal.NIGHT);
    }

    @Override
    public Boolean isBasecalDay() {
        return basecals.contains(Basecal.DAY);
    }

    @Override
    public int hashCode() {
        final long expTimeBits = Double.doubleToLongBits(exposureTime);

        return
            shutter.hashCode() +
            filter.hashCode() +
            diffuser.hashCode() +
            (int)(expTimeBits ^ (expTimeBits >>> 32)) +
            13*coadds +
            17*observe +
            (isArc ? 13 : 17);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CalibrationImpl)) {
            return false;
        }

        CalibrationImpl other = (CalibrationImpl)o;
        if (!(this.shutter.equals(other.shutter))) return false;
        if (!(this.filter.equals(other.filter))) return false;
        if (!(this.diffuser.equals(other.diffuser))) return false;
        if (this.observe != other.observe) return false;
        if (this.exposureTime != other.exposureTime) return false;
        if (this.coadds != other.coadds) return false;
        if (!(this.lamps.equals(other.lamps))) return false;
        if (this.isArc != other.isArc) return false;
        return this.basecals.equals(other.basecals);

    }

    @Override
    public ImList<String> export() {
        return DefaultImList.create(
                Integer.toString(observe),
                filter.sequenceValue(),
                diffuser.sequenceValue(),
                DefaultImList.create(lamps).map(l -> l.sequenceValue()).mkString("", ";", ""),
                shutter.sequenceValue(),
                Long.toString(Math.round(exposureTime * 1000)),
                Integer.toString(coadds),
                DefaultImList.create(basecals).map(b -> b.name()).mkString("", ";", ""));
    }
}
