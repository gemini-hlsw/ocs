package edu.gemini.ictd.dao

import doobie.imports._
import edu.gemini.spModel.core.Semester

//mysql> select * from Semester;
//+----------+----------+
//| Semester | Sem_Code |
//+----------+----------+
//| A        | 0        |
//| B        | 1        |
//+----------+----------+

trait SemesterHalfMeta {

  implicit val SemesterHalfMeta: Meta[Semester.Half] =
    Meta[String].xmap(Semester.Half.valueOf, _.name)

}

object SemesterHalfMeta extends SemesterHalfMeta
