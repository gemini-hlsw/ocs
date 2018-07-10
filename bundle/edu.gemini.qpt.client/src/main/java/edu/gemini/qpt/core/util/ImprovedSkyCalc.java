package edu.gemini.qpt.core.util;

import static java.lang.Math.cos;

import java.util.Date;

import edu.gemini.spModel.core.Site;
import jsky.coords.WorldCoords;

/**
 * Improved version of SkyCalc that supports lunar calculations. All instance stuff is here;
 * the superclass is exclusively static stuff.
 * @author brighton, rnorris
 */
@SuppressWarnings("serial")
public final class ImprovedSkyCalc extends ImprovedSkyCalcMethods {

    // Site parameters
    private final double hoursLongitude;
    private final double degreesLatitude;
    private final double siteAltitude;

    // calculated results
    private double altitude;
    private double hourAngle;
    private double azimuth;
    private double parallacticAngle;
    private double airmass;
    private Double lunarSkyBrightness;
    private double lunarDistance;
    private float  lunarIlluminatedFraction;
    private double totalSkyBrightness;
    private double lunarPhaseAngle;
    private double sunAltitude;
    private double lunarElevation;

    // caching for calculate()
    private WorldCoords cachedCoordinates;
    private Date cachedDate;
    private boolean cachedCalculateMoon;

    public ImprovedSkyCalc(final Site site) {
        hoursLongitude = -site.longitude/15.;
        degreesLatitude = site.latitude;
        siteAltitude = site.altitude;
    }

    public void calculate(final WorldCoords obj, final Date date, final boolean calculateMoon) {

        // Early exit if the parameters haven't changed.
        if (obj.equals(cachedCoordinates) &&
            date.equals(cachedDate) &&
            calculateMoon == cachedCalculateMoon)
            return;

        cachedCoordinates = obj;
        cachedDate = date;
        cachedCalculateMoon = calculateMoon;

        final DateTime dateTime = new DateTime(date);
        final DoubleRef jdut = new DoubleRef();
        final DoubleRef sid = new DoubleRef();
        final DoubleRef curepoch = new DoubleRef();

        setup_time_place(dateTime, hoursLongitude, jdut, sid, curepoch);

        final double objra = obj.getRaDeg()/15;
        final double objdec = obj.getDecDeg();
        final double objepoch = 2000.;

        getCircumstances(objra, objdec, objepoch, curepoch.d, sid.d, degreesLatitude, jdut, calculateMoon);
    }

    private void getCircumstances(double objra, double objdec, double objepoch,
                                  double curep, double sid, double lat, DoubleRef jdut, boolean calculateMoon) {

        final double ha, alt;
        final DoubleRef az = new DoubleRef();
        final DoubleRef par = new DoubleRef();
        final DoubleRef curra = new DoubleRef();
        final DoubleRef curdec = new DoubleRef();

        cooxform(objra, objdec, objepoch, curep, curra, curdec, XFORM_JUSTPRE, XFORM_FROMSTD);

        ha = adj_time(sid - curra.d);
        alt = altit(curdec.d, ha, lat, az, par);

        airmass = getAirmass(alt);
        altitude = alt;
        azimuth = az.d;
        parallacticAngle = par.d;
        hourAngle = ha;

        if (calculateMoon) {

            final DoubleRef ramoon = new DoubleRef();
            final DoubleRef decmoon= new DoubleRef();
            final DoubleRef distmoon = new DoubleRef();
            final DoubleRef georamoon = new DoubleRef();
            final DoubleRef geodecmoon = new DoubleRef();
            final DoubleRef geodistmoon = new DoubleRef();
            final DoubleRef rasun = new DoubleRef();
            final DoubleRef decsun = new DoubleRef();
            final DoubleRef distsun = new DoubleRef();
            final DoubleRef x = new DoubleRef();
            final DoubleRef y = new DoubleRef();
            final DoubleRef z = new DoubleRef();
            final DoubleRef toporasun = new DoubleRef();
            final DoubleRef topodecsun = new DoubleRef();
            final double elevsea = siteAltitude;

            accusun(jdut.d,sid,degreesLatitude,rasun,decsun,distsun, toporasun,topodecsun,x,y,z);
            sunAltitude=altit(topodecsun.d,(sid-toporasun.d),degreesLatitude,az, new DoubleRef() /* [out] parang, ignored */);

            accumoon(jdut.d,degreesLatitude,sid,elevsea,georamoon,geodecmoon,geodistmoon, ramoon,decmoon,distmoon);
            lunarElevation=altit(decmoon.d,(sid-ramoon.d),degreesLatitude,az, new DoubleRef()  /* [out] parang, ignored */);

            // Sky brightness
            lunarSkyBrightness = null;
            lunarDistance = DEG_IN_RADIAN * subtend(ramoon.d,decmoon.d,objra,objdec);
            lunarPhaseAngle = DEG_IN_RADIAN * subtend(ramoon.d,decmoon.d,toporasun.d,topodecsun.d);
            if(lunarElevation > -2.) {
                if((lunarElevation > 0.) && (altitude > 0.5) && (sunAltitude < -9.)) {
                  lunarSkyBrightness =
                     lunskybright(lunarPhaseAngle,lunarDistance,KZEN,lunarElevation,
                        altitude,distmoon.d);
                }
            }
            totalSkyBrightness = sb(180. - lunarPhaseAngle, lunarDistance, 90 - lunarElevation, 90 - altitude, 90 - sunAltitude);
            lunarIlluminatedFraction=(float) (0.5*(1.-cos(subtend(ramoon.d,decmoon.d,rasun.d,decsun.d))));

        }
    }

    /**
     * Return the LST time for the given UT time at the given site.
     */
    public Date getLst(final Date date) {
        final DateTime dateTime = new DateTime(date);
        final double jd = date_to_jd(dateTime);
        final double lstHours = lst(jd, hoursLongitude);
        return getLst(lstHours, date);
    }

    public double getAltitude() {
        return altitude;
    }

    public double getAzimuth() {
        return azimuth;
    }

    public double getParallacticAngle() {
        return parallacticAngle;
    }

    public double getAirmass() {
        return airmass;
    }

    public double getHourAngle() {
        return hourAngle;
    }

    public float getLunarIlluminatedFraction() {
        return lunarIlluminatedFraction;
    }

    public Double getLunarSkyBrightness() {
        return lunarSkyBrightness;
    }

    public Double getTotalSkyBrightness() {
        return totalSkyBrightness;
    }

    public double getLunarPhaseAngle() {
        return lunarPhaseAngle;
    }
    public double getSunAltitude() {
        return sunAltitude;
    }

    public double getLunarDistance() {
        return lunarDistance;
    }

    public double getLunarElevation() {
        return lunarElevation;
    }

}

