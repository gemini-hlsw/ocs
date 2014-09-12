package edu.gemini.smartgcal.servlet;

import edu.gemini.smartgcal.servlet.repository.CalibrationSubversionRepository;
import edu.gemini.spModel.gemini.calunit.smartgcal.Calibration;
import edu.gemini.spModel.gemini.calunit.smartgcal.CalibrationFile;
import edu.gemini.spModel.gemini.calunit.smartgcal.SmartGcalService;
import edu.gemini.spModel.gemini.calunit.smartgcal.Version;
import edu.gemini.spModel.smartgcal.CalibrationMapReader;
import edu.gemini.spModel.smartgcal.repository.CalibrationFileCache;
import edu.gemini.spModel.smartgcal.repository.CalibrationResourceRepository;
import edu.gemini.spModel.smartgcal.repository.CalibrationUpdater;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A simple and very basic servlet for implementing the SmartGcal backend services.
 * Functionality is very limited: validating, uploading and downloading calibration
 * files and receiving a list of the newest available calibration versions for the
 * different instruments.
 *
 * Valid requests are:
 * <ul>
 *  <li>http://localhost:8888/gcal?command=validate&type=ARC&instrument=GNIRS</li>
 *  <li>http://localhost:8888/gcal?command=upload&type=ARC&instrument=GNIRS</li>
 *  <li>http://localhost:8888/gcal?command=download&type=ARC&instrument=GNIRS</li>
 *  <li>http://localhost:8888/gcal?command=versions[&instrument=GNIRS][&type=FLAT]</li>
 *  <li>http://localhost:8888/gcal?command=updatecache</li>
 *  <li>http://localhost:8888/gcal?command=clearcache</li>
 * </ul>
 */
