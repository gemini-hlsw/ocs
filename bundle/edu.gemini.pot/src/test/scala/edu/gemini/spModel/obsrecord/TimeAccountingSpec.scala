package edu.gemini.spModel.obsrecord

import edu.gemini.pot.sp.Instrument
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

  final case class Test(
    instrument: Option[Instrument],
    obsClass:   ObsClass,
    events:     VisitEvents,
    datasetQa:  Map[DatasetLabel, DatasetQaState],
    datasetOc:  Map[DatasetLabel, ObsClass]
  ) {

    def isAlwaysCharged: Boolean =
      isVisitor || isGpiAcq

    def isVisitor: Boolean =
      instrument.exists(_.isVisitor)

    def isGpiAcq: Boolean =
      instrument.exists(_ == Instrument.Gpi) && (obsClass == ObsClass.ACQ)

    def visitTimes: VisitTimes =
      TimeAccounting.calc(instrument, obsClass, events.sorted, datasetQa, datasetOc)

    def show: String = {
      def format(evt: ObsExecEvent): String = {
        val base = s"${evt.getTimestamp} - ${evt.getClass.getName}"
        evt match {
          case s: StartDatasetEvent =>
            val lab = s.getDataset.getLabel
            s"$base - $lab - ${datasetQa(lab)} - ${datasetOc(lab)}"
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
      inst   <- Gen.option(Gen.oneOf(Instrument.values.toList))
      oclass <- arbitrary[ObsClass]
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

      Test(inst, oclass, VisitEvents((es ::: des).toVector), qaMap, ocMap)
    }

  }

  val GmosSouth     = Some(Instrument.GmosSouth)
  val Semester2019A = new Semester(2019, Semester.Half.A)
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
        // FAIL or USABLE nothing is charged in some cases).
        val v = TimeAccounting.calc(t.instrument, t.obsClass, t.events.sorted, const(PASS), t.datasetOc)

        // If there were no datasets, then we don't charge at all.
        if ((u.sum === 0) && !t.isAlwaysCharged)
          v.getTotalTime shouldEqual v.getClassifiedTime(NONCHARGED)
        else
          v.getUnclassifiedTime shouldEqual (t.events.chargeable - u).sum
      }
    }

    "not charge (except for visitor instruments or GPI acq), if there are no passing datasets" in {
      forAll(genTest(GS19AStart, GS19AEnd)) { (t: Test) =>
        t.datasetQa.values.exists(_.isChargeable) ||
          t.isAlwaysCharged ||
          (t.visitTimes === VisitTimes.noncharged(t.events.total.sum))
      }
    }

    // Checks a Test that has been modified to be a special case.  So even if
    // there are no passing datasets there should be a charge here anyway.
    def specialCaseCharges(t: Test): Boolean = {

      // Of course, we only charge in this case for the time outside of the
      // failed datasets so if there is no time outside of datasets then there
      // is no charge here anyway.

      // All potentially chargeable time (i.e., dark time that isn't
      // overlapped by another observation).
      val chargeable: Union[Interval] = t.events.chargeable

      // All potentially chargeable time that corresponds to datasets
      val datasetChargeable: Union[Interval] =
        new Union(t.events.datasetIntervals.map(_._2).asJava) ∩ chargeable

      // Either:
      (t.visitTimes.getChargedTime > 0)  ||       // there is some charge
        (chargeable.sum == 0)            ||       // there is no chargeable time anyway
        (datasetChargeable.sum == chargeable.sum) // all the chargeable time was for failed datasets

    }

    "charge even if there are no passing datasets for visitor instruments" in {
      forAll(genTest(GS19AStart, GS19AEnd)) { (t: Test) =>

        // Modify the test to use a visitor instrument and make all the
        // datasets be failed.  This should still be charged according to the
        // time accounting rule update in REL-3625.
        specialCaseCharges {
          t.copy(
            instrument = Some(Instrument.Visitor),
            datasetQa = t.datasetQa.mapValues(_ => DatasetQaState.FAIL)
          )
        }
      }
    }

    "charge even if there are no passing datasets for GPI acquisitions" in {
      forAll(genTest(GS19AStart, GS19AEnd)) { (t: Test) =>

        // Modify the test to GPI and make all the datasets be failed.  This
        // should still be charged according to the time accounting rule update
        // in REL-3628.
        specialCaseCharges {
          t.copy(
            instrument = Some(Instrument.Gpi),
            obsClass = ObsClass.ACQ,
            datasetQa = t.datasetQa.mapValues(_ => DatasetQaState.FAIL)
          )
        }
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
          if (i.getLength > 0) new Interval(i.getStart + 1, i.getEnd) else i
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
        val charge = t.isAlwaysCharged ||
          events.datasetIntervals.exists { case (lab, _) => t.datasetQa(lab).isChargeable }

        // If the new algorithm won't charge at all we can't really compare so
        // just assign noncharged to vtOld.
        val vtOld = if (charge) VisitCalculator.Primordial.calc(t.instrument, ObsClass.SCIENCE, events, t.datasetQa, t.datasetOc)
                    else VisitTimes.noncharged(events.total.sum)

        // Now the two methods should produce the same results.
        val vtNew = VisitCalculator.Update2019A.calc(t.instrument, ObsClass.SCIENCE, events, t.datasetQa, t.datasetOc)

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

      val actual = TimeAccounting.calc(GmosSouth, SCIENCE, events, const(PASS), const(SCIENCE))

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

      val actual = TimeAccounting.calc(GmosSouth, SCIENCE, events, const(PASS), const(SCIENCE))

      val expected = new VisitTimes()
      expected.addClassifiedTime(PROGRAM,    1000)
      expected.addClassifiedTime(NONCHARGED, 1000)

      expected shouldEqual actual
    }
  }


}
