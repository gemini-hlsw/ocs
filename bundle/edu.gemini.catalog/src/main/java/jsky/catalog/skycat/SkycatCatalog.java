/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: SkycatCatalog.java 47126 2012-08-01 15:40:43Z swalker $
 */

package jsky.catalog.skycat;

import jsky.catalog.*;
import jsky.coords.CoordinateRadius;
import jsky.coords.Coordinates;
import jsky.coords.ImageCoords;
import jsky.coords.WorldCoords;
import jsky.util.gui.ProgressBarFilterInputStream;
import jsky.util.gui.ProgressPanel;
import jsky.util.gui.StatusLogger;
import java.util.logging.Logger;
import java.util.logging.Level;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Represents a catalog as described in a Skycat style catalog
 * config file. The (keyword: value) pairs in the config file are stored
 * here in a Properties object.
 *
 * @author Allan Brighton
 * @version $Revision: 47126 $
 */
public class SkycatCatalog implements PlotableCatalog {

    private static Logger LOG = Logger.getLogger(SkycatCatalog.class.getName());

    /**
     * The catalog configuration entry for this catalog.
     */
    private SkycatConfigEntry _entry;

    /**
     * Optional handler, used to report HTML format errors from servers
     */
    private HTMLQueryResultHandler _htmlQueryResultHandler;

    /**
     * If this is a local catalog, this may optionally point to the data
     */
    private SkycatTable _table;

    /**
     * Used to assign a unique name to query results
     */
    private int _queryCount = 0;

    /**
     * List of Filters to be applied over the catalog result
     */

    private List<ICatalogFilter> catFilterList;

    /**
     * Initialize the catalog from the given catalog configuration entry.
     *
     * @param entry the catalog configuration file entry describing the catalog
     */
    public SkycatCatalog(SkycatConfigEntry entry) {
        _entry = entry;
    }

    /**
     * Initialize the catalog from the given catalog configuration entry.
     *
     * @param entry the catalog configuration file entry describing the catalog
     * @param table the data for the catalog (optional, only for local catalgs)
     */
    public SkycatCatalog(SkycatConfigEntry entry, SkycatTable table) {
        this(entry);
        _table = table;
    }

    /**
     * Initialize the catalog from the given table.
     *
     * @param table the data for the catalog (optional, only for local catalgs)
     */
    public SkycatCatalog(SkycatTable table) {
        this(table.getConfigEntry());
        _table = table;
        _table.setCatalog(this);
    }


    /**
     * Initialize the catalog from the given catalog configuration entry.
     *
     * @param entry   the catalog configuration file entry describing the catalog
     * @param handler used to report HTML errors from the HTTP server
     */
    public SkycatCatalog(SkycatConfigEntry entry, HTMLQueryResultHandler handler) {
        this(entry);
        setHTMLQueryResultHandler(handler);
    }


