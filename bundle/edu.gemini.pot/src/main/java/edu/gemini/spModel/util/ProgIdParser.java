// Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: ProgIdParser.java 6686 2005-10-19 19:51:16Z shane $
//
package edu.gemini.spModel.util;

import edu.gemini.pot.sp.SPNodeKey;
import edu.gemini.shared.util.immutable.MapOp;
import edu.gemini.shared.util.immutable.None;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.shared.util.immutable.Some;
import edu.gemini.spModel.core.SPBadIDException;
import edu.gemini.spModel.core.SPProgramID;
import java.util.logging.Logger;
import java.util.logging.Level;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.net.URL;


/**
 * Gets the progId and progKey from an OT XML file by scanning the
 * start of the file with a simple SAX parser. After calling parse,
 * the getProgId() and getProgKey() methods return the values, or
 * null if not found.
 */
public final class ProgIdParser {
    private static final Logger LOG = Logger.getLogger(ProgIdParser.class.getName());

    // Shady use of exceptions for control flow in this class...
    private static final String CONTROL_FLOW_EXCEPTION = "control flow exception: parsed program id";

    /**
     * Contains the result of parsing an XML string or file for the program id
     * information.
     */
    public static final class ParsedProgId {
        public final SPNodeKey progKey;
        public final String kind;
        public final Option<SPProgramID> progId;

        public ParsedProgId(SPNodeKey progKey, String kind, Option<SPProgramID> progId) {
            this.progKey = progKey;
            this.kind    = kind;
            this.progId  = progId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ParsedProgId that = (ParsedProgId) o;

            if (!kind.equals(that.kind)) return false;
            if (!progId.equals(that.progId)) return false;
            if (!progKey.equals(that.progKey)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = progId.hashCode();
            result = 31 * result + progKey.hashCode();
            result = 31 * result + kind.hashCode();
            return result;
        }

        public String toString() {
            String id = progId.map(new MapOp<SPProgramID, String>() {
                @Override public String apply(SPProgramID pid) {
                    return pid.toString();
                }
            }).getOrElse("");
            return String.format("ParsedProgId {id=%s, key=%s, kind=%s}", id, progKey, kind);
        }
    }

    private ParsedProgId parse(InputSource is) {
        Handler h = new Handler();
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            XMLReader parser = factory.newSAXParser().getXMLReader();
            parser.setEntityResolver(h);
            parser.setContentHandler(h);
            parser.setErrorHandler(h);
            parser.parse(is);
        } catch (Exception ex) {
            // ignore the control flow exception, log any others.
            if (!((ex instanceof SAXException) && CONTROL_FLOW_EXCEPTION.equals(ex.getMessage()))) {
                String msg = "Problem while importing xml string.";
                LOG.log(Level.WARNING, msg, ex);
                throw new RuntimeException(ex);
            }
        }
        return h.createParsedProgId();
    }


    public ParsedProgId parse(File file) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            return parse(new InputSource(reader));
        } catch (FileNotFoundException ex) {
            throw new RuntimeException(ex);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    public ParsedProgId parse(String xml) {
        return parse(new InputSource(new StringReader(xml)));
    }

    private static class Handler extends DefaultHandler {
        private String _progId;
        private String _progKey;
        private String _kind;

        ParsedProgId createParsedProgId() {
            // prog key and kind are required
            String missing = null;
            if (_progKey == null) {
                missing = "prog key";
            } else if (_kind == null) {
                missing = "kind";
            }
            if (missing != null) {
                String msg = "Could not find " + missing;
                LOG.log(Level.WARNING, msg);
                throw new RuntimeException(msg);
            }

            // id, if present, must be legal
            Option<SPProgramID> id = None.instance();
            if (_progId != null) {
                try {
                    id = new Some<SPProgramID>(SPProgramID.toProgramID(_progId));
                } catch (SPBadIDException ex) {
                    String msg = "Bad program id: " + _progId;
                    LOG.log(Level.WARNING, msg);
                    throw new RuntimeException(msg);
                }
            }

            return new ParsedProgId(new SPNodeKey(_progKey), _kind, id);
        }

        public InputSource resolveEntity(String publicId, String systemId) {
            try {
                String fileName = systemId;
                if (fileName.startsWith("jar:")
                        || fileName.startsWith("file:")
                        || fileName.startsWith("http:")) {
                    fileName = new File(new URL(fileName).getFile()).getName();
                }
                if (fileName.equals("Gemini.dtd")
                        || fileName.equals("GeminiData.dtd")) {
                    // check to change path to Gemini.dtd in gemini_p1Model
                    fileName = "gemini/xml/" + fileName;
                } else if (fileName.equals("AstronomyPhase1.dtd")
                        || fileName.equals("AstronomyPhase1Data.dtd")
                        || fileName.equals("target.mod")) {
                    // check to change path to astronomy phase 1
                    fileName = "pit/xml/" + fileName;
                }
                URL url = Resources.getResource(fileName);
                return new InputSource(url.openStream());
            } catch (Exception e) {
                throw new RuntimeException("Error resolving entity: " + systemId, e);
            }
        }

        public void fatalError(SAXParseException e) throws SAXParseException {
            throw e;
        }

        public void error(SAXParseException e) throws SAXParseException {
            throw e;
        }

        public void warning(SAXParseException e) throws SAXParseException {
        }

        public void startElement(String namespaceURI, String localName, String qName, Attributes attrs) throws SAXException {
            if (qName.equals("phase1Document")) {
                throw new SAXException("not a phase2 document"); // don't need to parse phase1 doc, since no NodeKeys are store there
            }
            if (qName.equals("container") || qName.equals("program")) {
                int n = attrs.getLength();
                for (int i = 0; i < n; i++) {
                    String name = attrs.getQName(i);
                    String value = attrs.getValue(i);
                    if (name.equals("name")) {
                        _progId = value;
                    } else if (name.equals("key")) {
                        _progKey = value;
                    } else if (name.equals("kind")) {
                        _kind = value;
                    }
                }
                throw new SAXException(CONTROL_FLOW_EXCEPTION); // stop parsing here
            }
        }
    }
}
