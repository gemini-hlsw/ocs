package edu.gemini.gsa.client

package object impl {

  // Scrapped data from the page returned by the GSA service.  The map keys
  // are table columns and the values are the values for that column in each
  // row.
  type GsaMap = Map[String, List[String]]

  // Timeout on GSA queries in ms.
  val GSA_TIMEOUT = 60000
}