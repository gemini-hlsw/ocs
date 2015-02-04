package edu.gemini.pit.ui

import edu.gemini.model.p1.immutable.Semester

/**
 * Contains constant for URLs used in help contexts
 */
object URLConstants {
  val GET_TEMPLATES = (s"http://software.gemini.edu/phase1/templates/${Semester.current.display}/", "Download LaTeX/Word templates for the text sections.")
}
