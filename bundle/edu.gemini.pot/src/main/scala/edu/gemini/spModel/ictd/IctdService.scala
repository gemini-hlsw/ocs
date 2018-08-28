package edu.gemini.spModel.ictd

import edu.gemini.spModel.core.Site

/**
 * A minimal service that obtains ICTD availability information for ODB clients.
 */
trait IctdService {

  /**
   * Produces ICTD availability information for the given site.
   */
  def summary(site: Site): IctdSummary

}
