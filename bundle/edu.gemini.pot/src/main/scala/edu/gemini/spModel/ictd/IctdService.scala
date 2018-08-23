package edu.gemini.spModel.ictd

import edu.gemini.spModel.core.Site

/**
 *
 */
trait IctdService {

  def summary(site: Site): IctdSummary

}
