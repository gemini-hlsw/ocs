//
// $Id: OdbStateIO.java 4336 2004-01-20 07:57:42Z gillies $
//
package edu.gemini.dbTools.odbState;

import edu.gemini.shared.util.FileUtil;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public final class OdbStateIO {

    private static final DocumentFactory FACTORY = new DocumentFactory();

    private final Logger log;
    private final File stateFile;

    public OdbStateIO(final Logger log, final File stateFile) {
        this.stateFile = stateFile;
        this.log = log;
    }

    public void writeState(final ProgramState[] psA) throws IOException {

        final StringWriter sw = new StringWriter();
        sw.write("<odb>\n");
        final XMLWriter xw = new XMLWriter(sw, new OutputFormat("   ", true));

        // Convert the reports into an XML element.
        if (psA != null) {
            for (final ProgramState ps : psA) {
                final Element e = ps.toElement(FACTORY);
                xw.write(e);
            }
        }
        sw.write("\n</odb>\n");
        FileUtil.writeString(stateFile, sw.toString());

    }

    public ProgramState[] readState() throws IOException {
        return readState(stateFile, log);
    }

    public static ProgramState[] readState(final File f, final Logger log) throws IOException {

        // Check for an empty file -- which will be the case for the first run.
        if (f.length() == 0) {
            log.info("state file is missing or empty; using empty program state.");
            return ProgramState.EMPTY_STATE_ARRAY;
        }

        final SAXReader saxReader = new SAXReader();
        final List<ProgramState> lst = new ArrayList<ProgramState>();
        final String s = FileUtil.readFile(f);

        // Could just build a huge DOM file, but it might be faster to just create the DOM for one element at a time.
        int pos = s.indexOf("<prog ");
        while (pos >= 0) {
            final int end = s.indexOf("</prog>", pos) + 7;
            final String progStr = s.substring(pos, end);
            final Reader rdr = new StringReader(progStr);
            final Document doc;
            try {
                doc = saxReader.read(rdr);
                lst.add(new ProgramState(doc.getRootElement()));
                rdr.close();
            } catch (IOException ioe) {
                throw ioe;
            } catch (Exception ex) {
                throw new IOException(ex);
            }
            pos = s.indexOf("<prog ", end);
        }

        return lst.toArray(ProgramState.EMPTY_STATE_ARRAY);

    }
}
