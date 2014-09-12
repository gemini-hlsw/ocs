package edu.gemini.horizons.api;

import jsky.coords.WorldCoords;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

//$Id: EphemerisEntry.java 630 2006-11-28 19:32:20Z anunez $
/**
 * Description of the Ephemeris for an object at a given UT Date.
 * If an {@link edu.gemini.horizons.api.HorizonsReply} has
 * ephemeris, then a collection of <code>EphemerisEntry</code>
 * objects are stored in that class.
 * @see edu.gemini.horizons.api.HorizonsReply
 */
public final class EphemerisEntry implements Serializable {
    /**
     * Ephemiris Date (UT).
     */
    private Date _date;
    /**
     * J2000.0 Astrometric RA and declination of target center, corrected for ligth time  <br/>
     * Units: HMS (HH MM SS.ff) and DMS (DD MM SS.f)
     */
    private WorldCoords _coordinates;

    /**
     * Rate of change of target center apparent RA (airless). <br/>
     * d(RA)/dt is multiplied by the cosine of the declination.  <br/>
     * Units: Arcseconds per hour
     */
    private double _raTrack;

    /**
     * Rate of change of target center apparent Dec (airless). <br/>
     * d(RA)/dt is multiplied by the cosine of the declination. <br/>
     * Units: Arcseconds per hour
     */
    private double _decTrack;

    /**
     * Relative optical airmass at target center point. Topocentric
     * EARTH sites, above horizon only. If not available, -1 is set. Unitless.
     */
    private double _airmass;

    /**
     * Approximate apparent visual magintude & surface brightness. A -1
     * value is output for phase angles greater than 120 degrees, since
     * the errors could be large and unknown.
     * <br/>
     * Units: None & Visual magnitudes per square arcsecond
     */
    private double _magnitude;

    /**
     * Constructor for an Ephemeris Entry.
     * @param date {@link java.util.Date} representing the UT Date of the Ephemeris
     * @param coords  J2000.0 Astrometric RA and declination of target center, corrected for ligth time
     * @param raTrack Rate of change of target center apparent Dec (airless)
     * @param decTrack Rate of change of target center apparent Dec
     * @param airmass Relative optical airmass at target center point
     * @param mag Approximate apparent visual magintude & surface brightness.
     */
    public EphemerisEntry(Date date, WorldCoords coords, double raTrack,
                          double decTrack, double airmass, double mag) {
        _date = date;
        _coordinates = coords;
        _raTrack = raTrack;
        _decTrack = decTrack;
        _airmass = airmass;
        _magnitude = mag;
    }

    /**
     * Get the Ephemeris UT date
     * @return {@link java.util.Date} representing the UT Date of the Ephemeris
     */
    public Date getDate() {
        return _date;
    }

    /**
     * Get the J2000.0 Astrometric RA and declination of target center, corrected for ligth time <br/>
     * Units: HMS (HH MM SS.ff) and DMS (DD MM SS.f)
     * @return a {@link jsky.coords.WorldCoords} object with the RA and Dec of the target center
     */
    public WorldCoords getCoordinates() {
        return _coordinates;
    }

    /**
     * Get the rate of change of target center apparent RA (airless) in arcseconds per hour
     * @return rate of change of target center apparent RA
     */
    public double getRATrack() {
        return _raTrack;
    }

    /**
     * Get the rate of change of target center apparent Dec (airless) in arcseconds per hour
     * @return rate of change of target center apparent Dec
     */
    public double getDecTrack() {
        return _decTrack;
    }

    /**
     * Get the relative optical airmass at target center point. If not available, -1 is returned
     * @return The relative optical airmas at target center point if available, -1 otherwise
     */
    public double getAirmass() {
        return _airmass;
    }

    /**
     * Get the approximate apparent visual magnitude & surface brightness. A -1
     * value is returned for phase angles greater than 120 degrees, since
     * the errors could be large and unknown.
     * <br/>
     * Units: None & Visual magnitudes per square arcsecond
     * @return The approximate apparent visual magnitude & surface brightnes if
     * phase angle is less that 120 degress, -1 otherwise.
     */
    public double getMagnitude() {
        return _magnitude;
    }


    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof EphemerisEntry)) return false;

        EphemerisEntry that = (EphemerisEntry)o;

        if (getCoordinates() == null) {
            if (that.getCoordinates() != null) return false;
        } else {
            if (!getCoordinates().equals(that.getCoordinates())) return false;
        }

        if (getDate() == null) {
            if (that.getDate() != null) return false;
        } else {
            if (!getDate().equals(that.getDate())) return false;
        }

        return that._airmass == this._airmass &&
                that._decTrack == this._decTrack &&
                that._raTrack == this._raTrack &&
                that._magnitude == this._magnitude;
    }

    public int hashCode() {
        int hash = Double.valueOf(_airmass).hashCode();
        hash = 31*hash + Double.valueOf(_magnitude).hashCode();
        hash = 31*hash + Double.valueOf(_decTrack).hashCode();
        hash = 31*hash + Double.valueOf(_raTrack).hashCode();

        if (getCoordinates() != null) {
            hash = 31*hash + getCoordinates().hashCode();
        }

        if (getDate() != null) {
            hash = 31*hash + getDate().hashCode();
        }

        return hash;
    }

    public String toString() {
        DateFormat formatter = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss z");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        StringBuilder sb = new StringBuilder();
        sb.append(formatter.format(_date));sb.append("\t");
        sb.append(_coordinates.getRA());sb.append("\t");
        sb.append(_coordinates.getDec());sb.append("\t");
        sb.append(_raTrack);sb.append("\t");
        sb.append(_decTrack);sb.append("\t");
        sb.append(_airmass);sb.append("\t");
        sb.append(_magnitude);
        return sb.toString();
    }
}
