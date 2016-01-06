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
import java.util.StringTokenizer;

/**
 * Represents a catalog as described in a Skycat style catalog
 * config file. The (keyword: value) pairs in the config file are stored
 * here in a Properties object.
 */
@Deprecated
public class SkycatCatalog implements PlotableCatalog {

    private static Logger LOG = Logger.getLogger(SkycatCatalog.class.getName());

    /**
     * The catalog configuration entry for this catalog.
     */
    private SkycatConfigEntry _entry;

    /**
     * If this is a local catalog, this may optionally point to the data
     */
    private SkycatTable _table;

    /**
     * Initialize the catalog from the given catalog configuration entry.
     *
     * @param entry the catalog configuration file entry describing the catalog
     */
    public SkycatCatalog(final SkycatConfigEntry entry) {
        _entry = entry;
    }

    /**
     * Initialize the catalog from the given catalog configuration entry.
     *
     * @param entry the catalog configuration file entry describing the catalog
     * @param table the data for the catalog (optional, only for local catalgs)
     */
    public SkycatCatalog(final SkycatConfigEntry entry, final SkycatTable table) {
        this(entry);
        _table = table;
    }

    /**
     * Implementation of the clone method (makes a shallow copy).
     */
    @SuppressWarnings({"CloneDoesntDeclareCloneNotSupportedException"})
    public Object clone() {
        try {
            return super.clone();
        } catch (final CloneNotSupportedException ex) {
            throw new InternalError(); // won't happen
        }
    }


