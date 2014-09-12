package edu.gemini.spModel.rich

import edu.gemini.spModel.core.{Site, Semester}

package object core {
  implicit val siteOrdering = new Ordering[Site] {
    def compare(s1: Site, s2: Site): Int = s1.compareTo(s2)
  }

  implicit val semesterOrdering = new Ordering[Semester] {
    def compare(s1: Semester, s2: Semester): Int = s1.compareTo(s2)
  }

  implicit def semesterWrapper(semester: Semester) = new RichSemester(semester)
}
