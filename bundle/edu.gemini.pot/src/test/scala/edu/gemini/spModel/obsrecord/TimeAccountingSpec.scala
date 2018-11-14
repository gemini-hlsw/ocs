package edu.gemini.spModel.obsrecord

import edu.gemini.pot.sp.SPObservationID
import edu.gemini.skycalc.{ Interval, ObservingNight, TwilightBoundType, Union }
import edu.gemini.spModel.config2.Config
import edu.gemini.spModel.core.Semester
import edu.gemini.spModel.core.Site.GS
import edu.gemini.spModel.dataset.{ Dataset, DatasetLabel, DatasetQaRecord, DatasetQaState }
import edu.gemini.spModel.dataset.DatasetQaState.{ FAIL, PASS, USABLE }
import edu.gemini.spModel.event._
import edu.gemini.spModel.obsclass.ObsClass
import edu.gemini.spModel.obsclass.ObsClass.SCIENCE
import edu.gemini.spModel.pio.{ ParamSet, PioFactory }
import edu.gemini.spModel.syntax.all._
import edu.gemini.spModel.time.ChargeClass._

import org.scalacheck.{ Properties, Gen, Arbitrary }
import org.scalacheck.Arbitrary._
import org.scalacheck.Prop.forAll
import org.scalacheck.Prop._
import org.scalacheck.Gen

import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import java.time.{ Duration, Instant }
import scala.collection.JavaConverters._
import scala.Function.const

import scalaz._
import scalaz.Scalaz._

object TimeAccountingSpec extends Specification with ScalaCheck {

  implicit val arbQaState: Arbitrary[DatasetQaState] =
    Arbitrary {
      Gen.oneOf(DatasetQaState.values.toList)
    }

  implicit val arbObsClass: Arbitrary[ObsClass] =
    Arbitrary {
      Gen.oneOf(ObsClass.values.toList)
    }

  val ObsId = new SPObservationID("GS-2019A-Q-1")

  val eventConstructors: List[Long => ObsExecEvent] = List(
    new AbortObserveEvent(_, ObsId, ""),
    new ContinueObserveEvent(_, ObsId),
    new EndSequenceEvent(_, ObsId),
    new EndVisitEvent(_, ObsId),
    new OverlapEvent(_, ObsId),
    new PauseObserveEvent(_, ObsId, ""),
    new SlewEvent(_, ObsId),
    new StartSequenceEvent(_, ObsId),
    new StartVisitEvent(_, ObsId),
    new StopObserveEvent(_, ObsId)
  )

  def genEvent(at: Long): Gen[ObsExecEvent] =
    Gen.oneOf(eventConstructors).map(c => c(at))

  case class Test(
    events: VisitEvents,
    qa:     Map[DatasetLabel, DatasetQaState],
    oc:     Map[DatasetLabel, ObsClass]
  ) {

    def visitTimes: VisitTimes =
      TimeAccounting.calc(events.sorted, qa, oc)

    def show: String = {
      def format(evt: ObsExecEvent): String = {
        val base = s"${evt.getTimestamp} - ${evt.getClass.getName}"
        evt match {
          case s: StartDatasetEvent =>
            val lab = s.getDataset.getLabel
            s"$base - $lab - ${qa(lab)} - ${oc(lab)}"
          case e: EndDatasetEvent   =>
            s"$base - ${e.getDatasetLabel}"
          case _                    =>
            base
        }
      }

      events.sorted.map(format).mkString("\n")
    }

  }

  def genTest(start: Instant, end: Instant): Gen[Test] = {

    val timeGen = Gen.choose(start.toEpochMilli, end.toEpochMilli)

    for {
      dstart <- Gen.choose(0, 100)
      dcount <- Gen.choose(0,  50)
      qas    <- Gen.listOfN(dcount, arbitrary[DatasetQaState])
      ocs    <- Gen.listOfN(dcount, arbitrary[ObsClass])
      dtimes <- Gen.listOfN(dcount * 2, timeGen)
      ecount <- Gen.choose(0,  50)
      es     <- Gen.listOfN(ecount, timeGen.flatMap(genEvent))
    } yield {
      val labs = (dstart until (dstart + dcount)).toList.map(new DatasetLabel(ObsId, _))
      val des  = labs.zip(dtimes.sorted.grouped(2).toList).collect { case (lab, List(s, e)) =>
        val d = new Dataset(lab, lab.toString, s)
        List(new StartDatasetEvent(s, d), new EndDatasetEvent(e, lab))
      }.flatten

      val qaMap = labs.zip(qas).toMap
      val ocMap = labs.zip(ocs).toMap

      Test(VisitEvents((es ::: des).toVector), qaMap, ocMap)
    }

  }

  val Semester2019A = new Semester(2018, Semester.Half.B)
  val GS19AStart    = Instant.ofEpochMilli(Semester2019A.getStartDate(GS).getTime)
  val GS19AEnd      = Instant.ofEpochMilli(Semester2019A.getEndDate(GS).getTime)
  val DayInMs       = Duration.ofDays(1l).toMillis

