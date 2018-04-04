package edu.gemini.ictd.dao

import doobie.imports._
import edu.gemini.spModel.core.Semester

//mysql> SELECT DISTINCT Year, Semester from MOS ORDER BY Year DESC LIMIT 10;
//+------+----------+
//| Year | Semester |
//+------+----------+
//| 2018 | A        |
//| 2017 | A        |
//| 2017 | B        |
//| 2016 | A        |
//| 2016 | B        |
//| 2015 | A        |
//| 2015 | B        |
//| 2014 | A        |
//| 2014 | B        |
//| 2013 | A        |
//+------+----------+

// Semester is distributed over two columns for some reason, so we need a
// composite.


trait SemesterComposite {

  import SemesterHalfMeta._

  implicit val SemesterComposite: Composite[Semester] =
    Composite[(Int, Semester.Half)].xmap(
      (t: (Int, Semester.Half)) => new Semester(t._1, t._2),
      (s: Semester)             => (s.getYear, s.getHalf)
    )

}

object SemesterComposite extends SemesterComposite
