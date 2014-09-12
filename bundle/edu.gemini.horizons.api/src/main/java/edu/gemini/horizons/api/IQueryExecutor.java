package edu.gemini.horizons.api;

//$Id: IQueryExecutor.java 630 2006-11-28 19:32:20Z anunez $
/**
 * Interface for query executors. A query executor is an object able to execute
 * a {@link edu.gemini.horizons.api.HorizonsQuery} into a Horizons service.
 */
public interface IQueryExecutor {
    /**
     * Execute the given query into an Horizons service.
     * @param query A query definition with the details needed to perform the
     * query
     * @return an {@link edu.gemini.horizons.api.HorizonsReply} with the
     * results of applying the given query.
     */
    HorizonsReply execute(HorizonsQuery query) throws HorizonsException;
}
