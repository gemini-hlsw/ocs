package edu.gemini.spModel.core

import java.util.GregorianCalendar
import javax.xml.datatype.XMLGregorianCalendar

// N.B. magnitude here is APmag from Horizons
case class EphemerisElement(coords: Coordinates, magnitude: Option[Double], validAt: Long)

