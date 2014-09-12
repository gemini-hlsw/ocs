package edu.gemini.qv.plugin.filter.core

import edu.gemini.qpt.shared.sp.Obs

/**
 * Some utility classes and objects used in conjunction with filters.
 */

/** Helper methods to decide on states of observations. */
object FilterUtil {

  def aoUsage(o: Obs): AoUsage = {
    if (o.getAO) { if (o.getNGS) AoUsage.Ngs else AoUsage.Lgs }
    else AoUsage.None
  }

}

// Helper enums to represent observation and program states relevant for filtering which are
// not yet represented by existing enumerations and/or seemed to be too filter specific to be
// added to the general model.

/** Enumeration of possible adaptive optics configurations:
  * No AO, AO with laser guide star or AO with natural guide star. */
sealed trait AoUsage
object AoUsage {
  case object None extends AoUsage
  case object Lgs extends AoUsage
  case object Ngs extends AoUsage
}
