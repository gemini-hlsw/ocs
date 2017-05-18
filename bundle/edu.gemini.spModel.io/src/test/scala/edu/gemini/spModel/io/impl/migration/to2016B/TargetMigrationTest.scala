package edu.gemini.spModel.io.impl.migration.to2016B

import edu.gemini.pot.sp.{ISPObservation, ISPNode, SPComponentType, ISPProgram}
import edu.gemini.spModel.core.HorizonsDesignation.MajorBody
import edu.gemini.spModel.core._
import edu.gemini.spModel.io.impl.migration.MigrationTest
import edu.gemini.spModel.obs.{SchedulingBlock, SPObservation}
import edu.gemini.spModel.obscomp.{SPNote, ProgramNote}
import edu.gemini.spModel.target.SPTarget
import edu.gemini.spModel.target.obsComp.TargetObsComp
import org.specs2.mutable.Specification
import edu.gemini.spModel.rich.pot.sp._
import edu.gemini.shared.util.immutable.ScalaConverters._
import scalaz._, Scalaz._

class TargetMigrationTest extends Specification with MigrationTest {

  implicit class MoreProgramOps(p: ISPProgram) {

    def findBaseByObsTitle(title: String): Option[Target] =
      for {
        isp <- p.allObservations.find(_.title == title)
        x   <- isp.findObsComponentByType(SPComponentType.TELESCOPE_TARGETENV)
        spt <- x.dataObject.map(_.asInstanceOf[TargetObsComp].getAsterism.allSpTargets.head)
      } yield spt.getTarget // <| println

  }

  "2016B Target Migration" should {

    "Migrate TOO Target" in withTestProgram2("targetMigrationToo.xml") { p =>
      p.findBaseByObsTitle("origin") must_== Some(
        TooTarget("zero")
      )
    }
    
    "Migrate Sidereal Target at the Origin" in withTestProgram2("targetMigration.xml") { p =>
      p.findBaseByObsTitle("origin") must_== Some(
        SiderealTarget(
          "zero",
          Coordinates.zero,
          None,
          Some(Redshift(0.0)),
          Some(Parallax(0.0)),
          List(),
          None,
          None)
      )
    }

    "Migrate Sidereal Targets" in withTestProgram2("targetMigration.xml") { p =>
      import MagnitudeBand._, MagnitudeSystem._
      p.findBaseByObsTitle("sidereal") must_== Some(
        SiderealTarget(
          "barnard's star",
          Coordinates.fromDegrees(269.4520749999999, 4.693391666666685).get,
          Some(ProperMotion(
            RightAscensionAngularVelocity(AngularVelocity(-798.58)),
            DeclinationAngularVelocity(AngularVelocity(10328.12)), Epoch.J2000)
          ),
          Some(Redshift(-3.69E-4)),
          Some(Parallax(548.31)),
          List(
            Magnitude( 4.524, K, None, Vega),
            Magnitude( 4.83,  H, None, Vega),
            Magnitude( 5.244, J, None, Vega),
            Magnitude( 6.741, I, None, Vega),
            Magnitude( 8.298, R, None, Vega),
            Magnitude( 9.511, V, None, Vega),
            Magnitude(11.24,  B, None, Vega),
            Magnitude(12.497, U, None, Vega)),
          None,
          None
        )
      )
    }

    "Migrate Named Targets" in withTestProgram2("targetMigration.xml") { p =>
      p.findBaseByObsTitle("named") must_== Some(
        NonSiderealTarget(
          "Mars",
          Ephemeris(Site.GN, IMap(1457994429000L -> Coordinates.fromDegrees(240.99149999999997, 340.4314722222223).get)),
          Some(MajorBody(499)),
          List(),
          None,
          None)
      )
    }

    "Migrate JPL Minor Bodies" in withTestProgram2("targetMigration.xml") { p =>
      p.findBaseByObsTitle("jpl-minor-body") must_== Some(
        NonSiderealTarget(
          "halley",
          Ephemeris(Site.GN, IMap(1457994460000L -> Coordinates.fromDegrees(125.49337500000001, 1.8927499999999782).get)),
          None,
          List(),
          None,
          None)
      )
    }

    "Migrate MPC Minor Planets" in withTestProgram2("targetMigration.xml") { p =>
      p.findBaseByObsTitle("mpc-minor-planet") must_== Some(NonSiderealTarget(
        "beer",
        Ephemeris(Site.GN, IMap(1457994485000L -> Coordinates.fromDegrees(88.769, 21.08299999999997).get)),
        None,
        List(),
        None,
        None)
      )
    }

    "Add a migration note for MPC Minor Planet 'beer'" in withTestProgram2("targetMigration.xml") { p =>
      findNoteTextByPath(p, "mpc-minor-planet", "Migration: beer").exists(_ contains "W: 180.0542620681246")
    }

    "Add a migration note for JPL Major Body 'Halley'" in withTestProgram2("targetMigration.xml") { p =>
      findNoteTextByPath(p, "jpl-minor-body", "Migration: halley").exists(_ contains "EC: 0.9671429084623044")
    }

  }

  def findNoteTextByPath(n: ISPNode, title: String, more: String*): Option[String] =
    findByPath(n, title, more: _*).map(_.getDataObject).collect {
      case n: SPNote => n.getNote
    }

  def findByPath(n: ISPNode, title: String, more: String*): Option[ISPNode] =
    findByName(n, title).flatMap { node =>
      more.toList match {
        case Nil    => Some(node)
        case t :: m => findByPath(node, t, m: _*)
      }
    }

  def findByName(n: ISPNode, title: String): Option[ISPNode] =
    n.findDescendant(_.getDataObject.getTitle == title)

}
