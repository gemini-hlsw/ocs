package edu.gemini.dbTools.weather;

import edu.gemini.pot.sp.ISPNightlyRecord;
import edu.gemini.pot.client.SPDB;
import edu.gemini.pot.spdb.DBIDClashException;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.data.ISPDataObject;
import edu.gemini.spModel.gemini.plan.NightlyRecord;
import edu.gemini.spModel.gemini.plan.WeatherInfo;
import edu.gemini.spModel.util.NightlyProgIdGenerator;
import edu.gemini.spdb.cron.Storage;
import edu.gemini.weather.IWeatherBean;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import java.security.Principal;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public final class WeatherUpdater {

    private final BundleContext context;

    public WeatherUpdater(final BundleContext context) {
        this.context = context;
    }

    private static String format(final Double d) {
        return String.format("%4.2f", d);
    }

    private static WeatherInfo createWeatherInfo(final IWeatherBean wb) {
        final WeatherInfo info = new WeatherInfo();
        info.setDimm(format(wb.getDimm()));
        info.setRelativeHumidity(format(wb.getHumidity()));
        info.setBarometricPressure(format(wb.getPressure()));
        info.setTemperature(format(wb.getTemperature()));
        info.setWindDirection(format(wb.getWindDirection()));
        info.setWindSpeed(format(wb.getWindSpeed()));
        info.setWaterVapor(format(wb.getWaterVapor()));
        info.setTime(System.currentTimeMillis());
        return info;
    }

    private static String dumpWeatherInfo(final WeatherInfo wi) {
        final StringBuilder buf = new StringBuilder();
        buf.append("\ndimm           = ").append(wi.getDimm());
        buf.append("\nhumidity       = ").append(wi.getRelativeHumidity());
        buf.append("\npressure       = ").append(wi.getBarometricPressure());
        buf.append("\ntemperature    = ").append(wi.getTemperature());
        buf.append("\nwind direction = ").append(wi.getWindDirection());
        buf.append("\nwind speed     = ").append(wi.getWindSpeed());
        buf.append("\nwater vapor    = ").append(wi.getWaterVapor());
        return buf.toString();
    }

    private static ISPNightlyRecord getOrCreateNightlyRecord(final Logger log) throws DBIDClashException {
        final Site site = Site.currentSiteOrNull;
        if (site != null) {
            final SPProgramID planId = NightlyProgIdGenerator.getPlanID(site);
            log.info("Fetching plan ID: " + planId);
            final IDBDatabaseService db = SPDB.get();
            final ISPNightlyRecord plan = db.lookupNightlyRecordByID(planId);
            return (plan != null) ? plan : createNightlyRecord(log, planId, db);
        } else {
            throw new IllegalStateException("site is not set; can't continue");
        }
    }

    private static ISPNightlyRecord createNightlyRecord(final Logger log, final SPProgramID planId, final IDBDatabaseService db) throws DBIDClashException {
        log.info("Not found, creating nightly record: " + planId);
        final ISPNightlyRecord record = db.getFactory().createNightlyRecord(null, planId);
        final ISPDataObject dObj = record.getDataObject();
        dObj.setTitle(planId.stringValue());
        record.setDataObject(dObj);
        db.put(record);
        return record;
    }

    public void run(final Storage.Temp temp, final Storage.Perm perm, final Logger log, final Map<String, String> env, Set<Principal> user) throws DBIDClashException {
        final ServiceReference<IWeatherBean> ref = context.getServiceReference(IWeatherBean.class);
        if (ref != null) {
            final IWeatherBean wb = context.getService(ref);
            try {
                if (wb.isConnected()) {
                    final WeatherInfo winfo = createWeatherInfo(wb);
                    final ISPNightlyRecord record = getOrCreateNightlyRecord(log);
                    final NightlyRecord obsLog = (NightlyRecord) record.getDataObject();
                    obsLog.addWeatherInfo(winfo);
                    record.setDataObject(obsLog);
                    log.info("updated weather: " + dumpWeatherInfo(winfo));
                } else {
                    log.warning("weather service is available not not connnected. can't continue.");
                }
            } finally {
                context.ungetService(ref);
            }
        } else {
            log.warning("no weather service available. can't continue.");
        }
    }

}
