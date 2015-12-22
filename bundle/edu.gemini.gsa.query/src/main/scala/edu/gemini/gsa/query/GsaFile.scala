package edu.gemini.gsa.query

import scalaz.Equal

/** Representation of a GSA file, with  the minimum fields required for the PIT
  */
case class GsaFile(name: String)

object GsaFile {
  implicit val equal: Equal[GsaFile] = Equal.equalA
}