  "TimeAccounting" should {

    "account for all time" in {
      forAll(genTest(GS19AStart, GS19AEnd)) { (t: Test) =>
        t.events.total.sum shouldEqual t.visitTimes.getTotalTime
      }
    }

    "never charge more than available dark time" in {
      forAll(genTest(GS19AStart, GS19AEnd)) { (t: Test) =>
        t.visitTimes.getChargedTime must be_<=(t.events.dark.sum)
      }
    }

    "never charge more than available dark time - overlap" in {
      forAll(genTest(GS19AStart, GS19AEnd)) { (t: Test) =>
        t.visitTimes.getChargedTime must be_<=(t.events.chargeable.sum)
      }
    }

    "calculate dark time <= total time" in {
      forAll(genTest(GS19AStart, GS19AEnd)) { (t: Test) =>
        t.events.dark.sum must be_<=(t.events.total.sum)
      }
    }

    "include in noncharged all overlap time" in {
      forAll(genTest(GS19AStart, GS19AEnd)) { (t: Test) =>
        t.events.overlap.sum must be_<=(t.visitTimes.getClassifiedTime(NONCHARGED))
      }
    }

    "add chargeable time between datasets to unclassified" in {
      forAll(genTest(GS19AStart, GS19AEnd)) { (t: Test) =>
        // Union of the dataset intervals.
        val u = new Union(t.events.datasetIntervals.unzip._2.asJava)

        // Use a calc assuming all datasets are PASS (because if they are all
        // FAIL or USABLE nothing is charged).
        val v = TimeAccounting.calc(t.events.sorted, const(PASS), t.oc)

        // If there were no datasets, then we don't charge at all.
        if (u.sum === 0) v.getTotalTime shouldEqual v.getClassifiedTime(NONCHARGED)
        else v.getUnclassifiedTime shouldEqual (t.events.chargeable - u).sum
      }
    }

    "not charge if there are no passing datasets" in {
      forAll(genTest(GS19AStart, GS19AEnd)) { (t: Test) =>
        t.qa.values.exists(_.isChargeable) ||
          (t.visitTimes === VisitTimes.noncharged(t.events.total.sum))
      }
    }

    "produce the same result as pre-2019A for 'normal' chargeable event sequences" in {
      forAll(genTest(GS19AStart, GS19AStart.plusMillis(DayInMs - 1))) { (t: Test) =>
        // For this to work, we have to produce an event list that doesn't
        // trigger any of the bugs in the old implementation.

        // Eliminate events that separate start/end dataset pairs.  First, get
        // a union of intervals that doesn't include the time between start and
        // and end events
        val intervals = t.events.sorted.collect {
          case s: StartDatasetEvent => (s.getDataset.getLabel, s)
          case e: EndDatasetEvent   => (e.getDatasetLabel, e)
        }.groupBy(_._1).values.map { v =>
          // Bump the start of the interval so that the start time itself isn't
          // included.
          val i = VisitEvents(v.map(_._2)).intervalOption.get
          i.minus(new Interval(i.getStart, i.getStart + 1))
        }
        val union = t.events.total - new Union(intervals.asJavaCollection)

        // Remove any events that fall inside dataset intervals and skip any
        // leading start dataset events since those are eaten by the old impl.
        val events = VisitEvents(t.events.sorted.filter(e => union.contains(e.timestamp)).dropWhile {
          case _: StartDatasetEvent => true
          case _                    => false
        })

        // Of the remaining datasets, we need at least one to be PASS (or
        // undefined or check) because the new algorithm explicitly doesn't
        // charge if they are all failing.
        val charge = events.datasetIntervals.exists { case (lab, _) =>
          t.qa(lab).isChargeable
        }

        // If the new algorithm won't charge at all we can't really compare so
        // just assign noncharged to vtOld.
        val vtOld = if (charge) VisitCalculator.Primordial.calc(events, t.qa, t.oc)
                    else VisitTimes.noncharged(events.total.sum)

        // Now the two methods should produce the same results.
        val vtNew = VisitCalculator.Update2019A.calc(events, t.qa, t.oc)

        vtNew shouldEqual vtOld
      }
    }

    "handle event lists that begin with a start dataset event" in {
      val night      = new ObservingNight(GS, Semester2019A.getStartDate(GS).getTime).getDarkTime(TwilightBoundType.NAUTICAL)
      val startTime  = night.getStartTime
      val label      = new DatasetLabel(ObsId, 1)
      val dataset    = new Dataset(label, "", startTime)
      val startEvent = new StartDatasetEvent(startTime, dataset)
      val endEvent   = new EndDatasetEvent(startTime + 1000, label)
      val events     = Vector(startEvent, endEvent)

      val actual = TimeAccounting.calc(events, const(PASS), const(SCIENCE))

      val expected = new VisitTimes()
      expected.addClassifiedTime(PROGRAM, 1000)

      expected shouldEqual actual
    }

    "charge for the portion of a dataset before an intervening overlap" in {
      val night        = new ObservingNight(GS, Semester2019A.getStartDate(GS).getTime).getDarkTime(TwilightBoundType.NAUTICAL)
      val startTime    = night.getStartTime
      val label        = new DatasetLabel(ObsId, 1)
      val dataset      = new Dataset(label, "", startTime)
      val startEvent   = new StartDatasetEvent(startTime, dataset)
      val overlapEvent = new OverlapEvent(startTime + 1000, ObsId)
      val endEvent     = new EndDatasetEvent(startTime + 2000, label)
      val events       = Vector(startEvent, overlapEvent, endEvent)

      val actual = TimeAccounting.calc(events, const(PASS), const(SCIENCE))

      val expected = new VisitTimes()
      expected.addClassifiedTime(PROGRAM,    1000)
      expected.addClassifiedTime(NONCHARGED, 1000)

      expected shouldEqual actual
    }
  }


}