    /**
     * Implementation of the clone method (makes a shallow copy).
     */
    @SuppressWarnings({"CloneDoesntDeclareCloneNotSupportedException"})
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new InternalError(); // won't happen
        }
    }


    /**
     * Set the name of the catalog
     */
    public void setName(String name) {
        _entry.setName(name);
    }

    /**
     * Return the name of the catalog
     */
    public String getName() {
        return _entry.getName();
    }

    /**
     * Return the object used to manage the configuration info for this catalog
     */
    public SkycatConfigEntry getConfigEntry() {
        return _entry;
    }

    /**
     * Returns the number of querries made so far
     */
    public int getQueryCount() {
        return _queryCount;
    }

    /**
     * Set the object used to manage the configuration info for this catalog
     */
    public void setConfigEntry(SkycatConfigEntry entry) {
        _entry = entry;
    }

    /**
     * Return the table data (for local catalogs) or null, if not known.
     */
    public SkycatTable getTable() {
        return _table;
    }

    /**
     * Return the name of the catalog
     */
    public String toString() {
        return getName();
    }

    /**
     * Return the id of the catalog (same as name here)
     */
    public String getId() {
        return _entry.getShortName();
    }

    /**
     * Return the title of the catalog (same as name here)
     */
    public String getTitle() {
        return _entry.getName();
    }

    /**
     * Return a description of the catalog (same as name here)
     */
    public String getDescription() {
        return _entry.getProperty("copyright");
    }

    /**
     * Return a URL pointing to documentation for the catalog, or null if not available
     */
    public URL getDocURL() {
        return _entry.getDocURL();
    }

    /**
     * If this catalog can be querried, return the number of query parameters that it accepts
     */
    public int getNumParams() {
        return _entry.getNumParams();
    }

    /**
     * Return a description of the ith query parameter
     */
    public FieldDesc getParamDesc(int i) {
        return _entry.getParamDesc(i);
    }

    /**
     * Return a description of the named query parameter
     */
    public FieldDesc getParamDesc(String name) {
        return _entry.getParamDesc(name);
    }


    /**
     * Return the number of plot symbol definitions associated with this catalog.
     */
    public int getNumSymbols() {
        return _entry.getNumSymbols();
    }

    /**
     * Return the ith plot symbol description
     */
    public TablePlotSymbol getSymbolDesc(int i) {
        return _entry.getSymbolDesc(i);
    }

    /**
     * Return the array of symbol descriptions
     */
    public TablePlotSymbol[] getSymbols() {
        return _entry.getSymbols();
    }

    /**
     * Set the array of catalog table plot symbol definitions for use with this catalog
     */
    public void setSymbols(TablePlotSymbol[] symbols) {
        _entry.setSymbols(symbols);
    }

    /**
     * Set to true if the user edited the plot symbol definitions (default: false)
     */
    public void setSymbolsEdited(boolean edited) {
        _entry.setSymbolsEdited(edited);
    }

    /**
     * Return true if the user edited the plot symbol definitions otherwise false
     */
    public boolean isSymbolsEdited() {
        return _entry.isSymbolsEdited();
    }

    /**
     * Save the catalog symbol information to disk with the user's changes
     */
    public void saveSymbolConfig() {
        SkycatConfigFile.getConfigFile().save();
    }

    /**
     * Return a short name or alias for the catalog
     */
    public String getShortName() {
        return _entry.getShortName();
    }

    /**
     * Return true if the catalog has RA and DEC coordinate columns
     */
    public boolean isWCS() {
        return _entry.getRowCoordinates().isWCS();
    }

    /**
     * Return the value of the "equinox" property, if defined, otherwise 2000.
     */
    public double getEquinox() {
        return _entry.getRowCoordinates().getEquinox();
    }

    /**
     * Return true if the catalog has X and Y columns (assumed to be image pixel coordinates)
     */
    public boolean isPix() {
        return _entry.getRowCoordinates().isPix();
    }

    /**
     * Return true if this is a local catalog, and false if it requires
     * network access or if a query could hang. A local catalog query is
     * run in the event dispatching thread, while others are done in a
     * separate thread.
     */
    public boolean isLocal() {
        return _entry.getServType().equals(LOCAL);
    }

    /**
     * Return true if this object represents an image server.
     */
    public boolean isImageServer() {
        return _entry.getServType().equals(IMAGE_SERVER);
    }


    /**
     * Return the catalog type (normally one of the Catalog constants: CATALOG, ARCHIVE, DIRECTORY, LOCAL, IMAGE_SERVER)
     */
    public String getType() {
        return _entry.getServType();
    }

    /**
     * Set the parent catalog directory
     */
    public void setParent(CatalogDirectory catDir) {
        _entry.setConfigFile(catDir);
    }

    /**
     * Return a reference to the parent catalog directory, or null if not known.
     */
    public CatalogDirectory getParent() {
        return _entry.getConfigFile();
    }


    /**
     * Return an array of Catalog or CatalogDirectory objects representing the
     * path from the root catalog directory to this catalog.
     */
    public Catalog[] getPath() {
        CatalogDirectory parent = getParent();
        if (parent == null)
            return null;

        return parent.getPath(this);
    }

    /** Return queryArgs */
    public QueryArgs getQueryArgs() {
        return new BasicQueryArgs(this);
    }

    /**
     * Query the catalog using the given argument and return the result.
     *
     * @param queryArgs An object describing the query arguments.
     * @return An object describing the result of the query.
     */
    public QueryResult query(QueryArgs queryArgs) throws IOException, CatalogException {
        return query(queryArgs, true);
    }

    /**
     * Query the catalog using the given argument and return the result.
     *
     * @param queryArgs An object describing the query arguments.
     * @param interruptable if true, remote catalog query results are read in a background thread
     * and a progress dialog is displayed where the user can cancel the operation.
     * @return An object describing the result of the query.
     */
    public QueryResult query(QueryArgs queryArgs, boolean interruptable) throws IOException, CatalogException {
        _queryCount++;
        String servType = _entry.getServType();
        if (servType.equals(LOCAL))
            return _queryLocalCatalog(queryArgs);

        if (servType.equals(CATALOG) || servType.equals(ARCHIVE) || servType.equals(NAME_SERVER))
            return _queryCatalog(queryArgs, interruptable);

        if (servType.equals(IMAGE_SERVER))
            return _queryImageServer(queryArgs);

        if (servType.equals(DIRECTORY))
            return _queryCatalogDirectory();

        // XXX other catalog types...
        throw new CatalogException("Query not supported for this catalog type: " + servType);
    }


    /**
     * Query the local catalog using the given argument and return the result.
     *
     * @param queryArgs An object describing the query arguments.
     * @return An object describing the result of the query.
     */
    private QueryResult _queryLocalCatalog(QueryArgs queryArgs) throws CatalogException, IOException {
        String urlStr = _entry.getURL(0);
        if (urlStr != null && urlStr.startsWith("java://"))
            return _queryJavaCatalog(queryArgs);

        // determine the query region and max rows settings
        SearchCondition[] sc = queryArgs.getConditions();
        _setQueryRegion(queryArgs, sc);
        _setMaxRows(queryArgs, sc);

        // The conditions were handled above, so remove them in query args
        queryArgs.setParamValues(null);

        // If this is a local catalog file and the table has been loaded already, use it
        // (The URL is really just a file path name in this case)
        SkycatTable cat = _table;
        if (cat == null) {
            if (urlStr != null) {
                try {
                    cat = new SkycatTable(this, urlStr);
                } catch (Exception e) {
                    InputStream is;
                    try {
                        is = Thread.currentThread().getContextClassLoader()
                                .getResourceAsStream(urlStr);
                    } catch (Exception ee) {
                        ee.printStackTrace();
                        return null;
                    }

                    cat = new SkycatTable(this, is);
                }
            } else {
                return null;
            }
        }

        // do the query
        QueryResult result = cat.query(queryArgs);

        // set a reference to this catalog in the resulting table
        if (result instanceof SkycatTable) {
            ((SkycatTable) result).setCatalog(this);
        }

        return result;
    }


    /**
     * Return an object for displaying the progress of a query.
     */
    protected synchronized StatusLogger getStatusLogger(String title, QueryArgs queryArgs) {
        StatusLogger logger = queryArgs.getStatusLogger();
        if (logger == null) {
            logger = ProgressPanel.makeProgressPanel(title);
        }
        return logger;
    }

