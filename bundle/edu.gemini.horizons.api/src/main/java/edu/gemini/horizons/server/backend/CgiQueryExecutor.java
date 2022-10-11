package edu.gemini.horizons.server.backend;

import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.spModel.core.Angle;
import edu.gemini.spModel.core.Angle$;
import edu.gemini.spModel.core.Site;

import edu.gemini.horizons.api.HorizonsException;
import edu.gemini.horizons.api.HorizonsQuery;
import edu.gemini.horizons.api.HorizonsReply;
import edu.gemini.horizons.api.IQueryExecutor;
import edu.gemini.util.ssl.GemSslSocketFactory;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;


/**
 * A CGI Query Executor. Implements the {@link edu.gemini.horizons.api.IQueryExecutor}
 * interface, allowing this executor to get an answer for a given
 * {@link edu.gemini.horizons.api.HorizonsQuery} using the CGI interface provided
 * by the JPL Horizons' service
 */
public enum CgiQueryExecutor implements IQueryExecutor {
    instance;

    private static final Logger LOG = Logger.getLogger(CgiQueryExecutor.class.getName());

    private static final int MINUTE_IN_MS = 60 * 1000;
    private static final int HOUR_IN_MS   = MINUTE_IN_MS * 60;


    // A data formatter suitable for get date elements in the format used in the JPL Horizons' service.
    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm:ss").withZone(ZoneId.of("UTC"));

    /**
     * Format the <code>date</object> in the same way that the JPL Horizons' service uses.
     *
     * @param date the <code>Date</code> to be formatted
     * @return String representation of the date, using the JPL Horizons' format
     */
    private static synchronized String _formatDate(final Date date) {
        return "'" + formatter.format(date.toInstant()) + "'";
    }

    /**
     * Executes the given query into the Horizons' service using its CGI interface
     *
     * @param query The Query to be performed in the Horizons' service
     * @return the reply from the Horizons' service for the given query
     */
    public HorizonsReply execute(final HorizonsQuery query) throws HorizonsException {
        final Map<String, String> queryParams = _initQueryParams();
        _initSite(query, queryParams);
        _initDateParams(query, queryParams);

        //if the object id contains spaces, surround the id in ' '
        final String objectId = query.getObjectId().trim();
        queryParams.put(CgiHorizonsConstants.COMMAND, objectId.contains(" ") ? "'" + objectId + "'" : objectId);

        if (query.getStepSize() > 0) {
            queryParams.put(CgiHorizonsConstants.STEP_SIZE, query.getStepSize() + query.getStepUnits().suffix);
        }

        try {
            // Build the parameter string and URL.
            final String charSet = "UTF-8";
            final StringBuilder queryParamString = new StringBuilder();
            boolean first = true;
            for (final Map.Entry<String, String> e: queryParams.entrySet()) {
                if (first) first = false;
                else queryParamString.append("&");
                queryParamString.append(e.getKey()).append("=").append(URLEncoder.encode(e.getValue(), charSet));
            }
            final URL url = new URL(CgiHorizonsConstants.HORIZONS_URL + "?" + queryParamString);
            LOG.info("Horizons request " + url);

            final HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setHostnameVerifier((v1,v2) -> true);
            conn.setSSLSocketFactory(GemSslSocketFactory.get());
            conn.setReadTimeout(MINUTE_IN_MS);
            conn.setRequestProperty("Accept-Charset", charSet);

            return CgiReplyBuilder.buildResponse(conn.getInputStream(), charSet);
        } catch (final IOException ex) {
            throw HorizonsException.create(ex);
        }
    }

    /**
     * Initialize the query arguments
     */
    private static Map<String, String> _initQueryParams() {
        final Map<String, String> queryParams = new HashMap<>();
        queryParams.put(CgiHorizonsConstants.BATCH, "1");
        queryParams.put(CgiHorizonsConstants.TABLE_TYPE, CgiHorizonsConstants.OBSERVER_TABLE);
        queryParams.put(CgiHorizonsConstants.CSV_FORMAT, CgiHorizonsConstants.NO);
        queryParams.put(CgiHorizonsConstants.EPHEMERIS, CgiHorizonsConstants.YES);
        queryParams.put(CgiHorizonsConstants.TABLE_FIELDS_ARG, CgiHorizonsConstants.TABLE_FIELDS);
        queryParams.put(CgiHorizonsConstants.CENTER, CgiHorizonsConstants.CENTER_COORD);
        queryParams.put(CgiHorizonsConstants.COORD_TYPE, CgiHorizonsConstants.COORD_TYPE_GEO);
        return queryParams;
    }

    /**
     * Initialize the site argument for the given query
     * @param query the query to be executed. Contains the site information
     */
    private void _initSite(final HorizonsQuery query, final Map<String, String> queryParams) {
        final Site site = query.getSite();
        queryParams.put(CgiHorizonsConstants.SITE_COORD, CgiHorizonsConstants.formatSiteCoord(site));
    }

    /**
     * Initialize the date arguments for the query. If no date arguments are
     * present, they will be the current time, and the current time plus one
     * hour.
     *
     * @param query The <code>HorizonsQuery</code> object with the query arguments
     */
    private void _initDateParams(final HorizonsQuery query, final Map<String, String> queryParams) {
        final Date startDate = ImOption.apply(query.getStartDate()).getOrElse(Date::new);
        final Date endDate   = ImOption.apply(query.getEndDate()).getOrElse(() -> new Date(startDate.getTime() + HOUR_IN_MS));
        queryParams.put(CgiHorizonsConstants.START_TIME, _formatDate(startDate));
        queryParams.put(CgiHorizonsConstants.STOP_TIME, _formatDate(endDate));
    }
}