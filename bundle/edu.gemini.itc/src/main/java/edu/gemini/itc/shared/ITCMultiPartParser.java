package edu.gemini.itc.shared;

import org.apache.commons.fileupload.DiskFileUpload;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadBase.InvalidContentTypeException;
import org.apache.commons.fileupload.FileUploadBase.SizeLimitExceededException;
import org.apache.commons.fileupload.FileUploadException;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Class that Allows the Parsing of Multipart posts.
 * Allows text files to be submitted.
 */
public class ITCMultiPartParser {

    private static String TEXT = "text/plain";

    private final List<FileItem>          items;
    private final ArrayList<String>       fileNames = new ArrayList<>();
    private final ArrayList<String>       parameterNames = new ArrayList<>();
    private final HashMap<String, String> files = new HashMap<>();
    private final HashMap<String, String> parameters = new HashMap<>();
    private final HashMap<String, String> fileTypes = new HashMap<>();
    private final HashMap<String, String> remoteFileNames = new HashMap<>();

    /**
     * Constructor for the MultipartParser.
     *
     * @param req                The Submitted request.  Must be Multipart.
     * @param maxLength          Maximum Content length of the submitted request.
     *                           Usefull for limiting the size of the uploaded file.
     * @throws IllegalArgumentException      Throws IllegalArgumentException if the submitted file is not
     *                                       of a type that can be handled.
     */
    public ITCMultiPartParser(final HttpServletRequest req, final int maxLength) {
        final DiskFileUpload upload;
        try {
            upload = new DiskFileUpload();
            upload.setSizeMax(maxLength);
            items = upload.parseRequest(req);
        } catch (InvalidContentTypeException e) {
            throw new IllegalArgumentException("Submitted form is not Multipart.");
        } catch (SizeLimitExceededException e) {
            throw new IllegalArgumentException("Request Size limit exceeded.  Please submit smaller file.");
        } catch (FileUploadException e) {
            throw new IllegalArgumentException("Unable to parse multipart request.");

        }

        for (final FileItem item : items) {
            if (item.isFormField()) {
                parameterNames.add(item.getFieldName());
                parameters.put(item.getFieldName(), item.getString());
            } else {
                addFile(item);
            }
        }

    }


    private void addFile(final FileItem file) {
        if (file.getName().length() == 0) return;  //If there is no filename exit

        if (file.getContentType().equals(TEXT) || file.getName().endsWith(".dat") || file.getName().endsWith(".nm")) {

            files.put(file.getFieldName(), file.getString());
            fileNames.add(file.getName());
            remoteFileNames.put(file.getFieldName(), file.getName());
            fileTypes.put(file.getFieldName(), TEXT);

        } else {

            throw new IllegalArgumentException("Submitted file, " + file.getName()
                    + ", is a " + file.getContentType() + " file which is not supported. ");
        }

    }

    /**
     * Returns the string value of a submitted parameter.
     *
     * @param name String Name of the requested parameter, from the HTML form.
     * @return String of the value from the requested Parameter.
     * @throws IllegalArgumentException Thrown if the parameter was not parsed from the HTTP request
     */
    public String getParameter(final String name) {
        final String parameter = parameters.get(name);
        if (parameter == null) {
            throw new IllegalArgumentException("Parameter " + name + "not found in request.");
        }
        return parameter;
    }

    /**
     * Returns true if the parameter name has been parsed.
     *
     * @param name String Name of the requested parameter, from the HTML form.
     * @return Boolean. True if parameter exists.
     */
    public boolean parameterExists(final String name) {
        return parameters.containsKey(name);
    }

    /**
     * Allows access to all of the file names in the form of an Iterator.
     *
     * @return Returns and Iterator of all the file names.
     */
    public Iterator getFileNames() {
        return fileNames.iterator();
    }

    /**
     * Method that allows access to the Remote path and name of the uploaded file
     *
     * @param fileName Local Representation of the remote filename
     * @return Returns the path and filename of the uploaded file.
     */
    public String getRemoteFileName(final String fileName) {
        return remoteFileNames.get(fileName);
    }

    /**
     * The getTextFile method allows access to any file uploaded of type <CODE>text/plain</CODE>
     *
     * @param fileName the fileName identifier of the text file passed (from the html form)
     * @return returns the text file as a String.
     * @throws IllegalArgumentException If requested file is not of type <CODE>text/plain</CODE> an IllegalArgumentException is thrown.
     */
    public String getTextFile(final String fileName) {
        if ((fileTypes.get(fileName)).equals(TEXT))
            return files.get(fileName);
        else
            throw new IllegalArgumentException("Submitted file is not a text/plain file.  Resubmit with a Text file");
    }

}
