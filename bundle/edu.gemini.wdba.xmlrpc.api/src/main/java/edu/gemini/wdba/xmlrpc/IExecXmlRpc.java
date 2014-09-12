//
// $Id: IExecXmlRpc.java 756 2007-01-08 18:01:24Z gillies $
//
package edu.gemini.wdba.xmlrpc;

/**
 * The SeqExec can fetch the sequence file for an observation form the ODB.
 * The format of the response is an Sequence XML file specified KGillies and DTerret.
 */
public interface IExecXmlRpc {

    String NAME = "WDBA_Exec";

   /**
    * Returns the sequence data for a specific observation.
    *
    * @param observationId is the observation to use
    * @return the XML result for the observation sequence.
    */
   public String getSequence(String observationId) throws ServiceException;
}
