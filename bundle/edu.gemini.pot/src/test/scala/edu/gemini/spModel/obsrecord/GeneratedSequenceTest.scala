package edu.gemini.spModel.obsrecord

import edu.gemini.pot.sp.SPObservationID
import edu.gemini.spModel.config2.Config
import edu.gemini.spModel.core.Semester
import edu.gemini.spModel.core.Site.GS
import edu.gemini.spModel.dataset.{ Dataset, DatasetLabel, DatasetQaRecord, DatasetQaState }
import edu.gemini.spModel.event._
import edu.gemini.spModel.obsclass.ObsClass
import edu.gemini.spModel.pio.{ ParamSet, PioFactory }

import org.scalacheck.{ Properties, Gen, Arbitrary }
import org.scalacheck.Arbitrary._
import org.scalacheck.Prop.forAll
import org.scalacheck.Prop._
import org.scalacheck.Gen

import org.specs2.ScalaCheck
import org.specs2.mutable.Specification

import java.time.Instant
import scala.collection.JavaConverters._


object GeneratedSequenceTest extends Specification with ScalaCheck {

  case class DummyConfigStore(f: DatasetLabel => Option[ObsClass]) extends ConfigStore {
    override def toParamSet(factory: PioFactory): ParamSet = null
    override def addConfigAndLabel(config: Config, label: DatasetLabel): Unit = {}
    override def getConfigForDataset(label: DatasetLabel): Config = null
    override def remove(label: DatasetLabel): Unit = {}
    override def containsDataset(label: DatasetLabel): Boolean = true

    // This is the one method we're interested in
    override def getObsClass(label: DatasetLabel): ObsClass =
      f(label).getOrElse(ObsClass.SCIENCE)
  }

  implicit val arbQaState: Arbitrary[DatasetQaState] =
    Arbitrary {
      Gen.oneOf(DatasetQaState.values.toList)
    }

  implicit val arbObsClass: Arbitrary[ObsClass] =
    Arbitrary {
      Gen.oneOf(ObsClass.values.toList)
    }

  val ObsId = new SPObservationID("GS-2018A-Q-1")

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
    events: List[ObsExecEvent],
    qa:     ObsQaRecord,
    store:  ConfigStore
  ) {

    def show: String = {
      def format(evt: ObsExecEvent): String = {
        val base = s"${evt.getTimestamp} - ${evt.getClass.getName}"
        evt match {
          case s: StartDatasetEvent =>
            val lab = s.getDataset.getLabel
            s"$base - $lab - ${qa.qaState(lab)} - ${store.getObsClass(lab)}"
          case e: EndDatasetEvent   =>
            s"$base - ${e.getDatasetLabel}"
          case _                    =>
            base
        }
      }

      events.map(format).mkString("\n")
    }

  }


  def genTest(start: Instant, end: Instant): Gen[Test] = {

    val timeGen = Gen.choose(start.toEpochMilli, start.toEpochMilli + (86399999 * 2))//end.toEpochMilli)

    for {
      dstart <- Gen.choose(0, 100)
      dcount <- Gen.choose(0,   2)
      qas    <- Gen.listOfN(dcount, arbitrary[DatasetQaState])
      ocs    <- Gen.listOfN(dcount, arbitrary[ObsClass])
      dtimes <- Gen.listOfN(dcount * 2, timeGen)
      ecount <- Gen.choose(0,   2)
      es     <- Gen.listOfN(ecount, timeGen.flatMap(genEvent))
    } yield {
      val labs = (dstart until (dstart + dcount)).toList.map(new DatasetLabel(ObsId, _))
      val des  = labs.zip(dtimes.sorted.grouped(2).toList).collect { case (lab, List(s, e)) =>
        val d = new Dataset(lab, lab.toString, s)
        List(new StartDatasetEvent(s, d), new EndDatasetEvent(e, lab))
      }.flatten

      val qaMap = labs.zip(qas).map { case (lab, qa) =>
        (lab, new DatasetQaRecord(lab, qa, ""))
      }.toMap

      val ocMap = labs.zip(ocs).toMap

      // A bug in the original time accounting code will not categorize the
      // first dataset if the first event is a start dataset event.  Since that
      // shouldn't have ever happened, I choose not to try to reproduce it in
      // the new algorithm but we do have to skip these cases in randomly
      // generated sequences if we want to compare results from the two
      // implementations.
      val evts = (es ::: des).sortBy(_.getTimestamp).dropWhile {
        case _: StartDatasetEvent => true
        case _                    => false
      }

      Test(evts, new ObsQaRecord(qaMap), DummyConfigStore(ocMap.get))
    }

  }

  val Semester2017B = new Semester(2017, Semester.Half.B)
  val GS17BStart  = Instant.ofEpochMilli(Semester2017B.getStartDate(GS).getTime)
  val GS17BEnd    = Instant.ofEpochMilli(Semester2017B.getEndDate(GS).getTime)

  "TimeAccounting" should {
    "match old results for old dates" in {
      forAll(genTest(GS17BStart, GS17BEnd)) { (t: Test) =>
        val events = t.events.asJava
        val vtOld  = OldTimeAccounting.getTimeCharges(events, t.qa, t.store)
        val vtNew  = TimeAccounting.calcAsJava(events, t.qa, t.store)

        if (vtOld != vtNew) println(t.show)

        vtOld shouldEqual vtNew
      }
    }
  }


}
