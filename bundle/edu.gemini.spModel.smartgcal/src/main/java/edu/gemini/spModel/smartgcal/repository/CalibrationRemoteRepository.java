package edu.gemini.spModel.smartgcal.repository;

import edu.gemini.spModel.gemini.calunit.smartgcal.*;
import edu.gemini.util.ssl.apache.HttpsSupport;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.IOException;

/**
 * Implementation of a remote repository that uses the smart gcal service on the OODB server to query the
 * most current versions and downloads them if necessary in order to update the locally cached files.
 */
public class CalibrationRemoteRepository implements CalibrationRepository {

    private final String protocol;
    private final String host;
    private final int port;
    private final String urlHead;

    public CalibrationRemoteRepository(String host, int port) {
        this.protocol =  "https";
        this.host = host;
        this.port = port;
        this.urlHead = getUrlHead();
    }

    @Override
    public Version getVersion(Calibration.Type type, String instrument) throws IOException {
        String command = getCommand(SmartGcalService.COMMAND_VERSIONS, type, instrument);
        String result = executeCommand(command);
        String version = result.substring(result.indexOf("=")+1);
        return Version.parse(version.trim());
    }

    @Override
    public CalibrationFile getCalibrationFile(Calibration.Type type, String instrument) throws IOException {
        // download a file from the server with a version header -> versioned parameter = "true"
        String command = getCommand(SmartGcalService.COMMAND_DOWNLOAD, type, instrument, "true");
        String result = executeCommand(command);
        return CalibrationFile.fromString(result);
    }

    private String executeCommand(String command) throws IOException {
        HttpMethod get = new GetMethod(command);
        HttpsSupport.execute(get, 30000);
        if (get.getStatusCode() != 200) {
            throw new RuntimeException("could not connect to smartgcal server: " + get.getStatusLine());
        }
        return get.getResponseBodyAsString();
    }

    private String getCommand(String command, Calibration.Type type, String instrument) {
        return getCommand(command, type, instrument, null);
    }

    private String getCommand(String command, Calibration.Type type, String instrument, String versioned) {
        StringBuffer sb = new StringBuffer(urlHead);
        sb.append(SmartGcalService.PARAMETER_COMMAND).append("=").append(command);
        if (type != null) {
            sb.append("&").append(SmartGcalService.PARAMETER_TYPE).append("=").append(type);
        }
        if (instrument != null) {
            sb.append("&").append(SmartGcalService.PARAMETER_INSTRUMENT).append("=").append(instrument);
        }
        if (versioned != null) {
            sb.append("&").append(SmartGcalService.PARAMETER_VERSIONED).append("=").append(versioned);
        }
        return sb.toString();
    }

    private String getUrlHead() {
        StringBuffer sb = new StringBuffer();
        sb.append(protocol);
        sb.append("://");
        sb.append(host);
        sb.append(":");
        sb.append(port);
        sb.append("/");
        sb.append(SmartGcalService.SERVER_CONTEXT_NAME);
        sb.append("?");
        return  sb.toString();
    }

}
