package edu.gemini.pit.ui

import edu.gemini.model.p1.immutable.Semester

/**
 * Contains constant for URLs used in help contexts
 */
object URLConstants {
  val GET_TEMPLATES = (s"http://software.gemini.edu/phase1/templates/${Semester.current.display}/", "Open directory for LaTeX/Word text section templates in browser.")
  val OPEN_ITC = ("http://www.gemini.edu/node/10241", "Open Integration Time Calculator page in browser.")
}
