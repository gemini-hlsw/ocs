package edu.gemini.dbTools.ephemeris

import edu.gemini.dbTools.ephemeris.ExportError.FileError
import edu.gemini.spModel.core.HorizonsDesignation

import java.net.{URLDecoder, URLEncoder}
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.{Path, Files}
import java.time.Instant

import scala.collection.JavaConverters._

import scalaz._, Scalaz._

/** Support for working with ephemeris files in a directory. */
sealed trait EphemerisFiles {

  /** Gets an action that will return the id of every ephemeris file in the
    * directory.
    */
  def list: TryExport[ISet[HorizonsDesignation]]

  /** Obtains the `Path` corresponding to the given id. */
  def path(hid: HorizonsDesignation): Path

  /** Gets an action that deletes the file corresponding to the given id, if
    * any.
    * @return `true` if the file is deleted, `false` otherwise
    */
  def delete(hid: HorizonsDesignation): TryExport[Boolean]

  /** Gets an action that deletes all the files corresponding to the given
    * ids, if any.
    * @return `true` if any file is actually deleted, `false` otherwise
    */
  def deleteAll(hids: ISet[HorizonsDesignation]): TryExport[Boolean]

  /** Gets an action that reads the content of the ephemeris file with the
    * given id into a String if it exists, but produces a `FileError` otherwise.
    */
  def read(hid: HorizonsDesignation): TryExport[String]

  /** Gets an action that parses the content of an ephemeris file corresponding
    * to the given id if it exists and is readable, but produces a `FileError`
    * otherwise.
    */
  def parse(hid: HorizonsDesignation): TryExport[EphemerisMap]

  /** Gets an action that parses the timestamps of an ephemeris file
    * corresponding to the given id if it exists and is readable, but produces
    * a `FileError` otherwise.
    */
  def parseTimes(hid: HorizonsDesignation): TryExport[ISet[Instant]]

  /** Gets an action that will write the given ephemeris map to a file with a
    * name corresponding to the given id, replacing any existing file.
    */
  def write(hid: HorizonsDesignation, em: EphemerisMap): TryExport[Path]
}

object EphemerisFiles {
  private val EphRegex = """(.*).eph$""".r

  def filename(hid: HorizonsDesignation): String =
    URLEncoder.encode(s"${hid.show}.eph", UTF_8.name)

  def horizonsDesignation(filename: String): Option[HorizonsDesignation] =
    filename match {
      case EphRegex(prefix) => HorizonsDesignation.read(URLDecoder.decode(prefix, UTF_8.name))
      case _                => None
    }

  def apply(dir: Path): EphemerisFiles = new EphemerisFiles {
    val list: TryExport[ISet[HorizonsDesignation]] =
      TryExport.fromTryCatch(ex => FileError("Error listing existing ephemeris files", None, Some(ex))) {
        ISet.fromList {
          Files.list(dir).iterator.asScala.toList.flatMap { p =>
            horizonsDesignation(p.getFileName.toString)
          }
        }
      }

    def path(hid: HorizonsDesignation): Path =
      dir.resolve(filename(hid))

    def error(action: String, hid: HorizonsDesignation, ex: Throwable): FileError =
      FileError(s"Error $action ephemeris file", Some(hid), Some(ex))

    def fileOp[T](action: String, hid: HorizonsDesignation)(op: Path => T): TryExport[T] =
      TryExport.fromTryCatch(error(action, hid, _)) { op(path(hid)) }

    def delete(hid: HorizonsDesignation): TryExport[Boolean] =
      fileOp("deleting", hid) { Files.deleteIfExists }

    def deleteAll(hids: ISet[HorizonsDesignation]): TryExport[Boolean] =
      hids.toList.traverseU { delete }.map(_.any(identity))

    def read(hid: HorizonsDesignation): TryExport[String] =
      fileOp("reading", hid) { Files.readAllLines(_, UTF_8).asScala.mkString("\n") }

    def parseContent[T](hid: HorizonsDesignation)(parser: String => String \/ T): TryExport[T] = {
      def parseString(content: String): TryExport[T] =
        TryExport.fromDisjunction {
          parser(content).leftMap { s =>
            FileError(s"Could not parse ephemeris data: $s", Some(hid), None)
          }
        }

      read(hid) >>= parseString
    }

    def parse(hid: HorizonsDesignation): TryExport[EphemerisMap] =
      parseContent(hid)(EphemerisFileFormat.parse)

    def parseTimes(hid: HorizonsDesignation): TryExport[ISet[Instant]] =
      parseContent(hid)(EphemerisFileFormat.parseTimestamps)

    def write(hid: HorizonsDesignation, em: EphemerisMap): TryExport[Path] =
      fileOp("writing", hid) { p =>
        Files.write(p, EphemerisFileFormat.format(em).getBytes(UTF_8))
        p
      }

  }
}
