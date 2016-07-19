package edu.gemini.lchquery.servlet;

import edu.gemini.odb.browser.QueryResult;
import edu.gemini.pot.spdb.IDBDatabaseService;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import java.io.*;
import java.rmi.RemoteException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>A web service that allows for simple get requests with some
 * (optional) query parameters that replies with an XML containing the
 * program/observation/target data as defined in the attached
 * queryResultExample.xml</p>
 *
 * <p>The queries can have any combination of the following parameters, the
 * values should allow the same wildcards (*,?) and OR operators (||) as does
 * the OT browser and missing parameters equal to "Any":</p>
 *
 * programSemester
 * programTitle
 * programReference
 * programActive
 * programCompleted
 *
 * observationTooStatus
 * observationName
 * observationStatus
 * observationInstrument
 * observationAo
 *
 * <p>A typical query will be something like this (queries all GN observations
 * and targets for Semester 2012B with the given observation status and using
 * a LGS):</p>
 *
 * <p>
 * http://localhost:8296/lchquery?programSemester=2012B&programReference=GN*&observationStatus=Phase2|For Review|In Review|For Activation|On Hold|Ready|Ongoing*&observationAo=Altair + LGS
 * </p>
 *
 * <p>The program active and completed flags and the too status might be
 * important at a later stage, also the program title and observation name in
 * case they keep "marking" the engineering programs by using distinct
 * program and/or observation names.</p>
 *
 * <p>See LCH-63:</p>
