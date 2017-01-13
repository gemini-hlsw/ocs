/**
 * $Id: PioSpXmlWriter.java 46768 2012-07-16 18:58:53Z rnorris $
 */

package edu.gemini.spModel.io.impl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;

import java.util.logging.Logger;
import java.util.logging.Level;

import edu.gemini.pot.sp.ISPNightlyRecord;
import edu.gemini.pot.sp.ISPProgram;
import edu.gemini.pot.sp.ISPNode;
import edu.gemini.spModel.io.PioDocumentBuilder;
import edu.gemini.spModel.pio.Document;
import edu.gemini.spModel.pio.xml.PioXmlUtil;

public class PioSpXmlWriter {
    private static final Logger LOG = Logger.getLogger(PioSpXmlWriter.class.getName());

    // Used to store the XML
    private Writer _writer;

    // The last parsing problem
    private String _problem;

    /**
     * Create a writer for an SpProgram from a file.
     */
    public PioSpXmlWriter(File file) throws IOException {
        this(new FileWriter(file));
    }

    /**
     * Create a writer for an SpProgram from an output stream.
     */
    public PioSpXmlWriter(OutputStream os) {
        this(new OutputStreamWriter(os, Charset.forName("ISO-8859-1")));
    }

    /**
     * Create a writer for an SpProgram.
     */
    public PioSpXmlWriter(Writer writer) {
        _writer = writer;
    }


    /**
     * If the parsing failed, this method may be called get the problem
     * description, including the line of the Science Program file on
     * which the error occurred.
     */
    public String getProblemDescription() {
        return _problem;
    }

    /**
     * Write a program or nightly plan document
     */
    public boolean printDocument(ISPNode node) {
        if (node instanceof ISPProgram) {
            return printDocument((ISPProgram)node);
        }
        if (node instanceof ISPNightlyRecord) {
            return printDocument((ISPNightlyRecord)node);
        }
        return false;
    }

    /**
     * Write a program document
     */
    public boolean printDocument(ISPProgram prog) {
        Document doc = PioDocumentBuilder.instance.toDocument(prog);

        try {
            PioXmlUtil.write(doc, _writer);
            _writer.close();
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "problem writting science program", ex);
            _problem = "Failed while outputting the science program.";
            return false;
        }
        return true;
    }

    /**
     * Write a program document
     */
    public boolean printDocument(ISPNightlyRecord record) {
        Document doc = PioDocumentBuilder.instance.toDocument(record);

        try {
            PioXmlUtil.write(doc, _writer);
            _writer.close();
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "problem writing the nightly record", ex);
            _problem = "Failed while outputting the nightly record.";
            return false;
        }
        return true;
    }
}
