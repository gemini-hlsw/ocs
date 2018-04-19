package edu.gemini.ictd.dao

import edu.gemini.spModel.ictd.Availability
import edu.gemini.spModel.ictd.Availability._

import doobie.imports._

import scalaz._
import Scalaz._


//mysql> select Location from Location;
//+-----------------+
//| Location        |
//+-----------------+
//| Archive Cabinet |
//| Conceptual      |
//| CPO Cabinet     |
//| Cutting Queue   |
//| HBF Archive     |
//| HBF Cabinet     |
//| In Transit      |
//| Instrument      |
//| MKO Cabinet     |
//| SBF Cabinet     |
//+-----------------+


/** ICTD Location values. Locations are finer grained than Availability, which
  * is what we are interested in for the OCS. We read Location from the ICTD
  * but turn it into Availability for use outside of the DAO package.
  *
  * One reason for this is that instrument enums are sometimes combinations of
  * more granular features - for example we offer a Hartmann A + r filter which
  * consists of two independent filters, both of which are tracked individually
  * in the ICTD. It is clear how to combine Availability to get the least
  * available of the combination but unclear how to combine Locations in all
  * cases.  For example, which is least available between HBF Cabinet and SBF
  * Cabinet?
  */
sealed abstract class Location(val dbKey: String, val availability: Availability)

object Location {

  case object ArchiveCabinet extends Location("Archive Cabinet", Unavailable  )
  case object Conceptual     extends Location("Conceptual",      Unavailable  )
  case object CpoCabinet     extends Location("CPO Cabinet",     SummitCabinet)
  case object CuttingQueue   extends Location("Cutting Queue",   Unavailable  )
  case object HbfArchive     extends Location("HBF Archive",     Unavailable  )
  case object HbfCabinet     extends Location("HBF Cabinet",     Unavailable  )
  case object InTransit      extends Location("In Transit",      Unavailable  )
  case object Instrument     extends Location("Instrument",      Installed    )
  case object MkoCabinet     extends Location("MKO Cabinet",     SummitCabinet)
  case object SbfCabinet     extends Location("SBF Cabinet",     Unavailable  )

  val All: List[Location] =
    List(
      ArchiveCabinet,
      Conceptual,
      CpoCabinet,
      CuttingQueue,
      HbfArchive,
      HbfCabinet,
      InTransit,
      Instrument,
      MkoCabinet,
      SbfCabinet
    )

}

//
// N.B.: I pulled LocationMeta out of the Location companion itself because of a
// (shapeless?) compile issue (bug?):
//
// [error] knownDirectSubclasses of Location observed before subclass ArchiveCabinet registered
//

object LocationMeta {

  implicit val LocationMeta: Meta[Location] =
    Meta[String].xmap(
      Location.All.map(l => (l.dbKey, l)).toMap,
      Location.All.fproduct(_.dbKey).toMap
    )

}