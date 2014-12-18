// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.
//
// $Id: TextFileReader.java,v 1.2 2003/11/21 14:31:02 shane Exp $
//
package edu.gemini.itc.shared;

import java.io.InputStream;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.StringTokenizer;

import java.text.ParseException;

/**
 * This class offers convenience in reading data files.
 * This class automatically skips blank lines and lines beginning
 * with a # sybmol.  The readDataType() methods automatically parse
 * tokens on whitespace and commas.
 */
public class TextFileReader {
    // Set up the delimiters for string tokenization.
    // Reader will break on space, comma, and tab.
    private static final char DELIM[] = {' ', ',', '\t'};
    private static final String _DELIMITER = new String(DELIM);

    private BufferedReader _br = null;

    private String _currentLine = null;
    private int _currentLineNumber = 0;

    private String _lookaheadLine = null;
    private int _lookaheadLineNumber = 0;

    private StringTokenizer _st = null;
    private String _fileName = null;

    private boolean _eof = false;

    /**
     * Constructs TextFileReader on a stream.
     */
    public TextFileReader(InputStream input) {
        _br = new BufferedReader(new InputStreamReader(input));
        try {
            _readLine();  // prime the pump
        } catch (Exception e) {
        }
        try {
            _readLine();  // prime the pump
        } catch (Exception e) {
        }
    }

    /**
     * Constructs TextFileReader on a reader.
     */
    public TextFileReader(Reader reader) {
        _br = new BufferedReader(reader);
        try {
            _readLine();  // prime the pump
        } catch (Exception e) {
        }
        try {
            _readLine();  // prime the pump
        } catch (Exception e) {
        }
    }

    /**
     * Constructs TextFileReader on the named file.
     * @throws FileNotFoundException if fileName not found.
     */
    public TextFileReader(String fileName) throws Exception {
        InputStream input = TextFileReader.class.getResourceAsStream(fileName);
        if (input == null) {
            throw new Exception("Failed to find resource " + fileName);
        }

        _br = new BufferedReader(new InputStreamReader(input));
        _fileName = fileName;
        try {
            _readLine();  // prime the pump
        } catch (Exception e) {
        }
        try {
            _readLine();  // prime the pump
        } catch (Exception e) {
        }
    }

    /**
     * @return The line number in the file.  This counts blank lines
     * and # comment lines.
     */
    public int getLineNumber() {
        return _currentLineNumber;
    }

    /**
     * @return The current line in the file.  This skips blank lines
     * and # comment lines.
     */
    public String getCurrentLine() {
        return _currentLine;
    }

    /**
     * Returns the number of tokens on the current line.
     * Note the reader may be in the middle of reading the line so it
     * is not necessarily the number of tokens left to read on the line.
     * When end of file is reached, returns 0.
     * @return number of tokens on current line
     */
    public int countTokens() {
        if (getCurrentLine() == null) return 0;
        StringTokenizer st = new StringTokenizer(getCurrentLine(), _DELIMITER);
        return st.countTokens();
    }

    /**
     * Read a line of text. A line is considered to be terminated by any
     * one of a line feed ('\n'), a carriage return ('\r'), or a carriage
     * return followed immediately by a linefeed.
     * This method is not meant to be used at the same time as the
     * readDataType() methods.
     * @return A String containing the contents of the line, not including
     * line-termination characters, or null if the end of the stream has been
     * reached.
     */
    // To support the hasMoreData() method, there is always a
    // line cached.  _readLine() will read the next line and
    // return the cached line.  Then cache the line just read.
    public String readLine() throws IOException {
        String s = getCurrentLine();
        _st = null;
        return s;
    }

    /**
     * Read a line of text. A line is considered to be terminated by any
     * one of a line feed ('\n'), a carriage return ('\r'), or a carriage
     * return followed immediately by a linefeed.
     * This method is not meant to be used at the same time as the
     * readDataType() methods.
     * @return A String containing the contents of the line, not including
     * line-termination characters, or null if the end of the stream has been
     * reached.
     */
    // To support the hasMoreData() method, there is always a
    // line cached.  _readLine() will read the next line and
    // return the cached line.  Then cache the line just read.
    private String _readLine() throws IOException {
        _currentLine = _lookaheadLine;
        _currentLineNumber = _lookaheadLineNumber;
        boolean validLine = false;
        while (!validLine) {
            // BufferedReader currently returns null (no Exception) at eof
            _lookaheadLine = _br.readLine();
            if (_lookaheadLine != null) _lookaheadLine = _lookaheadLine.trim();
            _lookaheadLineNumber++;
            //System.out.println("just read line " + _lookaheadLineNumber + ": " + _lookaheadLine);
            //System.out.println("Current line " + _currentLineNumber + ": " + _currentLine);
            if (_lookaheadLine == null) {
                validLine = true;  // end of file
            } else {
                if (!_lookaheadLine.equals("") &&
                        _lookaheadLine.charAt(0) != '#') {
                    // This is a valid line of data
                    validLine = true;
                }
            }
        }
        if (_currentLine != null) {
            _st = new StringTokenizer(_currentLine, _DELIMITER);
        } else {
            _throwEOF();
        }
        return _currentLine;
    }

