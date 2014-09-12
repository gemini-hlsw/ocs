package edu.gemini.pit.ui

import edu.gemini.model.p1.immutable.Semester

/**
 * Contains constant for URLs used in help contexts
 */
object URLConstants {
  val GET_TEMPLATES = (s"http://files.gemini.edu/~software/phase1/${Semester.current.display}/", "Download LaTeX/Word templates for the text sections.")
}
