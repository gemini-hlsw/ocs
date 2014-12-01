package edu.gemini.spModel.core

import java.util.GregorianCalendar
import javax.xml.datatype.XMLGregorianCalendar

// TODO: in what band is the magnitude? do we need a map here?
case class EphemerisElement(coords: Coordinates, magnitude: Option[Double], validAt: Long)

