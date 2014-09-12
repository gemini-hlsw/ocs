//
// $
//

package edu.gemini.dataman.gsa.query;

import edu.gemini.dataman.context.DatamanConfig;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A helper class for fetching CRC information from the GSA.  The
 * queries that they provide work for a single file at a time.  To acheive
 * reasonable performance, this class does multiple queries simultaneously.
 *
 * <p><b>WARNING</b>: This class uses a thread pool internally.  The usage
 * pattern should be:
 * <ul>
 * <li><tt>MultiCrcQuery q = new MultiCrcQuery(config)</tt></li>
 * <li><tt>q.lookup(files, timeout, TimeUnit.SECONDS);
 * <li><tt>// as many lookups as desired here as above</tt></li>
 * <li><tt>q.stop()</tt></li>
 * </ul>
 */
public final class MultiCrcQuery {
    private static final Logger LOG = Logger.getLogger(MultiCrcQuery.class.getName());

    // How many queries we can potentially run at the same time on the GSA.
    private static final int CONCURRENT_QUERY_COUNT = 50;

    private static class CrcResult {
        final String filename;
        final Long crc;

        CrcResult(String filename, Long crc) {
            this.filename = filename;
            this.crc      = crc;
        }
    }

    private static class Task implements Callable<CrcResult> {
        private final DatamanConfig config;
        private final String filename;

        Task(DatamanConfig config, String filename) {
            this.config   = config;
            this.filename = filename;
        }

        public CrcResult call() throws Exception {
            Long res = (new CrcQuery(config)).getCrc(filename);
            return new CrcResult(filename, res);
        }
    }

    private final DatamanConfig config;
    private final ExecutorService pool;

    public MultiCrcQuery(DatamanConfig config) {
        this(config, CONCURRENT_QUERY_COUNT);
    }

    public MultiCrcQuery(DatamanConfig config, int threadCount) {
        this.config = config;
        this.pool = Executors.newFixedThreadPool(threadCount);
    }

    private Collection<Future<CrcResult>> _gsaLookup(Set<String> filenames, long timeout, TimeUnit unit)
            throws InterruptedException {

        Collection<Callable<CrcResult>> tasks;
        tasks = new ArrayList<Callable<CrcResult>>();

        for (String filename : filenames) {
            tasks.add(new Task(config, filename));
        }

        return pool.invokeAll(tasks, timeout, unit);
    }

    /**
     * Queries for multiple CRC values in a single method call.  Concurrently
     * executes the queries to improve performance.  For each file in the
     * <code>filenames</code> list, there are three possible results:
     * <ol>
     * <li>A CRC is obtained for the file because it has been accepted in the
     * GSA.  This CRC will appear as the value associated with the filename key
     * in the map that is returned.</li>
     * <li>No CRC is obtained for the file because it has not been accepted in
     * the GSA.  A <code>null</code> value will be associated with the filename
     * key in the map that is returned.</li>
     * <li>There was some problem communicating with the GSA to determine the
     * file's CRC.  In this case, the filename key will not appear in the map
     * that is returned.</li>
     * </ol>
     *
     * @param filenames files for which the CRC is sought
     *
     * @return map indicating the CRC values of the provided files, as
     * described above
     *
     * @throws InterruptedException if no response is obtained before the
     * specified timeout
     */
    public Map<String, Long> lookup(Set<String> filenames, long timeout, TimeUnit unit)
            throws InterruptedException {

        Map<String, Long> res = new HashMap<String, Long>();
        if (filenames.size() == 0) return res;

        LOG.log(Level.INFO, "GsaFileStatusFunctor.lookup(): " + filenames);

        long start = System.currentTimeMillis();

        Collection<Future<CrcResult>> tmp = _gsaLookup(filenames, timeout, unit);
        for (Future<CrcResult> f : tmp) {
            try {
                CrcResult crc = f.get();
                String filename = crc.filename;
                res.put(filename, crc.crc);
            } catch (ExecutionException e) {
                LOG.log(Level.WARNING, e.getMessage(), e);
            }
        }

        long end = System.currentTimeMillis();

        if (LOG.isLoggable(Level.INFO)) {
            LOG.log(Level.INFO, "GsaFileStatusFunctor.lookup() end: " + (end-start) + " ms\n" + toString(res));
        }

        return res;
    }

    private String toString(Map<String, Long> map) {
        StringBuilder buf = new StringBuilder();
        for (Map.Entry<String, Long> me : map.entrySet()) {
            buf.append(String.format("%25s -> %d\n", me.getKey(), me.getValue()));
        }
        return buf.toString();
    }

    /**
     * Stops the internal thread pool. 
     * TODO: make this transparent to the client!
     */
    public void stop() {
        pool.shutdownNow();
    }
}