package edu.gemini.itc.baseline.util

import java.io.{ByteArrayOutputStream, PrintWriter}

import edu.gemini.itc.shared.Recipe

import scala.io.Source

/**
 * Representation of ITC recipe input values.
 * @param obs the observation
 * @param env the environment
 */
case class Input(obs: Observation, env: Environment) {
  val hash: Long = env.hash.toLong*37 + obs.hash.toLong
}

/**
 * Representation of ITC recipe output values.
 * @param string
 */
case class Output(string: String) {
  val hash: Int = fixString(string).hashCode()
  private def fixString(s: String) = s.replaceAll("SessionID\\d*", "SessionIDXXX")
}

/**
 * Representation of a baseline.
 * @param in  hash value of input
 * @param out hash value of expected output
 */
case class Baseline(in: Long, out: Int)

/**
 * Helper methods to load existing baselines from resources and create and store updated baselines.
 * See [[BaselineTest]] for details.
 */
object Baseline {

  private lazy val entry = """(-?\d*),(-?\d*)""".r
  private lazy val File = getClass.getResource("/baseline.txt").getFile

  private lazy val baseline: Map[Long, Int] = {
    val lines = Source.fromFile(File).getLines()
    val map = lines.map(parse).map(b => b.in -> b.out).toMap
    map
  }

  def write(bs: Seq[Baseline]): Unit = {
    val w = new PrintWriter(File)
    bs.foreach(b => w.println(s"${b.in},${b.out}"))
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
      case Some(true)  => true
      case Some(false) => false
      case None        => throw new Exception("Unknown input, try recreating baseline!")
    }

  private def parse(s: String): Baseline = s match {
    case entry(in, out) => Baseline(in.toLong, out.toInt)
    case _              => throw new Exception(s"Could not parse baseline: $s")
  }

}
