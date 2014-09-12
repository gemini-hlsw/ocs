package edu.gemini.spModel.rich.core

import edu.gemini.spModel.core.Semester

/**
 * Extends Semester with Ordered.
 */
final class RichSemester(val semester: Semester) extends Proxy with Ordered[Semester] {

  // Proxy
  def self: Any = semester

  // Ordered[Semester]
  def compare(that: Semester): Int = semester.compareTo(that)

  def min(that: Semester): Semester = if (semester.compareTo(that) < 0) semester else that
  def max(that: Semester): Semester = if (semester.compareTo(that) > 0) semester else that
}
