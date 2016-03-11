package edu.gemini.spModel.io.impl.migration.to2016B

import edu.gemini.pot.sp.{SPComponentType, ISPProgram}
import edu.gemini.spModel.core._
import edu.gemini.spModel.io.impl.migration.MigrationTest
import edu.gemini.spModel.obs.{SchedulingBlock, SPObservation}
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
        spt <- x.dataObject.map(_.asInstanceOf[TargetObsComp].getBase)
      } yield spt.getNewTarget <| println

  }

  "2016B Target Migration" should {

    "Migrate Sidereal Targets" in withTestProgram2("targetMigration.xml") { p =>
      import MagnitudeBand._, MagnitudeSystem._
      p.findBaseByObsTitle("sidereal") must_== Some(
        SiderealTarget(
          "barnard's star",
          Coordinates.fromDegrees(269.4520749999999, 4.693391666666685).get,
          Some(ProperMotion(
            RightAscensionAngularVelocity(AngularVelocity(-798.58)),
            DeclinationAngularVelocity(AngularVelocity(10328.12)),Epoch(2000.0))
          ),
          Some(Redshift(-3.69E-4)),
          Some(Parallax(548.31)),
          List(
            Magnitude(12.497,U,None,Vega),
            Magnitude(11.24,B,None,Vega),
            Magnitude(9.511,V,None,Vega),
            Magnitude(8.298,R,None,Vega),
            Magnitude(6.741,I,None,Vega),
            Magnitude(5.244,J,None,Vega),
            Magnitude(4.83,H,None,Vega),
            Magnitude(4.524,K,None,Vega)),
          None,
          None
        )
      )
    }

    "Migrate Named Targets" in withTestProgram2("targetMigration.xml") { p =>
      p.findBaseByObsTitle("named") must beSome
      failure("not implemented")
    }

    "Migrate JPL Minor Bodies" in withTestProgram2("targetMigration.xml") { p =>
      p.findBaseByObsTitle("jpl-minor-body") must beSome
      failure("not implemented")
    }

    "Migrate MPC Minor Planets" in withTestProgram2("targetMigration.xml") { p =>
      p.findBaseByObsTitle("mpc-minor-planet") must beSome
      failure("not implemented")
    }

    "Migrate TOO Targets" in withTestProgram2("targetMigration.xml") { p =>
      p.findBaseByObsTitle("too") must_== Some(TooTarget("bob"))
    }

  }

}
