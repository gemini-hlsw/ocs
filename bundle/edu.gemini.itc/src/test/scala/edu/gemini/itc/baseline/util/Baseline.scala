package edu.gemini.itc.baseline.util

import java.io.{ByteArrayOutputStream, PrintWriter}

import edu.gemini.itc.shared.Recipe

import scala.io.Source

case class Input(obs: Observation, env: Environment) {
  val hash: Long = env.hash.toLong*37 + obs.hash.toLong
}

case class Output(string: String) {
  val hash: Int = fixString(string).hashCode()
  private def fixString(s: String) = s.replaceAll("SessionID\\d*", "SessionIDXXX")
}

case class Baseline(in: Long, out: Int)

/**
 * Baseline of the expected output for given inputs.
 */
object Baseline {

  private lazy val entry = """Baseline\((-?\d*),(-?\d*)\)""".r
  private lazy val File = getClass.getResource("/baseline.txt").getFile

  private lazy val baseline: Map[Long, Int] = {
    val lines = Source.fromFile(File).getLines()
    val map = lines.map(parse).map(b => b.in -> b.out).toMap
    map
  }

  def write(b: Seq[Baseline]) = {
    val w = new PrintWriter(File)
    b.foreach(w.println)
    w.close()
  }

  def from(env: Environment, obs: Observation, out: Output): Baseline =
    Baseline(Input(obs, env).hash, out.hash)

  def cookRecipe(f: PrintWriter => Recipe): Output = {
    val o = new ByteArrayOutputStream(5000)
    val w = new PrintWriter(o)
    f(w).writeOutput()
    w.flush()
    Output(o.toString)
  }

  def checkAgainstBaseline(b: Baseline): Boolean =
    baseline.get(b.in).map(_ == b.out) match {
      case Some(true) => true
      case Some(false) => false
      case None => throw new Exception("Unknown input, try recreating baseline!")
    }

  private def parse(s: String): Baseline = s match {
    case entry(int, out) => Baseline(int.toLong, out.toInt)
    case _               => throw new Exception(f"Could not parse baseline: {s}")
  }

}
