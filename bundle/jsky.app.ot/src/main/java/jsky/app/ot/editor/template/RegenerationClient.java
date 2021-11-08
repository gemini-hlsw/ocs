package jsky.app.ot.editor.template;

import edu.gemini.phase2.core.model.TemplateFolderExpansion;
import edu.gemini.spModel.core.Peer;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.PioNode;
import edu.gemini.spModel.pio.xml.PioXmlException;
import edu.gemini.spModel.pio.xml.PioXmlFactory;
import edu.gemini.spModel.pio.xml.PioXmlUtil;
import edu.gemini.spModel.template.Phase1Folder;
import edu.gemini.util.ssl.apache.HttpsSupport;

import org.apache.commons.httpclient.methods.MultipartPostMethod;
import org.apache.commons.httpclient.methods.multipart.ByteArrayPartSource;
import org.apache.commons.httpclient.methods.multipart.FilePart;

import java.io.*;
import java.net.URLDecoder;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.ws.http.HTTPException;

/**
 * Template regeneration client.  Obtains a TemplateFolderExpansion object from
 * the template server.  A functor can be used to apply the changes to a
 * science program.
 */
public final class RegenerationClient {
    private RegenerationClient() {}

    private static final Logger LOG = Logger.getLogger(RegenerationClient.class.getName());

    public enum FailureCode {
        NETWORK("There was a problem contacting the server, check your network connection and try again."),
        INCOMPATIBLE("Your version of the OT appears to be incompatible with the server. Check for updates."),
        REMOTE("The server could not fulfill the request at this time.  Try again later."),
        ;

        public final String message;

        private FailureCode(String message) {
            this.message = message;
        }
    }

    public static final class Failure {
        public final FailureCode code;
        public final String detail;
        public Failure(FailureCode code, String detail) {
            this.code   = code;
            this.detail = detail == null ? "" : detail;
        }
    }

    // A very poor man's Either[Failure, TemplateFolderExpansion]
    public static final class Result {
        public final Failure failure;
        public final TemplateFolderExpansion expansion;

        public Result(FailureCode code) {
            this(code, null);
        }

        public Result(FailureCode code, String detail) {
            this(new Failure(code, detail));
        }

        public Result(Failure failure) {
            if (failure == null) throw new IllegalArgumentException();
            this.failure   = failure;
            this.expansion = null;
        }

        public Result(TemplateFolderExpansion expansion) {
            this.failure   = null;
            this.expansion = expansion;
        }

        public boolean isFailure() { return failure != null; }
        public boolean isSuccess() { return failure == null; }
    }

    private static final String TEMPLATE_SERVLET = "template";
    private static final String ENC = "UTF-8";

    public static String getUrl(Peer peer) {
        return String.format("https://%s:%d/%s", peer.host, peer.port, TEMPLATE_SERVLET);
    }

    private static String decode(String msg) {
        try {
            return URLDecoder.decode(msg, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            LOG.log(Level.SEVERE, "unsupported encoding");
            throw new RuntimeException(ex);
        }
    }

    public static Result expand(Phase1Folder folder, SPProgramID pid, Peer peer, int timeoutMs) {
        // Setup to post the template folder XML.
        MultipartPostMethod post;
        post = new MultipartPostMethod(getUrl(peer));
        post.addParameter("pid", pid.toString());
        post.addPart(makeFilePart(folder));
        post.setRequestHeader( "Accept-Encoding", ENC);

        // Execute the request.  A bad request means that the OT and OODB aren't
        // speaking the same language.  Otherwise any other error will be
        // assumed to be a problem with the OODB talking to the ODB.
        try {
            switch (HttpsSupport.execute(post, timeoutMs)) {
                case 200: return parseResponse(post.getResponseBodyAsStream());
                case 400: return new Result(FailureCode.INCOMPATIBLE, decode(post.getStatusText()));
                default:  return new Result(FailureCode.REMOTE,       decode(post.getStatusText()));
            }

        } catch (HTTPException ex) {
            // This should only happen if the OT and and OODB versions are not
            // compatible somehow.
            LOG.log(Level.WARNING, "Unrecoverable HTTP exception contacting the template expansion service.", ex);
            return new Result(FailureCode.INCOMPATIBLE);

        } catch (IOException ex) {
            // This is probably a network issue.
            LOG.log(Level.WARNING, "IOException contacting template expansion service.", ex);
            return new Result(FailureCode.NETWORK);

        } catch (Throwable t) {
            LOG.log(Level.SEVERE, "Unexpected error processing template generation response", t);
            return new Result(FailureCode.INCOMPATIBLE);

        } finally {
            post.releaseConnection();
        }
    }

    private static Result parseResponse(InputStream stream) {
        try {
            // Parse the XML string into a PioNode, which should be a ParamSet.
            PioNode node = PioXmlUtil.read(new InputStreamReader(stream, ENC));
            if (!(node instanceof ParamSet)) {
                LOG.log(Level.WARNING, "Expected ParamSet but got: " + node);
                return new Result(FailureCode.INCOMPATIBLE, "The server returned an unexpected result.");
            }

            // Okay, turn the ParamSet into a TemplateFolderExpansion.
            ParamSet pset = (ParamSet) node;
            TemplateFolderExpansion exp = new TemplateFolderExpansion(pset);
            if ((exp.templateGroups.size() == 0) && (exp.baselineCalibrations.size() == 0)) {
                return new Result(FailureCode.INCOMPATIBLE, "The server did not recognize the Phase 1 information in your program.");
            }
            return new Result(exp);

        } catch (PioXmlException ex) {
            // The server shouldn't be returning something other than a
            // ParamSet XML string.
            LOG.log(Level.WARNING, "Couldn't parse the template expansion", ex);
            return new Result(FailureCode.INCOMPATIBLE, "Couldn't parse the response from the server.");

        } catch (UnsupportedEncodingException ex) {
            // Not going to happen.
            LOG.log(Level.SEVERE, "Unsupported encoding eh?", ex);
            throw new RuntimeException(ex);
        }
    }

    private static FilePart makeFilePart(Phase1Folder folder) {
        try {
            final ParamSet ps = folder.getParamSet(new PioXmlFactory());
            final String  xml = PioXmlUtil.toXmlString(ps);
            final ByteArrayOutputStream baos = new ByteArrayOutputStream(xml.length() * 2 + 1);
            final OutputStreamWriter w = new OutputStreamWriter(baos, ENC);
            w.write(xml);
            w.close();

            final ByteArrayPartSource baps = new ByteArrayPartSource("folder", baos.toByteArray());

            final FilePart fp = new FilePart("folder", baps);
            fp.setContentType("text/xml");
            return fp;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
