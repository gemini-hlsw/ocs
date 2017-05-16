package jsky.app.ot.gemini.editor.sitequality

import java.time.Instant

import edu.gemini.spModel.gemini.obscomp.SPSiteQuality.TimingWindow
import jsky.app.ot.gemini.editor.sitequality.TimingWindowParser.{TimingWindowParseFailure, TimingWindowParseResults}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import scalaz._
import Scalaz._

object TimingWindowImporterSpec extends Specification with ScalaCheck {
  "timing window parsing" should {
    "handle empty inputs" in {
      val results = TimingWindowParser.parseTimingWindows(Nil)
      (results.successes.isEmpty must beTrue) and (results.failures.isEmpty must beTrue)
    }

    "ignore empty lines and comments" in {
      val lines = List("", "  ", "#", "# Comment", " # abc ", "    ## nothing here")
      val results = TimingWindowParser.parseTimingWindows(lines)
      (results.successes.isEmpty must beTrue) and (results.failures.isEmpty must beTrue)
    }

    "create timing windows from complete data, ignoring whitespace" in {
      val t1Tup = ("2018-01-01", "01:02:03", "04:05", 1.some, "01:02:03".some)
      val t2Tup = ("   2018-01-02  ", "  02:03:04   ", "   06:07  ", 2.some, "   03:04:05    ".some)
      val (successes, _, results) = runParsing(t1Tup.right, t2Tup.right)
      (successes must === (results.successes)) and (results.failures.isEmpty must beTrue)
    }

    "identify a success and a failure" in {
      val t1Tup = ("2017-06-10", "23:59:59", "48:00", 3.some, "123:45:00".some)
      val fail1 = "Ceci n'est pas un timing window."
      val (successes, failTuples, results) = runParsing(t1Tup.right, fail1.left)
      val failures = failTuplesToParseResults(failTuples)
      (successes must === (results.successes)) and (failures must === (results.failures))
    }

    "default to a repetition of forever if -1 is specified" in {
      val t1Tup = ("2018-03-04", "11:12:13", "04:40", (-1).some, "11:22:33".some)
      val (successes, _, results) = runParsing(t1Tup.right)
      (results.successes.length must_=== 1) and (results.successes.head.getRepeat must_=== TimingWindow.REPEAT_FOREVER)
    }

    "default to a period of 00:00:00 if no period is specified" in {
      val t1Tup = ("2017-12-10", "03:33:33", "12:00", 5.some, noString)
      val (successes, _, results) = runParsing(t1Tup.right)
      (results.successes.length must_=== 1) and (results.successes.head.getPeriod must_=== 0L)
    }

    "default to a repetition of never and a period of 00:00 if neither is specified" in {
      val t1Tup = ("2018-01-01", "13:13:13", "11:11", noInt, noString)
      val (successes, _, results) = runParsing(t1Tup.right)
      (results.successes.length must_=== 1) and (results.successes.head.getRepeat must_=== TimingWindow.REPEAT_NEVER) and (results.successes.head.getPeriod must_=== 0L)
    }

    "default to a duration of forever if not specified" in {
      val t1Tup = ("2017-11-11", "01:01:01", "00:00", noInt, noString)
      val t2Tup = ("2017-11-12", "02:02:02", "00:00", 1.some, noString)
      val t3Tup = ("2017-11-13", "03:03:03", "00:00", 2.some, "11:11:11".some)
      val (successes, _, results) = runParsing(t1Tup.right, t2Tup.right, t3Tup.right)
      (results.successes.length must_=== 3) and (results.successes.forall(_.getDuration == TimingWindow.WINDOW_REMAINS_OPEN_FOREVER) must beTrue)
    }

    "not overflow for large times" in {
      val t1Tup = ("2017-05-05", "00:00:00", "720:00", noInt, noString)
      val (successes, _, results) = runParsing(t1Tup.right)
      (results.successes.length must_=== 1) and (results.successes.forall(_.getDuration > 0) must beTrue)
    }
  }

  // To avoid repeated type specifiers.
  private val noInt:    Option[Int]    = None
  private val noString: Option[String] = None

  // Convenience method to take a bunch of fail lines (lefts) and valid tuples (rights) and return
  // a list of timing windows, indexed failed tuples, and the results of running the parse.
  private def runParsing(lineTups: (String \/ (String, String, String, Option[Int], Option[String]))*): (List[TimingWindow], List[(Int, String)], TimingWindowParseResults) = {
    def makeTimingWindow(bDate: String, bTime: String, d: String,
                         rOpt: Option[Int] = None, pOpt: Option[String] = None): Option[TimingWindow] =
      \/.fromTryCatchNonFatal {
        val btw = Instant.from(TimingWindowParser.dateTimeFormat.parse(s"${bDate.trim} ${bTime.trim}")).toEpochMilli
        val dtw = TimingWindowParser.hhmmssStringToLong(d.trim)
        val rtw = rOpt.getOrElse(0)
        val ptw = pOpt.map(p => TimingWindowParser.hhmmssStringToLong(p.trim)).getOrElse(0L)
        new TimingWindow(btw, dtw, rtw, ptw)
      }.toOption

    // Index and split on the disjunction into fail lines and success lines.
    val indexedLineTups = lineTups.zipWithIndex
    val failures        = indexedLineTups.collect { case (-\/(s), i) => (i, s) }.toList
    val lineTupsSuccess = indexedLineTups.collect { case (\/-(t), _) => t }.toList

    val successes = for {
      (bDate, bTime, d, tOpt, pOpt) <- lineTupsSuccess
      tw                            <- makeTimingWindow(bDate, bTime, d, tOpt, pOpt)
    } yield tw

    val results = {
      val lines = lineTups.map {
        case -\/(s)                             => s
        case \/-((bDate, bTime, d, rOpt, pOpt)) =>
          s"$bDate $bTime $d" + rOpt.map(r => s" $r").getOrElse("") + pOpt.map(p => s" $p").getOrElse("")
      }.toList
      TimingWindowParser.parseTimingWindows(lines)
    }

    (successes, failures, results)
  }

  // Convenience method to turn failure tuples into parse results for comparison.
  private def failTuplesToParseResults(failTuples: List[(Int, String)]): List[TimingWindowParseFailure] =
    failTuples.map { case (i, s) => TimingWindowParseFailure(i, s) }
}