    /**
     * Set the name of the catalog
     */
    public void setName(final String name) {
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
    public FieldDesc getParamDesc(final int i) {
        return _entry.getParamDesc(i);
    }

    /**
     * Return a description of the named query parameter
     */
    public FieldDesc getParamDesc(final String name) {
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
    public TablePlotSymbol getSymbolDesc(final int i) {
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
    public void setSymbols(final TablePlotSymbol[] symbols) {
        _entry.setSymbols(symbols);
    }

    /**
     * Set to true if the user edited the plot symbol definitions (default: false)
     */
    public void setSymbolsEdited(final boolean edited) {
        _entry.setSymbolsEdited(edited);
    }

    /**
     * Return true if the user edited the plot symbol definitions otherwise false
     */
    public boolean isSymbolsEdited() {
        return _entry.isSymbolsEdited();
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
    public void setParent(final CatalogDirectory catDir) {
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
        final CatalogDirectory parent = getParent();
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
    public QueryResult query(final QueryArgs queryArgs) throws IOException, CatalogException {
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
    public QueryResult query(final QueryArgs queryArgs, final boolean interruptable) throws IOException, CatalogException {
        final String servType = _entry.getServType();
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
    private QueryResult _queryLocalCatalog(final QueryArgs queryArgs) throws CatalogException, IOException {
        final String urlStr = _entry.getURL(0);
        if (urlStr != null && urlStr.startsWith("java://"))
            return _queryJavaCatalog(queryArgs);

        // determine the query region and max rows settings
        final SearchCondition[] sc = queryArgs.getConditions();
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
                } catch (final Exception e) {
                    final InputStream is;
                    try {
                        is = Thread.currentThread().getContextClassLoader()
                                .getResourceAsStream(urlStr);
                    } catch (final Exception ee) {
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
        final QueryResult result = cat.query(queryArgs);

        // set a reference to this catalog in the resulting table
        if (result instanceof SkycatTable) {
            ((SkycatTable) result).setCatalog(this);
        }

        return result;
    }


    /**
     * Return an object for displaying the progress of a query.
     */
    protected synchronized StatusLogger getStatusLogger(final String title, final QueryArgs queryArgs) {
        final StatusLogger logger = queryArgs.getStatusLogger();
        return logger == null ? ProgressPanel.makeProgressPanel(title) : logger;
    }

    /**
     * Query the catalog using the given argument and return the result.
     *
     * @param queryArgs An object describing the query arguments.
     * @param interruptable if true, remote catalog query results are read in a background thread
     * and a progress dialog is displayed where the user can cancel the operation.
     * @return An object describing the result of the query.
     */
    private QueryResult _queryCatalog(final QueryArgs queryArgs, final boolean interruptable) throws CatalogException, IOException {
        final int n = _entry.getNumURLs();
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
    private QueryResult _queryCmdCatalog(final QueryArgs queryArgs, final String urlStr) throws CatalogException, IOException {
        final CatalogDirectory catDir = _entry.getConfigFile();
        if (catDir != null && !catDir.isLocal())
            throw new RuntimeException("Invalid catalog URL: " + urlStr
                    + ", in remote config file");
        final Process process = Runtime.getRuntime().exec(urlStr);
        final InputStream stdout = process.getInputStream();
        final SkycatTable cat = new SkycatTable(this, stdout, queryArgs);
        cat.setConfigEntry(_entry);
        return cat;
    }

    // Query a remote catalog using an interruptable  background thread with progress display.
    private QueryResult _queryRemoteCatalogBg(final QueryArgs queryArgs, final String urlStr) throws CatalogException, IOException {
        final String rurlStr = urlStr.replace(" ", "%20");
        final URL queryUrl = new URL(rurlStr);
        System.out.println("URL = " + rurlStr);
        final StatusLogger statusLogger = getStatusLogger("Downloading query results ...", queryArgs);
        ProgressBarFilterInputStream in = null;
        try {
            final URLConnection connection = statusLogger.openConnection(queryUrl);
            final String contentType = connection.getContentType();
            if (contentType != null && contentType.equals("text/html")) {
                // might be an HTML error from the catalog server
                return new URLQueryResult(queryUrl);
            }
            final InputStream ins = connection.getInputStream();
            in = statusLogger.getLoggedInputStream(ins, connection.getContentLength());
            final SkycatTable cat = new SkycatTable(this, in, queryArgs);
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
    private QueryResult _queryRemoteCatalog(final QueryArgs queryArgs, final String urlStr) throws CatalogException, IOException {
        final String rurlStr = urlStr.replace(" ", "%20");
        System.out.println("URL = " + rurlStr);
        URL queryUrl = new URL(rurlStr);
        InputStream ins = null;
        try {
            final URLConnection con = queryUrl.openConnection();
            final String contentType = con.getContentType();
            if (contentType != null && contentType.equals("text/html")) {
                // might be an HTML error from the catalog server
                return new URLQueryResult(queryUrl);
            }
            ins = con.getInputStream();
            final SkycatTable cat = new SkycatTable(this, ins, queryArgs);
            cat.setConfigEntry(_entry);
            return cat;
        } finally {
            if (ins != null) ins.close();
        }
    }

    /**
     * Query the catalog using the given argument and return the result.
     *
     * @param queryArgs An object describing the query arguments.
     * @return An object describing the result of the query.
     */
    private QueryResult _queryImageServer(final QueryArgs queryArgs) throws CatalogException, IOException {
        final int n = _entry.getNumURLs();
        for (int i = 0; i < n; i++) {
            final String urlStr = _entry.getURL(i);
            if (urlStr != null) {
                final String qurlStr = _getQueryUrl(urlStr, queryArgs);
                if (qurlStr.startsWith(File.separator)) {
                    // may be a local command path name
                    throw new RuntimeException("Local commands not supported for image server (yet)");
                } else {
                    // normal URL
                    return new URLQueryResult(new URL(qurlStr));
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
        final int numURLs = _entry.getNumURLs();
        for (int i = 0; i < numURLs; i++) {
            try {
                return new URLQueryResult(new URL(_entry.getURL(0)));
            } catch (final Exception e) {
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
    @SuppressWarnings("unchecked")
    private QueryResult _queryJavaCatalog(final QueryArgs queryArgs) throws CatalogException, IOException {
        QueryResult result = null;

        // determine the query region and max rows settings
        final SearchCondition[] sc = queryArgs.getConditions();
        _setQueryRegion(queryArgs, sc);
        _setMaxRows(queryArgs, sc);

        final String urlStr = _entry.getURL(0);
        if (urlStr != null) {
            final StringTokenizer token = new StringTokenizer(urlStr.substring(7), "?\t");
            //urlStr = _getQueryUrl(urlStr, queryArgs);
            final String className = token.nextToken();
            try {
                final Class<?> catalogClass = Class.forName(className);
                final Catalog catalog = (Catalog) catalogClass.newInstance();

                result = catalog.query(queryArgs);
                if (result instanceof MemoryCatalog && !(result instanceof SkycatTable)) {
                    final MemoryCatalog mcat = (MemoryCatalog) result;
                    result = new SkycatTable(_entry, mcat.getDataVector(), mcat.getFields());
                }
            } catch (final Exception e) {
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
    private String _getQueryUrl(final String urlStr, final QueryArgs queryArgs) throws CatalogException, IOException {

        if (_entry.getNumParams() == 0)
            return urlStr;

        final int n = urlStr.length();
        final StringBuilder buf = new StringBuilder(n * 2);
        boolean urlHasId = false, urlHasRaDec = false, urlHasXy = false;
        final SearchCondition[] sc = queryArgs.getConditions();

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
                    final CoordinateRadius region = queryArgs.getRegion();
                    if (region != null) {
                        final WorldCoords pos = (WorldCoords) region.getCenterPosition();
                        String ra = pos.getRA().toString();
                        if (ra.startsWith("+")) {
                            ra = ra.substring(1);
                        }
                        buf.append(ra);
                    }
                    c += 2;
                    urlHasRaDec = true;
                } else if (urlStr.startsWith("dec", c) || urlStr.startsWith("plusdec", c)) {
                    final boolean plusdec = urlStr.startsWith("plusdec", c);
                    final CoordinateRadius region = queryArgs.getRegion();
                    if (region != null) {
                        final WorldCoords pos = (WorldCoords) region.getCenterPosition();
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
                    final CoordinateRadius region = queryArgs.getRegion();
                    if (region != null) {
                        final ImageCoords pos = (ImageCoords) region.getCenterPosition();
                        buf.append(pos.getX());
                    }
                    c++;
                    urlHasXy = true;
                } else if (urlStr.charAt(c) == 'y') {
                    final CoordinateRadius region = queryArgs.getRegion();
                    if (region != null) {
                        final ImageCoords pos = (ImageCoords) region.getCenterPosition();
                        buf.append(pos.getY());
                    }
                    c++;
                    urlHasXy = true;
                } else if (urlStr.startsWith("r1", c)) {
                    final CoordinateRadius region = queryArgs.getRegion();
                    if (region != null)
                        if (region.getMinRadius() != 0.0 || region.getMaxRadius() != 0.0)
                            buf.append(region.getMinRadius());
                    c += 2;
                } else if (urlStr.startsWith("r2", c)) {
                    final CoordinateRadius region = queryArgs.getRegion();
                    if (region != null)
                        if (region.getMinRadius() != 0.0 || region.getMaxRadius() != 0.0)
                            buf.append(region.getMaxRadius());
                    c += 2;
                } else if (urlStr.charAt(c) == 'w') {
                    if (sc != null && sc.length > 0) {
                        for (final SearchCondition aSc : sc) {
                            if (aSc.getName().equals(SkycatConfigEntry.WIDTH)) {
                                buf.append(aSc.getValueAsString());
                                break;
                            }
                        }
                    }
                    c++;
                } else if (urlStr.charAt(c) == 'h') {
                    if (sc != null && sc.length > 0) {
                        for (final SearchCondition aSc : sc) {
                            if (aSc.getName().equals(SkycatConfigEntry.HEIGHT)) {
                                buf.append(aSc.getValueAsString());
                                break;
                            }
                        }
                    }
                    c++;
                } else if (urlStr.startsWith("m1", c)) { // brightest
                    if (sc != null && sc.length > 0) {
                        for (final SearchCondition aSc : sc) {
                            if (aSc.getName().equals(SkycatConfigEntry.BRIGHTEST)) {
                                buf.append(aSc.getValueAsString());
                                break;
                            }
                        }
                    }
                    c += 2;
                } else if (urlStr.startsWith("m2", c)) { // faintest
                    if (sc != null && sc.length > 0) {
                        for (final SearchCondition aSc : sc) {
                            if (aSc.getName().equals(SkycatConfigEntry.FAINTEST)) {
                                buf.append(aSc.getValueAsString());
                            }
                        }
                    }
                    c += 2;
                } else if (urlStr.startsWith("BAND", c)) { // band to apply magnitude limits
                    if (sc != null && sc.length > 0) {
                        for (final SearchCondition aSc : sc) {
                            if (aSc.getName().equals(SkycatConfigEntry.BAND)) {
                                String band = aSc.getValueAsString();
                                if (band.equals("R")){
                                    band = "f.";
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
                        final String s = urlStr.substring(c);
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
    private boolean _isStandardParam(final String name) {
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
    private double _getEquinox(final QueryArgs queryArgs) {
        final String equinoxStr = (String) queryArgs.getParamValue(SkycatConfigEntry.EQUINOX);
        double equinox = 2000.;
        if (equinoxStr != null && equinoxStr.endsWith("1950"))
            equinox = 1950.;
        return equinox;
    }

    /**
     * Determine the query region based on the given query arguments
     */
    protected void _setQueryRegion(final QueryArgs queryArgs, final SearchCondition[] sc) throws CatalogException, IOException {
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
            for (final SearchCondition aSc : sc) {
                final String name = aSc.getName();
                if (name.equalsIgnoreCase("radius") && aSc instanceof RangeSearchCondition) {
                    final RangeSearchCondition rsc = (RangeSearchCondition) aSc;
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
            final String objectName = (String) queryArgs.getParamValue(SkycatConfigEntry.OBJECT);
            if (objectName == null || objectName.length() == 0) {
                // no object name specified, check RA and Dec
                final String raStr = (String) queryArgs.getParamValue(SkycatConfigEntry.RA);
                final String decStr = (String) queryArgs.getParamValue(SkycatConfigEntry.DEC);
                if (raStr == null || decStr == null)
                    return;
                final double equinox = _getEquinox(queryArgs);
                wcs = new WorldCoords(raStr, decStr, equinox, true);
            } else {
                // an object name was specified, which needs to be resolved with a nameserver
                final Object o = queryArgs.getParamValue(SkycatConfigEntry.NAME_SERVER);
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
            final ImageCoords ic = new ImageCoords(x.intValue(), y.intValue());
            queryArgs.setRegion(new CoordinateRadius(ic, r1, r2));
        }
    }


    /**
     * Resolve the given astronomical object name using the given name server
     * and return the world coordinates corresponding the name.
     */
    private WorldCoords _resolveObjectName(final String objectName, final Catalog cat) throws CatalogException, IOException {
        final QueryArgs queryArgs = new BasicQueryArgs(cat);
        queryArgs.setId(objectName);
        final QueryResult r = cat.query(queryArgs);
        if (r instanceof TableQueryResult) {
            final Coordinates coords = ((TableQueryResult) r).getCoordinates(0);
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
    protected void _setMaxRows(final QueryArgs queryArgs, final SearchCondition[] sc) {
        if (queryArgs.getMaxRows() != 0 || sc == null || sc.length == 0)
            return;

        // look for a min and max radius parameters
        final Integer maxObjects = (Integer) queryArgs.getParamValue(SkycatConfigEntry.MAX_OBJECTS);
        if (maxObjects != null)
            queryArgs.setMaxRows(maxObjects);
    }

    /**
     * Given a description of a region of the sky (center point and radius range),
     * and the current query argument settings, set the values of the corresponding
     * query parameters.
     *
     * @param queryArgs (in/out) describes the query arguments
     * @param region    (in) describes the query region (center and radius range)
     */
    public void setRegionArgs(final QueryArgs queryArgs, final CoordinateRadius region) {
        final Coordinates coords = region.getCenterPosition();
        final RowCoordinates rowCoordinates = _entry.getRowCoordinates();
        final String equinoxStr = (String) queryArgs.getParamValue(SkycatConfigEntry.EQUINOX);
        final double equinox = _getEquinox(queryArgs);
        if (rowCoordinates.isWCS()) {
            final WorldCoords pos = (WorldCoords) coords;
            final String[] radec = pos.format(equinox);
            queryArgs.setParamValue(SkycatConfigEntry.RA, radec[0]);
            queryArgs.setParamValue(SkycatConfigEntry.DEC, radec[1]);
            queryArgs.setParamValue(SkycatConfigEntry.EQUINOX, equinoxStr);
            queryArgs.setParamValue(SkycatConfigEntry.MIN_RADIUS, region.getMinRadius());
            queryArgs.setParamValue(SkycatConfigEntry.MAX_RADIUS, region.getMaxRadius());
            queryArgs.setParamValue(SkycatConfigEntry.WIDTH, region.getWidth());
            queryArgs.setParamValue(SkycatConfigEntry.HEIGHT, region.getHeight());
        } else if (rowCoordinates.isPix()) {
            final ImageCoords pos = (ImageCoords) coords;
            queryArgs.setParamValue(SkycatConfigEntry.X, pos.getX());
            queryArgs.setParamValue(SkycatConfigEntry.Y, pos.getY());
            queryArgs.setParamValue(SkycatConfigEntry.MIN_RADIUS, region.getMinRadius());
            queryArgs.setParamValue(SkycatConfigEntry.MAX_RADIUS, region.getMaxRadius());
            queryArgs.setParamValue(SkycatConfigEntry.WIDTH, region.getWidth());
            queryArgs.setParamValue(SkycatConfigEntry.HEIGHT, region.getHeight());
        }
    }
}