    /**
     * Reads next token and returns it as a String.
     * @return next data item in stream
     */
    public String readString() throws IOException {
        if (!hasMoreData()) {
            _throwEOF();
        }
        return _st.nextToken();
    }

    /**
     * Reads next token and tries to parse as an integer.
     * @return next data item in stream if it parses to int.
     * @throws IOException if eof reached, ParseException
     * if next item can't be parsed as an integer.
     */
    public int readInt() throws ParseException, IOException {
        if (!hasMoreData()) {
            _throwEOF();
        }
        int i = 0;
        String s = _st.nextToken();
        try {
            i = Integer.parseInt(s);
        } catch (NumberFormatException e) {
            _throwParseException("int", s);
        }
        return i;
    }

    /**
     * Reads next token and tries to parse as an double.
     * @return next data item in stream if it parses to double.
     * @throws IOException if eof reached, ParseException
     * if next item can't be parsed as an double.
     */
    public double readDouble() throws ParseException, IOException {
        if (!hasMoreData()) {
            _throwEOF();
        }
        double d = 0;
        String s = _st.nextToken();
        try {
            d = Double.parseDouble(s);
        } catch (NumberFormatException e) {
            _throwParseException("double", s);
        }
        return d;
    }

    /**
     * Reads next token and tries to parse as a boolean.
     * @return next data item in stream if it parses to boolean.
     * @throws IOException if eof reached, ParseException
     * if next item can't be parsed as an boolean.
     */
    public boolean readBoolean() throws ParseException, IOException {
        if (!hasMoreData()) {
            _throwEOF();
        }
        String s = _st.nextToken();
        s = s.toUpperCase();
        if (s.equals("T") || s.equals("TRUE")) return true;
        if (s.equals("F") || s.equals("FALSE")) return false;
        _throwParseException("boolean", s);
        return false;
    }

    // Throws a ParseException when token fails to parse.
    private void _throwParseException(String type, String token)
            throws ParseException {
        String file = (_fileName != null) ? (" of file " + _fileName) : "";
        throw new ParseException("Failed to parse "
                                 + type + " on line "
                                 + getLineNumber() + file + ".  Token: " + token, getLineNumber());
    }

    // Throws a IOException when eof reached
    private void _throwEOF() throws IOException {
        _eof = true;
        String message = (_fileName != null) ? ("eof reached for " + _fileName) :
                "eof reached";
        IOException e = new IOException(message);
        //e.printStackTrace();
        throw e;
    }

    /**
     * @return true if there is more data to be read.
     */
    // Any get method calls this first to make sure data is
    // read in to get.
    // @return true if there is more data, false if eof.
    public boolean hasMoreData() {
        if (_st == null || !(_st.hasMoreTokens())) {
            try {
                _readLine();
            } catch (IOException e) {
            }
            if (_st == null || !(_st.hasMoreTokens())) return false;  // out of data
        }
        return true;
    }

    /**
     * Test driver.  Command line parameter is interpreted as name of
     * data file to read.  For this test each non-comment line of the
     * data file should contain an int followed by a float followed
     * by string followed by boolean.
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Usage: TextFileReader testFileName");
            return;
        }
        String fileName = args[0];
        System.out.println("Testing data file " + fileName);
        TextFileReader reader;
        reader = new TextFileReader(fileName);
        try {
            while (true) {
                int i = reader.readInt();
                System.out.println("line " + reader.getLineNumber()
                                   + " int: " + i);
                double d = reader.readDouble();
                System.out.println("line " + reader.getLineNumber()
                                   + " double: " + d);
                String s = reader.readString();
                System.out.println("line " + reader.getLineNumber()
                                   + " String: " + s);
                boolean b = reader.readBoolean();
                System.out.println("line " + reader.getLineNumber()
                                   + " boolean: " + b);
            }
        } catch (ParseException e) {
            System.out.println(e.toString());
        } catch (IOException e) {
            System.out.println(e.toString());
        }

        reader = new TextFileReader(fileName);
        try {
            while (true) {
                String line = reader.readLine();
                System.out.println("line " + reader.getLineNumber()
                                   + " line: " + line);
            }
        } catch (IOException e) {
            System.out.println(e.toString());
        }
    }
}
