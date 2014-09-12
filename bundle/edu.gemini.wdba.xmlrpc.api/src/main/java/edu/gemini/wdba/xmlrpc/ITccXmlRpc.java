package edu.gemini.wdba.xmlrpc;

/**
 * Gemini Observatory/AURA
 * $Id: ITccXmlRpc.java 756 2007-01-08 18:01:24Z gillies $
 */
public interface ITccXmlRpc {

    String NAME = "WDBA_Tcc";

    /**
     * Returns the coordinate data for a specific observation.
     *
     * @param observationId is the observation to use
     * @return the XML result for the coordinates.  If there are no coordinates an empty
     * document is still returned.
     */
    public String getCoordinates(String observationId) throws ServiceException;
}
