package edu.gemini.spModel.core

import java.util.GregorianCalendar
import javax.xml.datatype.XMLGregorianCalendar

object EphemerisElement {
  // TODO: do we need this?
  val empty = EphemerisElement(Coordinates.zero, None, 0L)
}

// TODO: in what banmd is the magnitude? do we need a map here?
case class EphemerisElement(coords: Coordinates, magnitude: Option[Double], validAt: Long)

