package edu.gemini.horizons.api;

import java.io.Serializable;
import java.util.Vector;

//$Id: ResultsTable.java 630 2006-11-28 19:32:20Z anunez $
/**
 * A table with the available options when a unique object can not be found in the server
 * for the given query arguments. This table will contain all the objects that matches
 * the given query and will provide a unique identifier for the objects. Clients can use
 * that identifier to perform the query again, and get the desired result.
 */
public final class ResultsTable implements Serializable {

    /**
     * The table header. First column should represent the object id
     */
    public Vector<String> _header;
    /**
     * Table data. It's content interpretation will depend on the header data
     */
    public Vector<Vector<String>> _results;

    /**
     * Default constructor.
     */
    public ResultsTable() {
        _results = new Vector<Vector<String>>();
    }

    /**
     * Get the header's table
     * @return The header of the table
     */
    public Vector<String> getHeader() {
        if (_header == null) {
            _header = new Vector<String>();
        }
        return _header;
    }

    /**
     * Get the data in the table.
     * @return The data stored in the table. The interpretation of its content
     * will depend on the header data
     */
    public Vector<Vector<String>> getResults() {
        return _results;
    }

    /**
     * Get the number of columns in the header
     * @return number of columns in the header
     */
    public int getHeaderSize() {
        return getHeader().size();
    }

    /**
     * Get the number of results stored in the table
     * @return number of results in the table
     */
    public int getTotalResults() {
        return _results.size();
    }

    /**
     * Set the header for this table
     * @param header the header of this table
     */
    public void setHeader(Vector<String> header) {
        _header = header;
    }

    /**
     * Add a row of data to the result table
     * @param result a row of data
     */
    public void addResult(Vector<String> result) {
        _results.add(result);
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        //build header
        for (String header : getHeader()) {
            sb.append(header).append("\t\t");
        }
        sb.append("\n");
        //separation line
        for (String header : getHeader()) {
            for (int i = 0; i < header.length(); i++) {
                sb.append("-");
            }
            sb.append("\t\t");
        }
        sb.append("\n");
        //data
        for (Vector<String> row : getResults()) {
            for (String line : row) {
                sb.append(line).append("\t\t\t");
            }
            sb.append("\n");
        }
        return sb.toString();
    }


    public boolean equals(Object obj) {
        if (this == obj)  return true;
        if (!(obj instanceof ResultsTable)) return false;
        ResultsTable that = (ResultsTable)obj;

        return getHeader().equals(that.getHeader()) &&
                getResults().equals(that.getResults());

    }

    public int hashCode() {
        int hash = getHeader().hashCode();
        hash = 31*hash + getResults().hashCode();
        return hash;
    }
}
