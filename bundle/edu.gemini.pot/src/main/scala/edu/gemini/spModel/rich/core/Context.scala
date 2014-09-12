package edu.gemini.spModel.rich.core

import edu.gemini.spModel.core.{Site, Semester}

case class Context(site: Site, semester: Semester) extends Ordered[Context] {
  def compare(that: Context): Int =
    semester.compareTo(that.semester) match {
      case 0 => site.compareTo(that.site)
      case n => n
    }
}

