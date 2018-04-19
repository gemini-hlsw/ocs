package edu.gemini.ictd.dao

import doobie.imports._
import edu.gemini.spModel.core.ProgramType

//mysql> select * from ProgramType;
//+-------------+------------------------+----------+
//| ProgramType | Description            | TypeCode |
//+-------------+------------------------+----------+
//| C           | Classical              | 1        |
//| DD          | Director Discretionary | 9        |
//| E           | Engineering            | 7        |
//| FT          | Fast Turnaround        | 3        |
//| LP          | Large Programs         | 2        |
//| Q           | Queue                  | 0        |
//| SV          | Science Verification   | 8        |
//+-------------+------------------------+----------+


trait ProgramTypeMeta {

  private def decode(s: String): ProgramType =
    s match {
      case "E" => ProgramType.Engineering
      case _   => ProgramType.read(s).getOrElse(sys.error(s"Unexpected ProgramType: $s"))
    }

  private def encode(t: ProgramType): String =
    t match {
      case ProgramType.Engineering => "E"
      case ProgramType.Calibration => sys.error(s"ICTD does not support CAL programs")
      case _                       => t.abbreviation
    }

  implicit val ProgramTypeMeta: Meta[ProgramType] =
    Meta[String].xmap(decode, encode)
}

object ProgramTypeMeta extends ProgramTypeMeta
