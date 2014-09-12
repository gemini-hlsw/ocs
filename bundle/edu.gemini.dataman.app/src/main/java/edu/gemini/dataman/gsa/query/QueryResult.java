//
// $
//

package edu.gemini.dataman.gsa.query;

/**
 * This class holds the result of calling a query on a particular file.  If the
 * query generated an exception, then the exception is returned when the client
 * executes {@link #get}.
 */
public abstract class QueryResult<R, E extends Exception> {
    private final R result;
    private final E exception;

    protected QueryResult(R result) {
        this.result    = result;
        this.exception = null;
    }

    protected QueryResult(E exception) {
        this.result    = null;
        this.exception = exception;
    }

    public final R get() throws E {
        if (exception != null) throw exception;
        return result;
    }
}