*/
public final class LchQueryServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(LchQueryServlet.class.getName());

    private static final int ALL_OK = 200;
    private static final int INVALID_REQUEST = 400;
    private static final int SERVER_ERROR = 500;

    private final IDBDatabaseService odb;
    private final Set<Principal> user;

    public LchQueryServlet(IDBDatabaseService odb, Set<Principal> user) {
        this.odb = odb;
        this.user = user;
    }

    /**
     * Handles gets requests.
     * @param request
     * @param response
     * @throws IOException
     */
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {

        final OutputStream out = response.getOutputStream();
        final BufferedOutputStream bos = new BufferedOutputStream(out);

        final String pathInfo = request.getPathInfo();
        final LchQueryFunctor.QueryType queryType;
        if (pathInfo.startsWith("/programs")) {
            queryType = LchQueryFunctor.QueryType.PROGRAMS;
        } else if (pathInfo.startsWith("/observations")) {
            queryType = LchQueryFunctor.QueryType.OBSERVATIONS;
        } else if (pathInfo.startsWith("/targets")) {
            queryType = LchQueryFunctor.QueryType.TARGETS;
        } else {
            response.setStatus(INVALID_REQUEST);
            response.setContentType("text/plain");
            bos.write(("ERROR: Invalid query selector: " + pathInfo
                    + ". Should start with /programs, /observations or /targets").getBytes());
            bos.close();
            return;
        }

        final String programSemester = request.getParameter(LchQueryService.PARAMETER_PROGRAM_SEMESTER);
        final String programTitle = request.getParameter(LchQueryService.PARAMETER_PROGRAM_TITLE);
        final String programInvestigatorNames = request.getParameter(LchQueryService.PARAMETER_PROGRAM_INVESTIGATOR_NAMES);
        final String programPiEmail = request.getParameter(LchQueryService.PARAMETER_PROGRAM_PI_EMAIL);
        final String programCoIEmails = request.getParameter(LchQueryService.PARAMETER_PROGRAM_COI_EMAILS);
        final String programAbstract = request.getParameter(LchQueryService.PARAMETER_PROGRAM_ABSTRACT);
        final String programBand = request.getParameter(LchQueryService.PARAMETER_PROGRAM_BAND);
        final String programPartners = request.getParameter(LchQueryService.PARAMETER_PROGRAM_PARTNERS);
        final String programReference = request.getParameter(LchQueryService.PARAMETER_PROGRAM_REFERENCE);

        final String programActive = request.getParameter(LchQueryService.PARAMETER_PROGRAM_ACTIVE);
        final String programCompleted = request.getParameter(LchQueryService.PARAMETER_PROGRAM_COMPLETED);
        final String programNotifyPi = request.getParameter(LchQueryService.PARAMETER_PROGRAM_NOTIFY_PI);
        final String programRollover = request.getParameter(LchQueryService.PARAMETER_PROGRAM_ROLLOVER);
        final String programTooStatus = request.getParameter(LchQueryService.PARAMETER_PROGRAM_TOO_STATUS);
        final String programAllocTime = request.getParameter(LchQueryService.PARAMETER_PROGRAM_ALLOC_TIME);
        final String programRemainTime = request.getParameter(LchQueryService.PARAMETER_PROGRAM_REMAIN_TIME);

        final String observationTooStatus = request.getParameter(LchQueryService.PARAMETER_OBSERVATION_TOO_STATUS);
        final String observationName = request.getParameter(LchQueryService.PARAMETER_OBSERVATION_NAME);
        final String observationStatus = request.getParameter(LchQueryService.PARAMETER_OBSERVATION_STATUS);
        final String observationInstrument = request.getParameter(LchQueryService.PARAMETER_OBSERVATION_INSTRUMENT);
        final String observationAo = request.getParameter(LchQueryService.PARAMETER_OBSERVATION_AO);
        final String observationClass = request.getParameter(LchQueryService.PARAMETER_OBSERVATION_CLASS);

        try {
            _checkParameters(request);
            response.setStatus(ALL_OK);
            response.setContentType("application/xml");
            bos.write(query(queryType, programSemester, programTitle, programReference, programActive,
                    programCompleted, programNotifyPi, programRollover, observationTooStatus,
                    observationName, observationStatus, observationInstrument, observationAo, observationClass));
        } catch(IllegalArgumentException e) {
            response.setStatus(INVALID_REQUEST);
            response.setContentType("text/plain");
            bos.write(("ERROR: " + e.getMessage()).getBytes());
        } catch (RemoteException e) {
            writeException(response, bos, e, SERVER_ERROR);
        } catch (IOException e) {
            writeException(response, bos, e, SERVER_ERROR);
        } catch (PropertyException e) {
            writeException(response, bos, e, INVALID_REQUEST);
        } catch (JAXBException e) {
            writeException(response, bos, e, INVALID_REQUEST);
        } catch (Exception e) {
            writeException(response, bos, e, INVALID_REQUEST);
        } finally {
            bos.close();
        }
    }

    private void _checkParameters(HttpServletRequest request) {
        for(Enumeration e = request.getParameterNames(); e.hasMoreElements();) {
            String s = (String)e.nextElement();
            if (!LchQueryService.isValidParameter(s)) {
                throw new IllegalArgumentException("Invalid parameter: " + s);
            }
        }
    }

    /**
     * Handles exceptions and writes them to the log and as text output into the response.
     * @param response
     * @param bos
     * @param e
     * @throws IOException
     */
    private void writeException(HttpServletResponse response, BufferedOutputStream bos, Exception e, int status)
            throws IOException {
        LOG.log(Level.WARNING, "could not process request", e);
        response.setStatus(status);
        response.setContentType("text/plain");
        bos.write("ERROR: ".getBytes());
        bos.write(e.getClass().getName().getBytes());
        bos.write("\n\n".getBytes());
        if (e.getMessage() != null) {
            bos.write(e.getMessage().getBytes());
            bos.write("\n\n".getBytes());
        }
        PrintWriter writer = new PrintWriter(bos);
        e.printStackTrace(writer);
        writer.close();
    }

    private byte[] _toXml(QueryResult queryResult) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(QueryResult.class.getName().substring(0, QueryResult.class.getName().lastIndexOf(".")));
        Marshaller m = context.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        m.marshal(queryResult, outputStream);
        return outputStream.toByteArray();
    }

    private byte[] query(LchQueryFunctor.QueryType queryType,
                         String programSemester, String programTitle, String programReference,
                         String programActive, String programCompleted, String programNotifyPi, String programRollover,
                         String observationTooStatus, String observationName, String observationStatus,
                         String observationInstrument, String observationAo, String observationClass)
            throws IOException, JAXBException {

        LchQueryFunctor functor = new LchQueryFunctor(queryType, programSemester, programTitle, programReference,
                programActive, programCompleted, programNotifyPi, programRollover,
                observationTooStatus, observationName, observationStatus, observationInstrument, observationAo,
                observationClass);
        functor = odb.getQueryRunner(user).queryPrograms(functor);

        return _toXml(functor.getResult());
    }
}
