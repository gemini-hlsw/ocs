package edu.gemini.ictd.dao

import doobie.imports._
import edu.gemini.spModel.core.{ProgramId, ProgramType, Semester, Site}

//mysql> SELECT DISTINCT Site, Year, Semester, ProgramType, ProgramNo from MOS ORDER BY Year DESC LIMIT 10;
//+------+------+----------+-------------+-----------+
//| Site | Year | Semester | ProgramType | ProgramNo |
//+------+------+----------+-------------+-----------+
//| GN   | 2018 | A        | DD          |         1 |
//| GN   | 2018 | A        | LP          |         4 |
//| GN   | 2018 | A        | Q           |       201 |
//| GN   | 2018 | A        | Q           |       302 |
//| GN   | 2018 | A        | Q           |       903 |
//| GS   | 2018 | A        | Q           |       125 |
//| GS   | 2018 | A        | Q           |       128 |
//| GS   | 2018 | A        | Q           |       219 |
//| GS   | 2018 | A        | Q           |       224 |
//| GS   | 2018 | A        | Q           |       317 |


trait ProgramIdComposite {

  import SiteMeta._
  import SemesterComposite._
  import ProgramTypeMeta._

  implicit val ProgramIdComposite: Composite[ProgramId.Science] =
    Composite[(Site, Semester, ProgramType, Int)].xmap(
      (t: (Site, Semester, ProgramType, Int)) => ProgramId.Science(t._1, t._2, t._3, t._4),
      (p: ProgramId.Science)                  => (p.siteVal, p.semesterVal, p.ptypeVal, p.index)
    )

}

object ProgramIdComposite extends ProgramIdComposite
