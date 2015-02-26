package edu.gemini.itc.baseline.util

import java.io.{File, ByteArrayOutputStream, PrintWriter}

import edu.gemini.itc.shared.{ITCParameters, ITCImageFileIO, Recipe}

import scala.io.Source

/**
 * Representation of ITC recipe output values.
 * @param string
 */
case class Output(string: String) {
  private val DatFiles = """SessionID\d*\.dat""".r
  val hash: Long = 37L*fixString(string).hashCode() + hashAllFiles(string)
  private def fixString(s: String) = s.replaceAll("SessionID\\d*", "SessionIDXXX")

  def hashAllFiles(s: String): Long =
    DatFiles.
      findAllIn(s).
      map(hashDatFile).
      foldLeft(17L)((acc, s) => 37L*acc + s.hashCode.toLong)

  def hashDatFile(f: String): Int = {
    val path = ITCImageFileIO.getImagePath
    val file = io.Source.fromFile(path + File.separator + f)
    // first line is a comment with timestamp, don't take into account for hash!
    // for testing it is safe to assume there is at least one line (header)
    file.getLines().drop(1).foldLeft(17)((acc, s) => 37*acc + s.hashCode)
  }
}

/**
 * Representation of a baseline.
 * @param in  hash value of input
 * @param out hash value of expected output
 */
case class Baseline(in: Long, out: Long)

/**
 * Helper methods to load existing baselines from resources and create and store updated baselines.
 * See [[BaselineTest]] for details.
 */
object Baseline {

  private lazy val entry = """(-?\d*),(-?\d*)""".r
  private lazy val File = getClass.getResource("/baseline.txt").getFile

  private lazy val baseline: Map[Long, Long] = {
    val lines = Source.fromFile(File).getLines()
    val map = lines.map(parse).map(b => b.in -> b.out).toMap
    map
  }

  def write(bs: Seq[Baseline]): Unit = {
    val w = new PrintWriter(File)
    bs.foreach(b => w.println(s"${b.in},${b.out}"))
    w.close()
  }

  def from[T <: ITCParameters](f: Fixture[T], out: Output): Baseline = Baseline(f.hash, out.hash)

  def cookRecipe(f: PrintWriter => Recipe): Output = {
    val o = new ByteArrayOutputStream(5000)
    val w = new PrintWriter(o)
    f(w).writeOutput()
    w.flush()
    Output(o.toString)
  }

  def checkAgainstBaseline(b: Baseline): Boolean =
    baseline.get(b.in).map(_ == b.out) match {
      case Some(true)  => true
      case Some(false) => false
      case None        => throw new Exception("Unknown input, try recreating baseline!")
    }

  private def parse(s: String): Baseline = s match {
    case entry(in, out) => Baseline(in.toLong, out.toLong)
    case _              => throw new Exception(s"Could not parse baseline: $s")
  }

}
