package edu.gemini.model.p1.targetio.api

/**
 * Data format options associated with different {@link FileType}s.
 */
sealed trait FileFormat

case object Binary extends FileFormat
case object Text extends FileFormat