public class SmartGcalConfigServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(SmartGcalConfigServlet.class.getName());

    private final CalibrationSubversionRepository svnRepository;
    private final CalibrationFileCache cachedRepository;
    private final String uploadPassword;
    private final String fileCachePath;

    public SmartGcalConfigServlet(String svnRootUrl, String svnUser, String svnPassword, String uploadPassword, String fileCachePath) {
        // NOTE: currently everything is set up to use a file cache, if this is not the case anymore just use
        // cacheRepository = new CalibrationFileCache(new File(fileCachePath), svnRepository) to access svn directly
        // and remove the lines below that update the file cache (not needed anymore)
        this.svnRepository = new CalibrationSubversionRepository(svnRootUrl, svnUser, svnPassword);
        this.cachedRepository = new CalibrationFileCache(new File(fileCachePath), new CalibrationResourceRepository());
        this.uploadPassword = uploadPassword;
        this.fileCachePath = fileCachePath;

        //update file cache
        // NOTE1: the file cache is here to avoid too much traffic on the SVN
        // NOTE2: this can be *very* slow when accessing the SVN repository in the South from the North. I recommend to
        // use a local svn repository for development and testing (especially for programmers based in the North).

        // RCN: do this on a worker
        new Thread("Smart GCAL initial update worker.") {
            public void run() {
                try {
                    LOG.info("Updating smart GCAL on a worker...");
                    CalibrationUpdater.getInstance().update(cachedRepository, svnRepository);
                    LOG.info("Smart GCAL update worker exiting.");
                } catch (Throwable t) {
                    LOG.log(Level.WARNING, "Trouble during initial smart GCAL update.", t);
                }
            }
        }.start();
    }

    /**
     * Handles gets requests.
     * @param request
     * @param response
     * @throws IOException
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        String command      = request.getParameter(SmartGcalService.PARAMETER_COMMAND);
        String typeString   = request.getParameter(SmartGcalService.PARAMETER_TYPE);
        String instrument   = request.getParameter(SmartGcalService.PARAMETER_INSTRUMENT);
        String versioned    = request.getParameter(SmartGcalService.PARAMETER_VERSIONED);
        Calibration.Type type = (typeString != null) ? Calibration.Type.valueOf(typeString) : null;

        OutputStream out = response.getOutputStream();
        BufferedOutputStream bos = new BufferedOutputStream(out);

        try {
            // -- handle DOWNLOAD command
            if (SmartGcalService.COMMAND_DOWNLOAD.equals(command)) {

                 checkInstrument(instrument);
                LOG.log(Level.FINE, "doGet(), command="+ SmartGcalService.COMMAND_DOWNLOAD);
                downloadCalibrationsFile(response, bos, type, instrument, versioned);

            // -- handle UPLOAD command
            } else if (SmartGcalService.COMMAND_UPLOAD.equals(command)) {

                checkInstrument(instrument);
                LOG.log(Level.INFO, "doGet(), command="+ SmartGcalService.COMMAND_UPLOAD);
                uploadCalibrationsFile(response, bos, type, instrument, SmartGcalService.COMMAND_UPLOAD);

            // -- handle VALIDATE command
            } else if (SmartGcalService.COMMAND_VALIDATE.equals(command)) {

                checkInstrument(instrument);
                LOG.log(Level.FINE, "doGet(), command="+ SmartGcalService.COMMAND_VALIDATE);
                uploadCalibrationsFile(response, bos, type, instrument, SmartGcalService.COMMAND_VALIDATE);

            // -- handle VERSIONS command
            } else if (SmartGcalService.COMMAND_VERSIONS.equals(command)) {

                checkInstrument(instrument);
                LOG.log(Level.FINE, "doGet(), command="+ SmartGcalService.COMMAND_VERSIONS);
                returnVersions(response, bos, instrument, type);

            // -- handle UPDATE command
            } else if (SmartGcalService.COMMAND_UPDATE_CACHE.equals(command)) {

                LOG.log(Level.INFO, "doGet(), command="+ SmartGcalService.COMMAND_UPDATE_CACHE);
                CalibrationUpdater.instance.update(cachedRepository, svnRepository);
                returnVersions(response, bos, null, null);

            // -- handle CLEAR CACHE command
            } else if (SmartGcalService.COMMAND_CLEAR_CACHE.equals(command)) {
                // clearing the cache also involves loading the newest versions from the repository
                // this command is meant to be used for testing and maybe for emergency administration
                LOG.log(Level.INFO, "doGet(), command="+ SmartGcalService.COMMAND_CLEAR_CACHE);
                cachedRepository.clear();
                CalibrationUpdater.getInstance().update(cachedRepository, svnRepository);
                bos.write(("cleared and updated cache in " + fileCachePath + "\n").getBytes());

            // -- unknown command
            } else {
                showMenu(request.getRequestURL(), response, bos);
            }

        } catch (Exception e) {
            writeException(response, bos, e);
        } finally {
            bos.close();
        }

    }

    /**
     * Handles post requests as an answer to upload and validate commands.
     * The response is a simple plain text message that either says "OK" or gives a list of errors that
     * were found when validating the file.
     * @param request
     * @param response
     * @throws IOException
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

        String command = request.getParameter(SmartGcalService.PARAMETER_COMMAND);
        String password = "";

        response.setContentType("text/plain");
        OutputStream out = response.getOutputStream();
        BufferedOutputStream bos = new BufferedOutputStream(out);

        LOG.log(Level.FINE, "doPost()");

        boolean isMultipart = ServletFileUpload.isMultipartContent(request);

         if (isMultipart) {
             try {
                 String instrument = request.getParameter(SmartGcalService.PARAMETER_INSTRUMENT);
                 Calibration.Type type = Calibration.Type.valueOf(request.getParameter(SmartGcalService.PARAMETER_TYPE));
                 checkInstrument(instrument);

                 // Create a new file upload handler
                 ServletFileUpload upload = new ServletFileUpload();
                 // Parse the request
                 FileItemIterator iter = upload.getItemIterator(request);
                 ByteArrayOutputStream data = new ByteArrayOutputStream(100000);

                 // read information from data that was sent to server
                 while (iter.hasNext()) {
                     FileItemStream item = iter.next();
                     String name = item.getFieldName();
                     InputStream stream = item.openStream();
                     // currently there are only two fields, so we don't care about their names...
                     if (item.isFormField()) {
                         // -- this must be the password
                         password = Streams.asString(stream);
                     } else {
                         // -- this must be the data
                         byte[] buffer = new byte[10000];
                         int read;
                         while ((read = stream.read(buffer)) > 0) {
                             data.write(buffer, 0, read);
                         }
                     }
                 }

                 // step 1: validate input file
                 if (validateCalibrationsFile(data.toByteArray(), instrument, bos)) {
                     if (SmartGcalService.COMMAND_UPLOAD.equals(command)) {
                         // step 2 in case of successful validation and the command was upload: commit the file
                         commitCalibrationsFile(data.toByteArray(), type, instrument, password, bos);
                     }
                     bos.write("\n\n\n".getBytes());
                     bos.write(data.toByteArray());
                 }

             } catch (Exception e) {
                 writeException(response, bos, e);
             }
         }

        bos.close();
    }

    /**
     * Validates a calibration file and write an "ok" message or a list of errors to the response.
     * @param data
     * @param instrument
     * @param bos
     * @return
     * @throws IOException
     */
    private boolean validateCalibrationsFile(byte[] data, String instrument, BufferedOutputStream bos) throws IOException {
        CalibrationFile file = new CalibrationFile(new Version(0, new Date()), new String(data));
        List<String> errors = CalibrationMapReader.validateData(instrument, file);
        if (errors.size() == 0) {
            // reply with ok message
            bos.write("validation of file was successful, no errors detected\n".getBytes());
            return true;
        } else {
            // reply with list of errors in case file is not valid
            bos.write("errors detected in file, validation failed\n".getBytes());
            bos.write("\n\n\n".getBytes());
            for (String error : errors) {
               bos.write(error.getBytes());
               bos.write("\n".getBytes());
            }
            return false;
        }
    }

    /**
     * Commits a calibration file; the password is checked and a "ok" message or a error message is written to the
     * response.
     * @param data
     * @param instrument
     * @param password
     * @param bos
     * @throws IOException
     */
    private synchronized void commitCalibrationsFile(byte[] data, Calibration.Type type, String instrument, String password, BufferedOutputStream bos) throws IOException {
        if (uploadPassword.equals(password)) {
            Number rvn = svnRepository.updateCalibrationFile(type, instrument, data);
            if (rvn.intValue() != -1) {
                Version version = svnRepository.getVersion(type, instrument);
                cachedRepository.updateCalibrationFile(type, instrument, version, data);
                bos.write("upload OK, new version = ".getBytes());
                bos.write(version.toString().getBytes());
                bos.write("\n".getBytes());
            } else {
                bos.write("uploaded file is identical to file in repository; not updated\n".getBytes());
            }
        } else {
            bos.write("password submitted was wrong\n".getBytes());
        }
    }

    /**
     * Handles get request for uploading a new calibration file by sending a very basic file upload form to the
     * client.
     * @param response
     * @param bos
     * @param instrument
     * @throws IOException
     */
    private void uploadCalibrationsFile(HttpServletResponse response, BufferedOutputStream bos, Calibration.Type type, String instrument, String command) throws IOException {
        response.setContentType("text/html");
        // writing HTML directly is ugly, I know, but as long as we don't need anything fancier this should do the trick...
        StringBuffer sb = new StringBuffer();
        sb.append(("<form action=\"gcal?command=" + command + "&type=" + type + "&instrument=" + instrument + "\" method=\"post\" enctype=\"multipart/form-data\">\n"));
        sb.append("  <table>\n");
        if (SmartGcalService.COMMAND_UPLOAD.equals(command)) {
            sb.append("    <tr>\n");
            sb.append("      <td><label for=\"password\">Password:</label></td>\n");
            sb.append("      <td><input type=\"password\" name=\"password\"/></td>\n");
            sb.append("    <tr>\n");
        }
        sb.append("    <tr>\n");
        sb.append("      <td><label for=\"file\">File:</label></td>\n");
        sb.append("      <td><input type=\"file\" name=\"file\"/></td>\n");
        sb.append("    <tr>\n");
        sb.append("  </table></p>\n");
        sb.append("  <input type=\"submit\" value=\""+ command +"\"/>\n");
        sb.append("</form>\n");
        bos.write(sb.toString().getBytes());
    }

    /**
     * Downloads the newest version of the calibrations file for an instrument and type.
     * @param response
     * @param bos
     * @param instrument
     * @throws IOException
     */
    private void downloadCalibrationsFile(HttpServletResponse response, BufferedOutputStream bos, Calibration.Type type, String instrument, String versioned) throws IOException {
        LOG.log(Level.FINE,  String.format("getting calibration file for %s %ss", instrument, type));
        CalibrationFile file = cachedRepository.getCalibrationFile(type, instrument);
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", String.format("attachment; filename=\"%s_%s.csv\"", instrument, type));
        byte[] bytes;
        if (versioned != null && (versioned.isEmpty() || versioned.equalsIgnoreCase("true"))) {
            // add version header (revision + timestamp), version is used internally by smartgcal updater
            bytes = file.toString().getBytes();
        } else {
            // just download the actual file, don't add a version
            bytes = file.getData().getBytes();
        }
        response.setContentLength(bytes.length);
        bos.write(bytes);
    }

    /**
     * Writes current version for one specific or all known instruments as text output to the response.
     * Could be done using XML or whatever but this is easy and simple and currently sufficient.
     * @param res
     * @param bos
     * @param theInstrument
     * @throws IOException
     */
    private void returnVersions(HttpServletResponse res, BufferedOutputStream bos, final String theInstrument, Calibration.Type theType) throws IOException {
        // default: show versions for all known instruments
        List<String> instruments = SmartGcalService.getInstrumentNames();
        if (theInstrument != null) {
            // select one specific instrument only
            instruments = new ArrayList<String>() {{ add(theInstrument); }};
        }
        // default: show versions for all known types
        Calibration.Type[] types = Calibration.Type.values();
        if (theType != null) {
            // select one specific type only
            types = new Calibration.Type[] { theType };
        }

        // show versions for all selected instruments and types
        res.setContentType("text/plain");
        for (String instrument : instruments) {
            for (Calibration.Type type : types) {
                if (SmartGcalService.getAvailableTypes(instrument).contains(type)) {
                    Version version = cachedRepository.getVersion(type, instrument);
                    bos.write((instrument+"_"+type).getBytes());
                    bos.write("=".getBytes());
                    bos.write(version.toString().getBytes());
                    bos.write("\n".getBytes());
                }
            }
        }
    }

    /**
     * Checks that the instrument name is valid.
     * @param instrument
     * @throws IllegalArgumentException if instrument is not known
     */
    private void checkInstrument(String instrument) {
        if (SmartGcalService.getInstrumentNames().contains(instrument)) {
            return;
        }
        throw new IllegalArgumentException("invalid instrument name");
    }

    /**
     * Handles exceptions and writes them to the log and as text output into the response.
     * @param response
     * @param bos
     * @param e
     * @throws IOException
     */
    private void writeException(HttpServletResponse response, BufferedOutputStream bos, Exception e) throws IOException {
        LOG.log(Level.WARNING, "could not process request", e);
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

    /**
     * For the undecided a little menu with useful links is provided
     * @param response
     * @param bos
     * @throws IOException
     */
    private void showMenu(StringBuffer url, HttpServletResponse response, BufferedOutputStream bos) throws IOException {
        response.setContentType("text/html");
        // writing HTML directly is ugly, I know, but as long as we don't need anything fancier this should do the trick...
        StringBuffer sb = new StringBuffer();
        sb.append("<h1>Welcome to smart gcal!</h1>\n");
        sb.append("<h3>Downloading calibrations:</h3>\n");
        sb.append("<ul>\n");
        printCommands(url, sb, SmartGcalService.COMMAND_DOWNLOAD);
        sb.append("</ul>\n");
        sb.append("<h3>Validating calibrations:</h3>\n");
        sb.append("<ul>\n");
        printCommands(url, sb, SmartGcalService.COMMAND_VALIDATE);
        sb.append("</ul>\n");
        sb.append("<h3>Uploading calibrations:</h3>\n");
        sb.append("<ul>\n");
        printCommands(url, sb, SmartGcalService.COMMAND_UPLOAD);
        sb.append("</ul>\n");
        bos.write(sb.toString().getBytes());
    }

    private void printCommands(StringBuffer url, StringBuffer sb, String command) {
        for (String instrument : SmartGcalService.getInstrumentNames()) {
            for (Calibration.Type type : SmartGcalService.getAvailableTypes(instrument)) {
                sb.append("<li><a href=\"");
                sb.append(url.toString());
                sb.append("?command=");
                sb.append(command);
                sb.append("&type=");
                sb.append(type.toString());
                sb.append("&instrument=");
                sb.append(instrument);
                sb.append("\">");
                sb.append(instrument);
                sb.append(" ");
                sb.append(type);
                sb.append("</a></li>\n");
            }
        }
    }


}
