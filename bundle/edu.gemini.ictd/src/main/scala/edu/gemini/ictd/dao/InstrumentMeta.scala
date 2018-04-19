package edu.gemini.ictd.dao

import doobie.imports._
import edu.gemini.pot.sp.Instrument
import edu.gemini.pot.sp.Instrument._

//mysql> select Instrument from Instrument;
//+--------------------+
//| Instrument         |
//+--------------------+
//| Acquisition Camera |
//| F2                 |
//| GMOS-N             |
//| GMOS-S             |
//| GNIRS              |
//| GSAOI              |
//| Michelle           |
//| NICI               |
//| NIFS               |
//| NIRI               |
//| Phoenix            |
//| T-ReCS             |
//+--------------------+


trait InstrumentMeta {

  private val kv = List(
    "Acquisition Camera" -> AcquisitionCamera,
    "F2"                 -> Flamingos2,
    "GMOS-N"             -> GmosNorth,
    "GMOS-S"             -> GmosSouth,
    "GNIRS"              -> Gnirs,
    "GSAOI"              -> Gsaoi,
    "Michelle"           -> Michelle,
    "NICI"               -> Nici,
    "NIFS"               -> Nifs,
    "NIRI"               -> Niri,
    "Phoenix"            -> Phoenix,
    "T-ReCS"             -> Trecs
  )

  private val decode: Map[String, Instrument] =
    kv.toMap.withDefault(s => sys.error(s"unsupported instrument $s"))

  private val encode: Map[Instrument, String] =
    kv.map(_.swap).toMap.withDefault(i => sys.error(s"unsupported instrument $i"))

  implicit val InstrumentMeta: Meta[Instrument] =
    Meta[String].xmap(decode, encode)

}

object InstrumentMeta extends InstrumentMeta
