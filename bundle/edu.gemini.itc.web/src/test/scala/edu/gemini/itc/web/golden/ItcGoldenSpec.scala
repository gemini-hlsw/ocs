package edu.gemini.itc.web.golden

import edu.gemini.itc.service.ItcServiceImpl
import edu.gemini.itc.shared.ItcResult
import org.specs2.mutable.Specification
import org.specs2.specification.core.Fragment

import java.io.File

/**
 * Pins the full numeric output of the ITC for every [[GoldenCases]] entry.
 *
 * Unlike the baseline test, which compares one hash per fixture, this compares every scalar and every
 * chart value and reports which ones moved and by how much.
 *
 * The default tolerance is 1e-12 relative. On JDK 8 the Math.pow and Math.exp intrinsics can differ
 * by one unit in the last place between interpreted and compiled code, so the same request can move
 * by about 1e-16 depending on what ran before it. Real changes seen so far were 1e-9 or larger.
 * Set ITC_GOLDEN_REL_TOL=0 for a bit-exact comparison.
 *
 * Environment variables (system properties of the same name also work):
 *  - ITC_GOLDEN_UPDATE=1        rewrite the golden file from the current code instead of comparing
 *  - ITC_GOLDEN_REL_TOL=1e-9    accept relative differences up to this value (default 1e-12)
 *  - ITC_GOLDEN_FILTER=gmos     only run cases whose key contains this text
 *  - ITC_GOLDEN_FILE=path       compare against this golden file instead of the one in test resources
 */
object ItcGoldenSpec extends Specification {

  private def setting(name: String): Option[String] =
    Option(System.getenv(name)).orElse(Option(System.getProperty(name))).map(_.trim).filter(_.nonEmpty)

  val update: Boolean = setting("ITC_GOLDEN_UPDATE").exists(v => v == "1" || v.equalsIgnoreCase("true"))
  val relTol: Double  = setting("ITC_GOLDEN_REL_TOL").map(_.toDouble).getOrElse(1e-12)
  val filter: String  = setting("ITC_GOLDEN_FILTER").getOrElse("")

  // Tests are forked from the project directory but the harness may run from the repo root.
  val goldenFile: File = setting("ITC_GOLDEN_FILE").map(new File(_)).getOrElse(
    List("src/test/resources/golden/itc-golden.json.gz", "bundle/edu.gemini.itc.web/src/test/resources/golden/itc-golden.json.gz")
      .map(new File(_)).find(f => f.getParentFile.getParentFile.exists()).getOrElse(new File("itc-golden.json.gz")))

  val cases: List[GoldenCase] = GoldenCases.all.filter(_.key.contains(filter))

  "ITC_GOLDEN_FILTER" should {
    s"match at least one case (filter '$filter')" in {
      cases must not be empty
    }
  }

  private val itc = new ItcServiceImpl

  def run(c: GoldenCase): ItcResult =
    itc.calculate(c.params, false).fold(e => sys.error(s"${c.key}: ITC error: ${e.msg}"), r => r)

  if (update) {
    "golden file" should {
      "be regenerated" in {
        val existing = if (goldenFile.exists()) GoldenSnapshot.read(goldenFile) else Map.empty[String, GoldenSnapshot]
        val fresh    = cases.map(c => c.key -> GoldenSnapshot.of(run(c))).toMap
        GoldenSnapshot.write(goldenFile, existing ++ fresh)
        println(s"Wrote ${fresh.size} golden snapshots to ${goldenFile.getAbsolutePath}")
        fresh.size mustEqual cases.size
      }
    }
  } else {
    lazy val golden: Map[String, GoldenSnapshot] = {
      if (!goldenFile.exists()) sys.error(s"Golden file ${goldenFile.getAbsolutePath} is missing. Run with ITC_GOLDEN_UPDATE=1 to create it.")
      GoldenSnapshot.read(goldenFile)
    }

    "ITC results" should {
      Fragment.foreach(cases) { c =>
        s"match the golden snapshot for ${c.key}" in {
          golden.get(c.key) match {
            case None    => ko(s"${c.key}: no golden entry. Run with ITC_GOLDEN_UPDATE=1 ITC_GOLDEN_FILTER=${c.key} to add it.")
            case Some(e) =>
              val ms = GoldenSnapshot.compare(e, GoldenSnapshot.of(run(c)), relTol)
              if (ms.isEmpty) ok else ko(GoldenSnapshot.report(c.key, ms))
          }
        }
      }
    }
  }

}
