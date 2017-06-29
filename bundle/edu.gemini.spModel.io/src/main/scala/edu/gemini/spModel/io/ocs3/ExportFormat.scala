package edu.gemini.spModel.io.ocs3

/** Identifies the desired export format.
  */
sealed trait ExportFormat

object ExportFormat {

  /** Export format optimized for straightforward conversion to the database
    * schema, which expects an expanded sequence.
    */
  case object Ocs3 extends ExportFormat

  /** Transitional export format for the seqexec, used to avoid binary
    * serialization.  This is simply the standard OCS PIO XML.
    */
  case object Pio extends ExportFormat
}
