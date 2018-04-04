package edu.gemini

import edu.gemini.pot.sp.Instrument
import edu.gemini.spModel.ictd.Availability

import shapeless.tag.@@

package object ictd {

  trait CircularTag
  trait FilterTag
  trait GratingTag
  trait IfuTag
  trait LongslitTag
  trait NSLongslitTag

  // Availability information from the ICTD database for named feature instances.
  // AvailabilityTable is tagged below to prevent mixing features in the same
  // table.
  type AvailabilityTable = Map[String, Availability]

  type CircularTable     = AvailabilityTable @@ CircularTag
  type FilterTable       = AvailabilityTable @@ FilterTag
  type GratingTable      = AvailabilityTable @@ GratingTag
  type IfuTable          = AvailabilityTable @@ IfuTag
  type LongslitTable     = AvailabilityTable @@ LongslitTag
  type NSLongslitTable   = AvailabilityTable @@ NSLongslitTag

}
