package edu.gemini.itc.web.baseline

import java.io.{ByteArrayOutputStream, File, PrintWriter}

import edu.gemini.itc.acqcam.AcquisitionCamParameters
import edu.gemini.itc.baseline.util.Fixture
import edu.gemini.itc.gnirs.GnirsParameters
import edu.gemini.itc.gsaoi.GsaoiParameters
import edu.gemini.itc.michelle.MichelleParameters
import edu.gemini.itc.nifs.NifsParameters
import edu.gemini.itc.niri.NiriParameters
import edu.gemini.itc.shared._
import edu.gemini.itc.trecs.TRecsParameters
import edu.gemini.itc.web.html._

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

  def from[T <: InstrumentDetails](f: Fixture[T], out: Output): Baseline = Baseline(f.hash, out.hash)

  def cookRecipe(f: PrintWriter => PrinterBase): Output = {
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

  // ====

  def executeAcqCamRecipe(f: Fixture[AcquisitionCamParameters]): Output =
    cookRecipe(w => new AcqCamPrinter(Parameters(f.src, f.odp, f.ocp, f.tep), f.ins, w))

  def executeF2Recipe(f: Fixture[Flamingos2Parameters]): Output =
    cookRecipe(w => new Flamingos2Printer(Parameters(f.src, f.odp, f.ocp, f.tep), f.ins, f.pdp, w))

  def executeGmosRecipe(f: Fixture[GmosParameters]): Output =
    cookRecipe(w => new GmosPrinter(Parameters(f.src, f.odp, f.ocp, f.tep), f.ins, f.pdp, w))

  def executeGnirsRecipe(f: Fixture[GnirsParameters]): Output =
    cookRecipe(w => new GnirsPrinter(Parameters(f.src, f.odp, f.ocp, f.tep), f.ins, f.pdp, w))

  def executeGsaoiRecipe(f: Fixture[GsaoiParameters]): Output =
    cookRecipe(w => new GsaoiPrinter(Parameters(f.src, f.odp, f.ocp, f.tep), f.ins, f.gem.get, w))

  def executeMichelleRecipe(f: Fixture[MichelleParameters]): Output =
    cookRecipe(w => new MichellePrinter(Parameters(f.src, f.odp, f.ocp, f.tep), f.ins, f.pdp, w))

  def executeNifsRecipe(f: Fixture[NifsParameters]): Output =
    cookRecipe(w => new NifsPrinter(Parameters(f.src, f.odp, f.ocp, f.tep), f.ins, f.pdp, w))

  def executeNiriRecipe(f: Fixture[NiriParameters]): Output =
    cookRecipe(w => new NiriPrinter(Parameters(f.src, f.odp, f.ocp, f.tep), f.ins, f.pdp, w))

  def executeTrecsRecipe(f: Fixture[TRecsParameters]): Output =
    cookRecipe(w => new TRecsPrinter(Parameters(f.src, f.odp, f.ocp, f.tep), f.ins, f.pdp, w))

}
