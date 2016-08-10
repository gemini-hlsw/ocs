package edu.gemini.horizons.server.backend;

import edu.gemini.spModel.core.Site;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;

import edu.gemini.horizons.api.HorizonsException;
import edu.gemini.horizons.api.HorizonsQuery;
import edu.gemini.horizons.api.HorizonsReply;
import edu.gemini.horizons.api.IQueryExecutor;
import edu.gemini.horizons.api.HorizonsQuery.ObjectType;
import edu.gemini.horizons.api.HorizonsQuery.StepUnits;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;


// $Id: CgiQueryExecutor.java 895 2007-07-24 20:18:09Z anunez $
/**
 * A CGI Query Executor. Implements the {@link edu.gemini.horizons.api.IQueryExecutor}
 * interface, allowing this executor to get an answer for a given
 * {@link edu.gemini.horizons.api.HorizonsQuery} using the CGI interface provided
 * by the JPL Horizons' service
 */
public enum CgiQueryExecutor implements IQueryExecutor {
    instance;

    /**
     * A data formatter suitable for get date elements in the format used in
     * the JPL Horizons' service
     */
    private static DateFormat formatter = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss");
    static {
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /**
     * Format the <code>date</object> in the same way that the JPL Horizons' service uses.
     *
     * @param date the <code>Date</code> to be formatted
     * @return String representation of the date, using the JPL Horizons' format
     */
    private static synchronized String _formatDate(Date date) {
        StringBuilder sb = new StringBuilder("'");
        sb.append(formatter.format(date));
        sb.append("'");
        return sb.toString();
    }

    /**
     * Executes the given query into the Horizons' service using its CGI interface
     *
     * @param query The Query to be performed in the Horizons' service
     * @return the reply from the Horizons' service for the given query
     */
    public HorizonsReply execute(HorizonsQuery query) throws HorizonsException {
        final List<NameValuePair> queryParams = _initQueryParams();
        _initSite(query, queryParams);
        _initDateParams(query, queryParams);

        String objectId = query.getObjectId().trim();
        //For asteroids and minor planets, add ";" at the end of the query. This
        //makes the JPL/Horizons to narrow the results.
        if (query.getObjectType() == HorizonsQuery.ObjectType.MINOR_BODY) {
            objectId += ";"; //
        }

        //if the object id contains spaces, surround the id in ' '
        if (objectId.contains(" ")) {
            StringBuffer sb = new StringBuffer();
            sb.append("'");
            sb.append(objectId);
            sb.append("'");
            objectId = sb.toString();
        }

        queryParams.add(new NameValuePair(CgiHorizonsConstants.COMMAND, objectId));
        if (query.getStepSize() > 0) {
            queryParams.add(new NameValuePair(CgiHorizonsConstants.STEP_SIZE, query.getStepSize() + query.getStepUnits().suffix));
        }

        final GetMethod method = new GetMethod(CgiHorizonsConstants.HORIZONS_URL);
        method.setQueryString(_buildParameterList(queryParams));
        HorizonsReply reply;
        HttpClient client = new HttpClient();
        try {
            client.executeMethod(method);
            reply = CgiReplyBuilder.buildResponse(method.getResponseBodyAsStream(), method.getRequestCharSet());
            method.releaseConnection();
        } catch (IOException ex) {
            throw HorizonsException.create(ex);
        }
        return reply;
    }

    /**
     * Builds the list of parameters in the format required to perform the query to the CGI
     *
     * @return an array of <code>NameValuePair</code> based on the content of the
     *         parameters for the query.
     */
    private NameValuePair[] _buildParameterList(List<NameValuePair> queryParams) {
        final NameValuePair[] nvList = new NameValuePair[0];
        return queryParams.toArray(nvList);
    }

    /**
     * Initialize the query arguments
     */
    private static List<NameValuePair> _initQueryParams() {
        final List<NameValuePair> queryParams = new ArrayList<NameValuePair>();
        queryParams.add(new NameValuePair(CgiHorizonsConstants.BATCH, "1"));
        queryParams.add(new NameValuePair(CgiHorizonsConstants.TABLE_TYPE, CgiHorizonsConstants.OBSERVER_TABLE));
        queryParams.add(new NameValuePair(CgiHorizonsConstants.CSV_FORMAT, CgiHorizonsConstants.NO));
        queryParams.add(new NameValuePair(CgiHorizonsConstants.EPHEMERIS, CgiHorizonsConstants.YES));
        queryParams.add(new NameValuePair(CgiHorizonsConstants.TABLE_FIELDS_ARG, CgiHorizonsConstants.TABLE_FIELDS));
        queryParams.add(new NameValuePair(CgiHorizonsConstants.CENTER, CgiHorizonsConstants.CENTER_COORD));
        queryParams.add(new NameValuePair(CgiHorizonsConstants.COORD_TYPE, CgiHorizonsConstants.COORD_TYPE_GEO));
        return queryParams;
    }

    /**
     * Initialize the site argument for the given query
     * @param query the query to be executed. Contains the site information
     */
    private void _initSite(HorizonsQuery query, List<NameValuePair> queryParams) {
        queryParams.add(new NameValuePair(CgiHorizonsConstants.SITE_COORD,
                query.getSite() == Site.GN ? CgiHorizonsConstants.SITE_COORD_GN :
                        CgiHorizonsConstants.SITE_COORD_GS));
    }

    /**
     * Initialize the date arguments for the query. If no date arguments are
     * present, they will be the current time, and the current time plus one
     * hour.
     *
     * @param query The <code>HorizonsQuery</code> object with the query arguments
     */
    private void _initDateParams(HorizonsQuery query, List<NameValuePair> queryParams) {
        Date startDate = query.getStartDate();
        Date endDate = query.getEndDate();
        if (startDate == null) startDate = new Date();
        if (endDate == null) endDate = new Date(startDate.getTime() + 1000 * 60 * 60); //one hour later
        queryParams.add(new NameValuePair(CgiHorizonsConstants.START_TIME, _formatDate(startDate)));
        queryParams.add(new NameValuePair(CgiHorizonsConstants.STOP_TIME, _formatDate(endDate)));
    }

    public static void main(String[] args) {
        IQueryExecutor executor = CgiQueryExecutor.instance;
        HorizonsQuery query = new HorizonsQuery(Site.GN);

        long MS_PER_DAY = 1000 * 60 * 60 * 24;

        try {

        	{
	        	query.setObjectType(ObjectType.MINOR_BODY);
	        	query.setObjectId("606");
	        	query.setStartDate(new Date());
	        	query.setEndDate(new Date(System.currentTimeMillis() + MS_PER_DAY));

	        	HorizonsReply reply = executor.execute(query);
	        	System.out.println(reply);

	        	query.setSteps(60, StepUnits.SPACE_ARCSECONDS);
	        	reply = executor.execute(query);
	        	System.out.println(reply);

        	}

//        	{
//	        	query.setObjectId("-6");
//	        	query.setObjectType(ObjectType.MAJOR_BODY);
//	        	HorizonsReply reply = executor.execute(query);
//	        	reply = buildReply(replyToMap(reply));
//	        	System.out.println(reply);
//        	}
//
//        	{
//	        	query.setObjectId("606");
//	        	query.setObjectType(ObjectType.MAJOR_BODY);
//	        	HorizonsReply reply = executor.execute(query);
//	        	reply = buildReply(replyToMap(reply));
//	        	System.out.println(reply);
//        	}

//        	{
//	        	query.setObjectId("900033");
//	        	query.setObjectType(ObjectType.COMET);
//	        	HorizonsReply reply = executor.execute(query);
//	        	reply = buildReply(replyToMap(reply));
//	        	System.out.println(reply);
//        	}


//            query.setObjectId("ceres");
//            query.setStepSize(15);
//            HorizonsReply reply;
//            reply = executor.execute(query);
//            System.out.println("Ceres");
//            System.out.println(reply);
//
//            query.setObjectId("tempel 1"); //SPK ephemeris.
//            query.setStepSize(15);
//            reply = executor.execute(query);
//            System.out.println("Comet 9P");
//            System.out.println(reply);
//
//            query.setObjectId("301"); //moon
//            reply = executor.execute(query);
//            System.out.println("Moon");
//            System.out.println(reply);
//
//            query.setObjectId("RES"); //Multiple major bodies answer
//            reply = executor.execute(query);
//            System.out.println("Multiple Answer Query");
//            System.out.println(reply);
//
//            query.setObjectId("HALLEY"); //Multiple minor bodies answer
//            reply = executor.execute(query);
//            System.out.println("Halley");
//            System.out.println(reply);
//
//            query.setObjectId("-41"); //Mars Express Spacecraft
//            reply = executor.execute(query);
//            System.out.println("Mars Express Spacecraft");
//            System.out.println(reply);
//
//            query.setObjectId("-248"); //Venus Express Spacecraft
//            reply = executor.execute(query);
//            System.out.println("Venus Express Spacecraft");
//            System.out.println(reply);
//
//            query.setObjectId("XE"); //No matches
//            reply = executor.execute(query);
//            System.out.println("No matches Query");
//            System.out.println(reply);
//
//            query.setObjectId("x"); //invalid command
//            reply = executor.execute(query);
//            System.out.println("Invalid Query");
//            System.out.println(reply);
//
//            query.setObjectId("2003 UB313"); //2003 UB313
//            reply = executor.execute(query);
//            System.out.println("2003 UB313");
//            System.out.println(reply);
//
//
//            query.setObjectId("1"); //Ceres, by ID
//            query.setObjectType(HorizonsQuery.ObjectType.MINOR_BODY);
//            reply = executor.execute(query);
//            System.out.println("Ceres by ID (1)");
//            System.out.println(reply);

        } catch (HorizonsException ex) {
            ex.printStackTrace();
            if (ex.getType() != null)  {
                System.out.println(ex.getType().getDescription());
            }
        }

    }

}