//    /**
//     * Query the catalog using the given argument and return the result.
//     *
//     * @param queryArgs An object describing the query arguments.
//     * @return An object describing the result of the query.
//     */
//    private QueryResult _queryCatalog(QueryArgs queryArgs) throws CatalogException, IOException {
//        return _queryCatalog(queryArgs, true);
//    }

    /**
     * Query the catalog using the given argument and return the result.
     *
     * @param queryArgs An object describing the query arguments.
     * @param interruptable if true, remote catalog query results are read in a background thread
     * and a progress dialog is displayed where the user can cancel the operation.
     * @return An object describing the result of the query.
     */
    private QueryResult _queryCatalog(QueryArgs queryArgs, boolean interruptable) throws CatalogException, IOException {
        int n = _entry.getNumURLs();
        for (int i = 0; i < n; i++) {
            String urlStr = _entry.getURL(i);
            if (urlStr != null) {
                urlStr = _getQueryUrl(urlStr, queryArgs);
                if (urlStr.startsWith(File.separator)
                        || (urlStr.length() > 2 && urlStr.charAt(1) == ':')) { // C:\dir\command ...
                    // may be a local command path name (for security, must be from a local config file)
                    return _queryCmdCatalog(queryArgs, urlStr);
                } else if (urlStr.startsWith("java://")) {
                    return _queryJavaCatalog(queryArgs);
                } else {
                    // normal URL
                    return interruptable ? _queryRemoteCatalogBg(queryArgs, urlStr)
                            : _queryRemoteCatalog(queryArgs, urlStr) ;
                }
            }
        }
        throw new RuntimeException("No query URL was specified in the config file.");
    }

    // Query using a local command path name (for security, must be from a local config file)
    private QueryResult _queryCmdCatalog(QueryArgs queryArgs, String urlStr) throws CatalogException, IOException {
        CatalogDirectory catDir = _entry.getConfigFile();
        if (catDir != null && !catDir.isLocal())
            throw new RuntimeException("Invalid catalog URL: " + urlStr
                    + ", in remote config file");
        Process process = Runtime.getRuntime().exec(urlStr);
        InputStream stdout = process.getInputStream();
        SkycatTable cat = new SkycatTable(this, stdout, queryArgs);
        cat.setConfigEntry(_entry);
        return cat;
    }

    // Query a remote catalog using an interruptable  background thread with progress display.
    private QueryResult _queryRemoteCatalogBg(QueryArgs queryArgs, String urlStr) throws CatalogException, IOException {
        urlStr = urlStr.replace(" ", "%20");
        URL queryUrl = new URL(urlStr);
        System.out.println("URL = " + urlStr);
        StatusLogger statusLogger = getStatusLogger("Downloading query results ...", queryArgs);
        ProgressBarFilterInputStream in = null;
        try {
            URLConnection connection = statusLogger.openConnection(queryUrl);
            String contentType = connection.getContentType();
            if (contentType != null && contentType.equals("text/html")) {
                // might be an HTML error from the catalog server
                return new URLQueryResult(queryUrl);
            }
            InputStream ins = _applyFilters(connection.getInputStream());
            in = statusLogger.getLoggedInputStream(ins, connection.getContentLength());
            SkycatTable cat = new SkycatTable(this, in, queryArgs);
            cat.setConfigEntry(_entry);
            return cat;
        } finally {
            if (in != null) {
                statusLogger.stopLoggingInputStream(in);
            }
            statusLogger.stop();
        }
    }

    // Query a remote catalog (no progress display of background thread)
    private QueryResult _queryRemoteCatalog(QueryArgs queryArgs, String urlStr) throws CatalogException, IOException {
        urlStr = urlStr.replace(" ", "%20");
        System.out.println("URL = " + urlStr);
        URL queryUrl = new URL(urlStr);
        InputStream ins = null;
        try {
            URLConnection con = queryUrl.openConnection();
            String contentType = con.getContentType();
            if (contentType != null && contentType.equals("text/html")) {
                // might be an HTML error from the catalog server
                return new URLQueryResult(queryUrl);
            }
            ins = _applyFilters(con.getInputStream());
            SkycatTable cat = new SkycatTable(this, ins, queryArgs);
            cat.setConfigEntry(_entry);
            return cat;
        } finally {
            if (ins != null) ins.close();
        }
    }


    // Hacking a way to do a catalog query without the popup dialog... This
    // is just the same code from above but without the popup.
    // TODO: Refactor SkycatCatalog to accept a listener for events that are
    // TODO: currently displayed in the popup.
    // TODO: Make the popup implement the listener interface and add a call
    // TODO: to query that takes an optional (possibly null) listener.
    // TODO: Add the query with listener method to the interface.
    // TODO: Make the current query create the popup dialog listener and call
    // TODO: the more generic method with the listener arg.  I guess ...
    //
    // XXX allan: Use query(args, false) to turn off background thread/progress bar
    public QueryResult noPopupCatalogQuery(QueryArgs queryArgs) throws CatalogException, IOException {
        ++_queryCount;
        int n = _entry.getNumURLs();
        for (int i = 0; i < n; i++) {
            String urlStr = _entry.getURL(i);
            if (urlStr == null) continue;
            urlStr = _getQueryUrl(urlStr, queryArgs).replace(" ", "%20");
            System.out.println("URL = " + urlStr);
            URL queryUrl = new URL(urlStr);
            InputStream ins = null;
            try {
                URLConnection con = queryUrl.openConnection();
                String contentType = con.getContentType();
                if (contentType != null && contentType.equals("text/html")) {
                    // might be an HTML error from the catalog server
                    return new URLQueryResult(queryUrl);
                }
                ins = _applyFilters(con.getInputStream());
                SkycatTable cat = new SkycatTable(this, ins, queryArgs);
                cat.setConfigEntry(_entry);
                return cat;
            } finally {
                if (ins != null) ins.close();
            }
        }
        throw new RuntimeException("No query URL was specified in the config file.");
    }

    /**
     * Query the catalog using the given argument and return the result.
     *
     * @param queryArgs An object describing the query arguments.
     * @return An object describing the result of the query.
     */
    private QueryResult _queryImageServer(QueryArgs queryArgs) throws CatalogException, IOException {
        int n = _entry.getNumURLs();
        for (int i = 0; i < n; i++) {
            String urlStr = _entry.getURL(i);
            if (urlStr != null) {
                urlStr = _getQueryUrl(urlStr, queryArgs);
                if (urlStr.startsWith(File.separator)) {
                    // may be a local command path name
                    throw new RuntimeException("Local commands not supported for image server (yet)");
                } else {
                    // normal URL
                    return new URLQueryResult(new URL(urlStr));
                }
            }
        }
        throw new RuntimeException("No query URL was specified in the config file.");
    }

    /**
     * Return a query result listing the contents of the catalog directory.
     *
     * @return An object describing the result of the query.
     */
    private QueryResult _queryCatalogDirectory() {
        int numURLs = _entry.getNumURLs();
        for (int i = 0; i < numURLs; i++) {
            try {
                return new URLQueryResult(new URL(_entry.getURL(0)));
            } catch (Exception e) {
                if (i == (numURLs - 1))
                    throw new RuntimeException(e);
            }
        }
        throw new RuntimeException("No URL was specified in the config file.");
    }

    /**
     * Return the result of a query to a Java class based catalog.
     * The catalog class name should be specified in the catalog config
     * URL in the format: java://<classname>?<arg1>&<arg2>&...&<argn>.
     *
     * @param queryArgs An object describing the query arguments (not used here)
     * @return An object describing the result of the query.
     */
    private QueryResult _queryJavaCatalog(QueryArgs queryArgs) throws CatalogException, IOException {
        QueryResult result = null;

        // determine the query region and max rows settings
        SearchCondition[] sc = queryArgs.getConditions();
        _setQueryRegion(queryArgs, sc);
        _setMaxRows(queryArgs, sc);

        String urlStr = _entry.getURL(0);
        if (urlStr != null) {
            StringTokenizer token = new StringTokenizer(urlStr.substring(7), "?\t");
            //urlStr = _getQueryUrl(urlStr, queryArgs);
            String className = token.nextToken();
            try {
                Class catalogClass = Class.forName(className);
                Catalog catalog = (Catalog) catalogClass.newInstance();
                result = catalog.query(queryArgs);
                if (result instanceof MemoryCatalog && !(result instanceof SkycatTable)) {
                    MemoryCatalog mcat = (MemoryCatalog) result;
                    result = new SkycatTable(_entry, mcat.getDataVector(), mcat.getFields());
                }
            } catch (Exception e) {
                if (e instanceof IOException)
                    throw (IOException) e;
                if (e instanceof CatalogException) {
                    throw (CatalogException)e;
                }
                throw new RuntimeException(e);
            }
        }

        // set a reference to this catalog in the resulting table
        if (result instanceof SkycatTable) {
            ((SkycatTable) result).setCatalog(this);
        }

        return result;
    }


    /**
     * Given a URL string with variables (%ra, %dec, etc.), substitute the
     * variables and return the new URL string.
     * <p/>
     * The following substitutions are then performed on the given URL:
     * <p/>
     * %ra, %dec   - world coordinates of center point (for catalogs based in wcs)
     * <p/>
     * %x, %y      - image coordinates of center point (for pixel based catalogs)
     * <p/>
     * %r1, %r2    - min and max radius (for circular query)
     * <p/>
     * %m1, %m2    - min and max magnitude
     * <p/>
     * %n          - max number of rows to return
     * <p/>
     * %id         - ID field of item to return (if supported)
     * <p/>
     * %mime-type  - value for http mime-type field
     * <p/>
     * %cond       - insert search condition, if any, in the format: col1=minVal,maxVal&col2=minVal,maxVal,...
     *
     * @param urlStr    A string containing the raw URL, before substitution.
     * @param queryArgs An object describing the query arguments.
     * @return The substituted, expanded URL to use to make the query.
     */
    private String _getQueryUrl(String urlStr, QueryArgs queryArgs) throws CatalogException, IOException {

        if (_entry.getNumParams() == 0)
            return urlStr;

        int n = urlStr.length();
        StringBuffer buf = new StringBuffer(n * 2);
        boolean urlHasId = false, urlHasRaDec = false, urlHasXy = false;
        SearchCondition[] sc = queryArgs.getConditions();

        // determine the query region and max rows settings
        _setQueryRegion(queryArgs, sc);
        _setMaxRows(queryArgs, sc);

        // expand the variables in the catalog server URL
        for (int c = 0; c < n;) {
            if (urlStr.charAt(c) == '%') {
                c++;
                if (urlStr.charAt(c) == '%') {
                    // make "%%" expand to "%"
                    buf.append('%');
                    c++;
                } else if (urlStr.startsWith("id", c)) {
                    // %id
                    String id = queryArgs.getId();
                    if (id == null)
                        id = queryArgs.getParamValueAsString("id", null);
                    if (id != null)
                        buf.append(id);
                    c += 2;
                    urlHasId = true;
                } else if (urlStr.startsWith("ra", c)) {
                    CoordinateRadius region = queryArgs.getRegion();
                    if (region != null) {
                        WorldCoords pos = (WorldCoords) region.getCenterPosition();
                        String ra = pos.getRA().toString();
                        if (ra.startsWith("+")) {
                            ra = ra.substring(1);
                        }
                        buf.append(ra);
                    }
                    c += 2;
                    urlHasRaDec = true;
                } else if (urlStr.startsWith("dec", c) || urlStr.startsWith("plusdec", c)) {
                    boolean plusdec = urlStr.startsWith("plusdec", c);
                    CoordinateRadius region = queryArgs.getRegion();
                    if (region != null) {
                        WorldCoords pos = (WorldCoords) region.getCenterPosition();
                        String dec = pos.getDec().toString();
                        // Some servers require that the dec start with a +
                        // sign.  Some won't work if it does.  Differentiate
                        // between the two in skycat.cfg by whether the key
                        // is "dec" (strip + if exists) or "plusdec" (add + if
                        // missing sign).
                        if (dec.startsWith("+")) {
                            if (!plusdec) dec = dec.substring(1);
                        } else if (plusdec && !dec.startsWith("-")) {
                            dec = "+" + dec;
                        }
                        buf.append(dec);
                    }
                    c += plusdec ? 7 : 3;
                    urlHasRaDec = true;
                } else if (urlStr.charAt(c) == 'x') {
                    CoordinateRadius region = queryArgs.getRegion();
                    if (region != null) {
                        ImageCoords pos = (ImageCoords) region.getCenterPosition();
                        buf.append(pos.getX());
                    }
                    c++;
                    urlHasXy = true;
                } else if (urlStr.charAt(c) == 'y') {
                    CoordinateRadius region = queryArgs.getRegion();
                    if (region != null) {
                        ImageCoords pos = (ImageCoords) region.getCenterPosition();
                        buf.append(pos.getY());
                    }
                    c++;
                    urlHasXy = true;
                } else if (urlStr.startsWith("r1", c)) {
                    CoordinateRadius region = queryArgs.getRegion();
                    if (region != null)
                        if (region.getMinRadius() != 0.0 || region.getMaxRadius() != 0.0)
                            buf.append(region.getMinRadius());
                    c += 2;
                } else if (urlStr.startsWith("r2", c)) {
                    CoordinateRadius region = queryArgs.getRegion();
                    if (region != null)
                        if (region.getMinRadius() != 0.0 || region.getMaxRadius() != 0.0)
                            buf.append(region.getMaxRadius());
                    c += 2;
                } else if (urlStr.charAt(c) == 'w') {
                    if (sc != null && sc.length > 0) {
                        for (SearchCondition aSc : sc) {
                            if (aSc.getName().equals(SkycatConfigEntry.WIDTH)) {
                                buf.append(aSc.getValueAsString());
                                break;
                            }
                        }
                    }
                    c++;
                } else if (urlStr.charAt(c) == 'h') {
                    if (sc != null && sc.length > 0) {
                        for (SearchCondition aSc : sc) {
                            if (aSc.getName().equals(SkycatConfigEntry.HEIGHT)) {
                                buf.append(aSc.getValueAsString());
                                break;
                            }
                        }
                    }
                    c++;
                } else if (urlStr.startsWith("m1", c)) { // brightest
                    if (sc != null && sc.length > 0) {
                        for (SearchCondition aSc : sc) {
                            if (aSc.getName().equals(SkycatConfigEntry.BRIGHTEST)) {
                                buf.append(aSc.getValueAsString());
                                break;
                            }
                        }
                    }
                    c += 2;
                } else if (urlStr.startsWith("m2", c)) { // faintest
                    if (sc != null && sc.length > 0) {
                        for (SearchCondition aSc : sc) {
                            if (aSc.getName().equals(SkycatConfigEntry.FAINTEST)) {
                                buf.append(aSc.getValueAsString());
                            }
                        }
                    }
                    c += 2;
                } else if (urlStr.startsWith("BAND", c)) { // band to apply magnitude limits
                    if (sc != null && sc.length > 0) {
                        for (SearchCondition aSc : sc) {
                            if (aSc.getName().equals(SkycatConfigEntry.BAND)) {
                                String band = aSc.getValueAsString();
                                if(band.equals("R")){
                                    band="f.";
                                }
                                buf.append(band);
                                break;
                            }
                        }
                    }
                    c += 4;
                } else if (urlStr.charAt(c) == 'n') {
                    if (queryArgs.getMaxRows() > 0)
                        buf.append(queryArgs.getMaxRows());
                    c++;
                } else if (urlStr.startsWith("cond", c)) {
                    // insert a list of conditions (param names and min/max values)
                    if (sc != null && sc.length > 0) {
                        String s = urlStr.substring(c);
                        String sep = ",";
                        // check for optional separator: for example: %cond(..)
                        if (s.startsWith("cond(") && s.contains(")")) {
                            sep = s.substring(5, s.indexOf(')'));
                            c += sep.length() + 2; // skip over
                        }
                        for (int i = 0; i < sc.length; i++) {
                            // Note: ignore the "standard" parameters: defined in SkycatConfigEntry.determineSearchParameters()
                            if (_isStandardParam(sc[i].getName())) {
                                continue;
                            }

                            buf.append(sc[i].toString(sep));
                            if (i < sc.length - 1)
                                buf.append('&');
                        }
                    }
                    c += 4;
                } else if (urlStr.startsWith("mime-type", c)) {
                    buf.append("application/x-fits"); // XXX should be hard coded in the config file?
                    c += 9;
                }
            } else {
                buf.append(urlStr.charAt(c++));
            }
        }

        // report an error if the caller specified an id, but there is none in the URL
        if (!urlHasId && queryArgs.getId() != null && queryArgs.getId().length() != 0)
            throw new CatalogException(_entry.getName() + " does not support search by id");

        // report an error if the caller supplied a position, but there is none in the URL
        if (queryArgs.getRegion() != null) {
            if (queryArgs.getRegion().getCenterPosition() instanceof WorldCoords && !urlHasRaDec)
                throw new CatalogException(_entry.getName() + " does not support search by World Coordinates");

            if (queryArgs.getRegion().getCenterPosition() instanceof ImageCoords && !urlHasXy)
                throw new CatalogException(_entry.getName() + " does not support search by image coordinates");
        }

        LOG.log(Level.FINE, "URL = " + buf.toString());
        return buf.toString();
    }

    // Returns true if name is one of the standard query parameters.
    // See SkycatConfigEntry.determineSearchParameters()
    private boolean _isStandardParam(String name) {
        return (isWCS() && (name.equalsIgnoreCase(SkycatConfigEntry.OBJECT)
                || name.equalsIgnoreCase(SkycatConfigEntry.RA)
                || name.equalsIgnoreCase(SkycatConfigEntry.DEC)
                || name.equalsIgnoreCase(SkycatConfigEntry.EQUINOX)))
                || (isPix() && (name.equalsIgnoreCase(SkycatConfigEntry.X)
                || name.equalsIgnoreCase(SkycatConfigEntry.Y)))
                || name.equalsIgnoreCase(SkycatConfigEntry.MIN_RADIUS)
                || name.equalsIgnoreCase(SkycatConfigEntry.MAX_RADIUS)
                || name.equalsIgnoreCase(SkycatConfigEntry.WIDTH)
                || name.equalsIgnoreCase(SkycatConfigEntry.HEIGHT)
                || name.equalsIgnoreCase(SkycatConfigEntry.FAINTEST)
                || name.equalsIgnoreCase(SkycatConfigEntry.BRIGHTEST)
                || name.equalsIgnoreCase(SkycatConfigEntry.BAND)
                || name.equalsIgnoreCase(SkycatConfigEntry.MAX_OBJECTS);
    }

    /**
     * Return the equinox setting from the given query arguments object.
     */
    private double _getEquinox(QueryArgs queryArgs) {
        String equinoxStr = (String) queryArgs.getParamValue(SkycatConfigEntry.EQUINOX);
        double equinox = 2000.;
        if (equinoxStr != null && equinoxStr.endsWith("1950"))
            equinox = 1950.;
        return equinox;
    }

    /**
     * Determine the query region based on the given query arguments
     */
    protected void _setQueryRegion(QueryArgs queryArgs, SearchCondition[] sc) throws CatalogException, IOException {
        if (queryArgs.getRegion() != null || sc == null || sc.length == 0)
            return;

        // look for a min and max radius parameters
        Double r1 = (Double) queryArgs.getParamValue(SkycatConfigEntry.MIN_RADIUS);
        Double r2 = (Double) queryArgs.getParamValue(SkycatConfigEntry.MAX_RADIUS);
        if (r1 != null || r2 != null) {
            if (r1 != null) {
                if (r2 == null) {
                    r2 = r1;
                    r1 = 0.;
                }
            } else {
                r1 = 0.;
            }
        } else {
            // look for a radius search condition
            for (SearchCondition aSc : sc) {
                String name = aSc.getName();
                if (name.equalsIgnoreCase("radius") && aSc instanceof RangeSearchCondition) {
                    RangeSearchCondition rsc = (RangeSearchCondition) aSc;
                    r1 = (Double) rsc.getMinVal();
                    r2 = (Double) rsc.getMaxVal();
                    break;
                }
            }
        }
        if (r1 == null && r2 == null) {
            // use default values
            r1 = 0.;
            r2 = 10.;
        }

        // look for the center position parameters
        if (isWCS()) {
            WorldCoords wcs = null;
            String objectName = (String) queryArgs.getParamValue(SkycatConfigEntry.OBJECT);
            if (objectName == null || objectName.length() == 0) {
                // no object name specified, check RA and Dec
                String raStr = (String) queryArgs.getParamValue(SkycatConfigEntry.RA);
                String decStr = (String) queryArgs.getParamValue(SkycatConfigEntry.DEC);
                if (raStr == null || decStr == null)
                    return;
                double equinox = _getEquinox(queryArgs);
                wcs = new WorldCoords(raStr, decStr, equinox, true);
            } else {
                // an object name was specified, which needs to be resolved with a nameserver
                Object o = queryArgs.getParamValue(SkycatConfigEntry.NAME_SERVER);
                if (o instanceof Catalog) {
                    wcs = _resolveObjectName(objectName, (Catalog) o);
                } else {
                    //just a sanity check. Shoouldn't ever happen.
                    if (queryArgs.getCatalog() == null) {
                        throw new RuntimeException("No name server was specified");
                    }
                }
            }
            //If the wcs are defined, we assume the catalog support WCS queries
            if (wcs != null) {
                queryArgs.setRegion(new CoordinateRadius(wcs, r1, r2));
            } else { //just query using the id supplied. At this stage, objectName can't be null
                if (objectName.length() > 0) {
                    queryArgs.setId(objectName);
                }
            }
        } else if (isPix()) {
            Double x = (Double) queryArgs.getParamValue(SkycatConfigEntry.X);
            Double y = (Double) queryArgs.getParamValue(SkycatConfigEntry.Y);
            if (x == null || y == null)
                return;
            ImageCoords ic = new ImageCoords(x.intValue(), y.intValue());
            queryArgs.setRegion(new CoordinateRadius(ic, r1, r2));
        }
    }


    /**
     * Resolve the given astronomical object name using the given name server
     * and return the world coordinates corresponding the name.
     */
    private WorldCoords _resolveObjectName(String objectName, Catalog cat) throws CatalogException, IOException {
        QueryArgs queryArgs = new BasicQueryArgs(cat);
        queryArgs.setId(objectName);
        QueryResult r = cat.query(queryArgs);
        if (r instanceof TableQueryResult) {
            Coordinates coords = ((TableQueryResult) r).getCoordinates(0);
            if (coords instanceof WorldCoords)
                return (WorldCoords) coords;
            if (coords == null) {
                throw new CatalogException("No result found in " + cat.toString());
            }
        }
        throw new RuntimeException("Unexpected result from " + cat.toString());
    }


    /**
     * Check for a "Max Objects" argument and if found, set queryArgs.maxRows with the value.
     */
    protected void _setMaxRows(QueryArgs queryArgs, SearchCondition[] sc) {
        if (queryArgs.getMaxRows() != 0 || sc == null || sc.length == 0)
            return;

        // look for a min and max radius parameters
        Integer maxObjects = (Integer) queryArgs.getParamValue(SkycatConfigEntry.MAX_OBJECTS);
        if (maxObjects != null)
            queryArgs.setMaxRows(maxObjects);
    }

    /**
     * Optional handler, used to report HTML format errors from HTTP servers
     */
    public void setHTMLQueryResultHandler(HTMLQueryResultHandler handler) {
        _htmlQueryResultHandler = handler;
    }

    /**
     * Given a description of a region of the sky (center point and radius range),
     * and the current query argument settings, set the values of the corresponding
     * query parameters.
     *
     * @param queryArgs (in/out) describes the query arguments
     * @param region    (in) describes the query region (center and radius range)
     */
    public void setRegionArgs(QueryArgs queryArgs, CoordinateRadius region) {
        Coordinates coords = region.getCenterPosition();
        RowCoordinates rowCoordinates = _entry.getRowCoordinates();
        String equinoxStr = (String) queryArgs.getParamValue(SkycatConfigEntry.EQUINOX);
        double equinox = _getEquinox(queryArgs);
        if (rowCoordinates.isWCS()) {
            WorldCoords pos = (WorldCoords) coords;
            String[] radec = pos.format(equinox);
            queryArgs.setParamValue(SkycatConfigEntry.RA, radec[0]);
            queryArgs.setParamValue(SkycatConfigEntry.DEC, radec[1]);
            queryArgs.setParamValue(SkycatConfigEntry.EQUINOX, equinoxStr);
            queryArgs.setParamValue(SkycatConfigEntry.MIN_RADIUS, region.getMinRadius());
            queryArgs.setParamValue(SkycatConfigEntry.MAX_RADIUS, region.getMaxRadius());
            queryArgs.setParamValue(SkycatConfigEntry.WIDTH, region.getWidth());
            queryArgs.setParamValue(SkycatConfigEntry.HEIGHT, region.getHeight());
        } else if (rowCoordinates.isPix()) {
            ImageCoords pos = (ImageCoords) coords;
            queryArgs.setParamValue(SkycatConfigEntry.X, pos.getX());
            queryArgs.setParamValue(SkycatConfigEntry.Y, pos.getY());
            queryArgs.setParamValue(SkycatConfigEntry.MIN_RADIUS, region.getMinRadius());
            queryArgs.setParamValue(SkycatConfigEntry.MAX_RADIUS, region.getMaxRadius());
            queryArgs.setParamValue(SkycatConfigEntry.WIDTH, region.getWidth());
            queryArgs.setParamValue(SkycatConfigEntry.HEIGHT, region.getHeight());
        }
    }

    /**
     * Add a <code>ICatalogFilter</code> to the list of filters to be applied to
     * the query result. Filters will be applied in the order they are added.
     *
     * @param filter the <code>ICatalogFilter</code> to be added.
     */
    public void addCatalogFilter(ICatalogFilter filter) {
        if (filter == null) return;
        if (catFilterList == null) {
            catFilterList = new ArrayList<>();
        }
        //avoid duplicate filters
        if (!catFilterList.contains(filter)) {
            catFilterList.add(filter);
        }
    }

    /**
     * Apply the filters (if any) to the InputStream.
     *
     * @param is The original Input stream
     * @return a filtered InputStream after applying the set of filters
     * @throws IOException in case of I/O error while processing the InputStream
     */

    private InputStream _applyFilters(InputStream is) throws IOException {
        if (catFilterList == null) return is;
        for (ICatalogFilter filter : catFilterList) {
            is = filter.filterContent(is);
        }
        return is;
    }

    /**
     * Return a the html query result handler
     */
    public HTMLQueryResultHandler getHtmlQueryResultHandler() {
        return _htmlQueryResultHandler;
    }

    /**
     * Test cases
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("usage: java -classpath ... SkycatCatalog catalogName");
            System.exit(1);
        }
        String catalogName = args[0];
        SkycatConfigFile configFile = SkycatConfigFile.getConfigFile();
        final Catalog cat = configFile.getCatalog(catalogName);
        if (cat == null) {
            System.out.println("Can't find entry for catalog: " + catalogName);
            System.exit(1);
        }

        try {
            String originalUrls[] = null;
            WorldCoords wc = null;
            QueryArgs queryArgs = new BasicQueryArgs(cat);
            queryArgs.setId("M31");
            queryArgs.setMaxRows(1);
            if (cat instanceof SkycatCatalog) {
                SkycatCatalog skycat = (SkycatCatalog) cat;
                if (skycat.getShortName().contains("simbad")) {
                    skycat.addCatalogFilter(FullMimeSimbadCatalogFilter.getFilter());
                    int n = skycat.getConfigEntry().getNumURLs();
                    String[] urls = new String[n];
                    originalUrls = new String[n];
                    for (int i = 0; i < n; i++) {
                        String urlStr = skycat.getConfigEntry().getURL(i);
                        originalUrls[i] = urlStr;
                        urlStr += "/mimetype=full-rec";
                        urls[i] = urlStr;
                    }
                    skycat.getConfigEntry().setURLs(urls);
                }
            }
            QueryResult r = cat.query(queryArgs);
            if (cat instanceof SkycatCatalog) {
                SkycatCatalog skycat = (SkycatCatalog) cat;
                if (skycat.getShortName().contains("simbad")) {
                    skycat.getConfigEntry().setURLs(originalUrls);
                }
            }

            if (r instanceof TableQueryResult) {
                TableQueryResult tqr = (TableQueryResult) r;
                if (tqr.getRowCount() > 0) {
                    wc = (WorldCoords) tqr.getCoordinates(0);
                    Vector<String> v = tqr.getColumnIdentifiers();
                    for (String s : v) {
                        System.out.println("Column: " + s);
                    }
                    int pm = tqr.getColumnIndex("pm1");
                    if (pm >= 0) {
                        Double pm1 = (Double) tqr.getValueAt(0, pm);
                        Double pm2 = (Double) tqr.getValueAt(0, pm + 1);
                        System.out.println("PM1 : " + pm1);
                        System.out.println("PM2 : " + pm2);
                    }
                } else {
                    throw new CatalogException("No objects were found.");
                }
            }
            if (wc != null) {
                System.out.println("RA  : " + wc.getRA().toString());
                System.out.println("Dec : " + wc.getDec().toString());
            }

            System.out.println("");
            System.out.println("test query: at center position/radius: ");
            QueryArgs q2 = new BasicQueryArgs(cat);
            q2.setRegion(new CoordinateRadius(new WorldCoords("03:19:44.44", "+41:30:58.21"), 2.));
            QueryResult r2 = cat.query(q2);
            System.out.println("result: " + r2);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }
}
